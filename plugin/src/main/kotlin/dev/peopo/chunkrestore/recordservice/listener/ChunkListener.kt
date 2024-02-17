package dev.peopo.chunkrestore.recordservice.listener

import dev.peopo.chunkrestore.recordservice.InternalRecordService
import dev.peopo.chunkrestore.util.coordinates
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

internal class ChunkListener(
    private val internalRecordService: InternalRecordService,
) : Listener {

    @EventHandler
    fun onChunkLoad(event: ChunkLoadEvent) {
        internalRecordService.processChunkLoad(event.chunk.coordinates)
    }

    @EventHandler
    fun onChunkUnload(event: ChunkUnloadEvent) {
        internalRecordService.processChunkUnload(event.chunk.coordinates)
    }
}