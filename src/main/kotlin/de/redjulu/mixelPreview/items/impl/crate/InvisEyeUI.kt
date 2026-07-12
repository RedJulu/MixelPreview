package de.redjulu.mixelPreview.items.impl.crate

import de.redjulu.mixelPreview.utils.DialogBuilder
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object InvisEyeUI {

    private val mm = MiniMessage.miniMessage()

    private val table = mapOf(
        1f to 3f,
        2f to 6f,
        3f to 9f,
        4f to 12f,
        5f to 15f,
        6f to 18f,
        7f to 21f,
        8f to 24f
    )

    fun open(player: Player) {
        val maxMinutes = table.filter { (_, cost) -> cost <= player.level }.keys.maxOrNull() ?: 1f

        DialogBuilder(mm.deserialize("<b><gradient:#B5CAD9:#CC99D7>Auge der Unsichtbarkeit</gradient></b>"))
            .addText(mm.deserialize("<aqua>Tausche hier <green>XP-Punkte <aqua>um <aqua>unsichtbar zu werden"))
            .addText(mm.deserialize("<gradient:#A43AE3:#D50EFF>Jede Minute kostet drei Level</gradient>"))

            .addNumberInput("minutes", mm.deserialize("<gold>Minuten"), 1f, maxMinutes, 1f, 1f)
            .addButton(mm.deserialize("Abbrechen")) {_, _ -> player.closeDialog() }
            .addButton(mm.deserialize("<green>Bestätigen")) { ctx, _ ->
                var minutes = ctx.getFloat("minutes") ?: 1f
                val levels = table[minutes] ?: 3f

                if (minutes > maxMinutes) minutes = maxMinutes
                if (minutes < 1f) minutes = 1f

                if (player.level < levels.toInt()) {
                    player.sendMessage(mm.deserialize("<red>Du hast nicht genug Level! Benötigt: <yellow>$levels</yellow></red>"))
                    player.playSound(player.location, Sound.ENTITY_VILLAGER_NO, 1f, 1f)
                    return@addButton
                }

                player.level -= levels.toInt()
                player.world.spawnParticle(
                    Particle.TOTEM_OF_UNDYING,
                    player.location.add(0.0, 1.0, 0.0),
                    30,
                    0.3, 0.3, 0.3,
                    0.2
                )
                player.playSound(player.location, Sound.ENTITY_CHICKEN_EGG, 1.3f, 0.6f)
                player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f)

                player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, minutes.toInt() * 60 * 20, 0, false, false))

                player.closeDialog()
            }
            .show(player)
    }
}