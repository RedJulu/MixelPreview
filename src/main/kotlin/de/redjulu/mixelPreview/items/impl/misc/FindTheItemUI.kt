package de.redjulu.mixelPreview.items.impl.misc

import de.redjulu.mixelPreview.MixelPreview
import de.redjulu.mixelPreview.utils.DialogBuilder
import de.redjulu.mixelPreview.utils.LoadingTitle
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Chest
import org.bukkit.block.Container
import org.bukkit.block.DoubleChest
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitTask

class FindTheItemUI {
    private val mm = MiniMessage.miniMessage()
    private val plugin = MixelPreview.instance

    private val containerMaterials = setOf(
        Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL,
        Material.HOPPER, Material.DROPPER, Material.DISPENSER,
        Material.FURNACE, Material.BLAST_FURNACE, Material.SMOKER,
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX
    )

    fun open(player: Player) {
        DialogBuilder()
            .title(mm.deserialize("<gradient:#3CBBE3:#9822AB>Find-The-Item</gradient>"))
            .addText(mm.deserialize("<green>Finde deine Items schnell (Mindestens 3 Zeichen)"))
            .addTextInput("text", mm.deserialize("<gold>Bitte gebe den Namen des Items ein:"), null, 32)
            .addButton(mm.deserialize("<green>Suchen")) { ctx, _ ->
                val rawInput = ctx.getText("text")
                if (rawInput.isNullOrEmpty()) {
                    open(player)
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return@addButton
                }

                val input = rawInput.trim()

                if (input.length < 3) {
                    open(player)
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    player.sendMessage(mm.deserialize("<red>Bitte mindestens 3 Zeichen eingeben."))
                    return@addButton
                }

                player.setCooldown(Material.WRITTEN_BOOK, 20 * 3)

                LoadingTitle.load(player)

                plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable {
                    val candidates = collectCandidateLocations(player)

                    plugin.server.scheduler.runTask(plugin, Runnable {
                        val containers = scanContainers(candidates, input)

                        LoadingTitle.finish(player, "<green><b>✔", "<gray>Suche erfolgreich")

                        if (containers.isEmpty()) {
                            player.sendMessage(mm.deserialize("<red>Keine Items gefunden."))
                            return@Runnable
                        }

                        val containerCount = containers.size
                        player.sendMessage(
                            mm.deserialize("<green>✔ <green>Item in <b>$containerCount</b> ${if (containerCount > 1) "Containern" else "Container"} gefunden!")
                        )

                        val task = markLocations(player, containers)
                        plugin.server.scheduler.runTaskLater(plugin, Runnable {
                            task.cancel()
                        }, 20L * 8)
                    })
                })
            }
            .show(player)
    }

    private fun collectCandidateLocations(player: Player, radius: Int = 40): List<Location> {
        val candidates = mutableListOf<Location>()
        val origin = player.location
        val world = player.world

        val minX = origin.blockX - radius
        val maxX = origin.blockX + radius
        val minY = (origin.blockY - radius).coerceAtLeast(world.minHeight)
        val maxY = (origin.blockY + radius).coerceAtMost(world.maxHeight)
        val minZ = origin.blockZ - radius
        val maxZ = origin.blockZ + radius

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    val block = world.getBlockAt(x, y, z)
                    if (block.type in containerMaterials) {
                        candidates.add(block.location)
                    }
                }
            }
        }

        return candidates
    }

    private fun scanContainers(locations: List<Location>, query: String): List<List<Location>> {
        val results = mutableListOf<List<Location>>()
        val processedDoubleChests = mutableSetOf<Location>()

        locations.forEach { loc ->
            val state = loc.block.state
            if (state !is Container) return@forEach

            if (state is Chest) {
                val inventory = state.inventory
                val holder = inventory.holder

                if (holder is DoubleChest) {
                    val leftLoc = (holder.leftSide as? Chest)?.location ?: loc
                    val rightLoc = (holder.rightSide as? Chest)?.location ?: loc

                    val keyLoc = minOf(
                        leftLoc, rightLoc,
                        compareBy({ it.blockX }, { it.blockY }, { it.blockZ })
                    )

                    if (keyLoc in processedDoubleChests) return@forEach
                    processedDoubleChests.add(keyLoc)

                    val found = inventory.contents
                        .filterNotNull()
                        .any { item -> matchesQuery(item, query) }

                    if (found) {
                        results.add(listOf(leftLoc, rightLoc))
                    }
                    return@forEach
                }
            }

            val found = state.inventory.contents
                .filterNotNull()
                .any { item -> matchesQuery(item, query) }

            if (found) results.add(listOf(loc))
        }

        return results
    }

    private fun matchesQuery(item: ItemStack, query: String): Boolean {
        val customName = item.itemMeta?.displayName()
            ?.let { mm.serialize(it) }
            ?.replace(Regex("<[^>]*>"), "")
            ?.trim()

        val translationKey = item.type.translationKey()
        val vanillaName = translationKey
            .removePrefix("item.minecraft.")
            .removePrefix("block.minecraft.")
            .replace("_", " ")

        val materialName = item.type.name.replace("_", " ")

        return listOfNotNull(customName, vanillaName, materialName)
            .any { it.contains(query, ignoreCase = true) }
    }

    private fun markLocations(player: Player, locations: List<List<Location>>): BukkitTask {
        var ticks = 0
        return plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            ticks += 2
            if (ticks >= 20 * 8) return@Runnable

            locations.forEach { group ->
                val minX = group.minOf { it.blockX }.toDouble()
                val minY = group.minOf { it.blockY }.toDouble()
                val minZ = group.minOf { it.blockZ }.toDouble()
                val maxX = group.maxOf { it.blockX }.toDouble() + 1.0
                val maxY = minY + 1.0
                val maxZ = group.maxOf { it.blockZ }.toDouble() + 1.0

                val world = group[0].world
                val dustOptions = Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.0f)

                for (t in 0..10) {
                    val s = t / 10.0
                    player.spawnParticle(Particle.DUST, lerp(world, minX, minY, minZ, maxX, minY, minZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, minX, minY, maxZ, maxX, minY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, minX, minY, minZ, minX, minY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, maxX, minY, minZ, maxX, minY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)

                    player.spawnParticle(Particle.DUST, lerp(world, minX, maxY, minZ, maxX, maxY, minZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, minX, maxY, maxZ, maxX, maxY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, minX, maxY, minZ, minX, maxY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, maxX, maxY, minZ, maxX, maxY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)

                    player.spawnParticle(Particle.DUST, lerp(world, minX, minY, minZ, minX, maxY, minZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, maxX, minY, minZ, maxX, maxY, minZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, minX, minY, maxZ, minX, maxY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    player.spawnParticle(Particle.DUST, lerp(world, maxX, minY, maxZ, maxX, maxY, maxZ, s), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                }
            }
        }, 0L, 4L)
    }

    private fun lerp(world: org.bukkit.World, x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double, t: Double): Location {
        return Location(world, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t)
    }
}