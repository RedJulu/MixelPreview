package de.redjulu.mixelPreview

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import de.redjulu.mixelPreview.items.SpecialItemRegistry
import de.redjulu.mixelPreview.items.impl.crate.creativeAxe.CreativeAxeVisualizer
import de.redjulu.mixelPreview.listeners.CreativeAxeListener
import de.redjulu.mixelPreview.listeners.SpecialItemListener
import de.redjulu.mixelPreview.utils.GUIListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

class MixelPreview : JavaPlugin() {

    companion object {
        lateinit var instance: MixelPreview
        lateinit var protocolManager: ProtocolManager
    }

    override fun onEnable() {
        instance = this
        protocolManager = ProtocolLibrary.getProtocolManager()

        SpecialItemRegistry.init()

        val specialItemListener = SpecialItemListener()
        specialItemListener.startTickTask(this)

        server.pluginManager.registerEvents(GUIListener(), this)
        server.pluginManager.registerEvents(specialItemListener, this)
        server.pluginManager.registerEvents(CreativeAxeListener(), this)
    }

    override fun onDisable() {
        Bukkit.getOnlinePlayers().forEach { CreativeAxeVisualizer.stopVisualizer(it) }
    }
}
