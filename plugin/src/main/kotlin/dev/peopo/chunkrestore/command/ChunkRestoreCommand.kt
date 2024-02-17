package dev.peopo.chunkrestore.command

import dev.peopo.chunkrestore.inspectorservice.InspectorService
import dev.peopo.chunkrestore.recordservice.RecordService
import dev.peopo.chunkrestore.restoreservice.RestoreService
import dev.peopo.chunkrestore.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method


public class ChunkRestoreCommand(
    override val plugin: Plugin,
    private val inspectorService: InspectorService,
    private val recordSercive: RecordService,
    private val restoreService: RestoreService
) : BukkitCommand(
    "chunkrestore",
    "Base command",
    "/cr [reload|enable|disable|inspect|restore]",
    listOf("cr")
), PluginOwner {

    private val noPermissionMessage: String by config {
        return@config it.getString("messages.no-permission") ?: "You do not have permission to use this command."
    }
    private val customUsageMessage: String by config {
        return@config it.getString("messages.usage") ?: "Usage: /cr [reload|enable|disable|inspect|restore]"
    }
    private val reloadMessage: String by config {
        return@config it.getString("messages.reload-successful") ?: "Reload successful."
    }
    private val enabledMessage: String by config {
        return@config it.getString("messages.enabled") ?: "Chunk restoration enabled."
    }
    private val disabledMessage: String by config {
        return@config it.getString("messages.disabled") ?: "Chunk restoration disabled."
    }
    private val inspectMessage: String by config {
        return@config it.getString("messages.inspect") ?: "Inspecting the chunk, restoration will be queued if necessary."
    }
    private val restoreMessage: String by config {
        return@config it.getString("messages.restore") ?: "Queueing restoration for the chunk."
    }

    public fun register() {
        Bukkit.getCommandMap().knownCommands["chunkrestore"] = this
        Bukkit.getCommandMap().register("chunkrestore", this)
        try {
            val server: Server = Bukkit.getServer()
            val syncCommandsMethod: Method = server::class.java.getDeclaredMethod("syncCommands")
            syncCommandsMethod.setAccessible(true)
            syncCommandsMethod.invoke(server)
            logInfo("Registered commands.")
        } catch (e: Exception) {
            logError("Could not register commands!", e)
        }
    }

    public override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {

        val miniMessage = MiniMessage.miniMessage()
        if(sender is Player && !sender.isOp) {
            sender.sendMessage(miniMessage.deserialize(noPermissionMessage))
            return true
        }
        if(args.isEmpty() || args.size != 1) {
            if(sender is Player) sender.sendMessage(MiniMessage.miniMessage().deserialize(customUsageMessage))
            else sender.sendMessage(Component.text("chunkrestore [reload|enable|disable]"))
            return true
        }

        when(args[0]) {
            "reload" -> {
                configurationAccessor.reload()
                sender.sendMessage(miniMessage.deserialize(reloadMessage))
            }
            "enable" -> {
                configurationAccessor.set("enabled", true)
                configurationAccessor.save()
                configurationAccessor.reload()
                sender.sendMessage(miniMessage.deserialize(enabledMessage))
            }
            "disable" -> {
                configurationAccessor.set("enabled", false)
                configurationAccessor.save()
                configurationAccessor.reload()
                sender.sendMessage(miniMessage.deserialize(disabledMessage))
            }
            "inspect" -> {
                if(sender !is Player) {
                    sender.sendMessage("Only players can use this command.")
                    return true
                }

                val changes = recordSercive.getChanges(sender.location.chunk.coordinates)
                inspectorService.inspect(sender.location.chunk.coordinates, changes)
                sender.sendMessage(miniMessage.deserialize(inspectMessage))
            }
            "restore" -> {
                if(sender !is Player) {
                    sender.sendMessage("Only players can use this command.")
                    return true
                }
                val changes = recordSercive.getChanges(sender.location.chunk.coordinates)
                restoreService.scheduleRestore(sender.location.chunk.coordinates, changes)
                sender.sendMessage(miniMessage.deserialize(restoreMessage))
            }

            else -> {
                if(sender is Player) sender.sendMessage(MiniMessage.miniMessage().deserialize(usageMessage))
                else sender.sendMessage(Component.text("chunkrestore [reload|enable|disable]"))
            }
        }

        return true
    }

}