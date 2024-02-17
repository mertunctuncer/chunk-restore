package dev.peopo.chunkrestore.recordservice.listener

import dev.peopo.chunkrestore.recordservice.InternalRecordService
import dev.peopo.chunkrestore.util.chunkCoordinates
import dev.peopo.chunkrestore.util.coordinates
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent

internal class BlockChangeListener(
    private val recordService: InternalRecordService
) : Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBreak(event: BlockBreakEvent) {
        recordService.processBlockChange(event.block.chunkCoordinates, event.block.coordinates, event.block.blockData)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockPlace(event: BlockPlaceEvent) {
        recordService.processBlockChange(event.block.chunkCoordinates, event.block.coordinates, Bukkit.createBlockData(Material.AIR))
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockBurn(event: BlockBurnEvent) {
        recordService.processBlockChange(event.block.chunkCoordinates, event.block.coordinates, event.block.blockData)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityExplosion(event: EntityExplodeEvent) {
        event.blockList().forEach { recordService.processBlockChange(it.chunkCoordinates, it.coordinates, it.blockData) }
    }

    /* // TODO POSSIBLY RE-ADD THIS EVENT
    @EventHandler (priority = EventPriority.MONITOR)
    public fun onExplosion(event: BlockExplodeEvent) {
        logPastState(event.block.chunk, event.block.toPositionData(), event.block.blockData)
    }
    */
    // TODO MORE DETECTIONS?
}