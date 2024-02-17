package dev.peopo.chunkrestore.recordservice.persistent.mongo.data

internal data class TimestampData(
    val world: String,
    val x: Int,
    val z: Int,
    val lastUpdate: Long
)
