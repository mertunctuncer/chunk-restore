package dev.peopo.chunkrestore.inspectorservice

import dev.peopo.chunkrestore.inspectorservice.inspector.ChunkInspector
import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import org.bukkit.block.data.BlockData

public interface InspectorService {
    public fun inspect(chunkCoordinate: ChunkCoordinate, changeMap: Map<BlockCoordinate, BlockData>)
    public fun registerInspector(inspector: ChunkInspector)
}