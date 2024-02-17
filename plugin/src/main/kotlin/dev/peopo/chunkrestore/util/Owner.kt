package dev.peopo.chunkrestore.util


import org.bukkit.plugin.Plugin
import java.util.logging.Logger


internal interface PluginOwner : LoggerOwner, ConfigurationOwner{
    val plugin: Plugin

    override val pLogger: Logger
        get() = plugin.logger

    override val debug: DebugLevel
        get() = (plugin as LoggerOwner).debug

    override val configurationAccessor: ConfigurationAccessor
        get() = (plugin as ConfigurationOwner).configurationAccessor
}

internal interface LoggerOwner {
    val pLogger: Logger
    val debug: DebugLevel
}

public interface ConfigurationOwner {
    public val configurationAccessor: ConfigurationAccessor
}