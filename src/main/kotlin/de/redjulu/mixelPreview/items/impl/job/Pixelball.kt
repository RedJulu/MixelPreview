package de.redjulu.mixelPreview.items.impl.job

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.impl.halloween.ReaperScythe.soulsKey
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Monster
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType


object Pixelball : SpecialItem("Pixelball", SpecialItemCategory.JOB) {
    override val displayName : String
        get() = "<b><white>Pixel<red>ball <gray><<yellow>0/$MAX_MOBS<gray>>"


    private const val MAX_MOBS = 25
    private val capturekey = NamespacedKey(MixelPreview.instance, "capturekey_pixelball")



    private val mm = MiniMessage.miniMessage()

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.FIREWORK_STAR)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                "<dark_gray><b>▸ </b><aqua>Spezialrezept des <red>Jägers",
                "",
                "<dark_gray><b>▸ </b><white>Sammle <green>$MAX_MOBS Tiere <white>der selben Art,",
                "    <white>um ein <gray>Spawn Ei <white>desselben",
                "    <white>Typs zu erhalten",
                "",
                "<dark_gray><b>▸ </b><gray>Klicke um dir das Rezept",
                "  <gray>anzeigen zu lassen"
            )
    ).build()

    private fun update(item: ItemStack): ItemStack {
        val itemMeta = item.itemMeta!!
        val mobs = itemMeta.persistentDataContainer.get(capturekey, PersistentDataType.INTEGER) ?: 0
        val finalmobs = if ( mobs + 1 >= MAX_MOBS) 0 else mobs + 1

        return ItemBuilder(item)
            .setName("<b><white>Pixel<red>ball <gray><<yellow>0/$MAX_MOBS<gray>>")
            .pdc(soulsKey, PersistentDataType.INTEGER, finalmobs)
            .build()
    }

    override fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked

        val whitelist = setOf<EntityType>(
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.COW,
            EntityType.MOOSHROOM,

            EntityType.CHICKEN,
            EntityType.WOLF,
            EntityType.OCELOT,
            EntityType.ARMADILLO,

            EntityType.HORSE,
            EntityType.DONKEY,
            EntityType.MULE,
            EntityType.LLAMA,
            EntityType.CAMEL,

            EntityType.RABBIT,
            EntityType.PARROT,
            EntityType.POLAR_BEAR,

            EntityType.CAT,
            EntityType.FOX,
            EntityType.GOAT,
            EntityType.PANDA,

            EntityType.DOLPHIN,
            EntityType.TURTLE,
            EntityType.SQUID,
            EntityType.GLOW_SQUID,

            EntityType.STRIDER
        )

        if (entity.type !in whitelist) {
            return
        }

    }

}