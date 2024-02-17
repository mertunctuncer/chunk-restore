package dev.peopo.chunkrestore.restoreservice.oregenerator.cleaner

import dev.peopo.chunkrestore.restoreservice.oregenerator.getOverriddenMaterial
import dev.peopo.chunkrestore.util.*
import org.bukkit.Bukkit
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal open class OreRemover(
    override val plugin: Plugin
) : PluginOwner, ConfigurationOwner{

    override val configurationAccessor: ConfigurationAccessor = (plugin as ConfigurationOwner).configurationAccessor


    protected val oreMaterials by config {
        val oresSection = it.getConfigurationSection("settings.ore-generator.ores") ?: return@config emptySet<Material>()
        val oreKeys = oresSection.getKeys(false)

        return@config oreKeys.mapNotNull { oreKey ->
            val oreSection = oresSection.getConfigurationSection(oreKey) ?: return@mapNotNull null

            return@mapNotNull oreSection.getString("material")?.uppercase()?.let { materialString ->
                Material.valueOf(materialString)
            } ?: return@mapNotNull null

        }.toSet()
    }

    protected val defaultMaterial by config {
        try {
            it.getString("settings.ore-generator.default-material")?.uppercase()?.let { materialString ->
                Material.valueOf(materialString)
            } ?: Material.STONE
        } catch (e: Exception) {
            logInfo("Invalid default material: ${it.getString("settings.ore-generator.default-material")}")
            Material.STONE
        }
    }

    fun calculateChanges(
        chunkSnapshot: ChunkSnapshot,
        changes: Map<BlockCoordinate, BlockData>
    ): Map<BlockCoordinate, BlockData> {
        val world = Bukkit.getWorld(chunkSnapshot.worldName) ?: return emptyMap()
        val maxHeight = world.maxHeight
        val minHeight = world.minHeight

        val changeMap = mutableMapOf<BlockCoordinate, BlockData>()

        for (x in 0..15) {
            for (z in 0..15) {
                for (y in minHeight until maxHeight) {
                    val material = getOverriddenMaterial(chunkSnapshot, changes, x, y, z)

                    if(!oreMaterials.contains(material)) continue

                    val globalCoordinates = asBlockCoordinate(chunkSnapshot.x, chunkSnapshot.z, x, y, z)

                    changeMap[globalCoordinates] = Bukkit.createBlockData(getMaterial(chunkSnapshot, changes, x, y, z, minHeight, maxHeight))
                }
            }
        }

        return changeMap
    }

    open fun getMaterial(chunkSnapshot: ChunkSnapshot, changes: Map<BlockCoordinate, BlockData>, x: Int, y: Int, z: Int, minHeight: Int, maxHeight: Int): Material { return defaultMaterial }
}