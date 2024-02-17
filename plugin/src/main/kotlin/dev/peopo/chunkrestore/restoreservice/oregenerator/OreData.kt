package dev.peopo.chunkrestore.restoreservice.oregenerator

import org.bukkit.Material

internal data class OreData(
    val material: Material,
    val spawnChance: Int,
    val minSpawnCount: Int,
    val maxSpawnCount: Int,
    val minHeight: Int,
    val maxHeight: Int,
)
