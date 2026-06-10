package de.redjulu.mixelPreview.items.impl.crate

import de.redjulu.mixelPreview.items.SpecialItem
import de.redjulu.mixelPreview.items.SpecialItemCategory
import de.redjulu.mixelPreview.utils.ItemBuilder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Block
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.Damageable
import org.bukkit.plugin.java.JavaPlugin
import java.util.LinkedList
import java.util.Queue
import kotlin.random.Random

object MushroomAxe : SpecialItem("mushroom_axe", SpecialItemCategory.CRATE) {
    override val displayName: String
        get() = "<b><gradient:#E93434:#877740:#045C5C>Pilzholzer</gradient></b>"

    override fun createItem(): ItemStack = tag(
        ItemBuilder(Material.NETHERITE_AXE)
            .setName(displayName)
            .setMiniMessageLore(
                "",
                " <dark_gray><b>▸</b> <gradient:#E93434:#877740:#045C5C>Pilzbäume weg im Nu...</gradient>",
                "",
                " <aqua><b>🛈</b> <white>Mit dieser Axt kannst du",
                "    <white>ganze <gradient:#E93434:#877740:#045C5C>Pilzbäume</gradient> mit",
                "    <white>Baumkrone abbauen."
            )
            .addEnchant(Enchantment.EFFICIENCY, 3, true)
    ).build()

    override fun onBreakWith(event: BlockBreakEvent) {
        val player = event.player
        val startBlock = event.block
        val startType = startBlock.type

        if (!isMushroomStem(startType)) return
        if (player.hasCooldown(Material.NETHERITE_AXE)) return

        var checkBlock = startBlock.getRelative(0, -1, 0)
        var stemBelowCount = 0
        while (isMushroomStem(checkBlock.type)) {
            stemBelowCount++
            checkBlock = checkBlock.getRelative(0, -1, 0)
        }
        if (stemBelowCount > 2) return

        player.setCooldown(Material.NETHERITE_AXE, 60)
        fellMushroom(startBlock, player)
    }

    private fun fellMushroom(startBlock: Block, player: Player) {
        val stemBlocks = mutableSetOf<Block>()
        val capBlocks = mutableSetOf<Block>()

        val stemQueue: Queue<Block> = LinkedList()
        stemQueue.add(startBlock)
        stemBlocks.add(startBlock)

        val maxBlocks = 600

        while (stemQueue.isNotEmpty() && (stemBlocks.size + capBlocks.size) < maxBlocks) {
            val current = stemQueue.poll()

            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val relative = current.getRelative(x, y, z)

                        if (relative !in stemBlocks && isMushroomStem(relative.type)) {
                            stemBlocks.add(relative)
                            stemQueue.add(relative)
                        }
                    }
                }
            }
        }

        val capQueue: Queue<Block> = LinkedList(stemBlocks)
        while (capQueue.isNotEmpty() && (stemBlocks.size + capBlocks.size) < maxBlocks) {
            val current = capQueue.poll()

            for (x in -1..1) {
                for (y in -1..1) {
                    for (z in -1..1) {
                        if (x == 0 && y == 0 && z == 0) continue
                        val relative = current.getRelative(x, y, z)

                        if (isMushroomStem(relative.type)) continue

                        if (relative !in stemBlocks && relative !in capBlocks && isMushroomCap(relative.type)) {
                            capBlocks.add(relative)
                            capQueue.add(relative)
                        }
                    }
                }
            }
        }

        val allBlocks = (stemBlocks + capBlocks).filter { it != startBlock }
        if (allBlocks.isEmpty()) return

        player.world.playSound(player.location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f)

        val blocksByY = allBlocks.groupBy { it.y }.toSortedMap()
        val yLayers = blocksByY.keys.toList()
        var currentLayerIndex = 0

        val plugin = JavaPlugin.getProvidingPlugin(MushroomAxe::class.java)

        Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            if (currentLayerIndex >= yLayers.size) return@Runnable

            val currentY = yLayers[currentLayerIndex]
            val blocksInLayer = blocksByY[currentY] ?: return@Runnable

            for (block in blocksInLayer) {
                if (!isMushroomStem(block.type) && !isMushroomCap(block.type)) continue

                val blockType = block.type
                val isCrimson = blockType == Material.CRIMSON_STEM ||
                        blockType == Material.STRIPPED_CRIMSON_STEM ||
                        blockType == Material.CRIMSON_HYPHAE ||
                        blockType == Material.STRIPPED_CRIMSON_HYPHAE ||
                        blockType == Material.NETHER_WART_BLOCK

                val primaryParticle = if (isCrimson) Particle.CRIMSON_SPORE else Particle.WARPED_SPORE
                val secondaryParticle = if (isCrimson) Particle.DRIPPING_LAVA else Particle.DRIPPING_DRIPSTONE_LAVA

                block.world.spawnParticle(primaryParticle, block.location.add(0.5, 0.5, 0.5), 15, 0.4, 0.4, 0.4, 0.05)
                block.world.spawnParticle(secondaryParticle, block.location.add(0.5, 0.5, 0.5), 3, 0.3, 0.3, 0.3, 0.0)

                if (isMushroomStem(blockType)) {
                    block.world.spawnParticle(Particle.CLOUD, block.location.add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0.02)
                }

                block.world.playSound(block.location, Sound.BLOCK_WOOD_BREAK, 0.4f, 1.2f)
                block.breakNaturally()
            }

            currentLayerIndex++
        }, 1L, 3L)

        damageTool(player, allBlocks.size)
    }

    private fun damageTool(player: Player, blocksBroken: Int) {
        val item = player.inventory.itemInMainHand
        if (item.type == Material.AIR) return

        val meta = item.itemMeta as? Damageable ?: return
        val unbreakingLevel = item.getEnchantmentLevel(Enchantment.UNBREAKING)

        val damageChance = 1.0 / (unbreakingLevel + 1)
        val customDamageModifier = 0.4

        var finalDamage = 0
        for (i in 0 until blocksBroken) {
            if (Random.nextDouble() < (damageChance * customDamageModifier)) {
                finalDamage++
            }
        }

        if (finalDamage > 0) {
            meta.damage += finalDamage

            if (meta.damage >= item.type.maxDurability) {
                player.inventory.setItemInMainHand(null)
                player.playSound(player.location, Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f)
            } else {
                item.itemMeta = meta
            }
        }
    }

    private fun isMushroomStem(material: Material): Boolean {
        return material == Material.WARPED_STEM ||
                material == Material.CRIMSON_STEM ||
                material == Material.STRIPPED_WARPED_STEM ||
                material == Material.STRIPPED_CRIMSON_STEM ||
                material == Material.WARPED_HYPHAE ||
                material == Material.CRIMSON_HYPHAE ||
                material == Material.STRIPPED_WARPED_HYPHAE ||
                material == Material.STRIPPED_CRIMSON_HYPHAE
    }

    private fun isMushroomCap(material: Material): Boolean {
        return material == Material.NETHER_WART_BLOCK ||
                material == Material.WARPED_WART_BLOCK ||
                material == Material.SHROOMLIGHT
    }
}
