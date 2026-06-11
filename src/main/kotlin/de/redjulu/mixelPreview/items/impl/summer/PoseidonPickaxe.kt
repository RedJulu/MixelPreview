package de.redjulu.mixelPreview.items.impl.summer

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


object PoseidonPickaxe : SpecialItem("poseidon_pickaxe", SpecialItemCategory.SUMMER) {
    override val displayName: String
        get() = "<gradient:#17DED6:#17DED6>P<gradient:#17DED6:#2EEE62>oseido<gradient:#2EEE62:#FFF800>ns Spitzha<gradient:#FFF800:#FFF800>cke"



    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.NETHERITE_PICKAXE)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <gradient:#BEFF00:#BEFF00>Ma<gradient:#BEFF00:#00FFEA>che dir Poseidons Kraf<gradient:#00FFEA:#00FFEA>t zu eigen!",
                "",
                " <aqua><b>🛈</b> <white>Mit dieser Spitzhacke bekommst",
                "    <white>du einen <green>Miningspeed boost <white>wenn",
                "    <white>du dich <blue>unterwasser <white>befindest",

            )
    ).build()

    override fun onTick(player: Player) {
        if (!player.isUnderWater) return

        player.addPotionEffect(PotionEffect(PotionEffectType.CONDUIT_POWER, 2, 1))
    }


}