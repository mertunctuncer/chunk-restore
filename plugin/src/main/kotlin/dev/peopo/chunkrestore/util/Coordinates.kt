package dev.peopo.chunkrestore.util

import org.bukkit.Chunk
import org.bukkit.block.Block


public data class BlockCoordinate(
    val x: Int,
    val y: Int,
    val z: Int
)

internal val Block.coordinates: BlockCoordinate
    get() = BlockCoordinate(this.x, this.y, this.z)

internal val Block.chunkCoordinates: ChunkCoordinate
    get() = ChunkCoordinate(this.chunk.world.name, this.chunk.x, this.chunk.z)

public data class ChunkCoordinate(
    val world: String,
    val x: Int,
    val z: Int
)

internal val Chunk.coordinates: ChunkCoordinate
    get () = ChunkCoordinate(this.world.name, this.x, this.z)

internal fun Int.toChunkCoordinates(): Int = this shr 4

internal fun Int.toBlockCoordinates(): Int = this shl 4

internal fun Int.toChunkRelative(): Int = this and 15

internal fun asBlockCoordinate(chunkX: Int, chunkZ: Int, chunkRelativeX: Int, chunkRelativeY: Int, chunkRelativeZ: Int): BlockCoordinate {
    return BlockCoordinate(chunkX.toBlockCoordinates() + chunkRelativeX, chunkRelativeY, chunkZ.toBlockCoordinates() + chunkRelativeZ)
}