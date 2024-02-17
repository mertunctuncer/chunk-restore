package dev.peopo.chunkrestore.restoreservice.updatescheduler

import dev.peopo.chunkrestore.util.BlockCoordinate
import org.bukkit.Chunk
import org.bukkit.block.data.BlockData

public data class ChunkUpdateTask(
    public val chunk: Chunk,
    public val changes: Map<BlockCoordinate, BlockData>,
)