package de.redjulu.mixelPreview.utils

import net.kyori.adventure.text.Component
import org.bukkit.event.inventory.InventoryType

abstract class SimpleGUI<T, S>(
    rows: Int,
    title: Component,
    t: Int, b: Int, l: Int, r: Int,
    inventoryType: InventoryType = InventoryType.CHEST
) : BaseGUI<T, SimpleGUI.NO_CATEGORY, S>(
    rows,
    title,
    t, b, l, r,
    NO_CATEGORY.NONE,
    inventoryType
) {
    enum class NO_CATEGORY { NONE }
}