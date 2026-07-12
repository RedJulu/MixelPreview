package de.redjulu.mixelPreview.gui

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.GroupDetailGUI
import de.redjulu.mixelPreview.items.GroupedItem
import de.redjulu.mixelPreview.items.SearchResultGUI
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.SpecialItemRegistry
import de.redjulu.mixelPreview.utils.BaseGUI
import de.redjulu.mixelPreview.utils.DialogBuilder
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
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
        private val ITEM_SEARCH_SLOT = 49

        private val selectedCategory = mutableMapOf<UUID, SpecialItemCategory>()
        private val categoryPage = mutableMapOf<UUID, Int>()
    }

    private val mm = MiniMessage.miniMessage()
    private val groupAnimationTasks = mutableMapOf<UUID, Int>()

    override fun compose(player: Player) {
        currentCategory = selectedCategory[player.uniqueId] ?: currentCategory
        val categoryPageIndex = categoryPage[player.uniqueId] ?: 0

        fillBackground(Material.GRAY_STAINED_GLASS_PANE)
        renderCategoryBar(player, categoryPageIndex)

        if (currentCategory == SpecialItemCategory.GROUPS) {
            renderGroups(player, onlyGroups = true)
        } else {
            val category = currentCategory ?: SpecialItemCategory.ALL

            data class DisplayEntry(val group: GroupedItem?, val item: SpecialItem?)

            val entries = mutableListOf<DisplayEntry>()
            val addedGroupIds = mutableSetOf<String>()

            for (regItem in SpecialItemRegistry.all()) {
                if (category != SpecialItemCategory.ALL && regItem.category != category) continue

                if (SpecialItemRegistry.isGroupedItem(regItem.id)) {
                    val group = SpecialItemRegistry.groups().firstOrNull { g ->
                        g.items.any { it.id == regItem.id }
                    }
                    if (group != null && group.id !in addedGroupIds) {
                        if (category == SpecialItemCategory.ALL || group.items.any { it.category == category }) {
                            entries.add(DisplayEntry(group, null))
                            addedGroupIds.add(group.id)
                        }
                    }
                } else {
                    entries.add(DisplayEntry(null, regItem))
                }
            }

            val start = page * pageSize
            for (i in 0 until pageSize) {
                val slot = contentSlots[i]
                val index = start + i
                if (index < entries.size) {
                    val (group, item) = entries[index]
                    if (group != null) {
                        val frames = group.displayFrames()
                        if (frames.isNotEmpty()) {
                            setAnimatedItem(slot, frames)
                            setButton(slot, frames.first()) { p, _ ->
                                GroupDetailGUI(group).open(p, saveToHistory = true)
                            }
                        }
                    } else if (item != null) {
                        setButton(slot, item.build()) { p, _ ->
                            giveItem(p, item)
                        }
                    }
                } else {
                    setButton(slot, null)
                }
            }
            startGroupAnimation(player)

            val hasPrev = page > 0
            val hasNext = (page + 1) * pageSize < entries.size
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

        setButton(ITEM_SEARCH_SLOT, ItemBuilder(Material.COMPASS)
            .setName("<b><aqua>🔍 Suchen</aqua></b>")
            .setMiniMessageLore("", " <gray>Durchsuche alle Items")
            .build()
        ) { p, _ -> openSearch(p) }
    }

    private fun renderGroups(player: Player, onlyGroups: Boolean) {
        val groups = SpecialItemRegistry.groups().filter { it.items.isNotEmpty() }
        if (groups.isEmpty()) return

        if (onlyGroups) {
            val start = page * pageSize
            for (i in 0 until pageSize) {
                val slot = contentSlots[i]
                val index = start + i
                if (index < groups.size) {
                    val group = groups[index]
                    val frames = group.displayFrames()
                    if (frames.isNotEmpty()) {
                        setAnimatedItem(slot, frames)
                        setButton(slot, frames.first()) { p, _ ->
                            GroupDetailGUI(group).open(p, saveToHistory = true)
                        }
                    }
                } else {
                    setButton(slot, null)
                }
            }
            startGroupAnimation(player)
            addGroupPagination(player, groups.size)
        } else {
            val category = currentCategory ?: SpecialItemCategory.ALL
            val filtered = allItems.filter { category == SpecialItemCategory.ALL || it.category == category }
            val categoryGroups = if (category == SpecialItemCategory.ALL) groups
            else groups.filter { group -> group.items.any { it.category == category } }
            val start = page * pageSize
            var groupIndex = 0

            for (i in 0 until contentSlots.size) {
                val slot = contentSlots[i]
                val index = start + i
                if (index >= filtered.size && groupIndex < categoryGroups.size) {
                    val group = categoryGroups[groupIndex]
                    val frames = group.displayFrames()
                    if (frames.isNotEmpty()) {
                        setAnimatedItem(slot, frames)
                        setButton(slot, frames.first()) { p, _ ->
                            GroupDetailGUI(group).open(p, saveToHistory = true)
                        }
                    }
                    groupIndex++
                }
            }
            startGroupAnimation(player)
        }
    }

    private fun addGroupPagination(player: Player, totalGroups: Int) {
        val hasPrev = page > 0
        val hasNext = (page + 1) * pageSize < totalGroups

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

    private fun startGroupAnimation(player: Player) {
        if (groupAnimationTasks.containsKey(player.uniqueId)) return
        val task = Bukkit.getScheduler().runTaskTimer(MixelPreview.instance, Runnable {
            tickAnimations(System.currentTimeMillis() / 1000)
        }, 20L, 20L)
        groupAnimationTasks[player.uniqueId] = task.taskId
    }

    private fun stopGroupAnimation(player: Player) {
        groupAnimationTasks.remove(player.uniqueId)?.let { Bukkit.getScheduler().cancelTask(it) }
    }

    override fun onClose(player: Player) {
        stopGroupAnimation(player)
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

            val count = if (category == SpecialItemCategory.GROUPS) {
                SpecialItemRegistry.groups().count { it.items.isNotEmpty() }
            } else {
                SpecialItemRegistry.byCategory(category).size
            }

            var builder = ItemBuilder(category.icon)
                .setName(if (active) "<b>${category.miniMessageName}</b>" else category.miniMessageName)
                .setMiniMessageLore(
                    "",
                    if (active) " <green>▸ Ausgewählt" else " <gray>Klicken zum Anzeigen",
                    " <dark_gray>$count ${if (category == SpecialItemCategory.GROUPS) "Gruppen" else "Items"}"
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

    private fun giveItem(player: Player, item: SpecialItem) {
        val stack = item.build()
        player.inventory.addItem(stack).values.forEach { leftover ->
            player.world.dropItemNaturally(player.location, leftover)
        }
        player.sendMessage(mm.deserialize(item.giveMessage()))
        player.playSound(player.location, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f)
    }

    private fun openSearch(player: Player) {
        val holder = player.openInventory.topInventory.holder
        if (holder is BaseGUI<*, *, *>) {
            BaseGUI.saveHistory(player.uniqueId, holder)
        }
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
}
