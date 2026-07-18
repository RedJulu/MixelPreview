package de.redjulu.mixelPreview.items.impl.job

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.impl.misc.ShrinkStaff
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Material
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object Baler : SpecialItem("baler", SpecialItemCategory.JOB) {
    override val displayName: String
        get() = "<gradient:#51FC16:#18BE37>Farmer</gradient> <gradient:#D0B726:#CB691A>Ballenpresse</gradient>"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.HONEYCOMB)
            .setName("<b>$displayName</b>")
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#D0B726:#CB691A>MEHR HEUBALLEN!!</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit diesem Item kannst du",
                "    <white>schnell <gradient:#D0B726:#CB691A>Weizen <white>in",
                "    <white><gradient:#D0B726:#CB691A>Heuballen <white>konvertieren",
                "",
            )
            .setMaxStackSize(1)
            .setEnchantmentGlintOverride(true)
            .setUnrenamable(true)
            .setUnenchantable(true)
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        event.isCancelled = true

        if (event.action != Action.RIGHT_CLICK_BLOCK && event.action != Action.RIGHT_CLICK_AIR) return
        BalerGUI().open(event.player, false)
    }
}