package de.redjulu.mixelPreview.listeners

import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxe
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxeUI
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxeVisualizer
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class CreativeAxeListener : Listener {

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        CreativeAxeVisualizer.stopVisualizer(event.player)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        CreativeAxeVisualizer.clearLastSelection(event.player)
        CreativeAxeVisualizer.startVisualizer(event.player)
    }

    @EventHandler
    fun pickupItem(event: EntityPickupItemEvent) {
        if (event.entity !is Player) return
        CreativeAxeVisualizer.startVisualizer(event.entity as Player)
    }

    @EventHandler
    fun getItemFromInv(event: PlayerInventorySlotChangeEvent) {
        CreativeAxeVisualizer.startVisualizer(event.player)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return

        val holder = player.openInventory.topInventory.holder
        if (holder !is CreativeAxeUI) return

        val item = event.currentItem
        val cursor = event.view.cursor

        if ((item != null && SpecialItemKeys.isSpecialItem(item, CreativeAxe.id)) ||
            SpecialItemKeys.isSpecialItem(cursor, CreativeAxe.id)) {
            event.isCancelled = true
        }
    }
}