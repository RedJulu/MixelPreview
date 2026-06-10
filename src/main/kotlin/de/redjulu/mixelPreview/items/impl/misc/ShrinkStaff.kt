package de.redjulu.mixelPreview.items.impl.misc

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.utils.ItemBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ShrinkStaff : SpecialItem("shrink_staff", SpecialItemCategory.MISC) {

    private val mm = MiniMessage.miniMessage()

    val stageKey: NamespacedKey
        get() = NamespacedKey(MixelPreview.instance, "shrink_stage")

    override val displayName = "<gradient:#79BCD7:#8554B6>Shrink Staff</gradient>"

    private val scales = floatArrayOf(1.5f, 1.25f, 1.0f, 0.5f, 0.25f)
    private val stageNames = arrayOf(
        "Stufe 1 • 150%",
        "Stufe 2 • 125%",
        "Normale Größe",
        "Stufe 3 • 50%",
        "Stufe 4 • 25%"
    )

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.BREEZE_ROD)
            .setName("<b>$displayName</b>")
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#79BCD7:#8554B6>Welche Größe darf es sein?</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit diesem Stab kannst du",
                "    <white>indem du <aqua>Rechtsklick <white>in die Luft",
                "    <white>machst dich <yellow>vergrößern <white>und <yellow>verkleinern",
                "",
                " <dark_gray>▪ <gray>Vergrößern: '<yellow>Rechtsklick<gray>'",
                " <dark_gray>▪ <gray>Verkleinern: '<yellow>Sneak-Rechtsklick<gray>'"
            )
            .addEnchant(Enchantment.EFFICIENCY, 1, true)
            .addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES)
    ).build()

    override fun onInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val player = event.player
        val item = player.inventory.itemInMainHand

        if (!SpecialItemKeys.isSpecialItem(item, id)) return

        event.isCancelled = true

        if (player.hasCooldown(item.type)) {
            player.sendActionBar(mm.deserialize("<dark_gray>» <red>Bitte warte einen Moment!"))
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            return
        }

        val currentStage = player.persistentDataContainer
            .getOrDefault(stageKey, PersistentDataType.INTEGER, 0)

        val newStage = when {
            player.isSneaking -> {
                if (currentStage >= scales.lastIndex) {
                    deny(player, item, "<dark_gray>» <red>Du bist bereits auf der kleinsten Größe!")
                    return
                }
                currentStage + 1
            }
            else -> {
                if (currentStage <= 0) {
                    deny(player, item, "<dark_gray>» <red>Du bist bereits auf der größten Größe!")
                    return
                }
                currentStage - 1
            }
        }

        player.setCooldown(item.type, 10)
        player.persistentDataContainer.set(stageKey, PersistentDataType.INTEGER, newStage)
        player.getAttribute(Attribute.SCALE)?.baseValue = scales[newStage].toDouble()

        player.world.spawnParticle(
            Particle.ENCHANT,
            player.location.add(0.0, 1.0, 0.0),
            30,
            0.3, 0.6, 0.3,
            0.05
        )

        player.playSound(
            player.location,
            Sound.ENTITY_CHICKEN_EGG,
            1f,
            1.4f + (newStage * 0.2f)
        )

        player.sendActionBar(mm.deserialize(
            "<dark_gray>» <gradient:#79BCD7:#8554B6>Größe geändert!</gradient> " +
                    "<gray>(${stageNames[newStage]})"
        ))
    }

    private fun deny(player: Player, item: ItemStack, message: String) {
        player.setCooldown(item.type, 10)
        player.sendActionBar(mm.deserialize(message))
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
    }
}