package de.redjulu.mixelPreview.utils

import de.redjulu.mixelPreview.MixelPreview
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitTask
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Predicate
import kotlin.collections.iterator

interface GUIStage {
    val displayName: String
}

abstract class BaseGUI<T, C : Enum<C>, S>(
    rows: Int,
    titleKey: Component,
    t: Int,
    b: Int,
    l: Int,
    r: Int,
    defaultCategory: C?,
    inventoryType: InventoryType = InventoryType.CHEST
) : InventoryHolder {

    companion object {
        private val HISTORY: MutableMap<UUID, BaseGUI<*, *, *>> = HashMap()

        @JvmStatic
        fun clearHistory(uuid: UUID) {
            HISTORY.remove(uuid)
        }

        private val placeholderKey by lazy {
            NamespacedKey(MixelPreview.instance, "gui_placeholder")
        }

        @JvmStatic
        fun saveHistory(uuid: UUID, gui: BaseGUI<*, *, *>) {
            HISTORY[uuid] = gui
        }

        @JvmStatic
        fun isPlaceholderItem(item: ItemStack?): Boolean {
            if (item == null || !item.hasItemMeta()) return false
            return item.itemMeta?.persistentDataContainer?.has(placeholderKey, PersistentDataType.BYTE) == true
        }
    }

    var size: Int = if (inventoryType == InventoryType.CHEST) rows * 9 else inventoryType.defaultSize
        private set
    var title: Component = titleKey
        private set
    private var titleSerialized: String = MiniMessage.miniMessage().serialize(titleKey)
    var type: InventoryType = inventoryType
        private set
    private var inv: Inventory
    protected val contentSlots = mutableListOf<Int>()
    private val storedRows = rows
    private val storedT = t
    private val storedB = b
    private val storedL = l
    private val storedR = r
    protected val animatedSlots = mutableMapOf<Int, List<ItemStack>>()
    protected val activeItems = mutableMapOf<Int, ItemStack>()
    protected val interactableSlots = mutableSetOf<Int>()
    protected val interactablePredicates = mutableMapOf<Int, (ItemStack) -> Boolean>()
    protected val ignoredSlots = mutableSetOf<Int>()
    protected val placeholderSlots = mutableSetOf<Int>()
    protected val prioritySlots = mutableSetOf<Int>()
    protected val placeholderItems = mutableMapOf<Int, ItemStack>()
    protected val placeholderPredicates = mutableMapOf<Int, (ItemStack) -> Boolean>()
    private val dynamicButtons = mutableListOf<DynamicButtonInfo>()
    private val toggleButtons = mutableListOf<DynamicButtonInfo>()
    private val pageItems = mutableMapOf<Int, MutableMap<Int, ItemStack>>()

    protected open var openSound: Sound? = Sound.BLOCK_CHEST_OPEN
    protected open var clickSound: Sound? = Sound.UI_BUTTON_CLICK
    protected open var backSound: Sound? = Sound.ITEM_ARMOR_EQUIP_GENERIC

    protected var allItems = mutableListOf<T>()
    protected var currentCategory: C? = defaultCategory
    protected var page: Int = 0
    protected var pageSize: Int = 0
        private set

    var isSwitching: Boolean = false
    private var internalClose = false
    private var closable = true
    private val countdownTasks = mutableMapOf<Int, BukkitTask>()

    protected var stages: List<S> = emptyList()
    protected var stageIndex: Int = 0
    val currentStage: S? get() = stages.getOrNull(stageIndex)

    init {
        this.inv = createInventory()
        recalculateContentSlots()
    }

    private fun createInventory(): Inventory {
        return if (type == InventoryType.CHEST)
            Bukkit.createInventory(this, size, title)
        else
            Bukkit.createInventory(this, type, title)
    }

    private fun recalculateContentSlots() {
        contentSlots.clear()
        if (type != InventoryType.CHEST) {
            for (i in 0 until size) {
                contentSlots.add(i)
            }
        } else {
            for (row in storedT until (storedRows - storedB)) {
                for (col in storedL until (9 - storedR)) {
                    contentSlots.add(col + (row * 9))
                }
            }
        }
        this.pageSize = contentSlots.size
    }

    fun setSize(newRows: Int, player: Player?) {
        this.type = InventoryType.CHEST
        val newSize = newRows * 9
        if (newSize == size) return
        this.size = newSize
        rebuildInventory(player)
    }

    fun setTitle(newTitle: Component, player: Player?) {
        val serialized = MiniMessage.miniMessage().serialize(newTitle)
        if (serialized == titleSerialized) return
        this.titleSerialized = serialized
        this.title = newTitle
        rebuildInventory(player)
    }

    fun setType(newType: InventoryType, player: Player?) {
        if (this.type == newType) return
        this.type = newType
        this.size = if (newType == InventoryType.CHEST) storedRows * 9 else newType.defaultSize
        rebuildInventory(player)
    }

    private fun rebuildInventory(player: Player?) {
        GUIListener.clearButtons(inv)
        this.inv = createInventory()
        recalculateContentSlots()

        if (player != null && player.openInventory.topInventory.holder === this) {
            val wasSwitching = isSwitching
            internalClose = true
            player.closeInventory()
            internalClose = false
            open(player, false)
            isSwitching = wasSwitching
        }
    }

    protected fun defineStages(vararg stageElements: S) {
        this.stages = stageElements.toList()
    }

    fun hasNextStage(): Boolean = stageIndex < stages.size - 1

    fun hasPrevStage(): Boolean = stageIndex > 0

    fun advanceStage(player: Player) {
        if (hasNextStage()) {
            stageIndex++
            update(player)
        }
    }

    fun retreatStage(player: Player) {
        if (hasPrevStage()) {
            stageIndex--
            update(player)
        }
    }

    fun goToStage(player: Player, index: Int) {
        if (index in stages.indices) {
            stageIndex = index
            update(player)
        }
    }

    fun open(player: Player, saveToHistory: Boolean) {
        isSwitching = false
        closable = true
        if (saveToHistory) {
            val holder = player.openInventory.topInventory.holder
            if (holder is BaseGUI<*, *, *>) {
                HISTORY[player.uniqueId] = holder
            }
        }
        update(player)
        if (player.openInventory.topInventory != inv) {
            player.openInventory(inv)
            openSound?.let { player.playSound(player.location, it, 0.5f, 1.0f) }
        }
    }

    fun update(player: Player) {
        cancelCountdowns()

        inv.clear()
        GUIListener.clearButtons(inv)
        animatedSlots.clear()
        interactableSlots.clear()
        interactablePredicates.clear()
        placeholderSlots.clear()
        prioritySlots.clear()
        placeholderItems.clear()
        placeholderPredicates.clear()
        ignoredSlots.clear()
        dynamicButtons.clear()
        toggleButtons.clear()

        compose(player)
        tickAnimations(0)
    }

    fun updateExceptPlaceholders(player: Player) {
        cancelCountdowns()

        val savedItems = placeholderSlots
            .filter { slot ->
                val item = inv.getItem(slot)
                item != null && item.type != Material.AIR && !isPlaceholderItem(item)
            }
            .associateWith { slot -> inv.getItem(slot)!!.clone() }

        inv.clear()
        GUIListener.clearButtons(inv)
        animatedSlots.clear()
        interactableSlots.clear()
        interactablePredicates.clear()
        placeholderSlots.clear()
        prioritySlots.clear()
        placeholderItems.clear()
        placeholderPredicates.clear()
        ignoredSlots.clear()
        dynamicButtons.clear()
        toggleButtons.clear()

        compose(player)
        tickAnimations(0)

        savedItems.forEach { (slot, item) -> inv.setItem(slot, item) }
    }

    fun updateDynamicButtons(player: Player) {
        for (info in dynamicButtons) {
            inv.setItem(info.slot, if (info.condition.test(player)) info.activeItem else info.inactiveItem)
        }
    }

    fun updateToggleButtons(player: Player) {
        for (info in toggleButtons) {
            inv.setItem(info.slot, if (info.condition.test(player)) info.activeItem else info.inactiveItem)
        }
    }

    private data class DynamicButtonInfo(
        val slot: Int,
        val condition: Predicate<Player>,
        val activeItem: ItemStack,
        val inactiveItem: ItemStack
    )

    protected fun setInteractable(slot: Int, interactable: Boolean) {
        if (interactable) interactableSlots.add(slot)
        else interactableSlots.remove(slot)
    }

    protected fun setInteractable(slot: Int) {
        setInteractable(slot, true)
    }

    protected fun setInteractable(slot: Int, predicate: (ItemStack) -> Boolean) {
        interactableSlots.add(slot)
        interactablePredicates[slot] = predicate
    }

    fun getInteractablePredicate(slot: Int): ((ItemStack) -> Boolean)? = interactablePredicates[slot]

    protected fun setIgnored(slot: Int) {
        ignoredSlots.add(slot)
    }

    protected fun setPlaceholder(slot: Int) {
        val current = inv.getItem(slot)
        if (current == null || current.type == Material.AIR) return

        val marked = markAsPlaceholderItem(current)
        inv.setItem(slot, marked)

        placeholderSlots.add(slot)
        placeholderItems[slot] = marked.clone()
    }

    protected fun setPlaceholder(slot: Int, predicate: (ItemStack) -> Boolean) {
        setPlaceholder(slot)
        placeholderPredicates[slot] = predicate
    }

    fun getPlaceholderPredicate(slot: Int): ((ItemStack) -> Boolean)? = placeholderPredicates[slot]

    private fun markAsPlaceholderItem(placeholderItem: ItemStack): ItemStack {
        return placeholderItem.clone().apply {
            itemMeta = itemMeta?.apply {
                persistentDataContainer.set(placeholderKey, PersistentDataType.BYTE, 1.toByte())
            }
        }
    }

    protected fun setPriority(slot: Int) {
        prioritySlots.add(slot)
    }

    fun getPlaceholderForSlot(slot: Int): ItemStack? = placeholderItems[slot]

    fun isPrioritySlot(slot: Int): Boolean = prioritySlots.contains(slot)

    protected fun setButton(slot: Int, item: ItemStack?) {
        inv.setItem(slot, item)
    }

    fun clearActiveItem(slot: Int) {
        activeItems.remove(slot)
    }

    protected fun getActiveItem(slot: Int): ItemStack? {
        if (placeholderSlots.contains(slot)) {
            val itemInInv = inv.getItem(slot)
            if (itemInInv != null && itemInInv.type != Material.AIR && !isPlaceholderItem(itemInInv)) {
                return itemInInv
            }
            return activeItems[slot]
        }
        if (interactableSlots.contains(slot)) {
            val itemInInv = inv.getItem(slot)
            if (itemInInv != null && itemInInv.type != Material.AIR) return itemInInv
        }
        return activeItems[slot]
    }

    protected fun setDynamicButton(
        player: Player,
        slot: Int,
        condition: Predicate<Player>,
        activeItem: ItemStack,
        inactiveItem: ItemStack,
        action: BiConsumer<Player, ClickType>
    ) {
        dynamicButtons.add(DynamicButtonInfo(slot, condition, activeItem, inactiveItem))
        inv.setItem(slot, if (condition.test(player)) activeItem else inactiveItem)

        GUIListener.registerButton(inv, slot) { p, click ->
            if (!condition.test(p)) {
                p.playSound(p.location, Sound.ENTITY_VILLAGER_NO, 0.5f, 1.0f)
                return@registerButton
            }
            clickSound?.let { p.playSound(p.location, it, 0.5f, 1.0f) }
            action.accept(p, click)
        }
    }

    protected fun setToggleButton(
        player: Player,
        slot: Int,
        condition: Predicate<Player>,
        activeItem: ItemStack,
        inactiveItem: ItemStack,
        action: BiConsumer<Player, ClickType>
    ) {
        toggleButtons.add(DynamicButtonInfo(slot, condition, activeItem, inactiveItem))
        GUIListener.registerButton(inv, slot) { p, click ->
            clickSound?.let { p.playSound(p.location, it, 0.5f, 1.0f) }
            action.accept(p, click)
            inv.setItem(slot, if (condition.test(p)) activeItem else inactiveItem)
        }
        inv.setItem(slot, if (condition.test(player)) activeItem else inactiveItem)
    }

    protected fun setConditionalButton(
        player: Player,
        slot: Int,
        condition: Predicate<Player>,
        item: ItemStack,
        action: BiConsumer<Player, ClickType>
    ) {
        if (condition.test(player)) {
            setButton(slot, item, action)
        }
    }

    fun setCategory(player: Player, category: C) {
        this.currentCategory = category
        this.page = 0
        update(player)
    }

    protected fun back(player: Player) {
        val last = HISTORY.remove(player.uniqueId)
        if (last != null) {
            last.open(player, false)
            backSound?.let { player.playSound(player.location, it, 0.5f, 1.2f) }
        } else {
            player.closeInventory()
        }
    }

    protected fun setButton(slot: Int, item: ItemStack, action: BiConsumer<Player, ClickType>) {
        inv.setItem(slot, item)
        GUIListener.registerButton(inv, slot) { p, click ->
            clickSound?.let { p.playSound(p.location, it, 0.5f, 1.0f) }
            action.accept(p, click)
        }
    }

    fun tickAnimations(tick: Long) {
        if (animatedSlots.isEmpty()) return
        for ((slot, frames) in animatedSlots) {
            if (frames.isEmpty()) continue
            inv.setItem(slot, frames[(tick % frames.size).toInt()])
        }
    }

    fun isInteractableSlot(slot: Int): Boolean = interactableSlots.contains(slot)

    fun isPlaceholderSlot(slot: Int): Boolean = placeholderSlots.contains(slot)

    protected fun renderPage(filter: Predicate<T>, renderer: BiConsumer<T, Int>) {
        val filteredItems = allItems.filter { filter.test(it) }
        val start = page * pageSize
        for (i in 0 until pageSize) {
            val slot = contentSlots[i]
            val index = start + i
            if (index < filteredItems.size) {
                renderer.accept(filteredItems[index], slot)
            } else {
                inv.setItem(slot, null)
            }
        }
    }

    protected fun fillBackground(material: Material, skipIgnoredSlots: Boolean = false) {
        val item = ItemStack(material).apply {
            itemMeta = itemMeta?.apply { displayName(Component.empty()) }
        }
        for (i in 0 until size) {
            if (contentSlots.contains(i)) continue
            if (skipIgnoredSlots && ignoredSlots.contains(i)) continue
            inv.setItem(i, item)
        }
    }

    protected fun fillSlots(item: ItemStack, range: IntRange, vararg skip: Int) {
        for (slot in range) {
            if (slot in skip) continue
            inv.setItem(slot, item)
        }
    }

    protected fun fillContentArea(material: Material) {
        val item = ItemStack(material).apply {
            itemMeta = itemMeta?.apply { displayName(Component.empty()) }
        }
        contentSlots.forEach { inv.setItem(it, item) }
    }

    protected fun addPaginationButtons(prev: Int, next: Int, p: Player, f: Predicate<T>) {
        addPaginationButtons(prev, next, p, f, -1)
    }

    protected fun addPaginationButtons(prev: Int, next: Int, p: Player, f: Predicate<T>, maxPages: Int) {
        val filtered = allItems.filter { f.test(it) }
        val hasPrev = page > 0
        val hasNext = if (maxPages > 0) page < maxPages - 1 else (page + 1) * pageSize < filtered.size

        val prevItem = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                displayName(if (hasPrev) MiniMessage.miniMessage().deserialize("<gray>« Vorherige Seite") else
                    MiniMessage.miniMessage().deserialize("<red><st>« Vorherige Seite"))
            }
        }
        setButton(prev, prevItem) { pl, _ ->
            if (hasPrev) {
                page--
                update(pl)
            }
        }

        val nextItem = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                displayName(if (hasNext) MiniMessage.miniMessage().deserialize("<gray>Nächste Seite »") else
                    MiniMessage.miniMessage().deserialize("<red><st>Nächste Seite »"))
            }
        }
        setButton(next, nextItem) { pl, _ ->
            if (hasNext) {
                page++
                update(pl)
            }
        }
    }

    protected fun addPlayerPaginationButtons(prev: Int, next: Int, maxPages: Int, player: Player) {
        val hasPrev = page > 0
        val hasNext = page < maxPages - 1

        val prevItem = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                displayName(MiniMessage.miniMessage().deserialize(
                    if (hasPrev) "<gray>« Vorherige Seite" else "<dark_gray>« Vorherige Seite"
                ))
            }
        }
        setButton(prev, prevItem) { pl, _ ->
            if (hasPrev) {
                savePageItems(page)
                page--
                update(pl)
                restorePageItems(page)
            }
        }

        val nextItem = ItemStack(Material.ARROW).apply {
            itemMeta = itemMeta?.apply {
                displayName(MiniMessage.miniMessage().deserialize(
                    if (hasNext) "<gray>Nächste Seite »" else "<dark_gray>Nächste Seite »"
                ))
            }
        }
        setButton(next, nextItem) { pl, _ ->
            if (hasNext) {
                savePageItems(page)
                page++
                update(pl)
                restorePageItems(page)
            }
        }
    }

    private fun savePageItems(pageNum: Int) {
        val saved = mutableMapOf<Int, ItemStack>()
        for (slot in contentSlots) {
            val item = inv.getItem(slot)
            if (item != null && item.type != Material.AIR) {
                saved[slot] = item.clone()
            }
        }
        if (saved.isNotEmpty()) {
            pageItems[pageNum] = saved
        } else {
            pageItems.remove(pageNum)
        }
    }

    private fun restorePageItems(pageNum: Int) {
        val saved = pageItems[pageNum] ?: return
        for ((slot, item) in saved) {
            inv.setItem(slot, item.clone())
        }
    }

    protected fun setAnimatedItem(slot: Int, frames: List<ItemStack>) {
        animatedSlots[slot] = frames
    }

    fun getInteractableItemCount(): Int {
        var total = 0
        for ((_, items) in pageItems) {
            for ((_, item) in items) {
                if (item.type != Material.AIR) total += item.amount
            }
        }
        for (slot in contentSlots) {
            val item = inv.getItem(slot)
            if (item != null && item.type != Material.AIR) total += item.amount
        }
        return total
    }

    fun isInteractableSlotEmpty(slot: Int): Boolean {
        if (!interactableSlots.contains(slot)) return false
        val item = inv.getItem(slot)
        return item == null || item.type == Material.AIR
    }

    abstract fun compose(player: Player)
    open fun onPlaceholderUpdate(player: Player, slot: Int, item: ItemStack) {}

    override fun getInventory(): Inventory = inv

    fun onClose(player: Player) {
        if (internalClose) return

        if (!closable) {
            Bukkit.getScheduler().runTask(MixelPreview.instance, Runnable {
                if (!closable && player.isOnline) {
                    reopenFor(player)
                }
            })
            return
        }

        cancelCountdowns()
        onGuiClose(player)
    }

    open fun onGuiClose(player: Player) {}

    fun setClosable(value: Boolean) {
        closable = value
    }

    fun isClosable(): Boolean = closable

    protected fun setCountdownItem(
        slot: Int,
        player: Player,
        seconds: Int,
        itemProvider: (timeLeft: Int, total: Int) -> ItemStack,
        onTick: ((timeLeft: Int, total: Int) -> Unit)? = null,
        onFinish: (Player) -> Unit
    ) {
        countdownTasks.remove(slot)?.cancel()
        var timeLeft = seconds
        inv.setItem(slot, itemProvider(timeLeft, seconds))
        onTick?.invoke(timeLeft, seconds)

        countdownTasks[slot] = Bukkit.getScheduler().runTaskTimer(MixelPreview.instance, Runnable {
            timeLeft--
            if (timeLeft <= 0) {
                countdownTasks.remove(slot)?.cancel()
                onFinish(player)
            } else {
                inv.setItem(slot, itemProvider(timeLeft, seconds))
                onTick?.invoke(timeLeft, seconds)
            }
        }, 20L, 20L)
    }

    private fun cancelCountdowns() {
        countdownTasks.values.forEach { it.cancel() }
        countdownTasks.clear()
    }

    @Volatile
    private var reopening = false

    fun reopenFor(player: Player) {
        if (reopening) return
        reopening = true
        try {
            internalClose = true
            player.openInventory(inv)
            internalClose = false
            openSound?.let { player.playSound(player.location, it, 0.5f, 1.0f) }
        } finally {
            reopening = false
        }
    }

    fun closeForExternalUI(player: Player) {
        internalClose = true
        player.closeInventory()
        internalClose = false
    }

    fun isInternalClose(): Boolean = internalClose
}