package dev.peopo.chunkrestore.recordservice.timeout

import dev.peopo.chunkrestore.recordservice.InternalRecordService
import dev.peopo.chunkrestore.util.ChunkCoordinate
import dev.peopo.chunkrestore.util.PluginOwner
import dev.peopo.chunkrestore.util.config
import dev.peopo.chunkrestore.util.logInfo
import org.bukkit.plugin.Plugin
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

internal class TimeoutNotifier (
    private val internalRecordService: InternalRecordService, override val plugin: Plugin
): PluginOwner, AutoCloseable {

    private val cacheTimeout by config {
        it.getLong("settings.cache-timeout", 10L).also {timeoutMinutes ->
            logInfo("Cache timeout set to $timeoutMinutes minutes.")
        } * 60_000L
    }

    private val timerMillis = 1000L
    private val continueScanning = AtomicBoolean(true)
    private val timeoutThread = TimeoutThread()


    internal fun initialize() {
        logInfo("Starting timeout thread...")
        timeoutThread.start()
    }

    override fun close() {
        logInfo("Stopping timeout notifier...")
        continueScanning.set(false)
    }

    private val timeMap = ConcurrentHashMap<ChunkCoordinate, Long>()

    internal fun markExpire(chunkCoordinate: ChunkCoordinate) {
        timeMap[chunkCoordinate] = System.currentTimeMillis() + cacheTimeout
    }

    internal fun removeExpire(chunkCoordinate: ChunkCoordinate) {
        timeMap.remove(chunkCoordinate)
    }

    private inner class TimeoutThread: Thread() {
        override fun run() {
            while(continueScanning.get()) {
                doCleanPass()
                try {
                    sleep(timerMillis)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }

        private fun doCleanPass() {
            val currentMillis = System.currentTimeMillis()
            for(key in timeMap.keys) {
                timeMap[key]?.let {
                    if(currentMillis > it) {
                        timeMap.remove(key)
                        internalRecordService.processExpire(key)
                    }
                }
            }
        }
    }
}