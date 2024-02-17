package dev.peopo.chunkrestore.restoreservice.updatescheduler

import dev.peopo.chunkrestore.util.PluginOwner

internal interface UpdateScheduler : PluginOwner{
    fun scheduleTask(task: ChunkUpdateTask)
}