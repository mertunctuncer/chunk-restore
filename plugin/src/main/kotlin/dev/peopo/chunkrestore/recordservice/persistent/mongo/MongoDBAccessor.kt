package dev.peopo.chunkrestore.recordservice.persistent.mongo

import com.mongodb.MongoException
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Projections
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.kotlin.client.MongoClient
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoDatabase
import dev.peopo.chunkrestore.recordservice.persistent.DatabaseAccessor
import dev.peopo.chunkrestore.recordservice.persistent.mongo.data.BlockRecord
import dev.peopo.chunkrestore.recordservice.persistent.mongo.data.ChunkRecord
import dev.peopo.chunkrestore.recordservice.persistent.mongo.data.ChunkResult
import dev.peopo.chunkrestore.recordservice.persistent.mongo.data.TimestampData
import dev.peopo.chunkrestore.util.*
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.bukkit.plugin.Plugin

internal class MongoDBAccessor(
    override val plugin: Plugin,
) : DatabaseAccessor, PluginOwner {

    // TODO MORE EFFICIENT STORAGE, SINCE ALL OF THE CHANGES ARE NEEDED, MAYBE PUT ALL CHANGES INTO JSON THEN COMPRESS, A LOT OF REPETITION SHOULD SAVE QUITE A BIT OF SPACE
    private data class MongoDBSettings(
        val host: String,
        val port: String,
        val username: String,
        val password: String,
        val database: String,
    ) {
        val uri: String get() = "mongodb://$username:$password@$host:$port"

        companion object {
            const val MONGODB_SETTINGS_KEY = "mongodb"
            const val MONGODB_HOST_KEY = "host"
            const val MONGODB_PORT_KEY = "port"
            const val MONGODB_USERNAME_KEY = "username"
            const val MONGODB_PASSWORD_KEY = "password"
            const val MONGODB_DATABASE_KEY = "database"
        }
    }

    private val settings by config {
        val mongoDBSection = it.getConfigurationSection(MongoDBSettings.MONGODB_SETTINGS_KEY)
        return@config MongoDBSettings(
            mongoDBSection?.getString(MongoDBSettings.MONGODB_HOST_KEY) ?: run {
                logError("MongoDB host not found, defaulting to localhost.")
                return@run "localhost"
            },
            mongoDBSection?.getString(MongoDBSettings.MONGODB_PORT_KEY) ?: run {
                logError("MongoDB port not found, defaulting to 27017.")
                return@run "27017"
            },
            mongoDBSection?.getString(MongoDBSettings.MONGODB_USERNAME_KEY) ?: run {
                logError("MongoDB username not found, defaulting to 'admin'.")
                return@run "admin"
            },
            mongoDBSection?.getString(MongoDBSettings.MONGODB_PASSWORD_KEY) ?: run {
                logError("MongoDB password not found, defaulting to 'admin'.")
                return@run "admin"
            },
            mongoDBSection?.getString(MongoDBSettings.MONGODB_DATABASE_KEY) ?: run {
                logError("MongoDB database not found, defaulting to 'chunkrestore'.")
                return@run "chunkrestore"
            },
        )
    }

    private lateinit var client: MongoClient

    private val database: MongoDatabase
        get() = settings.database.let { client.getDatabase(it) }

    private val recordCollection: MongoCollection<ChunkRecord>
        get() = database.getCollection<ChunkRecord>(RECORDS_COLLECTION)

    private val timestampCollection: MongoCollection<TimestampData>
        get() = database.getCollection<TimestampData>(TIMESTAMP_COLLECTION)

    override fun createConnectionPool(): Boolean {
        logInfo("Initializing MongoDB connection pool...")
        try {
            client = MongoClient.create(settings.uri)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    override fun createDirectories(): Unit = runBlocking{
        logInfo("Creating MongoDB directory structure...")
        database.createCollection(RECORDS_COLLECTION)
        database.createCollection(TIMESTAMP_COLLECTION)
        logInfo("Creating MongoDB indexes...")
        database.getCollection<ChunkRecord>(RECORDS_COLLECTION).createIndex(
            Indexes.ascending(ChunkRecord::chunkX.name, ChunkRecord::chunkZ.name, ChunkRecord::world.name)
        )
        database.getCollection<TimestampData>(TIMESTAMP_COLLECTION).createIndex(
            Indexes.ascending(TimestampData::x.name, TimestampData::z.name, TimestampData::world.name)
        )
        logInfo("MongoDB has been successfully initialized.")
    }

    override fun close() {
        logInfo("Closing MongoDB connection pool...")
        client.close()
        logInfo("MongoDB connection pool has been closed.")
    }

    override fun fetchRecords(world: String, chunkX: Int, chunkZ: Int): Map<BlockCoordinate, BlockData>  {
        try {
            val projection = Projections.fields(
                Projections.include(ChunkRecord::changes.name),
                Projections.excludeId()
            )

            val result = recordCollection.withDocumentClass<ChunkResult>()
                .find(
                    and(
                        eq(ChunkRecord::chunkX.name, chunkX),
                        eq(ChunkRecord::chunkZ.name, chunkZ),
                        eq(ChunkRecord::world.name, world)
                    )
                ).projection(projection)


            val resultMap = mutableMapOf<BlockCoordinate, BlockData>()

            result.firstOrNull()?.changes?.forEach {
                resultMap[BlockCoordinate(it.x, it.y, it.z)] = Bukkit.createBlockData(it.serializedData)
            }

            logVerbose("Fetched ${resultMap.size} block records from the database. Coordinates $world:$chunkX,$chunkZ")

            return resultMap
        } catch (e: Exception) {
            logError("Failed to fetch block records from the database. Coordinates $world:$chunkX,$chunkZ", e)
            return emptyMap()
        }
    }


    override fun pushRecords(world: String, chunkX: Int, chunkZ: Int, changes: Map<BlockCoordinate, BlockData>): Boolean {
        val query = and(
            eq(ChunkRecord::chunkX.name, chunkX),
            eq(ChunkRecord::chunkZ.name, chunkZ),
            eq(ChunkRecord::world.name, world)
        )

        if(changes.isEmpty()) {
            try {
                recordCollection.deleteOne(query)
                logVerbose("Block records cache is empty, deleting saved records. Coordinates $world:$chunkX,$chunkZ")
                return true
            } catch (e: MongoException) {
                logError("Failed to push changes to database. Coordinates $world:$chunkX,$chunkZ", e)
                return false
            }
        } else {
            val changeSet = changes.mapTo(mutableSetOf()) {
                BlockRecord(it.key.x, it.key.y, it.key.z, it.value.getAsString(false))
            }
            val chunkData = ChunkRecord(world, chunkX, chunkZ, changeSet)

            try {
                recordCollection.replaceOne(query, chunkData, ReplaceOptions().upsert(true))
                logVerbose("Pushed ${changes.size} block records for $world:$chunkX,$chunkZ")
                return true
            } catch (e: MongoException) {
                logError("Failed to push block records to database for $world:$chunkX,$chunkZ", e)
                return false
            }
        }
    }

    override fun pushTimestamp(world: String, chunkX: Int, chunkZ: Int, lastUpdate: Long) : Boolean{

        val query = and(
            eq(TimestampData::x.name, chunkX),
            eq(TimestampData::z.name, chunkZ),
            eq(TimestampData::world.name, world)
        )

        val changeData = TimestampData(world, chunkX, chunkZ, lastUpdate)

        try {
            timestampCollection.replaceOne(query, changeData, ReplaceOptions().upsert(true))
            logVerbose("Pushed update timestamp for chunk: $world:$chunkX,$chunkZ")
            return true
        } catch (e: MongoException) {
            logError("Failed to push timestamp at $world:$chunkX,$chunkZ", e)
            throw e
        }
    }

    override fun fetchAllTimestamps(): Map<ChunkCoordinate, Long> {
        try {
            val result = timestampCollection.find()

            val resultMap = mutableMapOf<ChunkCoordinate, Long>()

            result.forEach {
                resultMap[ChunkCoordinate(it.world, it.x, it.z)] = it.lastUpdate
            }
            logVerbose("Fetched ${resultMap.size} update timestamps from the database.")

            return resultMap
        } catch (e: Exception) {
            logError("Failed to fetch update timestamps from the database.", e)
            throw e
        }
    }

    private companion object {
        private const val RECORDS_COLLECTION = "chunk-regenerator-changes"
        private const val TIMESTAMP_COLLECTION = "chunk-regenerator-chunk-updates"
    }
}