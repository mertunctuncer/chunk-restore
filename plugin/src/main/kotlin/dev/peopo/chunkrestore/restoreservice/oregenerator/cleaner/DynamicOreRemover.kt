package dev.peopo.chunkrestore.restoreservice.oregenerator.cleaner

import dev.peopo.chunkrestore.restoreservice.oregenerator.getOverriddenMaterial
import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.logInfo
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class DynamicOreRemover(
    plugin: Plugin
) : OreRemover(plugin) {

    init {
        logInfo("Ores will be replaced dynamically. If material can not be determined, it will be replaced with $defaultMaterial.")
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
        val contextCount = mutableMapOf<Material, Int>()

        for(x1 in x - 1..x + 1) {
            if(x1 < 0 || x1 > 15) continue
            for(y1 in y - 1..y + 1) {
                if(y1 < minHeight || y1 > maxHeight) continue
                for (z1 in z - 1..z + 1) {
                    if(z1 < 0 || z1 > 15) continue
                    contextCount[getOverriddenMaterial(chunkSnapshot, changes, x1, y1, z1)] = contextCount.getOrDefault(
                        getOverriddenMaterial(chunkSnapshot, changes, x1, y1, z1), 0) + 1
                }
            }
        }

        return contextCount.filterNot { oreMaterials.contains(it.key) }.maxBy { it.value }.key
    }

}