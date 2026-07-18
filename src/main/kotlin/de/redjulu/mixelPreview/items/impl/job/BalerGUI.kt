package de.redjulu.mixelPreview.items.impl.job

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.utils.GUIStage
import de.redjulu.mixelPreview.utils.ItemBuilder
import de.redjulu.mixelPreview.utils.SimpleGUI
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

class BalerGUI : SimpleGUI<Unit, BalerGUI.BalerStages>(
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
                renderResult(player)
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
            val count = getInteractableItemCount()
            if (count < 9) {
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                return@setButton
            }
            wheatAmount = count
            isSwitching = true
            advanceStage(p)
        }
    }

    private var countdownTask: BukkitTask? = null
    private var countdownTick = 0

    fun renderWait(player: Player) {
        setType(InventoryType.HOPPER, player)
        setTitle(mm.deserialize("<gradient:#D0B726:#CB691A>Ballenpresse</gradient> <dark_gray>| ${currentStage?.displayName}"), player)
        isDialogOpen = true

        val clockFrames = (5 downTo 1).map { seconds ->
            ItemBuilder(Material.CLOCK)
                .setName("<gold>$seconds Sekunden")
                .setMiniMessageLore(
                    "",
                    " <dark_gray><b>▸</b> <white>Die Ballenpresse arbeitet...",
                    "   <white>Das GUI kann <red>nicht geschlossen</red> werden.",
                )
                .hideAdditionalInfo()
                .build()
        }
        setAnimatedItem(2, clockFrames)

        countdownTick = 0
        countdownTask?.cancel()
        countdownTask = Bukkit.getScheduler().runTaskTimer(MixelPreview.instance, Runnable {
            countdownTick++
            tickAnimations(countdownTick.toLong())
            if (countdownTick >= 5) {
                countdownTask?.cancel()
                countdownTask = null
                isDialogOpen = false
                isSwitching = true
                advanceStage(player)
            }
        }, 20L, 20L)
    }

    override fun onClose(player: Player) {
        if (isInternalClose()) return

        if (isDialogOpen) {
            Bukkit.getScheduler().runTask(MixelPreview.instance, Runnable {
                if (isDialogOpen && player.isOnline) {
                    reopenFor(player)
                }
            })
            return
        }

        if (currentStage == BalerStages.RESULT) {
            giveResults(player)
        }

        countdownTask?.cancel()
        countdownTask = null
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
        )

        setButton(2, ItemBuilder.placeholder(Material.BLACK_STAINED_GLASS_PANE).build())

        setButton(3, ItemBuilder(Material.WHEAT)
            .setName("<gradient:#D0B726:#CB691A>Weizen</gradient> <dark_gray>x<gold>$wheatCount")
            .hideAdditionalInfo()
            .build()
        )
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
}