package de.redjulu.mixelPreview.utils

import net.kyori.adventure.text.Component

abstract class SimpleGUI<T, S>(
    rows: Int,
    title: Component,
    t: Int, b: Int, l: Int, r: Int
) : BaseGUI<T, SimpleGUI.NO_CATEGORY, S>(
    rows,
    title,
    t, b, l, r,
    NO_CATEGORY.NONE
) {
    enum class NO_CATEGORY { NONE }
}