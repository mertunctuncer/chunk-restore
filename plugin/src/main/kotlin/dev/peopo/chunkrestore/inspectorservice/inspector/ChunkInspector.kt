package dev.peopo.chunkrestore.inspectorservice.inspector

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import org.bukkit.block.data.BlockData

public interface ChunkInspector {

    public fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>) : Boolean
}