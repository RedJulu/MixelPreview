package de.redjulu.mixelPreview.types

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.UUID

object BlockDataType : PersistentDataType<PersistentDataContainer, Location> {
    override fun getPrimitiveType(): Class<PersistentDataContainer> = PersistentDataContainer::class.java
    override fun getComplexType(): Class<Location> = Location::class.java

    override fun toPrimitive(complex: Location, context: PersistentDataAdapterContext): PersistentDataContainer {
        val container = context.newPersistentDataContainer()
        container.set(org.bukkit.NamespacedKey.minecraft("world"), PersistentDataType.STRING, complex.world.uid.toString())
        container.set(org.bukkit.NamespacedKey.minecraft("x"), PersistentDataType.INTEGER, complex.blockX)
        container.set(org.bukkit.NamespacedKey.minecraft("y"), PersistentDataType.INTEGER, complex.blockY)
        container.set(org.bukkit.NamespacedKey.minecraft("z"), PersistentDataType.INTEGER, complex.blockZ)
        return container
    }

    override fun fromPrimitive(primitive: PersistentDataContainer, context: PersistentDataAdapterContext): Location {
        val worldString = primitive.get(org.bukkit.NamespacedKey.minecraft("world"), PersistentDataType.STRING)
            ?: throw IllegalArgumentException("Missing world UUID in PersistentDataContainer")
        val world = Bukkit.getWorld(UUID.fromString(worldString))
            ?: throw IllegalArgumentException("World not found")
        val x = primitive.get(org.bukkit.NamespacedKey.minecraft("x"), PersistentDataType.INTEGER)
            ?: throw IllegalArgumentException("Missing X coordinate")
        val y = primitive.get(org.bukkit.NamespacedKey.minecraft("y"), PersistentDataType.INTEGER)
            ?: throw IllegalArgumentException("Missing Y coordinate")
        val z = primitive.get(org.bukkit.NamespacedKey.minecraft("z"), PersistentDataType.INTEGER)
            ?: throw IllegalArgumentException("Missing Z coordinate")
        return Location(world, x.toDouble(), y.toDouble(), z.toDouble())
    }
}