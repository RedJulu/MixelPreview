package de.redjulu.mixelPreview.listeners

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.items.SpecialItemRegistry
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityShootBowEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerHarvestBlockEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemBreakEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerItemDamageEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerPickupItemEvent
import org.bukkit.event.player.PlayerShearEntityEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class SpecialItemListener : Listener {

    private fun dispatch(item: ItemStack?, handler: (SpecialItem) -> Unit) {
        val id = SpecialItemKeys.getItemId(item) ?: return
        SpecialItemRegistry.get(id)?.let(handler)
    }

    private fun dispatchHands(player: Player, handler: (SpecialItem) -> Unit) {
        dispatch(player.inventory.itemInMainHand, handler)
        dispatch(player.inventory.itemInOffHand, handler)
    }

    fun startTickTask(plugin: JavaPlugin) {
        object : BukkitRunnable() {
            override fun run() {
                Bukkit.getOnlinePlayers().forEach { player ->
                    dispatchHands(player) { it.onTick(player) }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L)
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        dispatch(event.item) { it.onInteract(event) }
    }

    @EventHandler
    fun onInteractEntity(event: PlayerInteractEntityEvent) {
        dispatch(event.player.inventory.itemInMainHand) { it.onInteractEntity(event) }
        dispatch(event.player.inventory.itemInOffHand) { it.onInteractEntity(event) }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val item = event.itemInHand
        val id = SpecialItemKeys.getItemId(item) ?: return
        val special = SpecialItemRegistry.get(id) ?: return

        SpecialItemKeys.tagBlock(event.block, id)
        special.onPlace(event)
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val blockId = SpecialItemKeys.getBlockId(event.block)
        if (blockId != null) {
            SpecialItemRegistry.get(blockId)?.onBreak(event)
            SpecialItemKeys.untagBlock(event.block)
        }

        val player = event.player
        dispatch(player.inventory.itemInMainHand) { it.onBreakWith(event) }
        dispatch(player.inventory.itemInOffHand) { it.onBreakWith(event) }
    }

    @EventHandler
    fun onItemBreak(event: PlayerItemBreakEvent) {
        dispatch(event.brokenItem) { it.onItemBreak(event) }
    }

    @EventHandler
    fun onItemConsume(event: PlayerItemConsumeEvent) {
        dispatch(event.item) { it.onItemConsume(event) }
    }

    @EventHandler
    fun onItemDamage(event: PlayerItemDamageEvent) {
        dispatch(event.item) { it.onItemDamage(event) }
    }

    @EventHandler
    fun onItemHeld(event: PlayerItemHeldEvent) {
        dispatch(event.player.inventory.getItem(event.newSlot)) { it.onItemHeld(event) }
    }

    @EventHandler
    fun onDropItem(event: PlayerDropItemEvent) {
        dispatch(event.itemDrop.itemStack) { it.onDropItem(event) }
    }

    @EventHandler
    fun onPickupItem(event: PlayerPickupItemEvent) {
        dispatch(event.item.itemStack) { it.onPickupItem(event) }
    }

    @EventHandler
    fun onDamageEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager
        if (damager is Player) {
            dispatchHands(damager) { it.onDamageEntity(event) }
        }
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val entity = event.entity
        if (entity is Player) {
            dispatchHands(entity) { it.onDamage(event) }
        }
    }

    @EventHandler
    fun onFish(event: PlayerFishEvent) {
        dispatchHands(event.player) { it.onFish(event) }
    }

    @EventHandler
    fun onShearEntity(event: PlayerShearEntityEvent) {
        dispatchHands(event.player) { it.onShearEntity(event) }
    }

    @EventHandler
    fun onHarvestBlock(event: PlayerHarvestBlockEvent) {
        dispatchHands(event.player) { it.onHarvestBlock(event) }
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val shooter = event.entity.shooter
        if (shooter is Player) {
            dispatchHands(shooter) { it.onProjectileLaunch(event) }
        }
    }

    @EventHandler
    fun onShootBow(event: EntityShootBowEvent) {
        if (event.entity !is Player) return
        dispatch(event.consumable) { it.onShootBow(event) }
    }

    @EventHandler
    fun onSwapHands(event: PlayerSwapHandItemsEvent) {
        dispatch(event.mainHandItem) { it.onSwapHands(event) }
        dispatch(event.offHandItem) { it.onSwapHands(event) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onToggleSneak(event: PlayerToggleSneakEvent) {
        dispatchHands(event.player) { it.onToggleSneak(event) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onToggleSprint(event: PlayerToggleSprintEvent) {
        dispatchHands(event.player) { it.onToggleSprint(event) }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onKillEntity(event: EntityDeathEvent) {
        val killer = event.entity.killer ?: return
        dispatchHands(killer) { it.onKillEntity(event) }
    }
}
