package dev.peopo.chunkrestore

import com.sk89q.worldguard.WorldGuard
import dev.peopo.chunkrestore.command.ChunkRestoreCommand
import dev.peopo.chunkrestore.inspectorservice.InternalInspectorService
import dev.peopo.chunkrestore.inspectorservice.inspector.CooldownInspector
import dev.peopo.chunkrestore.inspectorservice.inspector.CountInspector
import dev.peopo.chunkrestore.inspectorservice.inspector.LandsInspector
import dev.peopo.chunkrestore.inspectorservice.inspector.WorldGuardInspector
import dev.peopo.chunkrestore.recordservice.InternalRecordService
import dev.peopo.chunkrestore.recordservice.cache.RestoreTimestampHolder
import dev.peopo.chunkrestore.recordservice.persistent.DatabaseAccessor
import dev.peopo.chunkrestore.recordservice.persistent.mongo.MongoDBAccessor
import dev.peopo.chunkrestore.restoreservice.InternalRestoreService
import dev.peopo.chunkrestore.util.*
import me.angeschossen.lands.api.LandsIntegration
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.util.logging.Logger


public class ChunkRestorePlugin : JavaPlugin(), LoggerOwner, ConfigurationOwner {


    override val pLogger: Logger
        get() = super.getLogger()

    override val configurationAccessor: ConfigurationAccessor = ConfigurationAccessor(this, File(this.dataFolder,"config.yml"))

    override val debug: DebugLevel by config {
        val debugString = it.getString("console-debug-level")?.uppercase() ?: run {
            it.logError("Debug level not found, defaulting to error only.")
            return@config DebugLevel.ERROR_ONLY
        }
        return@config try {
            DebugLevel.valueOf(debugString.uppercase()).also {
                logInfo("Debug level set to $debugString.")
            }
        } catch (e: IllegalArgumentException) {
            it.logError("Debug level not valid, defaulting to error only.")
            DebugLevel.ERROR_ONLY
        }
    }

    private val databaseAccessor: DatabaseAccessor by lazy { MongoDBAccessor(this) }

    private val restoreTimestampHolder: RestoreTimestampHolder by lazy { RestoreTimestampHolder(this, databaseAccessor) }

    private val internalInspectorService: InternalInspectorService by lazy { InternalInspectorService(this, restoreService) }

    private val internalRecordService: InternalRecordService by lazy {
        InternalRecordService(this, databaseAccessor, internalInspectorService)
    }

    private val restoreService: InternalRestoreService by lazy { InternalRestoreService(this, restoreTimestampHolder) }

    private val worldGuardAPI: WorldGuard? by lazy {
        try { WorldGuard.getInstance() } catch (e: Error) { null }
    }

    private val landsAPI: LandsIntegration? by lazy {
        try { LandsIntegration.of(this) } catch (e: Error) { null }
    }


    override fun onEnable() {
        // DB
        databaseAccessor.createConnectionPool()
        databaseAccessor.createDirectories()

        // Inspector
        internalInspectorService.registerInspector(CooldownInspector(this, restoreTimestampHolder))
        internalInspectorService.registerInspector(CountInspector(this))
        landsAPI?.let {
            internalInspectorService.registerInspector(LandsInspector(this, it))
        } ?: logInfo("Lands not found. Lands integration will be disabled.")
        worldGuardAPI?.let {
            internalInspectorService.registerInspector(WorldGuardInspector(this, it))
        }?: logInfo("WorldGuard not found. WorldGuard integration will be disabled.")

        internalRecordService.initialize()

        restoreService.initialize(internalRecordService)

        ChunkRestoreCommand(this, internalInspectorService, internalRecordService, restoreService).register()

        logInfo("Plugin enabled.")
    }


    override fun onDisable() {
        internalRecordService.close()
    }
}