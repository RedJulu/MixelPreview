package de.redjulu.mixelPreview.items.impl.job

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialBlockHolo
import de.redjulu.mixelPreview.items.SpecialBlockLock
import de.redjulu.mixelPreview.items.SpecialItemRegistry
import de.redjulu.mixelPreview.utils.GUIStage
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.SimpleGUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

class BalerGUI(private val blockLocation: Location) : SimpleGUI<Unit, BalerGUI.BalerStages>(
    6,
    MiniMessage.miniMessage().deserialize("<gradient:#D0B726:#CB691A>Ballenpresse</gradient> <dark_gray>| "),
    0, 1, 0, 0,
) {

    private val mm = MiniMessage.miniMessage()
    private var wheatAmount = 0

    init {
        defineStages(*BalerStages.entries.toTypedArray())
    }

    override fun compose(player: Player) {
        when (currentStage) {
            BalerStages.HAY -> {
                renderHay(player)
            }

            BalerStages.WAIT -> {
                renderWait(player)
            }

            BalerStages.RESULT -> {
                renderResult(player)
            }

            else -> {
                renderHay(player)
            }
        }
    }

    fun renderHay(player: Player) {
        setTitle(mm.deserialize("<gradient:#D0B726:#CB691A>Ballenpresse</gradient> <dark_gray>| ${currentStage?.displayName}"), player)
        contentSlots.forEach {
            setInteractable(it) { item -> item.type == Material.WHEAT }
        }

        fillSlots(ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE).build(), 45..53, 49)

        setButton(49, ItemBuilder(Material.PLAYER_HEAD)
            .setSkullTexture("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTc5YTVjOTVlZTE3YWJmZWY0NWM4ZGMyMjQxODk5NjQ5NDRkNTYwZjE5YTQ0ZjE5ZjhhNDZhZWYzZmVlNDc1NiJ9fX0=")
            .setName("<green>Konvertieren</green>")
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <white>Konvertiert alles an <gradient:#D0B726:#CB691A>Weizen</gradient>",
                "   <white>schnell zu <gradient:#D0B726:#CB691A>Heuballen</gradient>.",
                "",
                " <dark_gray><b>▸</b> <yellow>Überschüssige Items landen im Ausgabe GUI",
            )
            .hideAdditionalInfo()
            .build()
        ) { p, _ ->
            if (p.hasCooldown(Material.PLAYER_HEAD)) return@setButton
            val count = getInteractableItemCount()
            if (count < 9) {
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                p.setCooldown(Material.PLAYER_HEAD, 15)
                return@setButton
            }
            wheatAmount = count
            isSwitching = true
            advanceStage(p)
        }

        val block = blockLocation.block

        setToggleButton(player, 45, { SpecialBlockHolo.isHoloVisible(block) },
            ItemBuilder(Material.RED_DYE)
            .setName(mm.deserialize("<aqua><i>Hologram ausschalten"))
            .build(),
            ItemBuilder(Material.LIME_DYE)
                .setName(mm.deserialize("<aqua><i>Hologram einschalten"))
                .build()
        ) { p, _ ->
            if (p.hasCooldown(Material.RED_DYE) || p.hasCooldown(Material.LIME_DYE)) return@setToggleButton

            val currentlyVisible = SpecialBlockHolo.isHoloVisible(block)
            SpecialBlockHolo.toggle(block, !currentlyVisible)
            updateToggleButtons(player)

            if (currentlyVisible) {
                p.playSound(p.location, Sound.BLOCK_BEACON_DEACTIVATE, 1f, 1f)
                p.setCooldown(Material.LIME_DYE, 15)
            } else {
                p.playSound(p.location, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f)
                p.setCooldown(Material.RED_DYE, 15)
            }
        }

        setButton(53, ItemBuilder(Material.BARRIER)
            .setName("<red>Block aufheben")
            .build()
        ) { p, _ ->
            val block = blockLocation.block
            Baler.removeBlock(block)
            p.give(SpecialItemRegistry.get(Baler.id)!!.createItem())
            p.closeInventory()
            player.playSound(player.location, Sound.BLOCK_GRASS_PLACE, 1f, 1f)
        }

        addPlayerPaginationButtons(48, 50, 2, player)

    }


    fun renderWait(player: Player) {
        setType(InventoryType.HOPPER, player)
        setTitle(mm.deserialize("<gradient:#D0B726:#CB691A>Ballenpresse</gradient> <dark_gray>| ${currentStage?.displayName}"), player)
        setClosable(false)

        setCountdownItem(2, player, 5, { timeLeft, _ ->
            ItemBuilder(Material.CLOCK)
                .setName("<gold>$timeLeft Sekunden")
                .setMiniMessageLore(
                    "",
                    " <dark_gray><b>▸</b> <white>Die Ballenpresse arbeitet...",
                    "   <white>Das GUI kann <red>nicht geschlossen</red> werden.",
                )
                .hideAdditionalInfo()
                .build()
        }, { _, _ ->
            player.playSound(player.location, Sound.ENTITY_MINECART_RIDING, 0.6f, 1f)
        }) { p ->
            setClosable(true)
            advanceStage(p)
            player.stopSound(Sound.ENTITY_MINECART_RIDING)
            player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f)
        }
    }

    override fun onGuiClose(player: Player) {
        if (currentStage == BalerStages.RESULT) {
            giveResults(player)
        }
        SpecialBlockLock.unlock(blockLocation.block)
    }

    fun renderResult(player: Player) {
        val hayCount = wheatAmount / 9
        val wheatCount = wheatAmount % 9

        setType(InventoryType.HOPPER, player)
        setTitle(mm.deserialize("<gradient:#D0B726:#CB691A>Ballenpresse</gradient> <dark_gray>| ${currentStage?.displayName}"), player)

        setButton(1, ItemBuilder(Material.HAY_BLOCK)
            .setName("<gradient:#D0B726:#CB691A>Heuballen</gradient> <dark_gray>x<gold>$hayCount")
            .hideAdditionalInfo()
            .build()
        ) { p, _ ->
            giveResults(player)
            p.closeInventory()
        }

        setButton(2, ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE).build())

        setButton(3, ItemBuilder(Material.WHEAT)
            .setName("<gradient:#D0B726:#CB691A>Weizen</gradient> <dark_gray>x<gold>$wheatCount")
            .hideAdditionalInfo()
            .build()
        ) { p, _ ->
            giveResults(player)
            p.closeInventory()
        }
    }

    private fun giveResults(player: Player) {
        val hayCount = wheatAmount / 9
        val wheatCount = wheatAmount % 9

        val drops = mutableMapOf<Material, Int>()
        if (hayCount > 0) drops[Material.HAY_BLOCK] = hayCount
        if (wheatCount > 0) drops[Material.WHEAT] = wheatCount

        for ((mat, amount) in drops) {
            val leftover = player.inventory.addItem(ItemStack(mat, amount))
            for (stack in leftover.values) {
                player.world.dropItemNaturally(player.location, stack)
            }
        }

        if (drops.isNotEmpty()) {
            player.playSound(player.location, Sound.ENTITY_ITEM_PICKUP, 1f, 1f)
        }
    }

    enum class BalerStages(override val displayName: String) : GUIStage {
        HAY("<yellow>Weizen Abgabe"),
        WAIT("<blue><i>Konvertierung..."),
        RESULT("<green>Heu Ausgabe"),
    }

    override var openSound: Sound? = Sound.BLOCK_SHULKER_BOX_OPEN

}