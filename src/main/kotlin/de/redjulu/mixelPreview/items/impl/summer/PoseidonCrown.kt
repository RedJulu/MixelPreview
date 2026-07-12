package de.redjulu.mixelPreview.items.impl.summer

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.inventory.ItemFlag

object PoseidonCrown : SpecialItem("Poseidon_Krone", SpecialItemCategory.SUMMER) {

    override val displayName: String
        get() = "<white><obf>aa</obf> <b>[<yellow>Sommer <blue>'26<white>]</b> <b><gradient:#17DED6:#2EEE62>Poseidons Krone</gradient></b> <white><obf>aa</obf>"


    private val mm = MiniMessage.miniMessage()


    override fun createItem(): ItemStack = tag (
        ItemBuilder(Material.MEDIUM_AMETHYST_BUD)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                "<gradient:#17DED6:#2EEE62>Werde der König der Meere...</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Diese Krone gibt dir",
                "  <white>unvorstellbare <yellow>Macht <white>solange",
                "  <white>du dich <blue>unterwasser <white>befindest",
            )

            .addEnchant(Enchantment.AQUA_AFFINITY, 5, true)
            .addEnchant(Enchantment.RESPIRATION, 5, true)
            .setUnrenamable(true)
            .setUnenchantable(true)
            .setEquippable(EquipmentSlot.HEAD)
            .setMaxStackSize(1)
            .hideEnchants()
            .hideAdditionalInfo()
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .setEnchantmentGlintOverride(true)

    ).build()

    override fun onTickHelmet(player: Player) {
        when {
            player.isUnderWater -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 2, 1, false, false, false))
                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 2, 1, false, false, false))
            }
            player.isInRain -> {
                player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 2, 0, false, false, false))
                player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 2, 0, false, false, false))
            }
        }
    }

    override fun onPlace(event: BlockPlaceEvent) {
        event.isCancelled = true
    }
}