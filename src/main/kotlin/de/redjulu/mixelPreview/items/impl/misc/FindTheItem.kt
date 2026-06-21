package de.redjulu.mixelPreview.items.impl.misc

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

object FindTheItem : SpecialItem("find_the_item", SpecialItemCategory.MISC) {
    override val displayName: String
        get() = "<b><gradient:#3CBBE3:#9822AB>Find-The-Item</gradient></b>"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.WRITTEN_BOOK)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#3CBBE3:#9822AB>Verlierst du auch so oft Items wie ich?</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit diesem Buch kannst du",
                "    <white>Items schnell wiederfinden indem",
                "    <white>du den Namen in das Buch schreibst."
            )
            .hideAdditionalInfo()
            .setMaxStackSize(1)
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player

        event.isCancelled = true

        if (player.hasCooldown(Material.WRITTEN_BOOK)) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<dark_gray>» <red>Bitte warte einen Moment!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        FindTheItemUI().open(player)
    }
}