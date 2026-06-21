package de.redjulu.mixelPreview.items.impl.halloween

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object CondensedSoul : SpecialItem("CondensedSoul", SpecialItemCategory.HALLOWEEN) {

    override val displayName: String
        get() = "<b><gradient:#193139:#2AB3A8>Condensed Soul"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.ECHO_SHARD)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                "  <gradient:#193139:#2AB3A8>Eine Seele gemacht aus vielen...",
                "",
                " <aqua><b>🛈</b> <white>Diese Seele reisst mit einer",
                "    <white>Explosion alle <red>Monster <white>im",
                "    <white>Umkreis mit sich in den <red>Tod",
                "    <white>oder <green>rettet dich vor deinem",
                "",
                " <dark_gray><b>▸</b> <aqua>Explosion: <gray>'<yellow>Rechtsklick<gray>'",
                " <dark_gray><b>▸</b> <aqua>Wiederbelebung: <yellow>In der <gray>'<aqua>Main-<gray>' <yellow>oder <gray>'<aqua>Off-Hand<gray>' <yellow>halten"
            )
            .setEnchantmentGlintOverride(true)
            .hideAdditionalInfo()
            // .setUnenchantable(true) Bring nichts weil das nur ein PDC added und ich noch keinen Listener dafür hab (sollte ich ma machen xd)
            // .setRarity(ItemRarity.EPIC) Hey, bring hier nix lennox weil du den name overridest
            .setMaxStackSize(1)
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val action = event.action

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return
        if (event.hand != EquipmentSlot.HAND) return

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
            player.inventory.remove(item)
        }

        player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, 30 * 20, 0))
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 4))
        player.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, 10 * 20, 2))
    }


    override fun onDamage(event: EntityDamageEvent) {
        if (event.isCancelled) return
        val player = event.entity as? Player ?: return
        if (player.health + player.absorptionAmount - event.finalDamage > 0) return

        val mainHand = player.inventory.itemInMainHand
        val offHand = player.inventory.itemInOffHand

        if (SpecialItemKeys.isSpecialItem(mainHand, id)) {
            event.isCancelled = true
            mainHand.amount -= 1
            triggerSoulEffects(player)
            return
        }

        if (SpecialItemKeys.isSpecialItem(offHand, id)) {
            event.isCancelled = true
            offHand.amount -= 1
            triggerSoulEffects(player)
            return
        }
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