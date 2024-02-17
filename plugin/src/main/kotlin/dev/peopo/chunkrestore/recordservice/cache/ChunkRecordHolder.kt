package dev.peopo.chunkrestore.recordservice.cache


import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.ChunkCoordinate
import java.util.concurrent.ConcurrentHashMap

internal class ChunkRecordHolder {

    private val cache = ConcurrentHashMap<ChunkCoordinate, ConcurrentHashMap<BlockCoordinate, Record>>()

    // Get changes
    fun getRecords(chunkCoordinate: ChunkCoordinate) : Map<BlockCoordinate, Record> {
        return cache[chunkCoordinate]?.toMap() ?: emptyMap()
    }

    // Record changes
    fun addBlockRecord(chunkCoordinate: ChunkCoordinate, blockCoordinate: BlockCoordinate, record: ChangeRecord) {
        val chunkData = cache.getOrPut(chunkCoordinate) { ConcurrentHashMap() }

        val currentRecord = chunkData[blockCoordinate]

        if(currentRecord is ChangeRecord) return // Keep original state

        // Server state is changed, original state is held in cache
        chunkData[blockCoordinate] = record
    }

    // Record restoration
    fun addRestoreRecord(chunkCoordinate: ChunkCoordinate, blockCoordinate: BlockCoordinate) {
        val chunkData = cache.getOrPut(chunkCoordinate) { ConcurrentHashMap() }
        // Server state of the block is the original state, but database state might not reflect it.
        chunkData[blockCoordinate] = RestoreRecord
    }

    fun getAllRecords() = cache.toMap()

    // Remove cache
    fun remove(chunkCoordinate: ChunkCoordinate) {
        cache.remove(chunkCoordinate)
    }
}