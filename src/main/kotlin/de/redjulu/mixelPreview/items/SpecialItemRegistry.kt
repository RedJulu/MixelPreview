package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxe
import de.redjulu.mixelPreview.items.impl.crate.MushroomAxe
import de.redjulu.mixelPreview.items.impl.halloween.ReaperScythe
import de.redjulu.mixelPreview.items.impl.misc.FindTheItem
import de.redjulu.mixelPreview.items.impl.misc.ShrinkStaff
import de.redjulu.mixelPreview.items.impl.summer.CoralSplitter
import de.redjulu.mixelPreview.items.impl.summer.PoseidonKroneRework
import de.redjulu.mixelPreview.items.impl.summer.PoseidonPickaxe

object SpecialItemRegistry {

    private val items = linkedMapOf<String, SpecialItem>()

    fun register(item: SpecialItem) {
        items[item.id] = item
    }

    fun get(id: String): SpecialItem? = items[id]

    fun all(): List<SpecialItem> = items.values.toList()

    fun byCategory(category: SpecialItemCategory): List<SpecialItem> =
        if (category == SpecialItemCategory.ALL) all()
        else all().filter { it.category == category }

    fun init() {
        register(ShrinkStaff)
        register(MushroomAxe)
        register(FindTheItem)
        register(CoralSplitter)
        register(CreativeAxe)
        register(ReaperScythe)
        register(PoseidonPickaxe)
        register(PoseidonKroneRework)
    }
}
