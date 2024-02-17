package dev.peopo.chunkrestore.inspectorservice.inspector

import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldguard.WorldGuard
import dev.peopo.chunkrestore.util.*
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class WorldGuardInspector(
    override val plugin: Plugin,
    private val worldGuardAPI: WorldGuard,
) : ChunkInspector, PluginOwner {

    init {
        logInfo("Registering WorldGuard inspector...")
    }

    private val regionBlacklist by config {
        it.getStringList("settings.worldguard-blacklist").toSet().also { regionBlacklist ->
            logInfo("WorldGuard blacklist set to $regionBlacklist.")
        }
    }

    override fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>): Boolean {
        val bukkitWorld = Bukkit.getWorld(chunkCoordinate.world) ?: return false
        val worldGuardWorld = BukkitAdapter.adapt(bukkitWorld)

        val regionContainer = worldGuardAPI.platform.regionContainer

        val blacklistedRegions = regionBlacklist.mapNotNull { regionContainer.get(worldGuardWorld)?.getRegion(it) }


        changeMap.forEach {(blockCoordinate, _) ->
            blacklistedRegions.forEach {
                if(it.contains(blockCoordinate.x, blockCoordinate.y, blockCoordinate.z)) return false
            }
        }

        return true
    }
}