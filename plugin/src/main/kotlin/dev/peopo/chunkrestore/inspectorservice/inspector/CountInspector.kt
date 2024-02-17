package dev.peopo.chunkrestore.inspectorservice.inspector

import dev.peopo.chunkrestore.util.*
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class CountInspector(override val plugin: Plugin) : ChunkInspector, PluginOwner {


    init {
        logInfo("Registering block change threshold inspector...")
    }

    private val threshold by config {
        it.getLong("settings.restore-threshold").toInt().also { threshold -> logInfo("Restore threshold set to $threshold blocks.")  }
    }

    override fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>) : Boolean {
        return changeMap.size > threshold
    }
}