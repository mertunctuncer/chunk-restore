package dev.peopo.chunkrestore.util



public enum class DebugLevel {
    ALL,
    DEBUG,
    ERROR_ONLY,
}
private val allowedDebug = arrayOf(
    DebugLevel.DEBUG,
    DebugLevel.ALL
)

internal fun LoggerOwner.logDebug(message: String) {
    if(debug in allowedDebug) pLogger.info("[DEBUG] $message")
}

internal fun LoggerOwner.logVerbose(message: String) {
    if(debug == DebugLevel.ALL) pLogger.info("[VERBOSE] $message")
}

internal fun LoggerOwner.logInfo(message: String) {
    pLogger.info(message)
}

internal fun LoggerOwner.logError(message: String, e: Exception) {
    pLogger.warning(message)
    if(debug == DebugLevel.DEBUG) e.printStackTrace()
}

internal fun LoggerOwner.logError(message: String) {
    pLogger.warning(message)
}

internal fun PluginOwner.logSevere(message: String, e: Exception? = null) {
    pLogger.severe(message)
    if(debug == DebugLevel.DEBUG) e?.printStackTrace()
    plugin.server.pluginManager.disablePlugin(plugin)
}