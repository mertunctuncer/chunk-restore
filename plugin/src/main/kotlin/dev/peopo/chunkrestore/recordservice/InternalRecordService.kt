package dev.peopo.chunkrestore.recordservice

import dev.peopo.chunkrestore.inspectorservice.InspectorService
import dev.peopo.chunkrestore.recordservice.cache.*
import dev.peopo.chunkrestore.recordservice.listener.BlockChangeListener
import dev.peopo.chunkrestore.recordservice.listener.ChunkListener
import dev.peopo.chunkrestore.recordservice.persistent.DatabaseAccessor
import dev.peopo.chunkrestore.recordservice.timeout.TimeoutNotifier
import dev.peopo.chunkrestore.util.*
import org.bukkit.block.data.BlockData
import org.bukkit.event.HandlerList
import org.bukkit.plugin.Plugin

internal class InternalRecordService(
    override val plugin: Plugin,
    private val databaseAccessor: DatabaseAccessor,
    private val inspectorService: InspectorService
) : RecordService, PluginOwner, AutoCloseable {

    private val chunkChangeCache = ChunkRecordHolder()
    private val databaseStateCache = DatabaseStateHolder()

    private val timeoutNotifier: TimeoutNotifier = TimeoutNotifier(this, plugin)

    internal fun initialize() {
        logInfo("Initializing record service...")
        timeoutNotifier.initialize()
        registerListeners()
    }

    override fun recordChange(
        chunkCoordinate: ChunkCoordinate,
        blockCoordinate: BlockCoordinate,
        blockData: BlockData
    ) {
        processBlockChange(chunkCoordinate, blockCoordinate, blockData)
    }

    override fun getChanges(chunkCoordinate: ChunkCoordinate) : Map<BlockCoordinate, BlockData> {
        val changeMap = chunkChangeCache.getRecords(chunkCoordinate)
        val databaseState = databaseStateCache.getState(chunkCoordinate) ?: run {
            logError("Database state not found for $chunkCoordinate while processing cache expiration!")
            return emptyMap()
        }

        changeMap.forEach { (blockPosition, record) ->
            // If restore, remove from logs
            if(record is RestoreRecord) {
                databaseState.remove(blockPosition)
                // If not, it is a change. Log it if not logged
            } else if (!databaseState.containsKey(blockPosition)) {
                databaseState[blockPosition] = (record as ChangeRecord).blockData
            }
        }

        return databaseState
    }

    // New block added
    internal fun processBlockChange(chunkCoordinate: ChunkCoordinate, blockCoordinate: BlockCoordinate, blockData: BlockData) {
        chunkChangeCache.addBlockRecord(chunkCoordinate, blockCoordinate, ChangeRecord(blockData))
    }

    // Blocks restored
    internal fun processRestore(chunkCoordinate: ChunkCoordinate, restorationPositions: Set<BlockCoordinate>) {

        // TODO HOOK THIS UP
        restorationPositions.forEach { blockPosition ->
            chunkChangeCache.addRestoreRecord(chunkCoordinate, blockPosition)
        }
    }

    // Chunk load
    internal fun processChunkLoad(chunkCoordinate: ChunkCoordinate) {
        // If the chunk is cached, disable timeout
        if(databaseStateCache.isCached(chunkCoordinate)) {
            timeoutNotifier.removeExpire(chunkCoordinate)
            // Notify inspection service
            inspectorService.inspect(chunkCoordinate, getChanges(chunkCoordinate))
        } else {
            runAsync {
                // Otherwise fetch
                val chunkRecord = databaseAccessor.fetchRecords(chunkCoordinate)
                databaseStateCache.cacheState(chunkCoordinate, chunkRecord)
                // Notify inspection service
                inspectorService.inspect(chunkCoordinate, getChanges(chunkCoordinate))
            }
        }
    }

    // Chunk unload, mark the chunk for timeout
    internal fun processChunkUnload(chunkCoordinate: ChunkCoordinate) {
        timeoutNotifier.markExpire(chunkCoordinate)
    }

    internal fun processExpire(chunkCoordinate: ChunkCoordinate) {
        val databaseState = databaseStateCache.getState(chunkCoordinate) ?: run {
            logError("Database state not found for $chunkCoordinate while processing cache expiration! Extending timeout!")
            // extend timeout in case database is lagging behind somehow.
            timeoutNotifier.markExpire(chunkCoordinate)
            return
        }
        // Get changes that have not been pushed yet
        val changeRecords = chunkChangeCache.getRecords(chunkCoordinate)

        // Remove cache
        databaseStateCache.removeState(chunkCoordinate)
        chunkChangeCache.remove(chunkCoordinate)

        // Push if different
        runAsync {
            pushChanges(chunkCoordinate, databaseState, changeRecords)
        }
    }

    private fun pushChanges(chunkCoordinate: ChunkCoordinate, databaseState: MutableMap<BlockCoordinate, BlockData>, changeMap: Map<BlockCoordinate, Record>) {
        // No changes, no need to push
        if(changeMap.isEmpty()) return

        var changed = false

        changeMap.forEach { (blockPosition, record) ->

            // If block was last restored, clear the state
            if (databaseState.containsKey(blockPosition) && (record is RestoreRecord)) {
                databaseState.remove(blockPosition)
                changed = true
                // If block was changed AND was not pushed already
            } else if (!databaseState.containsKey(blockPosition) && (record is ChangeRecord)) {
                databaseState[blockPosition] = record.blockData
                changed = true
            }
        }
        // Push if different
        if (changed) {
            databaseAccessor.pushRecords(chunkCoordinate, databaseState)
        }
    }

    private val chunkListener = ChunkListener(this)
    private val blockListener = BlockChangeListener(this)

    private fun registerListeners() {
        logInfo("Registering logging listeners...")
        plugin.server.pluginManager.registerEvents(chunkListener, plugin)
        plugin.server.pluginManager.registerEvents(blockListener, plugin)
    }

    override fun close() {
        logInfo("Closing record service...")
        logInfo("Unregistering logging listeners...")
        HandlerList.unregisterAll(chunkListener)
        HandlerList.unregisterAll(blockListener)
        timeoutNotifier.close()

        logInfo("Saving caches...")
        runAsync {
            chunkChangeCache.getAllRecords().forEach { (chunkPosition, recordMap) ->
                if(recordMap.isEmpty()) return@forEach

                val databaseState = databaseStateCache.getState(chunkPosition) ?: run {
                    logError("Database state not found for $chunkPosition while pushing changes!")
                    // This should not happen, but in case it does push the changes and ignore database state.
                    // Possibly the database is lagging behind, will result in missing changes either case.
                    val changeMap = mutableMapOf<BlockCoordinate, BlockData>()
                    recordMap.forEach {(blockPosition, record) ->
                        if(record is ChangeRecord) changeMap[blockPosition] = record.blockData
                    }
                    databaseAccessor.pushRecords(chunkPosition, changeMap)
                    return@forEach
                }

                pushChanges(chunkPosition, databaseState, recordMap)
            }

            logInfo("Record service closed.")
            databaseAccessor.close()
        }
    }
}