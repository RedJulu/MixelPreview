package de.redjulu.mixelPreview.items.impl.summer

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import io.papermc.paper.datacomponent.item.Equippable
import io.papermc.paper.datacomponent.item.Equippable.equippable
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.block.spawner.SpawnerEntry
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import javax.naming.directory.Attributes
import org.bukkit.inventory.ItemFlag

object PoseidonKroneRework : SpecialItem("Poseidon_Krone", SpecialItemCategory.SUMMER) {

    override val displayName: String
        get() = "<white><obf>aa</obf> <b>[<yellow>Sommer <white>]</b> <b><gradient:#17DED6:#17DED6>P<gradient:#17DED6:#2EEE62>oseido<gradient:#2EEE62:#FFF800>ns Kro<gradient:#FFF800:#FFF800>ne </b> <white><obf>aa</obf>"


    private val mm = MiniMessage.miniMessage()


    override fun createItem(): ItemStack = tag (
        ItemBuilder(Material.MEDIUM_AMETHYST_BUD)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                "<gradient:#BEFF00:#BEFF00>Werd<gradient:#BEFF00:#3FFF00>e der K<gradient:#3FFF00:#00FFEA>önig de<gradient:#00FFEA:#00FFEA>r Meere",
                "",
                "<aqua><b>🛈</b><white>Diese Krone gibt dir",
                "<white>unvorstellbare <yellow>Macht <white>solange",
                "<white>du dich <blue>unterwasser <white>befindest",
            )

            .addEnchant(Enchantment.AQUA_AFFINITY, 5, true)
            .addEnchant(Enchantment.RESPIRATION, 5, true)
            .setUnrenamable(true)
            .setEquippable(EquipmentSlot.HEAD)
            .setMaxStackSize(1)
            .hideEnchants()
            .hideAdditionalInfo()
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)

    ).build()

    override fun onTick(player: Player) {
        if (!player.isInRain) return

        player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 2, 0, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 2, 0, false, false, false))

        if (!player.isUnderWater) return

        player.addPotionEffect(PotionEffect(PotionEffectType.ABSORPTION, 2, 1, false, false, false))
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 2, 1, false, false, false))

    }




}