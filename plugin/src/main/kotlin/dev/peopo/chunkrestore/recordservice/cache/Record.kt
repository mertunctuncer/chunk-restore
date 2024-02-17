package dev.peopo.chunkrestore.recordservice.cache

import org.bukkit.block.data.BlockData

internal sealed interface Record

internal class ChangeRecord(val blockData: BlockData) : Record
internal data object RestoreRecord : Record