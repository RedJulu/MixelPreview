package de.redjulu.mixelPreview.items.impl.crafting

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Firework
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object RocketBox : SpecialItem("rocket_box", SpecialItemCategory.CRAFTING) {

    private val mm = MiniMessage.miniMessage()

    override val displayName: String
        get() = "<b><gradient:#E73232:#274FD4>Raketen Schachtel</gradient> <dark_gray>[<yellow>1/</b><yellow>∞<b><dark_gray>]</b>"

    val rocketsKey = NamespacedKey(MixelPreview.instance, "rocketbox_rockets")

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.FIREWORK_ROCKET)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#E73232:#274FD4>Bumm, Bumm und noch mehr Bumm \uD83C\uDF86</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Du kannst diese Rakete aufladen",
                "    <white>und sie dann so oft nutzen",
                "    <white>wie aufgeladen",
                "",
                " <dark_gray>▪ <gray>Aufladen: '<yellow>Sneak-Rechtsklick<gray>'"
            )
            .pdc(rocketsKey, PersistentDataType.INTEGER, 1)
            .setEnchantmentGlintOverride(true)
            .setUnrenamable(true)
            .setUnenchantable(true)
            .hideAdditionalInfo()
            .setMaxStackSize(1)
    ).build()

    fun update(item: ItemStack, newAmount: Int): ItemStack {
        return ItemBuilder(item)
            .setName("<b><gradient:#E73232:#274FD4>Raketen Schachtel</gradient> <dark_gray>[<yellow>$newAmount/</b><yellow>∞<b><dark_gray>]</b>")
            .pdc(rocketsKey, PersistentDataType.INTEGER, newAmount)
            .build()
    }

    override fun onInteract(event: PlayerInteractEvent) {
        event.isCancelled = true
        val player = event.player
        val isOffHand = event.hand == EquipmentSlot.OFF_HAND

        when(event.action) {
            Action.RIGHT_CLICK_BLOCK -> {
                if (player.isSneaking) {
                    RocketBoxUI(event.hand ?: EquipmentSlot.HAND).open(player, false)
                    return
                }
                handleRocket(player, isOffHand, event.clickedBlock)
            }

            Action.RIGHT_CLICK_AIR -> {
                if (player.isSneaking) {
                    RocketBoxUI(event.hand ?: EquipmentSlot.HAND).open(player, false)
                    return
                }
                if (!player.isGliding) return
                handleRocket(player, isOffHand)
            }
            else -> return
        }
    }

    private fun handleRocket(player: Player, isOffHand: Boolean, clickedBlock: Block? = null) {
        if (player.hasCooldown(Material.FIREWORK_ROCKET)) return

        val item = if (isOffHand) player.inventory.getItemInOffHand() else player.itemInHand
        val charges = item.persistentDataContainer.get(rocketsKey, PersistentDataType.INTEGER) ?: 0

        if (charges == 0) {
            player.setCooldown(Material.FIREWORK_ROCKET, 10)
            player.sendActionBar(mm.deserialize("<red>Deine <gradient:#E73232:#274FD4>Raketen Schachtel</gradient> ist leer!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        if (!player.isGliding) {
            player.setCooldown(Material.FIREWORK_ROCKET, 10)
            player.world.spawn(clickedBlock?.location ?: player.location, Firework::class.java)
            val updated = update(item, charges - 1)
            if (isOffHand) player.inventory.setItemInOffHand(updated) else player.setItemInHand(updated)
            return
        }

        player.setCooldown(Material.FIREWORK_ROCKET, 10)
        player.fireworkBoost(ItemBuilder(Material.FIREWORK_ROCKET).setFireworkStrength(3).build())
        val updated = update(item, charges - 1)
        if (isOffHand) player.inventory.setItemInOffHand(updated) else player.setItemInHand(updated)
    }
}