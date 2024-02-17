package dev.peopo.chunkrestore.recordservice.persistent.mongo.data

internal data class BlockRecord(
    val x: Int,
    val y: Int,
    val z: Int,
    val serializedData: String
)

internal data class ChunkRecord(
    val world: String,
    val chunkX: Int,
    val chunkZ: Int,
    val changes: Set<BlockRecord>
)

internal data class ChunkResult(
    val changes: Set<BlockRecord>
)
