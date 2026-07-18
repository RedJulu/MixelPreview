package de.redjulu.mixelPreview.utils

import de.redjulu.mixelPreview.MixelPreview
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.*
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class GUIListener : Listener {

    companion object {
        private val actions = mutableMapOf<Inventory, MutableMap<Int, (Player, ClickType) -> Unit>>()

        fun registerButton(inv: Inventory, slot: Int, action: (Player, ClickType) -> Unit) {
            actions.getOrPut(inv) { mutableMapOf() }[slot] = action
        }

        fun clearButtons(inv: Inventory) {
            actions.remove(inv)
        }
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val gui = event.inventory.holder as? BaseGUI<*, *, *> ?: return

        val slot = event.rawSlot
        val invSize = event.inventory.size

        if (slot in 0 until invSize) {
            if (gui.isInteractableSlot(slot)) return

            if (gui.isPlaceholderSlot(slot)) {
                event.isCancelled = true
                val inSlot = event.currentItem
                val cursor = event.view.cursor
                val cursorHasItem = cursor.type != Material.AIR

                if (BaseGUI.isPlaceholderItem(inSlot)) {
                    if (cursorHasItem) {
                        val predicate = gui.getPlaceholderPredicate(slot)
                        if (predicate != null && !predicate(cursor)) return

                        event.view.setCursor(ItemStack(Material.AIR))
                        event.inventory.setItem(slot, cursor.clone())
                        gui.onPlaceholderUpdate(player, slot, cursor.clone())
                    }
                } else {
                    val userItem = if (inSlot != null && inSlot.type != Material.AIR) inSlot else null
                    if (userItem != null) {
                        val placeholder = gui.getPlaceholderForSlot(slot)
                        if (event.click.isShiftClick) {
                            event.inventory.setItem(slot, placeholder?.clone())
                            player.inventory.addItem(userItem).values.forEach {
                                player.world.dropItemNaturally(player.location, it)
                            }
                            gui.onPlaceholderUpdate(player, slot, userItem)
                        } else {
                            if (cursorHasItem) {
                                val predicate = gui.getPlaceholderPredicate(slot)
                                if (predicate != null && !predicate(cursor)) return

                                event.view.setCursor(userItem.clone())
                                event.inventory.setItem(slot, cursor.clone())
                                gui.onPlaceholderUpdate(player, slot, cursor.clone())
                            } else {
                                event.view.setCursor(userItem.clone())
                                event.inventory.setItem(slot, placeholder?.clone())
                                gui.onPlaceholderUpdate(player, slot, cursor.clone())
                            }
                        }

                        val itemNow = event.inventory.getItem(slot)
                        if (itemNow != null && BaseGUI.isPlaceholderItem(itemNow)) {
                            gui.clearActiveItem(slot)
                        }
                    }
                }
                gui.updateDynamicButtons(player)
                return
            }

            event.isCancelled = true
            actions[event.inventory]?.get(slot)?.invoke(player, event.click)
        } else {
            if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                val top = event.view.topInventory
                val guiTop = top.holder as? BaseGUI<*, *, *> ?: return
                val fromPlayer = event.currentItem ?: return

                if (fromPlayer.type != Material.AIR) {
                    for (i in 0 until top.size) {
                        if (!guiTop.isPlaceholderSlot(i) || !guiTop.isPrioritySlot(i)) continue
                        val s = top.getItem(i)
                        if (s == null || !BaseGUI.isPlaceholderItem(s)) continue

                        val predicate = guiTop.getPlaceholderPredicate(i)
                        if (predicate != null && !predicate(fromPlayer)) continue

                        val itemCopy = fromPlayer.clone()
                        top.setItem(i, itemCopy)
                        fromPlayer.amount = 0
                        event.isCancelled = true
                        guiTop.onPlaceholderUpdate(player, i, itemCopy)
                        guiTop.updateDynamicButtons(player)
                        return
                    }

                    for (i in 0 until top.size) {
                        if (!guiTop.isInteractableSlot(i)) continue
                        val s = top.getItem(i)
                        if (s != null && s.type != Material.AIR) continue

                        val predicate = guiTop.getInteractablePredicate(i)
                        if (predicate != null && !predicate(fromPlayer)) continue

                        val itemCopy = fromPlayer.clone()
                        top.setItem(i, itemCopy)
                        fromPlayer.amount = 0
                        event.isCancelled = true
                        guiTop.updateDynamicButtons(player)
                        return
                    }
                }
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        val gui = event.inventory.holder as? BaseGUI<*, *, *> ?: return

        if (event.inventory.type == InventoryType.ANVIL) return

        gui.onClose(player)

        if (gui.isDialogOpen) {
            gui.reopenFor(player)
            return
        }

        if (!gui.isSwitching) {
            for (i in 0 until event.inventory.size) {
                if (gui.isPlaceholderSlot(i) || gui.isInteractableSlot(i)) {
                    val item = event.inventory.getItem(i)
                    if (item != null && item.type != Material.AIR && !BaseGUI.isPlaceholderItem(item)) {
                        player.inventory.addItem(item).values.forEach {
                            player.world.dropItemNaturally(player.location, it)
                        }
                    }
                }
            }
        }

        clearButtons(event.inventory)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        BaseGUI.clearHistory(event.player.uniqueId)
    }
}