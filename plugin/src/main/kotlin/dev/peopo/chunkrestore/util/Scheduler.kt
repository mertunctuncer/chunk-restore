package dev.peopo.chunkrestore.util

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.Bukkit
import org.bukkit.Chunk


internal fun PluginOwner.runAsync(task: (ScheduledTask) -> Unit) {
    plugin.server.asyncScheduler.runNow(plugin, task)
}

internal fun PluginOwner.runTask(chunk: Chunk, task: (ScheduledTask) -> Unit) {
    plugin.server.regionScheduler.run(plugin, chunk.world, chunk.x, chunk.z, task)
}

internal fun PluginOwner.runTask(chunkCoordinate: ChunkCoordinate, task: (ScheduledTask) -> Unit) {
    plugin.server.regionScheduler.run(plugin, Bukkit.getWorld(chunkCoordinate.world)?: return, chunkCoordinate.x, chunkCoordinate.z, task)
}

internal fun PluginOwner.runTimer(delay: Long = 1L, interval: Long, task: (ScheduledTask) -> Unit) {
    plugin.server.globalRegionScheduler.runAtFixedRate(plugin, task, delay, interval)
}

internal fun PluginOwner.runTimer(chunk: Chunk, delay: Long = 1L, interval: Long, task: (ScheduledTask) -> Unit) {
    plugin.server.regionScheduler.runAtFixedRate(plugin, chunk.world, chunk.x, chunk.z, task, delay, interval)
}