package de.redjulu.mixelPreview.items.impl.crate.creativeAxe

import com.fastasyncworldedit.core.FaweAPI
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.math.BlockVector3
import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxe.block1Key
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxe.block2Key
import de.redjulu.mixelPreview.types.BlockDataType
import de.redjulu.mixelPreview.utils.BaseGUI
import de.redjulu.mixelPreview.utils.DialogBuilder
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.LoadingTitle
import de.redjulu.mixelPreview.utils.SimpleGUI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CreativeAxeUI(
    val axe: ItemStack,
    val showDeactivate: Boolean = true
) : SimpleGUI<Unit, Unit>(
    1,
    MiniMessage.miniMessage().deserialize("<b><gradient:#CC1DDE:#9836D4:#446CBB:#6BCD67>Kreative Axt</gradient></b>"),
    0, 0, 0, 0
) {

    private val mm = MiniMessage.miniMessage()
    private var selectedItem: ItemStack? = null

    override fun compose(player: Player) {
        val currentAxe = player.inventory.itemInMainHand
            .takeIf { SpecialItemKeys.isSpecialItem(it, CreativeAxe.id) }
            ?: axe

        setButton(2, ItemBuilder(currentAxe).build()) { p, click ->
            if (click != ClickType.LEFT) return@setButton
            CreativeAxeUI(currentAxe, false).openDialog(p)
        }

        val placeholder = ItemBuilder(Material.LIME_STAINED_GLASS_PANE)
            .setName(mm.deserialize("<green>Block einlegen"))
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <white>Lege hier den Block ein mit dem die",
                "   <blue>Selektion <white>gefüllt werden soll.",
                ""
            )
            .hideAdditionalInfo()
            .build()

        setButton(4, placeholder)
        setPlaceholder(4) { it.type.isBlock }
        setPriority(4)
        setIgnored(4)

        val currentItem = selectedItem ?: inventory.getItem(4)
        val selectedMaterialName = if (currentItem == null || currentItem.type.isAir || BaseGUI.isPlaceholderItem(currentItem)) {
            "<gray><em>Keine Auswahl</em>"
        } else {
            currentItem.type.name
                .lowercase()
                .replace("_", " ")
                .replaceFirstChar { it.uppercase() }
        }

        val needed = run {
            val m = currentAxe.itemMeta ?: return@run 0
            val l1 = m.persistentDataContainer.get(block1Key, BlockDataType) ?: return@run 0
            val l2 = m.persistentDataContainer.get(block2Key, BlockDataType) ?: return@run 0
            if (l1.world != l2.world) return@run 0

            val world = l1.world
            val minX = min(l1.blockX, l2.blockX)
            val minY = min(l1.blockY, l2.blockY)
            val minZ = min(l1.blockZ, l2.blockZ)
            val maxX = max(l1.blockX, l2.blockX)
            val maxY = max(l1.blockY, l2.blockY)
            val maxZ = max(l1.blockZ, l2.blockZ)

            var empty = 0
            for (x in minX..maxX)
                for (y in minY..maxY)
                    for (z in minZ..maxZ)
                        if (world.getBlockAt(x, y, z).type == Material.AIR) empty++
            empty
        }

        val isCreative = player.gameMode == GameMode.CREATIVE

        val available = if (isCreative) needed else {
            if (selectedItem != null && selectedItem!!.type != Material.AIR)
                countMaterial(player, selectedItem!!.type) + 1 else 0
        }

        val hasEnough = selectedItem != null && (isCreative || available >= needed)
        val countDisplay = if (selectedItem != null) {
            if (isCreative) "<green>∞<gray>/<yellow>$needed"
            else "<yellow>$available<gray>/<yellow>$needed"
        } else "<gray>-<gray>/<yellow>$needed"

        setButton(6, ItemBuilder(Material.PLAYER_HEAD)
            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMyMjQxODk5NjQ5NDRkNTYwZjE5YTQ0ZjE5ZjhhNDZhZWYzZmVlNDc1NiJ9fX0=")
            .setName(mm.deserialize("<green>Füllen"))
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <white>Füllt die aktuelle <blue>Selektion <white>mit",
                "   <white>dem eingelegtem Material.",
                "",
                " <dark_gray><b>▸</b> <yellow>$selectedMaterialName",
                " <dark_gray><b>▸</b> <white>Blöcke: $countDisplay"
            )
            .hideAdditionalInfo()
            .build()
        ) { p, click ->
            if (click != ClickType.LEFT) return@setButton

            if (selectedItem == null) {
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                return@setButton
            }

            if (!hasEnough) {
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                p.sendActionBar(mm.deserialize("<red>Nicht genug Blöcke! <dark_gray>(<yellow>$available<gray>/<yellow>$needed<dark_gray>)"))
                return@setButton
            }

            val placedItem = inventory.getItem(4)?.clone()
            inventory.setItem(4, null)
            selectedItem = null

            isSwitching = true
            p.closeInventory()

            val material = placedItem!!.type

            LoadingTitle.load(p)

            val success = fill(p, currentAxe, material, placedItem)

            if (!success) {
                val leftover = p.inventory.addItem(placedItem)
                leftover.values.forEach { p.world.dropItemNaturally(p.location, it) }
            }
        }
    }

    override fun onPlaceholderUpdate(player: Player, slot: Int, item: ItemStack) {
        if (slot == 4) {
            selectedItem = if (BaseGUI.isPlaceholderItem(item) || item.type.isAir) null else item

            if (item.amount > 1) {
                val excess = item.clone()
                excess.amount = item.amount - 1
                inventory.setItem(slot, item.clone().also { it.amount = 1 })
                val leftover = player.inventory.addItem(excess)
                leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
            }

            updateExceptPlaceholders(player)
        }
    }

    override fun onGuiClose(player: Player) {
        val slotItem = inventory.getItem(4)
        if (slotItem != null && !slotItem.type.isAir && !BaseGUI.isPlaceholderItem(slotItem)) {
            val leftover = player.inventory.addItem(slotItem.clone())
            leftover.values.forEach { player.world.dropItemNaturally(player.location, it) }
            inventory.setItem(4, null)
        }
    }

    private fun fill(player: Player, item: ItemStack, material: Material, placedItem: ItemStack): Boolean {
        val meta = item.itemMeta ?: return false
        val loc1 = meta.persistentDataContainer.get(block1Key, BlockDataType) ?: return false
        val loc2 = meta.persistentDataContainer.get(block2Key, BlockDataType) ?: return false

        if (loc1.world != loc2.world) return false

        val world = loc1.world
        val minX = min(loc1.blockX, loc2.blockX)
        val minY = min(loc1.blockY, loc2.blockY)
        val minZ = min(loc1.blockZ, loc2.blockZ)
        val maxX = max(loc1.blockX, loc2.blockX)
        val maxY = max(loc1.blockY, loc2.blockY)
        val maxZ = max(loc1.blockZ, loc2.blockZ)

        val foreignBlocks = mutableListOf<org.bukkit.block.Block>()
        val emptyBlocks = mutableListOf<org.bukkit.block.Block>()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    when {
                        block.type == Material.AIR -> emptyBlocks.add(block)
                        block.type != material -> foreignBlocks.add(block)
                    }
                }
            }
        }

        if (foreignBlocks.isNotEmpty()) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            LoadingTitle.finish(player, "<red><b>❌", "<gray>Füllen fehlgeschlagen <dark_gray>- <gray>Selektion nicht leer")
            return false
        }

        if (emptyBlocks.isEmpty()) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            LoadingTitle.finish(player, "<red><b>❌", "<gray>Füllen fehlgeschlagen <dark_gray>- <gray>bereits gefüllt")
            return false
        }

        val needed = emptyBlocks.size
        val availableInInv = countMaterial(player, material)
        val totalAvailable = if (player.gameMode == GameMode.CREATIVE) needed else availableInInv + 1

        if (player.gameMode != GameMode.CREATIVE && totalAvailable < needed) {
            player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
            LoadingTitle.finish(player, "<red><b>✗", "<gray>Füllen fehlgeschlagen <dark_gray>- <gray>nicht genug Blöcke")
            return false
        }

        if (player.gameMode != GameMode.CREATIVE) {
            removeMaterial(player, material, needed - 1)
        }

        val faweWorld = FaweAPI.getWorld(world.name)
        val blockType = BukkitAdapter.adapt(material.createBlockData()).blockType

        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(faweWorld)
            .build()

        editSession.use { session ->
            for (block in emptyBlocks) {
                session.setBlock(
                    BlockVector3.at(block.x, block.y, block.z),
                    blockType.defaultState
                )
            }
        }

        MixelPreview.instance.server.scheduler.runTask(MixelPreview.instance, Runnable {
            val axeMeta = player.inventory.itemInMainHand.itemMeta
            if (axeMeta != null) {
                axeMeta.persistentDataContainer.remove(block1Key)
                axeMeta.persistentDataContainer.remove(block2Key)
                player.inventory.itemInMainHand.itemMeta = axeMeta
            }

            CreativeAxeVisualizer.stopVisualizer(player)
            player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f)
            player.sendActionBar(mm.deserialize("<green>Selektion gefüllt! <dark_gray>(<gray>$needed Blöcke<dark_gray>)"))
            LoadingTitle.finish(player, "<green><b>✔", "<gray>Selektion gefüllt")
        })

        return true
    }

    private fun countMaterial(player: Player, material: Material): Int {
        return player.inventory.contents
            .filterNotNull()
            .filter { it.type == material }
            .sumOf { it.amount }
    }

    private fun removeMaterial(player: Player, material: Material, amount: Int) {
        var toRemove = amount
        val contents = player.inventory.contents

        for (i in contents.indices) {
            val stack = contents[i] ?: continue
            if (stack.type != material) continue

            if (stack.amount <= toRemove) {
                toRemove -= stack.amount
                player.inventory.setItem(i, null)
            } else {
                stack.amount -= toRemove
                toRemove = 0
            }

            if (toRemove == 0) break
        }
    }

    fun openDialog(player: Player) {
        val meta = axe.itemMeta
        val pdc = meta?.persistentDataContainer
        val loc1 = pdc?.get(CreativeAxe.block1Key, BlockDataType)
        val loc2 = pdc?.get(CreativeAxe.block2Key, BlockDataType)
        val hasSelection = loc1 != null && loc2 != null && loc1.world == loc2.world

        player.playSound(player.location, Sound.BLOCK_CHEST_OPEN, 1f, 1f)

        val dialog = DialogBuilder(mm.deserialize("<b><gradient:#CC1DDE:#9836D4:#446CBB:#6BCD67>Kreative Axt</gradient></b>"))
            .addText(mm.deserialize("<white>Diese Axt kann dir unglaublich viel Zeit und Arbeit ersparen," +
                    " jedoch muss <red>vorsichtig <white>damit umgegangen werden! Hier ein kleines Tutorial:"))
            .addText(mm.deserialize("<gold>1) <white>Markiere mit Linksklick eine Stelle und mit Rechtsklick eine andere Stelle."))
            .addText(mm.deserialize("   <aqua>(i) <white>Beachte, dass in der markierten Region keine anderen Blöcke" +
                    " oder Entitys (z.b. Armor Stands oder Tiere) sein dürfen."))
            .addText(mm.deserialize("<gold>2) <white>Wenn du mit der Markierung zufrieden bist, nutze 'Sneak-Rechtsklick'."))
            .addText(mm.deserialize("<gold>3) <white>Im Inventar werden alle möglichen Blöcke angezeigt," +
                    " womit du die markierte Region befüllen könntest. Wähle den jeweiligen Block aus."))
            .addText(mm.deserialize("<gold>4) <white>Bestätigt die Blockart und die Region wird gefüllt!"))
            .addText(Component.empty())
            .addText(mm.deserialize("<red><b>ACHTUNG</b><white>: Du kannst diese Aktion <red>nicht <white>rückgängig machen." +
                    " Gehe also <red>vorsichtig <white>mit dieser Axt um!"))
            .addButton(mm.deserialize("<green>Okay")) { _, _ ->
                player.closeDialog()
            }

        if (showDeactivate) {
            if (!hasSelection) {
                dialog.addButton(mm.deserialize("<gray><i>Keine Selektion")) { _, _ ->
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f)
                    return@addButton
                }
            } else {
                dialog.addButton(mm.deserialize("<red>Selektion leeren")) { _, _ ->
                    CreativeAxe.clearSelection(player)
                }
            }
        }

        dialog.show(player)
    }
}