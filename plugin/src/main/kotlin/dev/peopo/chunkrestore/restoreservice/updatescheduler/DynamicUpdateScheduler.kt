package dev.peopo.chunkrestore.restoreservice.updatescheduler

import dev.peopo.chunkrestore.util.*
import org.bukkit.plugin.Plugin
import java.lang.Integer.max
import java.lang.Integer.min
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal class DynamicUpdateScheduler(
    override val plugin: Plugin
) : UpdateScheduler {

    init {
        logInfo("Dynamic chunk restoration enabled.")
    }

    private val chunkQueues = ConcurrentLinkedQueue<ChunkUpdateTask>()

    private val restoreSpeedLimit by config {
        it.getLong("settings.restore-limit").toInt().also { limit ->
            logInfo("Dynamic restore speed limit set to $limit chunks per tick.")
        }
    }

    private val restoreTimeLimit by config {
        it.getLong("settings.restore-time-limit").also { limit ->
            logInfo("Dynamic restore time limit set to $limit ms.")
        }
    }

    private val active = AtomicBoolean(false)
    private val speed = AtomicInteger(restoreSpeedLimit)
    private val delta = AtomicLong(0L)

    override fun scheduleTask(task: ChunkUpdateTask) {
        chunkQueues.add(task)
        logDebug("Scheduled restore task with ${task.changes.size} changes.")
        if(!active.get()) startTask()
    }

    private fun startTask() {
        active.set(true)

        runTimer(1L, 1L) { scheduledTask ->
            val startMillis = System.currentTimeMillis()

            updateSpeed(delta.get())
            delta.set(0L)
            for(i in 1..speed.get()) {
                val currentTask = chunkQueues.poll() ?: break
                val world = currentTask.chunk.world
                val chunk = currentTask.chunk
                runTask(chunk) {
                    currentTask.changes.forEach { (blockCoordinate, blockData) ->
                        world.getBlockAt(blockCoordinate.x, blockCoordinate.y, blockCoordinate.z).setBlockData(blockData, false)
                    }

                    val delta = delta.get()
                    val newDelta = System.currentTimeMillis() - startMillis
                    if(newDelta > delta) {
                        this.delta.set(newDelta)
                    }
                }
            }

            if(chunkQueues.isEmpty()) {
                active.set(false)
                scheduledTask.cancel()
            }
        }
    }


    private fun updateSpeed(delta: Long) {
        if(delta > restoreTimeLimit) speed.set(max(speed.get() - 1, 1))
        else speed.set(min(speed.get() + 1, restoreSpeedLimit))
    }
}