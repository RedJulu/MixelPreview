package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.SimpleGUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import java.util.function.Predicate

class GroupDetailGUI(private val group: GroupedItem) : SimpleGUI<SpecialItem, Unit>(
    rows = ((group.items.size + 6) / 7 + 2).coerceIn(3, 6),
    title = MiniMessage.miniMessage().deserialize("<black>» </black><b>${group.name}</b>"),
    t = 1, b = 1, l = 1, r = 1
) {
    private val mm = MiniMessage.miniMessage()
    private val bottomRow: Int get() = inventory.size - 9

    override fun compose(player: Player) {
        fillBackground(Material.GRAY_STAINED_GLASS_PANE)

        setButton(0, ItemBuilder(Material.DARK_OAK_DOOR)
            .setName("<gray>↩ Zurück</gray>")
            .setMiniMessageLore("", " <gray>Klicke um zurück zu", " <gray>allen Items zu gehen")
            .build()
        ) { p, _ -> back(p) }

        allItems = group.items.toMutableList()
        val filter = Predicate<SpecialItem> { true }

        renderPage(filter) { item, slot ->
            val stack = item.createItem()
            val display = ItemBuilder(stack)
                .setLore(listOf(
                    mm.deserialize(""),
                    mm.deserialize(" <green>▸ <gray>Klicke zum Nehmen")
                ))
                .addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE)
                .build()
            setButton(slot, display) { p, _ -> giveItem(p, item) }
        }

        addPaginationButtons(bottomRow + 3, bottomRow + 5, player, filter)

        if (group.items.size > 1) {
            setButton(bottomRow + 4, ItemBuilder(Material.CHEST)
                .setName("<green>Alle nehmen</green>")
                .setMiniMessageLore("", " <gray>Nimmt alle <b>${group.items.size}</b> Items", " <gray>auf einmal")
                .build()
            ) { p, _ -> giveAll(p) }
        }
    }

    private fun giveItem(player: Player, item: SpecialItem) {
        val stack = item.build()
        player.inventory.addItem(stack).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
        player.sendMessage(mm.deserialize(item.giveMessage()))
        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)
    }

    private fun giveAll(player: Player) {
        for (item in group.items) {
            val stack = item.build()
            player.inventory.addItem(stack).values.forEach { leftover ->
                player.world.dropItemNaturally(player.location, leftover)
            }
        }
        player.sendMessage(mm.deserialize(" <dark_gray>» <green>Du hast alle <b>${group.items.size}</b> Items erhalten!"))
        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)
    }
}
