package de.redjulu.mixelPreview.items

import org.bukkit.Material

enum class SpecialItemCategory(
    val displayName: String,
    val miniMessageName: String,
    val icon: Material
) {
    ALL("Alle", "<gradient:#E0E0E0:#FFFFFF>Alle</gradient>", Material.NETHER_STAR),
    CRAFTING("Crafting", "<gradient:#79BCD7:#8554B6>Crafting</gradient>", Material.CRAFTING_TABLE),
    CRATE("Crate", "<gradient:#FFB347:#9B59B6>Crate</gradient>", Material.ENDER_CHEST),
    JOB("Job", "<gradient:#4A90D9:#2C3E50>Job</gradient>", Material.IRON_PICKAXE),
    VALENTINES("Valentinstag", "<gradient:#FF6B9D:#FF1744>Valentinstag</gradient>", Material.POPPY),
    ANNIVERSARY("Jubiläum", "<gradient:#FFD700:#FF8C00>Jubiläum</gradient>", Material.CAKE),
    SUMMER("Sommer", "<gradient:#FFD93D:#FF6B35>Sommer</gradient>", Material.SUNFLOWER),
    HALLOWEEN("Halloween", "<gradient:#FF7518:#1A1A2E>Halloween</gradient>", Material.JACK_O_LANTERN),
    WINTER("Winter", "<gradient:#A8D8EA:#FFFFFF>Winter</gradient>", Material.SNOWBALL),
    EASTER("Ostern", "<gradient:#FFB7C5:#98FB98>Ostern</gradient>", Material.EGG),
    MISC("Sonstiges", "<gradient:#888888:#BBBBBB>Sonstiges</gradient>", Material.CHEST);

    val filterable: Boolean get() = this != ALL
}
