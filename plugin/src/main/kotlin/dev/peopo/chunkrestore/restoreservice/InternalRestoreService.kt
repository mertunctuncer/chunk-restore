package dev.peopo.chunkrestore.restoreservice

import dev.peopo.chunkrestore.recordservice.InternalRecordService
import dev.peopo.chunkrestore.recordservice.cache.RestoreTimestampHolder
import dev.peopo.chunkrestore.restoreservice.oregenerator.OreGenerator
import dev.peopo.chunkrestore.restoreservice.oregenerator.cleaner.DynamicOreRemover
import dev.peopo.chunkrestore.restoreservice.oregenerator.cleaner.StaticOreRemover
import dev.peopo.chunkrestore.restoreservice.updatescheduler.ChunkUpdateTask
import dev.peopo.chunkrestore.restoreservice.updatescheduler.DynamicUpdateScheduler
import dev.peopo.chunkrestore.restoreservice.updatescheduler.InstantUpdateScheduler
import dev.peopo.chunkrestore.util.*
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin


internal class InternalRestoreService(
    override val plugin: Plugin,
    private val cooldownCache: RestoreTimestampHolder
) : RestoreService, PluginOwner {

    init {
        logInfo("Initializing restore service...")
    }

    private lateinit var recordService: InternalRecordService

    internal fun initialize(internalRecordService: InternalRecordService) {
        this.recordService = internalRecordService
    }

    private val enabled by config {
        it.getBoolean("enabled").also { enabled ->
            if(enabled) logInfo("Chunk restoration enabled.")
            else logInfo("Chunk restoration disabled.")
        }
    }

    private val materialBlacklist by config {
        it.getStringList("settings.material-blacklist").mapNotNull { materialString ->
            try {
                Material.valueOf(materialString.uppercase())
            } catch (e: IllegalArgumentException) {
                logInfo("Invalid blacklisted material: $materialString")
                null
            }
        }.toSet().also { blacklist ->
            logInfo("Material blacklist set to $blacklist")
        }
    }

    private val oreScrambleEnabled by config {
        it.getBoolean("settings.ore-generator.enabled").also { oreScramble ->
            if(oreScramble) logInfo("Ore scrambling enabled.")
            else logInfo("Ore scrambling disabled.")
        }
    }

    private val oreRemover by config {
        it.getString("settings.ore-generator.remove-strategy")?.uppercase()?.let { strategyString ->
            return@config when (strategyString) {
                "STATIC" -> StaticOreRemover(plugin)
                "DYNAMIC" -> DynamicOreRemover(plugin)
                else -> StaticOreRemover(plugin)
            }
        } ?: StaticOreRemover(plugin)
    }

    private val oreGenerator = OreGenerator(plugin)

    private val updateScheduler by config {
        val limit = it.getLong("settings.restore-limit")
        if(limit == 0L) return@config InstantUpdateScheduler(plugin)
        else return@config DynamicUpdateScheduler(plugin)
    }

    override fun scheduleRestore(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>) {
        if(!enabled) {
            logDebug("Skipping restore for ${chunkCoordinate.world}:${chunkCoordinate.x},${chunkCoordinate.z}.")
            return
        }

        val world = Bukkit.getWorld(chunkCoordinate.world) ?: run {
            logError("A restoration was requested in a non-existing world: ${chunkCoordinate.world}:${chunkCoordinate.x},${chunkCoordinate.z}")
            return
        }

        logDebug("Scheduling restore for ${chunkCoordinate.world}:${chunkCoordinate.x},${chunkCoordinate.z}")

        val chunk = world.getChunkAt(chunkCoordinate.x, chunkCoordinate.z)

        runTask(chunk) {
            val chunkSnapshot = world.getChunkAt(chunkCoordinate.x, chunkCoordinate.z).chunkSnapshot

            cooldownCache[chunkCoordinate] = System.currentTimeMillis()

            runAsync {
                val resultMap = changeMap.toMutableMap()

                // Ores
                if(oreScrambleEnabled) {
                    // Remove ores
                    val removerChanges = oreRemover.calculateChanges(chunkSnapshot, changeMap)
                    resultMap.putAll(removerChanges)

                    // Generate changes
                    val oreChanges = oreGenerator.calculateOres(chunkSnapshot, resultMap)
                    resultMap.putAll(oreChanges)

                    logDebug("Calculated ${removerChanges.size} ore block removals and ${oreChanges.size} ore block additions.")
                }

                // Record using unfiltered changes
                recordService.processRestore(chunkCoordinate, resultMap.keys)

                val filteredChanges = resultMap.filterNot { it.value.material in materialBlacklist }

                // Schedule update
                val updateTask = ChunkUpdateTask(chunk, filteredChanges)
                updateScheduler.scheduleTask(updateTask)
            }
        }
    }
}

