package de.redjulu.mixelPreview.items

import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.block.Block
import org.bukkit.entity.Display
import org.bukkit.entity.TextDisplay
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f

object SpecialBlockHolo {

    private val mm = MiniMessage.miniMessage()
    private val holoData = mutableMapOf<String, HoloData>()

    private class HoloData(
        val text: String,
        var display: TextDisplay?
    )

    private val zeroRotation = AxisAngle4f(0f, 0f, 0f, 1f)

    private fun TextDisplay.applyDefaults() {
        billboard = Display.Billboard.CENTER
        isPersistent = false
        isShadowed = false
        isSeeThrough = true
        brightness = Display.Brightness(15, 15)
        viewRange = 5.0f
        transformation = Transformation(
            Vector3f(0f, 0f, 0f),
            zeroRotation,
            Vector3f(1f, 1f, 1f),
            zeroRotation
        )
    }

    fun spawn(block: Block, text: String) {
        remove(block)
        SpecialItemKeys.saveBlockHolo(block, text)
        val loc = block.location.add(0.5, 1.5, 0.5)
        val display = block.world.spawn(loc, TextDisplay::class.java) { d ->
            d.text(mm.deserialize(text))
            d.applyDefaults()
        }
        holoData[locationKey(block)] = HoloData(text, display)
    }

    fun remove(block: Block) {
        SpecialItemKeys.clearBlockHolo(block)
        val key = locationKey(block)
        holoData.remove(key)?.display?.let {
            if (it.isValid) it.remove()
        }
    }

    fun toggle(block: Block, visible: Boolean) {
        val key = locationKey(block)
        val data = holoData[key]
        if (data == null) {
            val saved = SpecialItemKeys.loadBlockHolo(block) ?: return
            if (visible) {
                val loc = block.location.add(0.5, 1.5, 0.5)
                val display = block.world.spawn(loc, TextDisplay::class.java) { d ->
                    d.text(mm.deserialize(saved))
                    d.applyDefaults()
                }
                holoData[key] = HoloData(saved, display)
            }
            return
        }
        if (visible) {
            if (data.display == null || !data.display!!.isValid) {
                val loc = block.location.add(0.5, 1.5, 0.5)
                data.display = block.world.spawn(loc, TextDisplay::class.java) { d ->
                    d.text(mm.deserialize(data.text))
                    d.applyDefaults()
                }
            }
        } else {
            data.display?.let { if (it.isValid) it.remove() }
            data.display = null
        }
    }

    fun isHoloVisible(block: Block): Boolean {
        val data = holoData[locationKey(block)] ?: return false
        return data.display?.isValid == true
    }

    fun respawnAllForChunk(chunk: org.bukkit.Chunk) {
        for (state in chunk.tileEntities) {
            if (state is org.bukkit.block.TileState) {
                val holo = SpecialItemKeys.loadBlockHolo(state.block)
                if (holo != null) {
                    spawn(state.block, holo)
                }
            }
        }
    }

    private fun locationKey(block: Block): String =
        "${block.world.name}:${block.x}:${block.y}:${block.z}"
}
