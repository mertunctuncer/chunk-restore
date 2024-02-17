package dev.peopo.chunkrestore.recordservice

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate

import org.bukkit.block.data.BlockData

public interface RecordService {
    public fun recordChange(chunkCoordinate: ChunkCoordinate, blockCoordinate: BlockCoordinate, blockData: BlockData)
    public fun getChanges(chunkCoordinate: ChunkCoordinate) : Map<BlockCoordinate, BlockData>
}