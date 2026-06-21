package de.redjulu.mixelPreview.items.impl.halloween

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object CondensedSoul : SpecialItem("CondensedSoul", SpecialItemCategory.HALLOWEEN), Listener {

    override val displayName: String
        get() = "<b><gradient:#193139:#2AB3A8>Condensed Soul"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.ECHO_SHARD)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                "<gradient:#193139:#2AB3A8>eine Seele gemacht aus vielen",
                "",
                " <aqua><b>🛈</b> <white>Diese Seele reisst mit einer",
                "    <white>Explosion alle <red>Monster <white>im",
                "    <white>Umkreis mit sich in den <red>Tod",
                "    <white>oder <green>rettet dich vor deinem",
                "",
                " <dark_gray><b>▸</b> <aqua>Explosion: <gray>'<yellow>Rechtsklick<gray>'",
                " <dark_gray><b>▸</b> <aqua>revive: <gray>'<yellow>hold in <aqua>Main- <yellow>oder <aqua>Off-Hand<gray>'"
            )
            .setEnchantmentGlintOverride(true)
            .hideAdditionalInfo()
            .setUnenchantable(true)
            .setRarity(ItemRarity.EPIC)
            .setMaxStackSize(1)
    ).build()

    // --- RECHTSKLICK AKTION (EXPLOSION) ---
    override fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            event.isCancelled = true

            val loc = player.location

            loc.world?.createExplosion(loc, 0.0f, false, false)
            loc.world?.playSound(loc, Sound.BLOCK_SOUL_SAND_BREAK, 1.0f, 1.0f)

            val radius = 5.0
            val nearbyEntities = player.getNearbyEntities(radius, radius, radius)

            for (entity in nearbyEntities) {
                if (entity is Monster) {
                    entity.health = 0.0
                }
            }

            val item = event.item
            if (item != null) {
                item.amount = item.amount - 1
            }

            player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 30 * 20, 0))
            player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 4))
            player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 10 * 20, 2))
        }
    }


    @EventHandler
    fun onPlayerDeathPrevent(event: EntityResurrectEvent) {
        val player = event.entity as? Player ?: return

        val mainHand = player.inventory.itemInMainHand
        val offHand = player.inventory.itemInOffHand


        if (isCondensedSoul(mainHand)) {
            event.isCancelled = false
            mainHand.amount = mainHand.amount - 1
            triggerSoulEffects(player)
            return
        }


        if (isCondensedSoul(offHand)) {
            event.isCancelled = false
            offHand.amount = offHand.amount - 1
            triggerSoulEffects(player)
            return
        }
    }


    private fun isCondensedSoul(item: ItemStack?): Boolean {
        if (item == null || item.type != Material.ECHO_SHARD) return false
        if (!item.hasItemMeta()) return false

        val meta = item.itemMeta


        val container = meta.persistentDataContainer
        for (key in container.keys) {
            if (key.key.equals("specialitem", ignoreCase = true) || key.key.equals("id", ignoreCase = true)) {
                val value = container.get(key, PersistentDataType.STRING)
                if (value == "CondensedSoul") return true
            }
        }


        return meta.hasDisplayName() && meta.displayName.contains("Condensed Soul")
    }

    private fun triggerSoulEffects(player: Player) {
        val loc = player.location


        loc.world?.playSound(loc, Sound.ITEM_TOTEM_USE, 1.0f, 1.0f)
        loc.world?.playSound(loc, Sound.BLOCK_SOUL_SAND_BREAK, 1.2f, 0.8f)


        player.playEffect(org.bukkit.EntityEffect.valueOf("TOTEM_RESURRECT"))


        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 30 * 20, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 4))
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 10 * 20, 2))
        player.addPotionEffect(PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 20, 0))
    }
}