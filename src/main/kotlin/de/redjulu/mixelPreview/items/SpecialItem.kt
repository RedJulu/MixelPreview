package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.utils.ItemBuilder
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
import org.bukkit.persistence.PersistentDataType

abstract class SpecialItem(
    val id: String,
    val category: SpecialItemCategory
) {
    abstract val displayName: String

    abstract fun createItem(): ItemStack

    open fun giveMessage(): String =
        " <dark_gray>» <green>Du hast <b>$displayName</b> <green>erhalten!"

    open fun onInteract(event: PlayerInteractEvent) {}
    open fun onInteractEntity(event: PlayerInteractEntityEvent) {}

    open fun onPlace(event: BlockPlaceEvent) {}

    open fun onBreak(event: BlockBreakEvent) {}

    open fun onBreakWith(event: BlockBreakEvent) {}
    open fun onItemBreak(event: PlayerItemBreakEvent) {}
    open fun onItemConsume(event: PlayerItemConsumeEvent) {}
    open fun onItemDamage(event: PlayerItemDamageEvent) {}
    open fun onItemHeld(event: PlayerItemHeldEvent) {}
    open fun onDropItem(event: PlayerDropItemEvent) {}
    open fun onPickupItem(event: PlayerPickupItemEvent) {}
    open fun onDamageEntity(event: EntityDamageByEntityEvent) {}
    open fun onDamage(event: EntityDamageEvent) {}
    open fun onFish(event: PlayerFishEvent) {}
    open fun onShearEntity(event: PlayerShearEntityEvent) {}
    open fun onHarvestBlock(event: PlayerHarvestBlockEvent) {}
    open fun onProjectileLaunch(event: ProjectileLaunchEvent) {}
    open fun onShootBow(event: EntityShootBowEvent) {}
    open fun onSwapHands(event: PlayerSwapHandItemsEvent) {}
    open fun onToggleSneak(event: PlayerToggleSneakEvent) {}
    open fun onToggleSprint(event: PlayerToggleSprintEvent) {}
    open fun onKillEntity(event: EntityDeathEvent) {}

    fun build(): ItemStack = createItem()

    protected fun tag(builder: ItemBuilder.Builder): ItemBuilder.Builder =
        builder.pdc(SpecialItemKeys.itemIdKey(), PersistentDataType.STRING, id)
}
