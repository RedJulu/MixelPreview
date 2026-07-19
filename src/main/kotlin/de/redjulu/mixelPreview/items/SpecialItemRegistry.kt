package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.items.impl.crafting.InfiCarrot
import de.redjulu.mixelPreview.items.impl.crafting.RocketBox
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxe
import de.redjulu.mixelPreview.items.impl.crate.MushroomAxe
import de.redjulu.mixelPreview.items.impl.crate.InvisEye
import de.redjulu.mixelPreview.items.impl.halloween.CondensedSoul
import de.redjulu.mixelPreview.items.impl.halloween.ReaperScythe
import de.redjulu.mixelPreview.items.impl.job.Baler
import de.redjulu.mixelPreview.items.impl.job.MagnetPickaxe
import de.redjulu.mixelPreview.items.impl.job.Pixelball
import de.redjulu.mixelPreview.items.impl.misc.FindTheItem
import de.redjulu.mixelPreview.items.impl.misc.ShrinkStaff
import de.redjulu.mixelPreview.items.impl.summer.CoralSplitter
import de.redjulu.mixelPreview.items.impl.summer.PoseidonCrown
import de.redjulu.mixelPreview.items.impl.summer.PoseidonPickaxe

object SpecialItemRegistry {

    private val items = linkedMapOf<String, SpecialItem>()
    private val groupedItemIds = mutableSetOf<String>()

    fun register(item: SpecialItem) {
        items[item.id] = item
    }

    fun registerGroup(id: String, name: String, vararg items: SpecialItem) {
        items.forEach {
            register(it)
            groupedItemIds.add(it.id)
        }
        GroupedItemRegistry.register(GroupedItem(id, name, items.toList()))
    }

    fun groups(): List<GroupedItem> = GroupedItemRegistry.all()

    fun isGroupedItem(id: String): Boolean = id in groupedItemIds

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
        register(Pixelball)
        register(RocketBox)
        register(MagnetPickaxe)
        register(InfiCarrot)

        registerGroup("poseidon_tools", "<gradient:#17DED6:#2EEE62>Poseidon's Tools", PoseidonPickaxe, PoseidonCrown)
        registerGroup("halloween_weapons", "<gradient:#575B9B:#4D4F4F>Halloween Waffen", ReaperScythe, CondensedSoul)

        register(InvisEye)
        register(Baler)
    }
}
