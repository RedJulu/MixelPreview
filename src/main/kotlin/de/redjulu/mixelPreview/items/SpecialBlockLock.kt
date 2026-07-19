package de.redjulu.mixelPreview.items

import org.bukkit.block.Block
import java.util.UUID

object SpecialBlockLock {

    private val lockedBlocks = mutableMapOf<String, UUID>()
    private val playerBlocks = mutableMapOf<UUID, String>()

    fun lock(block: Block, player: UUID) {
        val key = locationKey(block)
        lockedBlocks[key] = player
        playerBlocks[player] = key
    }

    fun unlock(block: Block) {
        val key = locationKey(block)
        val player = lockedBlocks.remove(key)
        if (player != null) {
            val current = playerBlocks[player]
            if (current == key) playerBlocks.remove(player)
        }
    }

    fun isLocked(block: Block): Boolean =
        lockedBlocks.containsKey(locationKey(block))

    fun getLocker(block: Block): UUID? =
        lockedBlocks[locationKey(block)]

    fun getBlockForPlayer(player: UUID): Block? {
        val key = playerBlocks[player] ?: return null
        val parts = key.split(":")
        if (parts.size != 4) return null
        val world = org.bukkit.Bukkit.getWorld(parts[0]) ?: return null
        return world.getBlockAt(parts[1].toInt(), parts[2].toInt(), parts[3].toInt())
    }

    fun unlockPlayer(player: UUID) {
        val key = playerBlocks.remove(player) ?: return
        val block = lockedBlocks.remove(key) // just remove, we already got it
    }

    fun cleanupOnQuit(player: UUID) {
        val key = playerBlocks.remove(player) ?: return
        lockedBlocks.remove(key)
    }

    private fun locationKey(block: Block): String =
        "${block.world.name}:${block.x}:${block.y}:${block.z}"
}
