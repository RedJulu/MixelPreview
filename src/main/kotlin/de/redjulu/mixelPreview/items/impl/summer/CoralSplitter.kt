package de.redjulu.mixelPreview.items.impl.summer

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object CoralSplitter : SpecialItem("coral_splitter", SpecialItemCategory.SUMMER) {
    private val CORAL_BLOCKS = Material.entries
        .filter {
            (
                    it.name.endsWith("CORAL") ||
                            it.name.endsWith("CORAL_BLOCK") ||
                            it.name.endsWith("CORAL_FAN")
                    ) && !it.name.startsWith("DEAD_")
        }
        .toSet()

    override val displayName: String
        get() = "<white><obf>aa</obf> <b>[<yellow>Sommer <blue>'26<white>]</b> <b><gradient:#D35FB4:#F0EC60>Korallen Spalter</gradient></b> <white><obf>aa</obf>"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.NETHERITE_PICKAXE)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#D35FB4:#F0EC60>Wie viele werden es diesmal...?</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Spaltet <aqua>Korallenblöcke <white>in",
                "    <aqua>1-2 <white>(<yellow>Fächer<white>)-<yellow>Korallen <white>der",
                "    <white>jeweiligen Sorte.",
                "",
                " <dark_gray>▪ <gray>Zusätzlich werden Korallen instant abgebaut",
                "   <gray>und landen in deinem Inventar."
            )
            .addEnchant(Enchantment.UNBREAKING, 3, true)
            .addEnchant(Enchantment.SILK_TOUCH, 1, true)
            .setMaxStackSize(1)
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return
        val player = event.player

        if (event.action != Action.LEFT_CLICK_BLOCK || block.type !in CORAL_BLOCKS) return

        event.isCancelled = true

        val material = block.type
        block.type = Material.AIR

        val amount = if ((1..100).random() <= 32) 2 else 1

        val item = ItemBuilder(material)
            .setAmount(amount)
            .build()

        val leftover = player.inventory.addItem(item)

        leftover.values.forEach {
            player.world.dropItemNaturally(player.location, it)
        }

        if (amount == 2) {
            val loc = block.location.clone().add(0.5, 0.5, 0.5)
            val world = player.world

            world.spawnParticle(
                Particle.SPLASH,
                loc,
                30,
                0.35, 0.25, 0.35,
                0.05
            )

            world.spawnParticle(
                Particle.GLOW,
                loc,
                18,
                0.25, 0.25, 0.25,
                0.0
            )

            world.spawnParticle(
                Particle.BUBBLE_POP,
                loc,
                20,
                0.3, 0.2, 0.3,
                0.03
            )

            player.playSound(
                player.location,
                Sound.ENTITY_PLAYER_LEVELUP,
                0.55f,
                1.75f
            )

            player.playSound(
                player.location,
                Sound.BLOCK_AMETHYST_BLOCK_CHIME,
                0.9f,
                1.4f
            )

            player.playSound(
                player.location,
                Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                0.4f,
                1.8f
            )
        }
    }
}