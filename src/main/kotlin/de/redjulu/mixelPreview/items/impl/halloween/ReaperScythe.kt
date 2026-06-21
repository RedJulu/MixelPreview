package de.redjulu.mixelPreview.items.impl.halloween

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.items.impl.misc.ShrinkStaff
import de.redjulu.mixelPreview.utils.ItemBuilder
import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay.override
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.checkerframework.checker.units.qual.mm
import kotlin.random.Random

object ReaperScythe : SpecialItem("reaper_scythe", SpecialItemCategory.HALLOWEEN) {
    override val displayName: String
        get() = "<b><gold><obf>aa </obf><gradient:#575B9B:#72717C>Reape</gradient><gradient:#72717C:#4D4F4F>r Scythe</gradient> <dark_gray><b>[</b><yellow>0/$maxSouls<dark_gray><b>] <gold><obf>aa</obf>"

    private val mm = MiniMessage.miniMessage()
    val soulsKey = NamespacedKey(MixelPreview.instance, "reaper_scythe_souls")

    private const val maxSouls = 10


    val lootpool = mapOf<Any, Double>(
        ShrinkStaff.id to 15.0,
        Material.DIAMOND to 25.0,
        Material.GOLD_INGOT to 60.0
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
            .setUnbreakable(true)
            .hideAdditionalInfo()
            .setMaxStackSize(1)
    ).build()

    private fun update(item: ItemStack): ItemStack {
        val itemMeta = item.itemMeta!!
        val souls = itemMeta.persistentDataContainer.get(soulsKey, PersistentDataType.INTEGER) ?: 0
        val finalSouls = if (souls + 1 >= maxSouls) 0 else souls + 1

        return ItemBuilder(item)
            .setName("<b><gold><obf>aa </obf><gradient:#575B9B:#72717C>Reape</gradient><gradient:#72717C:#4D4F4F>r Scythe</gradient> <dark_gray><b>[</b><yellow>$finalSouls/$maxSouls<dark_gray><b>] <gold><obf>aa</obf>")
            .pdc(soulsKey, PersistentDataType.INTEGER, finalSouls)
            .build()
    }

    override fun onKillEntity(event: EntityDeathEvent) {
        val player = event.entity.killer ?: return
        val item = player.inventory.itemInMainHand
        if (!SpecialItemKeys.isSpecialItem(item, id)) return

        val souls = item.persistentDataContainer.get(soulsKey, PersistentDataType.INTEGER) ?: 0
        if ((souls + 1) >= maxSouls) {

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
        player.setItemInHand(update(item))
    }
}