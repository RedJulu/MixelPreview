package de.redjulu.mixelPreview.items.impl.crate.creativeAxe

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.types.BlockDataType
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.entity.Display
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.BoundingBox
import kotlin.math.max
import kotlin.math.min

object CreativeAxe : SpecialItem("creative_axe", SpecialItemCategory.CRATE) {
    override val displayName: String
        get() = "<b><gradient:#CC1DDE:#9836D4:#446CBB:#6BCD67>Kreative Axt</gradient></b>"

    var block1Key = NamespacedKey(MixelPreview.instance, "creativeaxe_block1")
    var block2Key = NamespacedKey(MixelPreview.instance, "creativeaxe_block2")

    private val mm = MiniMessage.miniMessage()

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.NETHERITE_AXE)
            .setName(displayName)
            .setMiniMessageLore(
                " <dark_gray><b>▸</b> <white>Diese Axt erleichtert viele <green>Bauprojekte<white>!",
                "   <white>Man kann <yellow>zwei Stellen <white>markieren und",
                "   <white>zwischen diesen Orten <yellow>automatisch",
                "   <blue>Blöcke <white>aufbauen lassen.",
                "",
                " <dark_gray><b>▸</b> <white>Die Blöcke müssen im <yellow>Inventar",
                "   <white>vorhanden sein.",
                "",
                " <dark_gray><b>▸</b> <aqua>Weitere Infos: <gray>'<yellow>Sneak-Linksklick<gray>'"
            )
            .setEnchantmentGlintOverride(true)
            .setMaxStackSize(1)
            .setUnrenamable(true)
            .setUnenchantable(true)
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = player.itemInHand
        val clicked = event.clickedBlock

        event.isCancelled = true

        when(event.action) {
            Action.LEFT_CLICK_AIR -> {
                if (player.isSneaking) CreativeAxeUI(item).openDialog(player)
            }

            Action.LEFT_CLICK_BLOCK -> {
                if (player.isSneaking) { CreativeAxeUI(item).openDialog(player); return}

                setPosition(player, block1Key, clicked, item)
            }

            Action.RIGHT_CLICK_AIR -> {

                if (player.isSneaking) {
                    val loc1 = item.itemMeta?.persistentDataContainer?.get(block1Key, BlockDataType)
                    val loc2 = item.itemMeta?.persistentDataContainer?.get(block2Key, BlockDataType)

                    if (loc1 == null || loc2 == null || loc1.world != loc2.world) {
                        player.sendActionBar(mm.deserialize("<dark_gray>» <red>Keine Selektion vorhanden."))
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                        return
                    }

                    if (hasEntitiesInSelection(item)) {
                        player.sendActionBar(mm.deserialize("<dark_gray>» <red>Die Selection ist nicht leer."))
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                        return
                    }

                    CreativeAxeUI(player.inventory.itemInHand).open(player, false)
                    return
                }

                val loc1 = item.itemMeta?.persistentDataContainer?.get(block1Key, BlockDataType)
                val loc2 = item.itemMeta?.persistentDataContainer?.get(block2Key, BlockDataType)

                val pos1Text = loc1?.let { "<gray>X: <yellow>${it.blockX} <gray>Y: <yellow>${it.blockY} <gray>Z: <yellow>${it.blockZ} <dark_gray>(<gray>${it.world.name}<dark_gray>)" } ?: "<gray><i>Nicht gesetzt</i>"
                val pos2Text = loc2?.let { "<gray>X: <yellow>${it.blockX} <gray>Y: <yellow>${it.blockY} <gray>Z: <yellow>${it.blockZ} <dark_gray>(<gray>${it.world.name}<dark_gray>)" } ?: "<gray><i>Nicht gesetzt</i>"

                player.sendMessage(mm.deserialize("<dark_gray>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"))
                player.sendMessage(mm.deserialize("<green><b>Kreative Axt</b> <dark_gray>» <gray>Aktuelle Markierungen:"))
                player.sendMessage(mm.deserialize(" <dark_gray>▸ <green>Position 1: $pos1Text"))
                player.sendMessage(mm.deserialize(" <dark_gray>▸ <green>Position 2: $pos2Text"))
                player.sendMessage(mm.deserialize("<dark_gray>⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯"))
            }

            Action.RIGHT_CLICK_BLOCK -> {
                if (player.isSneaking) {
                    if (hasEntitiesInSelection(item)) {
                        player.sendActionBar(mm.deserialize("<dark_gray>» <red>Die Selection ist nicht leer."))
                        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                        return
                    }

                    CreativeAxeUI(player.inventory.itemInHand).open(player, false)
                    return
                }

                setPosition(player, block2Key, clicked, item)
            }

            else -> return
        }
    }

    private fun setPosition(player: Player, key: NamespacedKey, block: Block?, item: ItemStack) {
        val newItem = ItemBuilder(item)
            .pdc(key, BlockDataType, block?.location ?: run {
                player.sendActionBar(mm.deserialize("<red>Interner Fehler :("))
                return
            })
            .build()

        val pointId = if (key == block1Key) 1 else 2
        val x = block.location.x.toString().removeSuffix(".0")
        val y = block.location.y.toString().removeSuffix(".0")
        val z = block.location.z.toString().removeSuffix(".0")

        player.setItemInHand(newItem)
        player.sendActionBar(mm.deserialize("<green>Punkt $pointId <dark_gray>» <gray>X: <yellow>$x <gray>Y: <yellow>$y <gray>Z: <yellow>$z"))

        CreativeAxeVisualizer.startVisualizer(player)
    }

    fun hasEntitiesInSelection(item: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val loc1 = meta.persistentDataContainer.get(block1Key, BlockDataType) ?: return false
        val loc2 = meta.persistentDataContainer.get(block2Key, BlockDataType) ?: return false

        if (loc1.world != loc2.world) return false

        val minX = min(loc1.blockX, loc2.blockX).toDouble()
        val minY = min(loc1.blockY, loc2.blockY).toDouble()
        val minZ = min(loc1.blockZ, loc2.blockZ).toDouble()
        val maxX = max(loc1.blockX, loc2.blockX) + 1.0
        val maxY = max(loc1.blockY, loc2.blockY) + 1.0
        val maxZ = max(loc1.blockZ, loc2.blockZ) + 1.0

        val box = BoundingBox(minX, minY, minZ, maxX, maxY, maxZ)

        return loc1.world.getNearbyEntities(box) { entity ->
            entity !is Display && entity !is Item
        }.isNotEmpty()
    }

    fun clearSelection(player: Player) {
        val item = player.inventory.itemInMainHand
        val meta = item.itemMeta ?: return
        meta.persistentDataContainer.remove(block1Key)
        meta.persistentDataContainer.remove(block2Key)
        item.itemMeta = meta
        CreativeAxeVisualizer.stopVisualizer(player)
        player.sendActionBar(mm.deserialize("<green>Selektion geleert!"))
    }
}