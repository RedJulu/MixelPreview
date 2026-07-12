package de.redjulu.mixelPreview.items.impl.summer

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.Particle.BUBBLE
import org.bukkit.Particle.DRIPPING_WATER
import org.bukkit.Particle.FALLING_WATER


object PoseidonPickaxe : SpecialItem("poseidon_pickaxe", SpecialItemCategory.SUMMER) {
    override val displayName: String
        get() = "<white><obf>aa</obf> <b>[<yellow>Sommer <blue>'26<white>]</b> <b><gradient:#17DED6:#2EEE62>Poseidons Spitzhacke</gradient></b> <white><obf>aa</obf>"



    private val mm = MiniMessage.miniMessage()

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.NETHERITE_PICKAXE)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <gradient:#17DED6:#2EEE62>Mache dir Poseidons Kraft zu eigen!</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit dieser Spitzhacke bekommst",
                "    <white>du einen <green>Miningspeed boost <white>wenn",
                "    <white>du dich <blue>unterwasser <white>befindest",

            )
            .setEnchantmentGlintOverride(true)
            .setMaxStackSize(1)
            .setUnrenamable(true)
            .setUnenchantable(true)
    ).build()

    override fun onTickMainHand(player: Player) {
        if (!player.isUnderWater) return

        player.addPotionEffect(PotionEffect(PotionEffectType.CONDUIT_POWER, 2, 3, false, false, false))
    }

    override fun onEnterWater(player: Player) {
        player.sendActionBar(mm.deserialize("<Aqua>Speedboost <green>Aktiviert!!!"))

    }

    override fun onLeaveWater(player: Player) {
        player.sendActionBar(mm.deserialize("<Aqua>Speedboost <red>Deaktiviert!!!"))

    }

    override fun onBreakWith(event: BlockBreakEvent) {
        if (!event.player.isUnderWater) return

        val block = event.block
        val world = block.world
        val location = block.location.add(0.5, 0.5, 0.5)

        world.spawnParticle(
            FALLING_WATER,
            location,
            50,
            0.5, 0.5, 0.5,
            5.0,

        )
    }

}