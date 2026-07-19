package de.redjulu.mixelPreview.items.impl.crafting

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object InfiCarrot : SpecialItem ("InfiCarrot", SpecialItemCategory.CRAFTING){

    private val mm = MiniMessage.miniMessage()

    override val displayName: String
        get() = "<b><gradient:#4F3512:#FF9100:#FFE75E>InfiCarrot</gradient> <dark_gray>[<yellow>1/</b><yellow>∞<b><dark_gray>]</b>"

    val foodKey = NamespacedKey(MixelPreview.instance, "inficarrot_ladung")

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.GOLDEN_CARROT)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸ </b><gradient:#4F3512:#FF9100:#FFE75E>Ein Happen und noch einer :P</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Du kannst diese <gold>Goldene Karotte <white>mit",
                "    <white>jedem <green>essbaren Item <white>aufladen und",
                "    <white>dann so oft <green>essen <white>wie <red>aufgeladen",
                "",
                " <dark_gray>▪ <gray>Aufladen: '<yellow>Sneak-Rechtsklick<gray>'"
            )
            .pdc(foodKey, PersistentDataType.INTEGER, 1)
            .setEnchantmentGlintOverride(true)
            .setUnrenamable(true)
            .setUnenchantable(true)
            .hideAdditionalInfo()
            .setMaxStackSize(1)
    ).build()

    fun update(item: ItemStack, newAmount: Int): ItemStack {
        return ItemBuilder(item)
            .setName("<b><gradient:#4F3512:#FF9100:#FFE75E>InfiCarrot</gradient> <dark_gray>[<yellow>$newAmount/</b><yellow>∞<b><dark_gray>]</b>")
            .pdc(foodKey, PersistentDataType.INTEGER, newAmount)
            .build()
    }

    override fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        if (event.player.isSneaking) {
            InfiCarrotUI(event.hand ?: EquipmentSlot.HAND).open(event.player, false)
            event.isCancelled = true
        }

    }

    override fun onItemConsume(event: PlayerItemConsumeEvent) {
        event.isCancelled = true

        val player = event.player
        val item = if (event.hand == EquipmentSlot.HAND) player.itemInHand else player.inventory.itemInOffHand
        val charges = item.persistentDataContainer.get(foodKey, PersistentDataType.INTEGER) ?: 0

        if (charges == 0) {
            player.sendActionBar(mm.deserialize("<red>Deine <b><gradient:#4F3512:#FF9100:#FFE75E>InfiCarrot</gradient> <red></b>ist nicht Infi!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        player.foodLevel += 6
        player.saturation += 10

        val updated = update(item, charges-1)
        if (event.hand == EquipmentSlot.HAND) player.inventory.setItemInHand(updated) else player.inventory.setItemInOffHand(updated)

    }
}