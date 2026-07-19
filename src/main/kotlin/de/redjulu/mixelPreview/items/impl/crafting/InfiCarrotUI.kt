package de.redjulu.mixelPreview.items.impl.crafting

import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.RechargeUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class InfiCarrotUI(hand: EquipmentSlot) : RechargeUI(
    MiniMessage.miniMessage().deserialize("<b><gradient:#4F3512:#FF9100:#FFE75E>InfiCarrot</gradient>"),
    hand
) {

    private val foods = setOf(
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

    override fun buildRechargeButton(): ItemStack = buildRechargeBtn(
        Material.BREWING_STAND,
        "<yellow>Aufladen",
        "Essen"
    )

    override fun onRecharge(player: Player, amount: Int): Boolean {
        val item = getItem(player) ?: return false
        val currentCharges = getCharges(item, InfiCarrot.foodKey)

        if (!hasInInventory(player, foods, amount) { !SpecialItemKeys.isSpecialItem(it, InfiCarrot.id) }) {
            player.sendActionBar(mm.deserialize("<red>Du hast nicht genug Essen!"))
            noFunds(player)
            return false
        }

        val removed = removeFromInventory(player, foods, amount) { !SpecialItemKeys.isSpecialItem(it, InfiCarrot.id) }
        if (removed < amount) return false

        val updated = InfiCarrot.update(InfiCarrot.createItem(), currentCharges + amount)
        setItem(player, updated)
        return true
    }
}
