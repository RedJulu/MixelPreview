package de.redjulu.mixelPreview.items.impl.job

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.event.block.Action
import org.bukkit.Location
import org.bukkit.event.block.BlockBreakEvent

object MagnetPickaxe : SpecialItem("magnet_pickaxe", SpecialItemCategory.JOB) {
    override val displayName: String
        get() = "<b><gradient:#831010:#868686:#404040>Magnet Pickaxe</gradient>"

    private val mm = MiniMessage.miniMessage()

    override fun createItem(): ItemStack = tag (
        ItemBuilder(Material.NETHERITE_PICKAXE)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                "<gradient:#831010:#868686:#404040>Zieht alle Erze in der nähe an</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Baue <gray>Erze <white>ab und baut <dark_red>automatisch,",
                "    <gray>Erze <white>der selben Art in der nähe ab",
            )
            .setUnbreakable(true)
            .setUnenchantable(true)
            .setUnrenamable(true)
            .addEnchant(Enchantment.EFFICIENCY, 1, true)
            .addEnchant(Enchantment.FORTUNE, 1, true)
    ).build()


    val ores = mapOf<Material, Material>(
        Material.DIAMOND_ORE to Material.STONE,
        Material.DEEPSLATE_DIAMOND_ORE to Material.DEEPSLATE,
        Material.EMERALD_ORE to Material.STONE,
        Material.DEEPSLATE_EMERALD_ORE to Material.DEEPSLATE,
        Material.IRON_ORE to Material.STONE,
        Material.DEEPSLATE_IRON_ORE to Material.DEEPSLATE,
        Material.GOLD_ORE to Material.STONE,
        Material.DEEPSLATE_GOLD_ORE to Material.DEEPSLATE,
        Material.COAL_ORE to Material.STONE,
        Material.DEEPSLATE_COAL_ORE to Material.DEEPSLATE,
        Material.COPPER_ORE to Material.STONE,
        Material.DEEPSLATE_COPPER_ORE to Material.DEEPSLATE,
        Material.LAPIS_ORE to Material.STONE,
        Material.DEEPSLATE_LAPIS_ORE to Material.DEEPSLATE,
        Material.NETHER_GOLD_ORE to Material.NETHERRACK,
        Material.ANCIENT_DEBRIS to Material.AIR,
        Material.NETHER_QUARTZ_ORE to Material.NETHERRACK,
    )

    override fun onBreakWith(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        val material = block.type

        event.isCancelled = true

        if (material !in ores.keys) {
            player.sendActionBar(mm.deserialize("<red>Das ist <b>kein <dark_gray>Erz </b><red>du <b>Hurensohn"))
            return
        }

        val origin = block.location
        val toMine = mutableSetOf(origin)
        val queue = ArrayDeque(listOf(origin))
        val checked = mutableSetOf(origin)

        while (queue.isNotEmpty() && toMine.size < 20) {
            val current = queue.removeFirst()
            var dx = -1
            while (dx <= 1 && toMine.size < 20) {
                var dy = -1
                while (dy <= 1 && toMine.size < 20) {
                    var dz = -1
                    while (dz <= 1 && toMine.size < 20) {
                        if (dx != 0 || dy != 0 || dz != 0) {
                            val loc = current.clone().add(dx.toDouble(), dy.toDouble(), dz.toDouble())
                            if (checked.add(loc) && loc.block.type in ores.keys) {
                                queue.add(loc)
                                toMine.add(loc)
                            }
                        }
                        dz++
                    }
                    dy++
                }
                dx++
            }
        }

        val itemInHand = player.inventory.itemInMainHand
        toMine.forEach { loc ->
            val b = loc.block
            val bType = b.type
            val replacement = ores[bType] ?: Material.AIR
            b.getDrops(itemInHand).forEach { drop ->
                player.inventory.addItem(drop).values.forEach { leftover ->
                    player.world.dropItemNaturally(loc, leftover)
                }
            }
            b.type = replacement
        }
    }
}