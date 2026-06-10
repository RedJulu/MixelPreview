package de.redjulu.mixelPreview.gui

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.SpecialItemRegistry
import de.redjulu.mixelPreview.utils.BaseGUI
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemFlag
import java.util.UUID
import java.util.function.Predicate

class SpecialItemsGUI : BaseGUI<SpecialItem, SpecialItemCategory, Unit>(
    rows = 6,
    titleKey = MiniMessage.miniMessage().deserialize("<dark_gray>» <gradient:#79BCD7:#8554B6>Special Items</gradient>"),
    t = 2,
    b = 1,
    l = 1,
    r = 1,
    defaultCategory = SpecialItemCategory.ALL
) {

    companion object {
        private const val CATEGORIES_PER_PAGE = 5

        private val CATEGORY_PREV_SLOT = 0
        private val CATEGORY_SLOTS = intArrayOf(2, 3, 4, 5, 6)
        private val CATEGORY_NEXT_SLOT = 8

        private val ITEM_PREV_SLOT = 48
        private val ITEM_NEXT_SLOT = 50

        private val selectedCategory = mutableMapOf<UUID, SpecialItemCategory>()
        private val categoryPage = mutableMapOf<UUID, Int>()
    }

    private val mm = MiniMessage.miniMessage()

    override fun compose(player: Player) {
        currentCategory = selectedCategory[player.uniqueId] ?: currentCategory
        val categoryPageIndex = categoryPage[player.uniqueId] ?: 0

        fillBackground(Material.GRAY_STAINED_GLASS_PANE)
        renderCategoryBar(player, categoryPageIndex)

        allItems = SpecialItemRegistry.all().toMutableList()
        val category = currentCategory ?: SpecialItemCategory.ALL

        val filter = Predicate<SpecialItem> { item ->
            category == SpecialItemCategory.ALL || item.category == category
        }

        renderPage(filter) { item, slot ->
            setButton(slot, item.build()) { p, click ->
                when (click) {
                    ClickType.LEFT, ClickType.SHIFT_LEFT -> giveItem(p, item)
                    ClickType.RIGHT, ClickType.SHIFT_RIGHT -> giveItem(p, item, fullStack = true)
                    else -> {}
                }
            }
        }

        addItemPagination(player, filter)
    }

    private fun renderCategoryBar(player: Player, categoryPageIndex: Int) {
        val allCategories = SpecialItemCategory.entries
        val pageStart = categoryPageIndex * CATEGORIES_PER_PAGE
        val pageCategories = allCategories.drop(pageStart).take(CATEGORIES_PER_PAGE)

        val hasPrevPage = categoryPageIndex > 0
        val hasNextPage = (categoryPageIndex + 1) * CATEGORIES_PER_PAGE < allCategories.size

        setButton(CATEGORY_PREV_SLOT, arrowItem(hasPrevPage, "«")) { p, _ ->
            if (!hasPrevPage) return@setButton
            categoryPage[p.uniqueId] = categoryPageIndex - 1
            update(p)
        }

        for (i in pageCategories.size until CATEGORY_SLOTS.size) {
            setButton(CATEGORY_SLOTS[i], ItemBuilder.placeholder(Material.GRAY_STAINED_GLASS_PANE).build())
        }

        pageCategories.forEachIndexed { index, category ->
            val slot = CATEGORY_SLOTS[index]
            val active = currentCategory == category

            var builder = ItemBuilder(category.icon)
                .setName(if (active) "<b>${category.miniMessageName}</b>" else category.miniMessageName)
                .setMiniMessageLore(
                    "",
                    if (active) " <green>▸ Ausgewählt" else " <gray>Klicken zum Anzeigen",
                    " <dark_gray>${SpecialItemRegistry.byCategory(category).size} Items"
                )

            if (active) {
                builder = builder
                    .addEnchant(Enchantment.UNBREAKING, 1, true)
                    .addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }

            setButton(slot, builder.build()) { p, _ ->
                selectCategory(p, category)
            }
        }

        setButton(CATEGORY_NEXT_SLOT, arrowItem(hasNextPage, "»")) { p, _ ->
            if (!hasNextPage) return@setButton
            categoryPage[p.uniqueId] = categoryPageIndex + 1
            update(p)
        }
    }

    private fun selectCategory(player: Player, category: SpecialItemCategory) {
        selectedCategory[player.uniqueId] = category
        setCategory(player, category)
    }

    private fun addItemPagination(player: Player, filter: Predicate<SpecialItem>) {
        val filtered = allItems.filter { filter.test(it) }
        val hasPrev = page > 0
        val hasNext = (page + 1) * pageSize < filtered.size

        setButton(ITEM_PREV_SLOT, arrowItem(hasPrev, "«")) { p, _ ->
            if (!hasPrev) return@setButton
            page--
            update(p)
        }

        setButton(ITEM_NEXT_SLOT, arrowItem(hasNext, "»")) { p, _ ->
            if (!hasNext) return@setButton
            page++
            update(p)
        }
    }

    private fun arrowItem(enabled: Boolean, label: String) = ItemBuilder(Material.ARROW)
        .setName(
            if (enabled) "<gray>$label"
            else "<red><st>$label"
        )
        .build()

    private fun giveItem(player: Player, item: SpecialItem, fullStack: Boolean = false) {
        val stack = item.build()
        val amount = if (fullStack) stack.type.maxStackSize else 1
        stack.amount = amount

        player.inventory.addItem(stack).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }

        val message = if (fullStack) {
            " <dark_gray>» <green>Du hast <b>${item.displayName}</b> <gray>x$amount <green>erhalten!"
        } else {
            item.giveMessage()
        }

        player.sendMessage(mm.deserialize(message))
        player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)
    }
}
