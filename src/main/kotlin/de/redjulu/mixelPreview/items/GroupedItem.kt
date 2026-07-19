package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

data class GroupedItem(
    val id: String,
    val name: String,
    val items: List<SpecialItem>
) {
    fun displayFrames(): List<ItemStack> = items.map { item ->
        ItemBuilder(item.createItem())
            .setName("<b>$name</b>")
            .addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE)
            .build()
    }
}

object GroupedItemRegistry {
    private val groups = linkedMapOf<String, GroupedItem>()

    fun register(group: GroupedItem) {
        groups[group.id] = group
    }

    fun all(): List<GroupedItem> = groups.values.toList()

    fun get(id: String): GroupedItem? = groups[id]
}
