package dev.peopo.chunkrestore.inspectorservice.inspector

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import dev.peopo.chunkrestore.util.PluginOwner
import dev.peopo.chunkrestore.util.logInfo
import me.angeschossen.lands.api.LandsIntegration
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class LandsInspector(
    override val plugin: Plugin,
    private val landsAPI: LandsIntegration
) : ChunkInspector, PluginOwner {

    init {
        logInfo("Registering Lands inspector...")
    }

    override fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>): Boolean {
        val world = Bukkit.getWorld(chunkCoordinate.world)?: return false

        return landsAPI.getLandByChunk(world, chunkCoordinate.x, chunkCoordinate.z) == null
    }
}