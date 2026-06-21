package de.redjulu.mixelPreview.items.impl.job

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object Pixelball : SpecialItem("Pixelball", SpecialItemCategory.JOB) {
    override val displayName : String
        get() = "<b><white>Pixel<red>ball <gray><<yellow>0/$MAX_MOBS<gray>>"

    private const val MAX_MOBS = 2
    private val capturekey = NamespacedKey(MixelPreview.instance, "pixelball_capturekey")
    private val capturedTypeKey = NamespacedKey(MixelPreview.instance, "pixelball_capturetype")

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
                "<dark_gray><b>▸ </b><gray>Eingefangener Typ: <gold>Keiner",
                "",
                "<dark_gray><b>▸ </b><gray>Klicke um dir das Rezept",
                "  <gray>anzeigen zu lassen"
            )
            .hideAdditionalInfo()
            .setFireworkColor(Color.RED)
    ).build()

    private fun update(item: ItemStack, type: EntityType): ItemStack {
        val meta = item.itemMeta!!
        val current = meta.persistentDataContainer.get(capturekey, PersistentDataType.INTEGER) ?: 0
        val next = current + 1

        if (next >= MAX_MOBS) {
            val eggMaterial = Material.entries.find { it.name == "${type.name}_SPAWN_EGG" } ?: Material.PIG_SPAWN_EGG
            return ItemStack(eggMaterial, 1)
        }

        val updatedItem = item.clone()
        updatedItem.amount = 1

        val formattedTypeName = type.name.lowercase().replace("_", " ").replaceFirstChar { it.uppercase() }

        return ItemBuilder(updatedItem)
            .setName("<b><white>Pixel<red>ball <gray><<yellow>$next/$MAX_MOBS<gray>>")
            .setMiniMessageLore(
                "",
                "<dark_gray><b>▸ </b><aqua>Spezialrezept des <red>Jägers",
                "",
                "<dark_gray><b>▸ </b><white>Sammle <green>$MAX_MOBS Tiere <white>der selben Art,",
                "    <white>um ein <gray>Spawn Ei <white>desselben",
                "    <white>Typs zu erhalten",
                "",
                "<dark_gray><b>▸ </b><gray>Eingefangener Typ: <gold>$formattedTypeName",
                "",
                "<dark_gray><b>▸ </b><gray>Klicke um dir das Rezept",
                "  <gray>anzeigen zu lassen"
            )
            .pdc(capturekey, PersistentDataType.INTEGER, next)
            .pdc(capturedTypeKey, PersistentDataType.STRING, type.name)
            .hideAdditionalInfo()
            .build()
    }

    override fun onInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val entity = event.rightClicked
        val itemInHand = player.inventory.itemInMainHand

        if (event.hand != EquipmentSlot.HAND) return

        val meta = itemInHand.itemMeta ?: return

        if (player.hasCooldown(itemInHand)) {
            player.sendActionBar(mm.deserialize("<dark_gray>» <red>Bitte warte einen Moment!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val whitelist = setOf(
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

        val entityType = entity.type

        if (entityType !in whitelist) {
            player.setCooldown(itemInHand, 10)
            player.sendActionBar(mm.deserialize("<dark_gray>» <red>Dieses Tier kann nicht eingefangen werden!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val container = meta.persistentDataContainer
        val currentTypeString = container.get(capturedTypeKey, PersistentDataType.STRING)

        if (currentTypeString != null && currentTypeString != entityType.name) {
            player.setCooldown(itemInHand, 10)
            player.sendActionBar(mm.deserialize("<dark_gray>» <red>Dieses Tier passt nicht in diesen Pixelball!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
            return
        }

        val currentCount = container.get(capturekey, PersistentDataType.INTEGER) ?: 0
        val isFinalCapture = (currentCount + 1) >= MAX_MOBS

        entity.world.spawnParticle(
            Particle.END_ROD,
            entity.location.add(0.0, 0.5, 0.0),
            35,
            0.3, 0.3, 0.3,
            -0.05
        )

        if (isFinalCapture) {
            player.world.spawnParticle(
                Particle.HAPPY_VILLAGER,
                player.location.add(0.0, 1.0, 0.0),
                40,
                0.4, 0.4, 0.4,
                0.1
            )
            player.world.spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                player.location.add(0.0, 1.0, 0.0),
                30,
                0.3, 0.3, 0.3,
                0.2
            )
            player.playSound(player.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f)
            player.sendActionBar(mm.deserialize("<dark_gray>» <green>Erfolgreich verwandelt! Du hast ein Spawn-Ei erhalten."))
        } else {
            player.playSound(entity.location, Sound.ENTITY_CHICKEN_EGG, 1.0f, 0.5f)
            player.playSound(entity.location, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f)
        }

        entity.remove()

        val resultItem = update(itemInHand, entityType)

        if (itemInHand.amount > 1) {
            itemInHand.amount -= 1
            val leftover = player.inventory.addItem(resultItem)
            if (leftover.isNotEmpty()) {
                leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
            }
        } else {
            player.inventory.setItemInMainHand(resultItem)
        }
    }
}