package de.redjulu.mixelPreview.items.impl.halloween

import com.google.common.collect.Iterators.advance
import com.ibm.icu.impl.SimpleFormatterImpl.IterInternal.step
import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxe
import de.redjulu.mixelPreview.items.impl.halloween.ReaperScythe.advancedKey
import de.redjulu.mixelPreview.items.impl.misc.ShrinkStaff
import de.redjulu.mixelPreview.utils.ItemBuilder
import io.papermc.paper.command.brigadier.argument.ArgumentTypes.player
import net.kyori.adventure.text.minimessage.MiniMessage
import org.apache.commons.lang3.ObjectUtils.mode
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.random.Random

object ReaperScythe : SpecialItem("reaper_scythe", SpecialItemCategory.HALLOWEEN) {
    override val displayName: String
        get() = "<b><gold><obf>aa </obf><gradient:#575B9B:#72717C>Reape</gradient><gradient:#72717C:#4D4F4F>r Scythe</gradient> <dark_gray><b>[</b><yellow>0/$maxSouls<dark_gray><b>] <gold><obf>aa</obf>"

    private val mm = MiniMessage.miniMessage()
    val soulsKey = NamespacedKey(MixelPreview.instance, "reaper_scythe_souls")
    val advancedKey = NamespacedKey(MixelPreview.instance, "reaper_scythe_advanced")

    private const val maxSouls = 3


    val lootpoolStandard = mapOf<Any, Double>(
        ShrinkStaff.id to 15.0,
        Material.DIAMOND to 25.0,
        Material.GOLD_INGOT to 60.0
    )

    val lootpoolAdvanced = mapOf<Any, Double>(
        CreativeAxe.id to 100.0
    )

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.NETHERITE_HOE)
            .setName(mm.deserialize("<b><gold><obf>aa </obf><gradient:#575B9B:#72717C>Reape</gradient><gradient:#72717C:#4D4F4F>r Scythe</gradient> <dark_gray><b>[</b><yellow>0/$maxSouls<dark_gray><b>] <gold><obf>aa</obf>"))
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#575B9B:#575B9B>\uD83D\uDC80 Sammle </gradient><gradient:#575B9B:#72717C>die Seelen deine</gradient><gradient:#72717C:#4D4F4F>r Feinde \uD83D\uDC80</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit dieser Sense kannst du die Seelen",
                "    <white>von <red>Monstern <white>sammeln, indem du sie",
                "    <white><dark_red>tötest <white>und wenn du genug sammelst",
                "    <white>bekommst du eine saftige <dark_purple>Belohnung. "
            )
            .addEnchant(Enchantment.SHARPNESS, 7, true)
            .pdc(soulsKey, PersistentDataType.INTEGER, 0)
            .pdc(advancedKey, PersistentDataType.BOOLEAN, false)
            .setUnbreakable(true)
            .hideAdditionalInfo()
            .setMaxStackSize(1)
    ).build()


    private fun update(item: ItemStack, stepSouls: Boolean = false, stepAdvanced: Boolean = false): ItemStack {
        val itemMeta = item.itemMeta!!
        val souls = itemMeta.persistentDataContainer.get(soulsKey, PersistentDataType.INTEGER) ?: 0
        var finalSouls = souls
        if (stepSouls) {
            finalSouls = if (souls + 1 >= maxSouls) 0 else souls + 1
        }

        var advanced = item.itemMeta.persistentDataContainer.get(advancedKey, PersistentDataType.BOOLEAN) ?: false
        if (stepAdvanced) advanced = !advanced

        return when (advanced) {
            true -> {
                ItemBuilder(item)
                    .setName("<b><gold><obf>aa </obf><rainbow>Reaper Scythe</rainbow> <dark_gray><b>[</b><yellow>$finalSouls/$maxSouls<dark_gray><b>] <gold><obf>aa</obf>")
                    .pdc(soulsKey, PersistentDataType.INTEGER, finalSouls)
                    .pdc(advancedKey, PersistentDataType.BOOLEAN, advanced)
                    .build()
            }

            false -> {
                ItemBuilder(item)
                    .setName("<b><gold><obf>aa </obf><gradient:#575B9B:#72717C>Reape</gradient><gradient:#72717C:#4D4F4F>r Scythe</gradient> <dark_gray><b>[</b><yellow>$finalSouls/$maxSouls<dark_gray><b>] <gold><obf>aa</obf>")
                    .pdc(soulsKey, PersistentDataType.INTEGER, finalSouls)
                    .pdc(advancedKey, PersistentDataType.BOOLEAN, advanced)
                    .build()
            }
        }
    }

    override fun onKillEntity(event: EntityDeathEvent) {
        val player = event.entity.killer ?: return
        val item = player.inventory.itemInMainHand
        if (!SpecialItemKeys.isSpecialItem(item, id)) return

        val souls = item.persistentDataContainer.get(soulsKey, PersistentDataType.INTEGER) ?: 0
        val advanced = item.persistentDataContainer.get(advancedKey, PersistentDataType.BOOLEAN) ?: false
        var updateAdvanced = false
        if ((souls + 1) >= maxSouls) {
            giveLoot(player, advanced)
            if (advanced) updateAdvanced = true
        }
        player.setItemInHand(update(item, true, updateAdvanced))
    }

    private fun giveLoot(player: Player, mode: Boolean) {
        val lootpool = if (mode) lootpoolAdvanced else lootpoolStandard
        val totalWeight = lootpool.values.sum()
        var randomValue = Random.nextDouble() * totalWeight

        var selectedReward: ItemStack? = null
        for ((reward, weight) in lootpool) {
            randomValue -= weight
            if (randomValue <= 0) {
                selectedReward = when (reward) {
                    is String -> SpecialItemKeys.getItem(reward)
                    is Material -> ItemStack(reward)
                    else -> null
                }
                break
            }
        }


        selectedReward?.let { rewardStack ->
            val leftover = player.inventory.addItem(rewardStack)
            leftover.forEach { (_, stack) ->
                player.world.dropItemNaturally(player.location, stack)
            }
        }
    }

    override fun onInteract(event: PlayerInteractEvent) {
        val item = event.item!!
        val player = event.player

        when(event.action) {
            Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> {
                if (item.persistentDataContainer.get(advancedKey, PersistentDataType.BOOLEAN) == true) return
                val condensed = player.inventory.contents
                    .filterNotNull()
                    .firstOrNull { SpecialItemKeys.isSpecialItem(it, CondensedSoul.id) }

                if (condensed == null) {
                    player.sendActionBar(mm.deserialize("<red>Du hast keine <b><gradient:#193139:#2AB3A8>Condensed Soul</b> <red>in deinem Inventar!"))
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return
                }

                player.inventory.removeItem(condensed)
                player.setItemInHand(update(item, false, true))
            }

            else -> return
        }
    }

    override fun onTickMainHand(player: Player) {
        if (player.itemInHand.persistentDataContainer.get(advancedKey, PersistentDataType.BOOLEAN) == true) {
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 2, 3, false, false, false))
        }
    }
}