package de.redjulu.mixelPreview.items

import de.redjulu.mixelPreview.MixelPreview
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object SpecialItemKeys {

    private val itemIdKey by lazy {
        NamespacedKey(MixelPreview.instance, "special_item_id")
    }

    private val blockIdKey by lazy {
        NamespacedKey(MixelPreview.instance, "special_block_id")
    }

    fun itemIdKey(): NamespacedKey = itemIdKey

    fun blockIdKey(): NamespacedKey = blockIdKey

    fun getItemId(item: ItemStack?): String? {
        if (item == null || !item.hasItemMeta()) return null
        return item.itemMeta.persistentDataContainer.get(itemIdKey, PersistentDataType.STRING)
    }

    fun getItem(id: String): ItemStack? {
        return SpecialItemRegistry.get(id)?.createItem()
    }

    fun isSpecialItem(item: ItemStack?, id: String): Boolean = getItemId(item) == id

    fun getBlockId(block: Block): String? {
        val state = block.state
        if (state is TileState) {
            return state.persistentDataContainer.get(blockIdKey, PersistentDataType.STRING)
        }
        return block.chunk.persistentDataContainer.get(blockLocationKey(block), PersistentDataType.STRING)
    }

    fun isSpecialBlock(block: Block, id: String): Boolean = getBlockId(block) == id

    fun tagBlock(block: Block, id: String) {
        val state = block.state
        if (state is TileState) {
            state.persistentDataContainer.set(blockIdKey, PersistentDataType.STRING, id)
            state.update(true, false)
            return
        }
        block.chunk.persistentDataContainer.set(blockLocationKey(block), PersistentDataType.STRING, id)
    }

    fun untagBlock(block: Block) {
        val state = block.state
        if (state is TileState) {
            state.persistentDataContainer.remove(blockIdKey)
            state.update(true, false)
            return
        }
        block.chunk.persistentDataContainer.remove(blockLocationKey(block))
    }

    private fun blockLocationKey(block: Block): NamespacedKey =
        NamespacedKey(MixelPreview.instance, "b_${block.x}_${block.y}_${block.z}")
}
