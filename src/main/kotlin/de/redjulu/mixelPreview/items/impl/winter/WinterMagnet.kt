package de.redjulu.mixelPreview.items.impl.winter

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

object WinterMagnet : SpecialItem("winter_magnet", SpecialItemCategory.WINTER) {

    private const val RADIUS = 5.0

    private val dropCooldowns = mutableMapOf<UUID, Long>()

    override val displayName = "<dark_red><obf>kk</obf> <white><b>[<aqua>Winter <red>'23<white>] <#f57c00>Spekulatius <dark_red>Mag<dark_blue>net <dark_gray>[<red>▪<dark_gray>]</b> <dark_red><obf>kk</obf>"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.IRON_INGOT)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <gray><b>●</b><i> <green>1, 2, 3 - Item komm herbei!",
                "",
                " <gray><b>▸</b> <white>Dieser Magnet wurde mit <#f57c00>Spekulatius",
                "   <white>verfeinert, sodass er sämtliche Items",
                "   <white>in einem <yellow>5-Block Radius <white>anzieht,",
                "   <white>während er gehalten wird!",
                "",
            )
            .setEnchantmentGlintOverride(true)
            .setMaxStackSize(1)
            .setUnrenamable(true)
            .setUnenchantable(true)
    ).build()

    override fun onDropItem(event: PlayerDropItemEvent) {
        dropCooldowns[event.itemDrop.uniqueId] = System.currentTimeMillis() + 1000
    }

    override fun onTickMainHand(player: Player) = pullItems(player)

    override fun onTickOffHand(player: Player) = pullItems(player)

    private fun pullItems(player: Player) {
        val now = System.currentTimeMillis()

        player.world.getNearbyEntities(player.location, RADIUS, RADIUS, RADIUS).forEach { entity ->
            if (entity is Item) {
                val cooldown = dropCooldowns[entity.uniqueId]
                if (cooldown != null) {
                    if (now < cooldown) return@forEach
                    dropCooldowns.remove(entity.uniqueId)
                }

                val direction = player.location.toVector().subtract(entity.location.toVector())
                val distance = direction.length()
                if (distance < 1.0) return@forEach
                val strength = 0.06 + (RADIUS - distance) / RADIUS * 0.25
                entity.velocity = direction.normalize().multiply(strength)
            }
        }
    }
}
