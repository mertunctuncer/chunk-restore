package dev.peopo.chunkrestore.util

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.Closeable
import java.io.File
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

public class ConfigurationAccessor(
    override val plugin: Plugin,
    private val configFile: File
) : YamlConfiguration(), PluginOwner{

    private val delegates: MutableSet<ReloadSubscriber<*>>

    init {
        plugin.saveResource(configFile.name, false)
        delegates = mutableSetOf()
        reload()
    }

    public fun save() {
        save(configFile)
        reload()
    }

    public fun reload() {
        this.load(configFile)
        delegates.forEach {
            it.reload()
        }
    }

    public fun registerDelegate(delegate: ReloadSubscriber<*>) {
        delegates.add(delegate)
    }
}

public class ReloadSubscriber<T>(
    private val configurationAccessor: ConfigurationAccessor,
    private val accessor: (ConfigurationAccessor) -> T,
) : ReadOnlyProperty<ConfigurationOwner, T> {


    private var value: T
        @Synchronized set
        @Synchronized get

    init {
        configurationAccessor.registerDelegate(this)
        value = accessor(configurationAccessor)
    }

    override fun getValue(thisRef: ConfigurationOwner, property: KProperty<*>): T {
        return value
    }

    public fun reload() {
        (value as? Closeable)?.close()
        value = this.accessor(configurationAccessor)
    }
}


internal fun <T: Any> ConfigurationOwner.config(accessor: (ConfigurationAccessor) -> T): ReloadSubscriber<T> =
    ReloadSubscriber(this.configurationAccessor, accessor)
internal fun <T: Any> ConfigurationAccessor.config(accessor: (ConfigurationAccessor) -> T): ReloadSubscriber<T> =
    ReloadSubscriber(this, accessor)