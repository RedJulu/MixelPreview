package de.redjulu.mixelPreview.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

abstract class RechargeUI(
    title: Component,
    private val hand: EquipmentSlot
) : SimpleGUI<Unit, Unit>(1, title, 0, 0, 0, 0) {

    protected val mm = MiniMessage.miniMessage()
    protected lateinit var rechargeItem: ItemStack

    override fun compose(player: Player) {
        rechargeItem = getItem(player) ?: return

        fillContentArea(Material.ORANGE_STAINED_GLASS_PANE)

        setButton(1, buildDisplay(rechargeItem))
        setButton(4, buildInfo())
        setButton(7, buildRechargeButton()) { _, click ->
            val amount = if (click.isLeftClick) 1 else if (click.isRightClick) 32 else return@setButton
            if (onRecharge(player, amount)) {
                val updated = getItem(player) ?: return@setButton
                rechargeItem = updated
                setButton(1, buildDisplay(updated))
                player.sendActionBar(mm.deserialize("<dark_gray>» <green>+ <yellow>$amount!"))
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f)
            }
        }
    }

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

    abstract fun buildDisplay(item: ItemStack): ItemStack
    abstract fun buildInfo(): ItemStack
    abstract fun buildRechargeButton(): ItemStack
    abstract fun onRecharge(player: Player, amount: Int): Boolean
}
