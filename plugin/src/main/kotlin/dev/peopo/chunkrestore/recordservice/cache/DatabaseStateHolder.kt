package dev.peopo.chunkrestore.recordservice.cache

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import org.bukkit.block.data.BlockData
import java.util.concurrent.ConcurrentHashMap

internal class DatabaseStateHolder {


    private val cache = ConcurrentHashMap<ChunkCoordinate, Map<BlockCoordinate, BlockData>?>()

    // Null means that the chunk is not fetched.
    fun getState(chunkCoordinate: ChunkCoordinate) = cache[chunkCoordinate]?.toMutableMap()

    // Cache the state
    fun cacheState(chunkCoordinate: ChunkCoordinate, changes: Map<BlockCoordinate, BlockData>) {
        cache[chunkCoordinate] = changes
    }

    // Stop caching.
    fun removeState(chunkCoordinate: ChunkCoordinate) {
        cache.remove(chunkCoordinate)
    }

    // Check if cached
    fun isCached(chunkCoordinate: ChunkCoordinate) = cache.containsKey(chunkCoordinate)

}