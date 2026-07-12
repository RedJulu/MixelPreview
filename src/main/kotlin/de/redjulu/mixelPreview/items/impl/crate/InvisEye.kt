package de.redjulu.mixelPreview.items.impl.crate

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

object InvisEye : SpecialItem("invis_eye", SpecialItemCategory.CRATE) {
    override val displayName: String
        get() = "<b><gradient:#B5CAD9:#CC99D7>Auge der Unsichtbarkeit</gradient></b>"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.ENDER_EYE)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#B5CAD9:#CC99D7>Damit bist du sehr durchschaubar..</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit diesem Auge kannst",
                "    <white>du dich im Austausch gegen <green>XP-",
                "    <green>Punkte <white>unsichtbar machen"
            )
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        event.isCancelled = true

        val player = event.player

        if (player.level < 3) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Du hast nicht genug Level!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            player.sendActionBar(MiniMessage.miniMessage().deserialize("<red>Du bist bereits unsichtbar!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        InvisEyeUI.open(player)
    }
}