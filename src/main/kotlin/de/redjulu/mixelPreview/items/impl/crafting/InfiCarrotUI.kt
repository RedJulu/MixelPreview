package de.redjulu.mixelPreview.items.impl.crafting

import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.RechargeUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class InfiCarrotUI(hand: EquipmentSlot) : RechargeUI(
    MiniMessage.miniMessage().deserialize("<b><gradient:#4F3512:#FF9100:#FFE75E>InfiCarrot</gradient>"),
    hand
) {
    override fun buildDisplay(item: ItemStack): ItemStack = item

    override fun buildInfo(): ItemStack = ItemBuilder(Material.PLAYER_HEAD)
            .setName("<green>Information")
            .setMiniMessageLore(
                "",
                " <aqua><b>🛈</b> <white>Du kannst mit dem <aqua>Braustand",
                "    <white>die <b><gradient:#4F3512:#FF9100:#FFE75E>InfiCarrot</gradient> aufladen und",
                "    <white>diese dann so oft nutzen.",
                ""
            )
            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzU3NDcwMTBkODRhYTU2NDgzYjc1ZjYyNDNkOTRmMzRjNTM0NjAzNTg0YjJjYzY4YTQ1YmYzNjU4NDAxMDVmZCJ9fX0=")
            .build()


    override fun buildRechargeButton(): ItemStack = ItemBuilder(Material.BREWING_STAND)
        .setName("<yellow>Aufladen")
        .setMiniMessageLore(
            "",
            " <dark_gray><b>▸</b> <yellow>Kaufe hier eine weitere Aufladung",
            "",
            " <dark_gray>▪ <gray>1 Aufladungen: '<yellow>1 Golden Carrot<gray>' <dark_gray>» <aqua>Linksklick",
            " <dark_gray>▪ <gray>32 Aufladungen: '<yellow>32 Golden Carrot<gray>' <dark_gray>» <aqua>Rechtsklick"
        )
        .build()

    override fun onRecharge(player: Player, amount: Int): Boolean {
        val inv = player.inventory

        val currentCharges = getItem(player)
            ?.persistentDataContainer
            ?.get(InfiCarrot.foodKey, PersistentDataType.INTEGER) ?: return false

        val foods = setOf(
            Material.BREAD,
            Material.GOLDEN_CARROT,
            Material.COOKED_MUTTON,
            Material.COOKED_PORKCHOP,
            Material.COOKED_BEEF,
            Material.COOKED_CHICKEN,
            Material.COOKED_SALMON,
            Material.COOKED_COD,
            Material.BAKED_POTATO,
            Material.CAKE,
        )

        var charges = 0
        for (i in 0 until inv.size) {
            val stack = inv.getItem(i) ?: continue
            if (stack.type !in foods) continue
            if (SpecialItemKeys.isSpecialItem(stack, InfiCarrot.id)) continue
            charges += stack.amount
        }

        if (charges < amount) {
            player.sendActionBar(mm.deserialize("<red>Du hast nicht genug Essen!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return false
        }

        removeItem(player)

        var remaining = amount
        for (i in 0 until inv.size) {
            if (remaining <= 0) break
            val stack = inv.getItem(i) ?: continue
            if (stack.type !in foods) continue
            if (SpecialItemKeys.isSpecialItem(stack, InfiCarrot.id)) continue
            val toRemove = minOf(remaining, stack.amount)
            if (toRemove >= stack.amount) {
                inv.setItem(i, null)
            } else {
                stack.amount -= toRemove
            }
            remaining -= toRemove
        }

        val updated = InfiCarrot.update(InfiCarrot.createItem(), currentCharges + amount)
        setItem(player, updated)
        return true
    }
}