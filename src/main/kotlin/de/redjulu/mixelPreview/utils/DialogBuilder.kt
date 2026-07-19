package de.redjulu.mixelPreview.utils

import io.papermc.paper.dialog.Dialog
import io.papermc.paper.dialog.DialogResponseView
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback
import io.papermc.paper.registry.data.dialog.body.DialogBody
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickCallback
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class DialogBuilder(private var title: Component = Component.empty()) {

    private val bodies = mutableListOf<DialogBody>()
    private val inputs = mutableListOf<DialogInput>()
    private val actions = mutableListOf<ActionButton>()
    private var type: DialogType? = null
    private var canCloseWithEscape = true
    private var externalTitle: Component? = null
    private var afterAction: DialogBase.DialogAfterAction? = null
    private var afterActionHandler: ((DialogResponseView, Audience) -> Unit)? = null

    companion object {
        @JvmStatic
        fun create(): DialogBuilder = DialogBuilder()

        @JvmStatic
        fun create(title: Component): DialogBuilder = DialogBuilder(title)
    }

    /**
     * Zentriert den Titel grob durch Padding.
     */
    fun centeredTitle(): DialogBuilder {
        val plain = PlainTextComponentSerializer.plainText().serialize(this.title)
        val spaces = (30 - plain.length).coerceAtLeast(0) / 2
        this.title = Component.text(" ".repeat(spaces)).append(this.title)
        return this
    }

    fun title(title: Component): DialogBuilder {
        this.title = title
        return this
    }

    fun addText(text: Component): DialogBuilder {
        bodies.add(DialogBody.plainMessage(text))
        return this
    }

    fun addItem(item: ItemStack): DialogBuilder {
        bodies.add(DialogBody.item(item).build())
        return this
    }

    fun addTextInput(key: String, label: Component, initial: String? = null, maxLength: Int = 0): DialogBuilder {
        val builder = DialogInput.text(key, label)
        initial?.let { builder.initial(it) }
        if (maxLength > 0) builder.maxLength(maxLength)
        inputs.add(builder.build())
        return this
    }

    fun addBooleanInput(key: String, label: Component, initial: Boolean): DialogBuilder {
        inputs.add(DialogInput.bool(key, label).initial(initial).build())
        return this
    }

    fun addNumberInput(key: String, label: Component, min: Float, max: Float, initial: Float, step: Float = 0f): DialogBuilder {
        val builder = DialogInput.numberRange(key, label, min, max).initial(initial)
        if (step > 0) builder.step(step)
        inputs.add(builder.build())
        return this
    }

    fun addOptionsInput(key: String, label: Component, options: Map<String, Component>, initialKey: String? = null): DialogBuilder {
        val entries = options.map { (id, display) ->
            val isInitial = id == initialKey
            SingleOptionDialogInput.OptionEntry.create(id, display, isInitial)
        }.toMutableList()

        if (entries.isNotEmpty() && entries.none { it.initial() }) {
            val first = entries[0]
            entries[0] = SingleOptionDialogInput.OptionEntry.create(first.id(), first.display(), true)
        }

        inputs.add(DialogInput.singleOption(key, label, entries).build())
        return this
    }

    fun addButton(text: Component, callback: DialogActionCallback? = null): DialogBuilder {
        actions.add(ActionButton.builder(text)
            .action(callback?.let { DialogAction.customClick(it, ClickCallback.Options.builder().uses(1).build()) })
            .build())
        return this
    }

    fun setConfirmation(yesText: Component, yesCallback: DialogActionCallback, noText: Component, noCallback: DialogActionCallback?): DialogBuilder {
        this.type = DialogType.confirmation(
            ActionButton.builder(yesText).action(DialogAction.customClick(yesCallback, ClickCallback.Options.builder().uses(1).build())).build(),
            ActionButton.builder(noText).action(noCallback?.let { DialogAction.customClick(it, ClickCallback.Options.builder().uses(1).build()) }).build()
        )
        return this
    }

    fun escape(canClose: Boolean): DialogBuilder {
        this.canCloseWithEscape = canClose
        return this
    }

    fun forceDecision(): DialogBuilder {
        this.canCloseWithEscape = false
        return this
    }

    fun externalTitle(externalTitle: Component?): DialogBuilder {
        this.externalTitle = externalTitle
        return this
    }

    fun afterAction(handler: (DialogResponseView, Audience) -> Unit): DialogBuilder {
        this.afterActionHandler = handler
        return this
    }

    fun build(): Dialog {
        val baseBuilder = DialogBase.builder(title)
            .canCloseWithEscape(canCloseWithEscape)
            .body(bodies)
            .inputs(inputs)

        externalTitle?.let { baseBuilder.externalTitle(it) }
        afterAction?.let { baseBuilder.afterAction(it) }

        val finalType = this.type ?: if (actions.isEmpty()) {
            DialogType.notice()
        } else {
            DialogType.multiAction(actions).build()
        }

        return Dialog.create { builder ->
            builder.empty()
                .base(baseBuilder.build())
                .type(finalType)
        }
    }

    fun show(audience: Audience) {
        if (audience is Player) {
            val holder = audience.openInventory.topInventory.holder
            if (holder is BaseGUI<*, *, *>) {
                holder.closeForExternalUI(audience)
            }
        }
        audience.showDialog(build())
    }
}