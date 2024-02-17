package dev.peopo.chunkrestore.restoreservice.oregenerator

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.asBlockCoordinate
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.block.data.BlockData


internal fun getOverriddenMaterial(
    chunkSnapshot: ChunkSnapshot,
    changeMap: Map<BlockCoordinate, BlockData>,
    chunkRelativeX: Int, chunkRelativeY: Int, chunkRelativeZ: Int
): Material {
    val blockCoordinate = asBlockCoordinate(chunkSnapshot.x, chunkSnapshot.z, chunkRelativeX, chunkRelativeY, chunkRelativeZ)
    return changeMap[blockCoordinate]?.material ?: chunkSnapshot.getBlockType(chunkRelativeX, chunkRelativeY, chunkRelativeZ)
}