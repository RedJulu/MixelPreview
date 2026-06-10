package de.redjulu.mixelPreview.items.impl.crate.creativeAxe

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.items.SpecialItemKeys
import de.redjulu.mixelPreview.types.BlockDataType
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.scheduler.BukkitRunnable
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object CreativeAxeVisualizer {

    private val mm = MiniMessage.miniMessage()
    private val activeDisplays = HashMap<UUID, MutableList<TextDisplay>>()
    private val activeTasks = HashMap<UUID, BukkitRunnable>()
    private val lastSelection = HashMap<UUID, String>()

    fun startVisualizer(player: Player) {
        if (activeTasks.containsKey(player.uniqueId)) return

        val task = object : BukkitRunnable() {
            override fun run() {
                if (!player.isOnline) {
                    stopVisualizer(player)
                    return
                }

                val item = player.inventory.contents
                    .filterNotNull()
                    .firstOrNull { SpecialItemKeys.isSpecialItem(it, CreativeAxe.id) }

                if (item == null) {
                    stopVisualizer(player)
                    return
                }

                val meta = item.itemMeta ?: run { clearDisplays(player); return }
                val pdc = meta.persistentDataContainer

                val loc1 = pdc.get(CreativeAxe.block1Key, BlockDataType)
                val loc2 = pdc.get(CreativeAxe.block2Key, BlockDataType)

                if (loc1 == null || loc2 == null || loc1.world != loc2.world) {
                    clearDisplays(player)
                    lastSelection.remove(player.uniqueId)
                    return
                }

                renderFrameAndLabels(player, loc1, loc2)
            }
        }

        task.runTaskTimer(MixelPreview.instance, 0L, 5L)
        activeTasks[player.uniqueId] = task
    }

    fun stopVisualizer(player: Player) {
        activeTasks.remove(player.uniqueId)?.cancel()
        clearDisplays(player)
        lastSelection.remove(player.uniqueId)
    }

    private fun clearDisplays(player: Player) {
        activeDisplays.remove(player.uniqueId)?.forEach { it.remove() }
    }

    private fun renderFrameAndLabels(player: Player, loc1: Location, loc2: Location) {
        val world = loc1.world

        val minX = min(loc1.blockX, loc2.blockX).toDouble()
        val maxX = max(loc1.blockX, loc2.blockX).toDouble() + 1.0
        val minY = min(loc1.blockY, loc2.blockY).toDouble()
        val maxY = max(loc1.blockY, loc2.blockY).toDouble() + 1.0
        val minZ = min(loc1.blockZ, loc2.blockZ).toDouble()
        val maxZ = max(loc1.blockZ, loc2.blockZ).toDouble() + 1.0

        val length = (abs(loc1.blockX - loc2.blockX) + 1)
        val height = (abs(loc1.blockY - loc2.blockY) + 1)
        val width = (abs(loc1.blockZ - loc2.blockZ) + 1)

        val dustOptions = Particle.DustOptions(Color.RED, 1.0f)
        val density = 0.5

        var x = minX
        while (x <= maxX) {
            player.spawnParticle(Particle.DUST, Location(world, x, minY, minZ), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, x, maxY, minZ), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, x, minY, maxZ), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, x, maxY, maxZ), 1, dustOptions)
            x += density
        }

        var y = minY
        while (y <= maxY) {
            player.spawnParticle(Particle.DUST, Location(world, minX, y, minZ), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, maxX, y, minZ), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, minX, y, maxZ), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, maxX, y, maxZ), 1, dustOptions)
            y += density
        }

        var z = minZ
        while (z <= maxZ) {
            player.spawnParticle(Particle.DUST, Location(world, minX, minY, z), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, maxX, minY, z), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, minX, maxY, z), 1, dustOptions)
            player.spawnParticle(Particle.DUST, Location(world, maxX, maxY, z), 1, dustOptions)
            z += density
        }

        val currentSelectionKey = "${loc1.blockX},${loc1.blockY},${loc1.blockZ}|${loc2.blockX},${loc2.blockY},${loc2.blockZ}"

        if (lastSelection[player.uniqueId] == currentSelectionKey) {
            val displays = activeDisplays[player.uniqueId]
            if (displays != null && displays.size == 3 && displays[0].isValid) {
                return
            }
        }

        lastSelection[player.uniqueId] = currentSelectionKey
        clearDisplays(player)

        val centerLength = Location(world, (minX + maxX) / 2.0, minY + 0.3, minZ - 0.3)
        val centerWidth = Location(world, minX - 0.3, minY + 0.3, (minZ + maxZ) / 2.0)
        val centerHeight = Location(world, maxX + 0.3, (minY + maxY) / 2.0, maxZ + 0.3)

        val textLength = "<red>Länge: $length"
        val textWidth = "<red>Breite: $width"
        val textHeight = "<red>Höhe: $height"

        val newDisplays = arrayListOf(
            spawnPrivateTextDisplay(player, centerLength, textLength),
            spawnPrivateTextDisplay(player, centerWidth, textWidth),
            spawnPrivateTextDisplay(player, centerHeight, textHeight)
        )
        activeDisplays[player.uniqueId] = newDisplays
    }

    private fun spawnPrivateTextDisplay(player: Player, location: Location, text: String): TextDisplay {
        return location.world.spawn(location, TextDisplay::class.java) { display ->
            display.text(mm.deserialize(text))
            display.billboard = Display.Billboard.CENTER
            display.isSeeThrough = true
            display.isShadowed = true
            display.isVisibleByDefault = false

            player.showEntity(MixelPreview.instance, display)
        }
    }

    fun isActive(player: Player): Boolean = activeTasks.containsKey(player.uniqueId)

    fun clearLastSelection(player: Player) {
        lastSelection.remove(player.uniqueId)
        activeDisplays.remove(player.uniqueId)?.forEach {
            if (it.isValid) it.remove()
        }
    }
}