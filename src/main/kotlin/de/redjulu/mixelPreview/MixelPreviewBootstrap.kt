package de.redjulu.mixelPreview

import com.mojang.brigadier.Command
import de.redjulu.mixelPreview.gui.SpecialItemsGUI
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.entity.Player

internal class MixelPreviewBootstrap : PluginBootstrap {

    override fun bootstrap(ctx: BootstrapContext) {
        ctx.lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()

            commands.register(
                Commands.literal("specialitems")
                    .requires { source ->
                        val executor = source.executor
                        executor is Player
                    }
                    .executes { context ->
                        val player = context.source.executor as Player
                        SpecialItemsGUI().open(player, saveToHistory = false)
                        Command.SINGLE_SUCCESS
                    }
                    .build(),
                "Öffnet das Special Items Menü",
                listOf("si")
            )
        }
    }
}
