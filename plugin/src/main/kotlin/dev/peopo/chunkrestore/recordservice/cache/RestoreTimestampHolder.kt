package dev.peopo.chunkrestore.recordservice.cache

import dev.peopo.chunkrestore.recordservice.persistent.DatabaseAccessor
import dev.peopo.chunkrestore.util.ChunkCoordinate
import dev.peopo.chunkrestore.util.PluginOwner
import dev.peopo.chunkrestore.util.runAsync
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap

internal class RestoreTimestampHolder (
    override val plugin: Plugin,
    private val databaseAccessor: DatabaseAccessor
) : ConcurrentHashMap<ChunkCoordinate, Long>(), PluginOwner {

    init {
        runAsync { super.putAll(databaseAccessor.fetchAllTimestamps()) }
    }

    override fun put(key: ChunkCoordinate, value: Long): Long? {
        val result = super.put(key, value)
        runAsync { databaseAccessor.pushTimestamp(key.world, key.x, key.z, value) }
        return result
    }
}