package dev.peopo.chunkrestore.inspectorservice.inspector

import dev.peopo.chunkrestore.recordservice.cache.RestoreTimestampHolder
import dev.peopo.chunkrestore.util.*
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class CooldownInspector(
    override val plugin: Plugin,
    private val restoreTimestampHolder: RestoreTimestampHolder,
) : ChunkInspector, PluginOwner{

    init {
        logInfo("Registering chunk cooldown inspector...")
    }

    private val cooldown by config {
        it.getLong("settings.restore-cooldown").also {
            cooldownMinutes -> logInfo("Restore cooldown set to $cooldownMinutes minutes.")
        } * 60_000L
    }
    override fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>): Boolean {
        val lastRestore = restoreTimestampHolder[chunkCoordinate] ?: return true
        return System.currentTimeMillis() > lastRestore + cooldown
    }
}