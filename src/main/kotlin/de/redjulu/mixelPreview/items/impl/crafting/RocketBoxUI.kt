package de.redjulu.mixelPreview.items.impl.crafting

import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.RechargeUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

class RocketBoxUI(hand: EquipmentSlot) : RechargeUI(
    MiniMessage.miniMessage().deserialize("<b><gradient:#E73232:#274FD4>Raketen Schachtel</gradient></b>"),
    hand,
    1,
    32
) {
    override fun buildDisplay(item: ItemStack): ItemStack = item

    override fun buildInfo(): ItemStack = ItemBuilder(Material.PLAYER_HEAD)
        .setName("<green>Information")
        .setMiniMessageLore(
            "",
            " <aqua><b>🛈</b> <white>Du kannst mit dem <aqua>Braustand",
            "    <white>die <gradient:#E73232:#274FD4>Raketen Schachtel</gradient> aufladen und",
            "    <white>diese dann so oft nutzen.",
            ""
        )
        .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzU3NDcwMTBkODRhYTU2NDgzYjc1ZjYyNDNkOTRmMzRjNTM0NjAzNTg0YjJjYzY4YTQ1YmYzNjU4NDAxMDVmZCJ9fX0=")
        .build()

    override fun buildRechargeButton(): ItemStack = buildRechargeBtn(
        Material.BREWING_STAND,
        "<yellow>Aufladen",
        "Rakete"
    )

    override fun onRecharge(player: Player, amount: Int): Boolean {
        val item = getItem(player) ?: return false
        val currentCharges = getCharges(item, RocketBox.rocketsKey)

        if (!hasInInventory(player, Material.FIREWORK_ROCKET, amount) { !SpecialItemKeys.isSpecialItem(it, RocketBox.id) }) {
            player.sendActionBar(mm.deserialize("<red>Du hast nicht genug Raketen!"))
            noFunds(player)
            return false
        }

        val removed = removeFromInventory(player, Material.FIREWORK_ROCKET, amount) { !SpecialItemKeys.isSpecialItem(it, RocketBox.id) }
        if (removed < amount) return false

        val updated = RocketBox.update(RocketBox.createItem(), currentCharges + amount)
        setItem(player, updated)
        return true
    }
}
