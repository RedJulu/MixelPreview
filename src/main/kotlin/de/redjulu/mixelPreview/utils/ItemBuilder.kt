package de.redjulu.mixelPreview.utils

import com.destroystokyo.paper.profile.ProfileProperty
import de.redjulu.mixelPreview.MixelPreview
import io.papermc.paper.registry.data.dialog.body.DialogBody.item
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.*
import org.bukkit.inventory.meta.components.FoodComponent
import org.bukkit.inventory.meta.components.ToolComponent
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.function.Consumer

object ItemBuilder {

    private val mm = MiniMessage.miniMessage()

    operator fun invoke(material: Material): Builder = Builder(material)

    operator fun invoke(material: Material, amount: Int): Builder = Builder(material, amount)

    operator fun invoke(item: ItemStack): Builder = Builder(item)

    @JvmStatic
    fun placeholder(material: Material): Builder = Builder(material).setName(Component.empty()).setHideTooltip(true)

    class Builder {
        private val itemStack: ItemStack
        private val itemMeta: ItemMeta

        constructor(material: Material, amount: Int = 1) {
            this.itemStack = ItemStack(material, amount)
            this.itemMeta = itemStack.itemMeta!!
        }

        constructor(item: ItemStack) {
            this.itemStack = item.clone()
            this.itemMeta = itemStack.itemMeta!!
        }

        fun setName(name: String?): Builder {
            if (name == null) {
                itemMeta.displayName(null)
            } else {
                itemMeta.displayName(mm.deserialize(name).decoration(TextDecoration.ITALIC, false))
            }
            return this
        }

        fun setName(component: Component?): Builder {
            if (component == null) {
                itemMeta.displayName(null)
            } else {
                itemMeta.displayName(component.decoration(TextDecoration.ITALIC, false))
            }
            return this
        }

        fun setLore(vararg lore: String): Builder {
            if (lore.isEmpty()) {
                itemMeta.lore(null)
            } else {
                itemMeta.lore(lore.map { mm.deserialize(it).decoration(TextDecoration.ITALIC, false) })
            }
            return this
        }

        fun setLore(lore: List<Component>?): Builder {
            if (lore == null || lore.isEmpty()) {
                itemMeta.lore(null)
            } else {
                itemMeta.lore(lore.map { it.decoration(TextDecoration.ITALIC, false) })
            }
            return this
        }

        fun setFireworkEffect(effect: FireworkEffect): Builder {
            (itemMeta as? FireworkEffectMeta)?.let {
                it.effect = effect
            }
            return this
        }

        fun setFireworkColor(vararg colors: Color): Builder {
            val effect = FireworkEffect.builder()
                .withColor(*colors)
                .build()

            return setFireworkEffect(effect)
        }

        fun setFireworkStrength(strength: Int): Builder {
            (itemMeta as? FireworkMeta)?.let {
                it.power = strength
            }
            return this
        }

        fun setMiniMessageLore(vararg lore: String): Builder {
            if (lore.isEmpty()) {
                itemMeta.lore(null)
            } else {
                itemMeta.lore(lore.map { line ->
                    if (line.isEmpty()) Component.empty()
                    else mm.deserialize(line).decoration(TextDecoration.ITALIC, false)
                })
            }
            return this
        }

        fun setAmount(amount: Int): Builder {
            itemStack.amount = amount
            return this
        }

        fun setCustomModelData(data: Int?): Builder {
            itemMeta.setCustomModelData(data)
            return this
        }

        fun setItemModel(key: NamespacedKey?): Builder {
            itemMeta.setItemModel(key)
            return this
        }

        fun setItemModel(namespace: String, key: String): Builder {
            return setItemModel(NamespacedKey(namespace, key))
        }

        fun setUnrenamable(unrenamable: Boolean): Builder {
            val key = NamespacedKey(MixelPreview.instance, "unrenamable")
            if (unrenamable) {
                pdc(key, PersistentDataType.BOOLEAN, true)
            } else {
                itemMeta.persistentDataContainer.remove(key)
            }
            return this
        }

        fun setUnenchantable(unenchantable: Boolean): Builder {
            val key = NamespacedKey(MixelPreview.instance, "unenchantable")
            if (unenchantable) {
                pdc(key, PersistentDataType.BOOLEAN, true)
            } else {
                itemMeta.persistentDataContainer.remove(key)
            }
            return this
        }

        fun setUnbreakable(unbreakable: Boolean): Builder {
            itemMeta.isUnbreakable = unbreakable
            return this
        }

        fun setMaterial(material: Material): Builder {
            itemStack.type = material
            return this
        }

        fun setEnchantmentGlintOverride(override: Boolean): Builder {
            itemMeta.setEnchantmentGlintOverride(override)
            return this
        }

        fun setFireResistant(fireResistant: Boolean): Builder {
            itemMeta.isFireResistant = fireResistant
            return this
        }

        fun setMaxStackSize(size: Int?): Builder {
            itemMeta.setMaxStackSize(size)
            return this
        }

        fun setRarity(rarity: ItemRarity): Builder {
            itemMeta.setRarity(rarity)
            return this
        }

        fun setHideTooltip(hide: Boolean): Builder {
            itemMeta.setHideTooltip(hide)
            return this
        }

        fun hideAdditionalInfo(): Builder {
            itemMeta.addItemFlags(ItemFlag.HIDE_ADDITIONAL_TOOLTIP)
            return this
        }

        fun hideEnchants(): Builder {
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
            return this
        }

        fun addEnchant(enchant: Enchantment, level: Int, ignoreRestriction: Boolean): Builder {
            itemMeta.addEnchant(enchant, level, ignoreRestriction)
            return this
        }

        fun addItemFlags(vararg flags: ItemFlag): Builder {
            itemMeta.addItemFlags(*flags)
            return this
        }

        fun <T : Any, Z : Any> setPersistentData(key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z): Builder {
            itemMeta.persistentDataContainer.set(key, type, value)
            return this
        }

        fun setColor(color: Color): Builder {
            when (val meta = itemMeta) {
                is LeatherArmorMeta -> meta.setColor(color)
                is PotionMeta -> meta.setColor(color)
                is MapMeta -> meta.setColor(color)
            }
            return this
        }

        fun setSkullOwner(uuid: UUID): Builder {
            (itemMeta as? SkullMeta)?.let {
                it.playerProfile = Bukkit.createProfile(uuid)
            }
            return this
        }

        fun setSkullTexture(base64: String): Builder {
            (itemMeta as? SkullMeta)?.let {
                val profile = Bukkit.createProfile(UUID.randomUUID())
                profile.setProperty(ProfileProperty("textures", base64))
                it.playerProfile = profile
            }
            return this
        }

        fun setFood(consumer: Consumer<FoodComponent>): Builder {
            val food = itemMeta.food
            consumer.accept(food)
            itemMeta.setFood(food)
            return this
        }

        fun setTool(consumer: Consumer<ToolComponent>): Builder {
            val tool = itemMeta.tool
            consumer.accept(tool)
            itemMeta.setTool(tool)
            return this
        }

        fun addAttribute(attribute: Attribute, modifier: AttributeModifier): Builder {
            itemMeta.addAttributeModifier(attribute, modifier)
            return this
        }

        fun setEquippable(slot: EquipmentSlot): Builder {
            val equippable = itemMeta.equippable
            equippable.slot = slot
            itemMeta.setEquippable(equippable)
            return this
        }

        fun <T : Any, Z : Any> pdc(key: NamespacedKey, type: PersistentDataType<T, Z>, value: Z): Builder {
            itemMeta.persistentDataContainer.set(key, type, value)
            return this
        }

        fun build(): ItemStack {
            itemStack.itemMeta = itemMeta
            return itemStack
        }
    }
}