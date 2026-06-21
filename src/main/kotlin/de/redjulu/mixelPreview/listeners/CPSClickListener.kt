package de.redjulu.mixelPreview.listeners

import de.redjulu.mixelPreview.commands.CPSCommandTest
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent

class CPSClickListener : Listener {
    @EventHandler
    fun onClick(event: PlayerInteractEvent) {
        val player = event.player
        CPSCommandTest.recordClick(player)
    }
}