package dev.peopo.chunkrestore.inspectorservice

import dev.peopo.chunkrestore.inspectorservice.inspector.ChunkInspector
import dev.peopo.chunkrestore.restoreservice.InternalRestoreService
import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import dev.peopo.chunkrestore.util.PluginOwner
import dev.peopo.chunkrestore.util.logInfo
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class InternalInspectorService(
    override val plugin: Plugin,
    private val restoreService: InternalRestoreService
    ) : InspectorService, PluginOwner {

    init {
        logInfo("Initializing inspector service...")
    }

    private val inspectors = mutableListOf<ChunkInspector>()


    override fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>) {
        val result = inspectors.all { it.inspect(chunkCoordinate, changeMap) }
        if (!result) return

        restoreService.scheduleRestore(chunkCoordinate, changeMap)
    }

    override fun registerInspector(inspector: ChunkInspector) {
        inspectors.add(inspector)
    }
}