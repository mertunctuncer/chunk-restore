package dev.peopo.chunkrestore.restoreservice.oregenerator

import dev.peopo.chunkrestore.util.*
import org.bukkit.Bukkit
import org.bukkit.ChunkSnapshot
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin
import java.io.IOException
import kotlin.random.Random

internal class OreGenerator(override val plugin: Plugin) : PluginOwner{

    init {
        logInfo("Ore generator initialized.")
    }

    private val maxTries by config {
        it.getLong("settings.ore-generator.max-tries").toInt()
    }

    private val oreDataSet by config {
        val oresSection = it.getConfigurationSection("settings.ore-generator.ores") ?: return@config emptySet<OreData>()
        val oreKeys = oresSection.getKeys(false)

        oreKeys.mapNotNull { oreKey ->
            try {
                val oreSection = oresSection.getConfigurationSection(oreKey) ?: throw IOException("Invalid ore section: $oreKey")
                val material = oreSection.getString("material")?.uppercase()?.let {
                        materialString -> Material.valueOf(materialString)
                } ?: throw IOException("Invalid material: $oreKey")
                val spawnChance = oreSection.getLong("spawn-chance").toInt()
                val minCount = oreSection.getLong("min-count").toInt()
                val maxCount = oreSection.getLong("max-count").toInt()
                val minHeight = oreSection.getLong("min-height").toInt()
                val maxHeight = oreSection.getLong("max-height").toInt()

                OreData(material, spawnChance, minCount, maxCount, minHeight, maxHeight).also { data ->
                    logInfo("Loaded ore: $oreKey - $data")
                }
            } catch (e: Exception) {
                logInfo("Invalid ore: $oreKey")
                null
            }
        }.toSet().also { oreSet ->
            logInfo("Loaded ${oreSet.size} ores.")
        }
    }

    private val disabledMaterials by config {
        it.getStringList("settings.ore-generator.disabled-materials").mapNotNull { materialString ->
            try {
                Material.valueOf(materialString.uppercase())
            } catch (e: IllegalArgumentException) {
                logInfo("Invalid ore-generator disabled material: $materialString")
                null
            }
        }.toSet()
    }

    fun calculateOres(chunkSnapshot: ChunkSnapshot, changes: Map<BlockCoordinate, BlockData>): Map<BlockCoordinate, BlockData> {

        val world = Bukkit.getWorld(chunkSnapshot.worldName) ?: return emptyMap()
        val maxHeight = world.maxHeight
        val minHeight = world.minHeight

        val oreChanges = mutableMapOf<BlockCoordinate, BlockData>()

        for(oreData in oreDataSet) {
            val spawnChance = Random.nextInt(0, 100)
            if(spawnChance > oreData.spawnChance) continue


            val count = Random.nextInt(oreData.minSpawnCount, oreData.maxSpawnCount)
            val oreCoordinates = mutableSetOf<ChunkRelativeCoordinate>()

            var randomCoordinate: ChunkRelativeCoordinate? = null

            for (i in 0 until maxTries) {
                val current = ChunkRelativeCoordinate(Random.nextInt(0, 16), Random.nextInt(oreData.minHeight, oreData.maxHeight), Random.nextInt(0, 16), minHeight, maxHeight)
                val currentMaterial = getOverriddenMaterial(chunkSnapshot, changes, current.x, current.y, current.z)

                if(currentMaterial !in disabledMaterials) {
                    randomCoordinate = current
                    break
                }
            }

            if(randomCoordinate == null) continue

            oreCoordinates.add(randomCoordinate)


            repeat(count - 1){
                randomCoordinate = randomCoordinate!!.randomMove()
                oreCoordinates.add(randomCoordinate!!)
            }
            for(oreCoordinate in oreCoordinates) {
                val currentMaterial = getOverriddenMaterial(chunkSnapshot, changes, oreCoordinate.x, oreCoordinate.y, oreCoordinate.z)

                if(currentMaterial in disabledMaterials) continue

                val globalCoordinates = asBlockCoordinate(chunkSnapshot.x, chunkSnapshot.z, oreCoordinate.x, oreCoordinate.y, oreCoordinate.z)
                oreChanges[globalCoordinates] = Bukkit.createBlockData(oreData.material)
            }
        }

        return oreChanges
    }



    private data class ChunkRelativeCoordinate(
        val x: Int,
        val y: Int,
        val z: Int,
        val minHeight: Int,
        val maxHeight: Int
    ) {
        fun randomMove(): ChunkRelativeCoordinate {
            val randomDirection = Random.nextInt(0, 6)

            var x = this.x
            var y = this.y
            var z = this.z

            when(randomDirection) {
                0 -> x--
                1 -> x++
                2 -> z--
                3 -> z++
                4 -> y--
                5 -> y++
            }

            if(x in 0..15 && z in 0..15 && y in minHeight..maxHeight) {
                return ChunkRelativeCoordinate(x, y, z, minHeight, maxHeight)
            }

            return this
        }
    }

}