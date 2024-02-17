package dev.peopo.chunkrestore.restoreservice.oregenerator.cleaner

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.logInfo
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class StaticOreRemover(
    plugin: Plugin
) : OreRemover(plugin) {

    init {
        logInfo("Ores will be replaced with $defaultMaterial.")
    }

    override fun getMaterial(
        chunkSnapshot: ChunkSnapshot,
        changes: Map<BlockCoordinate, BlockData>,
        x: Int,
        y: Int,
        z: Int,
        minHeight: Int,
        maxHeight: Int
    ): Material {
        return defaultMaterial
    }
}