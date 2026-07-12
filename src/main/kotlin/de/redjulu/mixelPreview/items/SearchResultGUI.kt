package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.gui.SpecialItemsGUI
import de.redjulu.mixelPreview.utils.DialogBuilder
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.SimpleGUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.function.Predicate

class SearchResultGUI(private val query: String) : SimpleGUI<SpecialItem, Unit>(
    rows = SearchResultGUI.calcRows(query),
    title = MiniMessage.miniMessage().deserialize("<black>» <gradient:#79BCD7:#8554B6>🔍 $query</gradient>"),
    t = 1, b = 1, l = 1, r = 1
) {
    private val mm = MiniMessage.miniMessage()
    private val results = SearchResultGUI.search(query)
    private val bottomRow: Int get() = inventory.size - 9

    override fun compose(player: Player) {
        fillBackground(Material.GRAY_STAINED_GLASS_PANE)

        setButton(0, ItemBuilder(Material.DARK_OAK_DOOR)
            .setName("<gray>↩ Zurück</gray>")
            .setMiniMessageLore("", " <gray>Klicke um zurück zu", " <gray>Special Items")
            .build()
        ) { p, _ ->
            p.closeInventory()
            SpecialItemsGUI().open(p, saveToHistory = false)
        }

        allItems = results.toMutableList()

        if (results.isEmpty()) {
            val center = inventory.size / 2
            setButton(center, ItemBuilder(Material.BARRIER)
                .setName("<i><red>Keine Ergebnisse für <b>$query</b></red></i>")
                .build()
            )
        } else {
            val filter = Predicate<SpecialItem> { true }

            renderPage(filter) { item, slot ->
                setButton(slot, item.build()) { p, _ ->
                    giveItem(p, item)
                }
            }

            addPaginationButtons(bottomRow + 3, bottomRow + 5, player, filter)
        }

        setButton(bottomRow + 4, ItemBuilder(Material.COMPASS)
            .setName("<b><aqua>🔍 Neue Suche</aqua></b>")
            .setMiniMessageLore("", " <gray>Erneute Suche starten")
            .build()
        ) { p, _ -> openNewSearch(p) }
    }

    private fun openNewSearch(player: Player) {
        DialogBuilder.create(
            mm.deserialize("<dark_gray>» <gradient:#79BCD7:#8554B6>Special Items</gradient> <dark_gray>[<aqua>Suche<dark_gray>]")
        )
            .addTextInput("query", mm.deserialize("<gold>Suchbegriff"))
            .addButton(mm.deserialize("<green>🔍 Suchen")) { ctx, _ ->
                val raw = ctx.getText("query")
                if (raw.isNullOrBlank()) return@addButton
                SearchResultGUI(raw.trim()).open(player, saveToHistory = false)
            }
            .show(player)
    }

    private fun giveItem(player: Player, item: SpecialItem) {
        val stack = item.build()
        player.inventory.addItem(stack).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
        player.sendMessage(mm.deserialize(item.giveMessage()))
        player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)
    }

    companion object {
        fun search(query: String): List<SpecialItem> {
            val q = query.lowercase()
            return SpecialItemRegistry.all().filter { item ->
                item.id.lowercase().contains(q) ||
                item.displayName.replace(Regex("<[^>]+>"), "").trim().lowercase().contains(q)
            }
        }

        fun calcRows(query: String): Int {
            return ((search(query).size + 6) / 7 + 2).coerceIn(3, 6)
        }
    }
}
