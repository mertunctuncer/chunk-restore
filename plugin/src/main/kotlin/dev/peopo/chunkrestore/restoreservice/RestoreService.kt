package dev.peopo.chunkrestore.restoreservice

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import org.bukkit.block.data.BlockData

public interface RestoreService {

    public fun scheduleRestore(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>)
}