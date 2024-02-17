package dev.peopo.chunkrestore.recordservice.persistent

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import org.bukkit.block.data.BlockData


internal interface DatabaseAccessor : AutoCloseable{
    // init and ctrl
    fun createConnectionPool(): Boolean
    fun createDirectories()

    // persistent changes
    fun fetchRecords(world: String, chunkX: Int, chunkZ: Int): Map<BlockCoordinate, BlockData>
    fun pushRecords(world: String, chunkX: Int, chunkZ: Int, changes: Map<BlockCoordinate, BlockData>) : Boolean

    fun fetchRecords(chunkCoordinate: ChunkCoordinate) = fetchRecords(chunkCoordinate.world, chunkCoordinate.x, chunkCoordinate.z)
    fun pushRecords(chunkCoordinate: ChunkCoordinate, changes: Map<BlockCoordinate, BlockData>) = pushRecords(chunkCoordinate.world, chunkCoordinate.x, chunkCoordinate.z, changes)

    fun pushTimestamp(world: String, chunkX: Int, chunkZ: Int, lastUpdate: Long): Boolean
    fun fetchAllTimestamps(): Map<ChunkCoordinate, Long>
}