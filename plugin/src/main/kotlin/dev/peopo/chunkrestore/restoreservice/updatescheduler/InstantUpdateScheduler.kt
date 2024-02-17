package dev.peopo.chunkrestore.restoreservice.updatescheduler

import dev.peopo.chunkrestore.util.BlockCoordinate
import dev.peopo.chunkrestore.util.logDebug
import dev.peopo.chunkrestore.util.logInfo
import dev.peopo.chunkrestore.util.runTask
import org.bukkit.Chunk
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

public class InstantUpdateScheduler(
    override val plugin: Plugin
): UpdateScheduler {

    init {
        logInfo("Restore limit is set to 0, instant updates will be used.")
    }

    override fun scheduleTask(task: ChunkUpdateTask) {
        logDebug("Scheduled restore task with ${task.changes.size} changes.")
        startTask(task.chunk, task.changes)
    }

    private fun startTask(chunk: Chunk, changes: Map<BlockCoordinate, BlockData>) {
        runTask(chunk) {
            val world = chunk.world
            changes.forEach { (blockPosition, blockData) ->
                world.getBlockAt(blockPosition.x, blockPosition.y, blockPosition.z).setBlockData(blockData, false)
            }
        }
    }
}