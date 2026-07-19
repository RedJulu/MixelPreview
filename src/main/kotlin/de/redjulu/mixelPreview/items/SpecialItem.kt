package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.items.SpecialBlockHolo
import org.bukkit.Material
import org.bukkit.block.Block
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
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class SpecialItem(
    val id: String,
    val category: SpecialItemCategory
) {
    abstract val displayName: String

    abstract fun createItem(): ItemStack

    open val placedBlock: Material? = null
    open val holoText: String? = null
    open val lockable: Boolean = false

    open fun giveMessage(): String =
        " <dark_gray>» <green>Du hast <b>$displayName</b> <green>erhalten!"

    open fun onInteract(event: PlayerInteractEvent) {}
    open fun onInteractEntity(event: PlayerInteractEntityEvent) {}
    open fun onBlockInteract(event: PlayerInteractEvent) {}
    open fun onPlace(event: BlockPlaceEvent) {}
    open fun onPlaceBlock(event: PlayerInteractEvent, block: Block) {}
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

    open fun onEnterWater(player: Player) {}
    open fun onLeaveWater(player: Player) {}

    open fun onTickMainHand(player: Player) {}
    open fun onTickOffHand(player: Player) {}
    open fun onTickHelmet(player: Player) {}
    open fun onTickChestplate(player: Player) {}
    open fun onTickLeggings(player: Player) {}
    open fun onTickBoots(player: Player) {}
    open fun onTickInInventory(player: Player) {}

    open fun onEquipMainHand(player: Player) {}
    open fun onUnequipMainHand(player: Player) {}
    open fun onEquipOffHand(player: Player) {}
    open fun onUnequipOffHand(player: Player) {}
    open fun onEquipHelmet(player: Player) {}
    open fun onUnequipHelmet(player: Player) {}
    open fun onEquipChestplate(player: Player) {}
    open fun onUnequipChestplate(player: Player) {}
    open fun onEquipLeggings(player: Player) {}
    open fun onUnequipLeggings(player: Player) {}
    open fun onEquipBoots(player: Player) {}
    open fun onUnequipBoots(player: Player) {}

    fun pickupBlock(player: Player, block: Block) {
        SpecialBlockHolo.remove(block)
        val item = createItem()
        val leftover = player.inventory.addItem(item)
        for (stack in leftover.values) {
            player.world.dropItemNaturally(player.location, stack)
        }
        block.type = Material.AIR
        SpecialItemKeys.untagBlock(block)
    }

    fun removeBlock(block: Block) {
        SpecialBlockHolo.remove(block)
        SpecialBlockLock.unlock(block)
        block.type = Material.AIR
        SpecialItemKeys.untagBlock(block)
    }

    fun lockBlock(block: Block, player: Player) {
        SpecialBlockLock.lock(block, player.uniqueId)
    }

    fun unlockBlock(block: Block) {
        SpecialBlockLock.unlock(block)
    }

    fun isLockedBlock(block: Block): Boolean =
        SpecialBlockLock.isLocked(block)


    fun build(): ItemStack = createItem()

    protected fun tag(builder: ItemBuilder.Builder): ItemBuilder.Builder =
        builder.pdc(SpecialItemKeys.itemIdKey(), PersistentDataType.STRING, id)
}