package de.redjulu.mixelPreview.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

abstract class RechargeUI(
    title: Component,
    private val hand: EquipmentSlot,
    private val leftAmount: Int = 1,
    private val rightAmount: Int = 32
) : SimpleGUI<Unit, Unit>(1, title, 0, 0, 0, 0, InventoryType.HOPPER) {

    protected val mm = MiniMessage.miniMessage()
    protected lateinit var rechargeItem: ItemStack

    private val effectiveLeft: Int get() = if (leftAmount != 0) leftAmount else rightAmount
    private val effectiveRight: Int get() = if (rightAmount != 0) rightAmount else leftAmount

    override fun compose(player: Player) {
        rechargeItem = getItem(player) ?: return

        fillSlots(ItemBuilder.placeholder(Material.ORANGE_STAINED_GLASS_PANE).build(), 1..3, 2)

        setButton(0, buildDisplay(rechargeItem))
        setButton(2, buildInfo())
        setButton(4, buildRechargeButton()) { _, click ->
            val amount = when {
                click.isLeftClick -> effectiveLeft
                click.isRightClick -> effectiveRight
                else -> return@setButton
            }
            if (amount <= 0) return@setButton
            if (onRecharge(player, amount)) {
                val updated = getItem(player) ?: return@setButton
                rechargeItem = updated
                setButton(0, buildDisplay(updated))
                player.sendActionBar(mm.deserialize("<dark_gray>» <green>+ <yellow>$amount!"))
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f)
            }
        }
    }

    // --- Item helpers ---

    protected fun getItem(player: Player): ItemStack? {
        return if (hand == EquipmentSlot.HAND) player.itemInHand
        else player.inventory.getItemInOffHand()
    }

    protected fun setItem(player: Player, item: ItemStack) {
        if (hand == EquipmentSlot.HAND) player.setItemInHand(item)
        else player.inventory.setItemInOffHand(item)
    }

    protected fun removeItem(player: Player) {
        if (hand == EquipmentSlot.HAND) player.setItemInHand(null)
        else player.inventory.setItemInOffHand(null)
    }

    // --- Charge helpers ---

    protected fun getCharges(item: ItemStack, key: org.bukkit.NamespacedKey): Int {
        return item.persistentDataContainer.get(key, PersistentDataType.INTEGER) ?: 0
    }

    protected fun setCharges(item: ItemStack, key: org.bukkit.NamespacedKey, charges: Int): ItemStack {
        val meta = item.itemMeta ?: return item
        meta.persistentDataContainer.set(key, PersistentDataType.INTEGER, charges)
        item.itemMeta = meta
        return item
    }

    protected fun addCharges(player: Player, key: org.bukkit.NamespacedKey, amount: Int): Boolean {
        val item = getItem(player) ?: return false
        val current = getCharges(item, key)
        val updated = setCharges(item, key, current + amount)
        setItem(player, updated)
        return true
    }

    // --- Inventory helpers ---

    protected fun countInInventory(player: Player, material: Material, ignore: ((ItemStack) -> Boolean)? = null): Int {
        var total = 0
        for (slot in 0 until player.inventory.size) {
            val stack = player.inventory.getItem(slot) ?: continue
            if (stack.type != material) continue
            if (ignore != null && ignore(stack)) continue
            total += stack.amount
        }
        return total
    }

    protected fun removeFromInventory(player: Player, material: Material, amount: Int, ignore: ((ItemStack) -> Boolean)? = null): Int {
        var remaining = amount
        for (slot in 0 until player.inventory.size) {
            if (remaining <= 0) break
            val stack = player.inventory.getItem(slot) ?: continue
            if (stack.type != material) continue
            if (ignore != null && ignore(stack)) continue
            val toRemove = minOf(remaining, stack.amount)
            if (toRemove >= stack.amount) {
                player.inventory.setItem(slot, null)
            } else {
                val updated = stack.clone()
                updated.amount = stack.amount - toRemove
                player.inventory.setItem(slot, updated)
            }
            remaining -= toRemove
        }
        return amount - remaining
    }

    protected fun hasInInventory(player: Player, material: Material, amount: Int, ignore: ((ItemStack) -> Boolean)? = null): Boolean {
        return countInInventory(player, material, ignore) >= amount
    }

    // --- Display helpers ---

    protected fun buildChargeDisplay(material: Material, name: String, charges: Int, maxCharges: Int = -1, extraLore: List<String> = emptyList()): ItemStack {
        val lore = mutableListOf<String>()
        lore.add("")
        lore.add(" <dark_gray><b>▸</b> <gray>Aufladungen: <yellow>$charges" + if (maxCharges > 0) " <dark_gray>/<dark_gray> <yellow>$maxCharges" else "")
        lore.addAll(extraLore)
        lore.add("")
        return ItemBuilder(material)
            .setName(name)
            .setMiniMessageLore(*lore.toTypedArray())
            .build()
    }

    protected fun buildRechargeBtn(material: Material, name: String, chargeName: String, leftAmt: Int = effectiveLeft, rightAmt: Int = effectiveRight, extraLore: List<String> = emptyList()): ItemStack {
        val lore = mutableListOf<String>()
        lore.add("")
        lore.add(" <dark_gray><b>▸</b> <yellow>Kaufe hier weitere Aufladungen")
        lore.add("")
        if (leftAmt > 0) lore.add(" <dark_gray>▪ <gray>$leftAmt $chargeName <dark_gray>» <aqua>Linksklick")
        if (rightAmt > 0) lore.add(" <dark_gray>▪ <gray>$rightAmt $chargeName <dark_gray>» <aqua>Rechtsklick")
        lore.addAll(extraLore)
        lore.add("")
        return ItemBuilder(material)
            .setName(name)
            .setMiniMessageLore(*lore.toTypedArray())
            .build()
    }

    protected fun buildInfoBtn(material: Material, name: String, lines: List<String>): ItemStack {
        val lore = mutableListOf("")
        lore.addAll(lines)
        lore.add("")
        return ItemBuilder(material)
            .setName(name)
            .setMiniMessageLore(*lore.toTypedArray())
            .build()
    }

    protected fun noFunds(player: Player) {
        player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
    }

    // --- Abstract ---

    abstract fun buildDisplay(item: ItemStack): ItemStack
    abstract fun buildInfo(): ItemStack
    abstract fun buildRechargeButton(): ItemStack
    abstract fun onRecharge(player: Player, amount: Int): Boolean
}
