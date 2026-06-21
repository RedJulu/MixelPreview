package de.redjulu.mixelPreview.commands

import com.mojang.brigadier.context.CommandContext
import de.redjulu.mixelPreview.listeners.CPSClickListener
import net.kyori.adventure.text.minimessage.MiniMessage
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID

object CPSCommandTest : Listener {

    private val mm = MiniMessage.miniMessage()
    private val tracked = mutableMapOf<UUID, UUID>()
    private val clickData = mutableMapOf<UUID, MutableList<Long>>()
    private val maxCps = mutableMapOf<UUID, Int>()
    private val tasks = mutableMapOf<UUID, BukkitRunnable>()

    fun register(plugin: JavaPlugin) {
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.server.pluginManager.registerEvents(CPSClickListener(), plugin)

        plugin.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            event.registrar().register(
                Commands.literal("cps")
                    .then(
                        Commands.argument("spieler", ArgumentTypes.players())
                            .executes { ctx -> execute(ctx, plugin) }
                    )
                    .build()
            )
        }
    }

    private fun execute(ctx: CommandContext<CommandSourceStack>, plugin: JavaPlugin): Int {
        val source = ctx.source
        val sender = source.sender
        if (sender !is Player) return 0

        val selector = ctx.getArgument("spieler", PlayerSelectorArgumentResolver::class.java)
        val target = selector.resolve(source).firstOrNull() ?: return 0

        if (tracked[sender.uniqueId] == target.uniqueId) {
            stop(sender.uniqueId)
            sender.sendMessage(mm.deserialize(
                "<blue>CPS <dark_gray>» <red>Deaktiviert für <yellow>${target.name}"
            ))
            return 1
        }

        val apostrophe = if (target.name.endsWith("s") || target.name.endsWith("x") || target.name.endsWith("z"))
            "<blue>'</blue>" else "<blue>'s</blue>"

        sender.sendMessage(mm.deserialize(
            "<blue>CPS <dark_gray>» <green>Aktiviert für <yellow>${target.name}"
        ))

        clickData.getOrPut(target.uniqueId) { mutableListOf() }
        maxCps.putIfAbsent(target.uniqueId, 0)
        tracked[sender.uniqueId] = target.uniqueId

        tasks[sender.uniqueId]?.cancel()

        val task = object : BukkitRunnable() {
            override fun run() {
                if (!target.isOnline) {
                    sender.sendMessage(mm.deserialize(
                        "<blue>CPS <dark_gray>» <yellow>${target.name} <red>ist offline gegangen"
                    ))
                    stop(sender.uniqueId)
                    return
                }

                val now = System.currentTimeMillis()
                val clicks = clickData[target.uniqueId] ?: mutableListOf()
                clicks.removeIf { now - it > 1000 }
                val cps = clicks.size
                if (cps > (maxCps[target.uniqueId] ?: 0)) maxCps[target.uniqueId] = cps
                val max = maxCps[target.uniqueId] ?: 0

                sender.sendActionBar(mm.deserialize(
                    "<yellow>${target.name}</yellow>$apostrophe <blue>CPS</blue> <gray><b>▸</b> <dark_green>$cps <dark_gray>[<gold>$max <b>↗</b><dark_gray>]"
                ))
            }
        }

        task.runTaskTimer(plugin, 0L, 2L)
        tasks[sender.uniqueId] = task

        return 1
    }

    fun recordClick(player: Player) {
        val clicks = clickData[player.uniqueId] ?: return
        clicks.add(System.currentTimeMillis())
    }

    private fun stop(viewerUuid: UUID) {
        tasks[viewerUuid]?.cancel()
        tasks.remove(viewerUuid)
        val targetUuid = tracked.remove(viewerUuid) ?: return

        val isStillTracked = tracked.containsValue(targetUuid)
        if (!isStillTracked) {
            clickData.remove(targetUuid)
            maxCps.remove(targetUuid)
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        stop(event.player.uniqueId)
        val targetsToRemove = tracked.filter { it.value == event.player.uniqueId }.keys
        targetsToRemove.forEach { stop(it) }
    }
}