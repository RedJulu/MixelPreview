package de.redjulu.mixelPreview.listeners

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class ItemProtectionListener : Listener {

    private val mm = MiniMessage.miniMessage()
    private val plainText = PlainTextComponentSerializer.plainText()
    private val barrierKey = NamespacedKey(MixelPreview.instance, "anvil_barrier")

    @EventHandler
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        val view = event.view
        val item = view.getItem(0) ?: return
        if (item.type.isAir) return

        val isUnrenamable = hasPdc(item, "unrenamable")
        val isUnenchantable = hasPdc(item, "unenchantable")
        if (!isUnrenamable && !isUnenchantable) return

        val secondItem = view.getItem(1)
        val hasSecondItem = secondItem != null && !secondItem.type.isAir
        val renameText = view.renameText ?: ""
        val originalName = item.itemMeta?.displayName()
        val originalPlain = if (originalName != null) plainText.serialize(originalName) else ""
        val isActuallyRenaming = renameText != originalPlain

        if ((isUnrenamable && isActuallyRenaming) || (isUnenchantable && hasSecondItem)) {
            val barrier = ItemBuilder(Material.BARRIER)
                .setName(mm.deserialize("<red>✗ Aktion nicht möglich").decoration(TextDecoration.ITALIC, false))
                .pdc(barrierKey, PersistentDataType.BOOLEAN, true)
                .build()
            event.result = barrier
            view.repairCost = -1
        }
    }

    @EventHandler
    fun onAnvilResultClick(event: InventoryClickEvent) {
        if (event.inventory.type != InventoryType.ANVIL) return
        if (event.slotType != org.bukkit.event.inventory.InventoryType.SlotType.RESULT) return
        val result = event.currentItem ?: return
        val meta = result.itemMeta ?: return
        if (meta.persistentDataContainer.has(barrierKey, PersistentDataType.BOOLEAN)) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onEnchantItem(event: EnchantItemEvent) {
        if (hasPdc(event.item, "unenchantable")) {
            event.isCancelled = true
        }
    }

    private fun hasPdc(item: ItemStack, key: String): Boolean {
        val meta = item.itemMeta ?: return false
        return meta.persistentDataContainer.has(
            NamespacedKey(MixelPreview.instance, key),
            PersistentDataType.BOOLEAN
        )
    }
}
