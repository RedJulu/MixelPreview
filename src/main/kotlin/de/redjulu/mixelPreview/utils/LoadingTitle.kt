package de.redjulu.mixelPreview.utils

import de.redjulu.mixelPreview.MixelPreview
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.title.Title
import net.kyori.adventure.util.Ticks
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import java.util.UUID

object LoadingTitle {

    private val tasks = mutableMapOf<UUID, BukkitTask>()
    private val mm = MiniMessage.miniMessage()

    fun load(player: Player) {
        cancel(player)
        val frames = listOf("", ".", "..", "...")
        var frame = 0
        val task = MixelPreview.instance.server.scheduler.runTaskTimer(MixelPreview.instance, Runnable {
            val dots = frames[frame % frames.size]
            frame++
            player.showTitle(
                Title.title(
                    mm.deserialize("<gray><i>Lade$dots"),
                    Component.empty(),
                    Title.Times.times(Ticks.duration(0), Ticks.duration(10), Ticks.duration(0))
                )
            )
        }, 0L, 8L)
        tasks[player.uniqueId] = task
    }

    fun cancel(player: Player) {
        tasks.remove(player.uniqueId)?.cancel()
    }

    fun finish(player: Player, title: String, subtitle: String) {
        cancel(player)
        player.showTitle(
            Title.title(
                mm.deserialize(title),
                mm.deserialize(subtitle),
                Title.Times.times(Ticks.duration(5), Ticks.duration(40), Ticks.duration(15))
            )
        )
    }
}
