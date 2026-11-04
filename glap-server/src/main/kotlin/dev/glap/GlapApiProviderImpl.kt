package dev.glap

import dev.glap.api.FakeEntity
import dev.glap.api.FakeEntityFactory
import dev.glap.api.GlapApi
import dev.glap.api.GlapServerThread
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.server.level.ServerEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.entity.Relative
import net.minecraft.world.entity.UpdateInterval
import net.minecraft.world.entity.LivingEntity as NmsLivingEntity
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.data.BlockData
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Entity
import org.bukkit.entity.FallingBlock
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import com.google.common.collect.Lists
import com.mojang.datafixers.util.Pair
import java.util.Collections
import java.util.WeakHashMap
import java.util.function.Consumer
import java.util.function.Predicate
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.server.level.ServerPlayer

class GlapApiProviderImpl : GlapApi.Provider {
    private val api = GlapApiImpl()

    override fun create(): GlapApi = api
}

private class GlapApiImpl : GlapApi {
    private val thread = GlapServerThreadImpl()
    private val fakeEntities = FakeEntityFactoryImpl(thread)

    override fun fakeEntities(): FakeEntityFactory = fakeEntities

    override fun serverThread(): GlapServerThread = thread
}

private class GlapServerThreadImpl : GlapServerThread {
    override fun isPrimaryThread(): Boolean = Bukkit.isPrimaryThread()

    override fun execute(plugin: Plugin, action: Runnable) {
        requireNotNull(plugin) { "plugin" }
        requireNotNull(action) { "action" }
        if (isPrimaryThread) action.run() else Bukkit.getScheduler().runTask(plugin, action)
    }
}

private class FakeEntityFactoryImpl(private val thread: GlapServerThread) : FakeEntityFactory {
    override fun <T : Entity> create(location: Location, entityClass: Class<T>): FakeEntity<T> {
        thread.requirePrimaryThread()
        requireNotNull(location.world) { "A fake entity requires a world" }
        require(!Player::class.java.isAssignableFrom(entityClass)) {
            "Fake players are not supported by this API yet"
        }
        val entity = location.world!!.createEntity(location, entityClass)
        return FakeEntityImpl(entity, thread)
    }

    override fun createItem(location: Location, item: ItemStack): FakeEntity<Item> {
        thread.requirePrimaryThread()
        val world = requireNotNull(location.world) { "A fake entity requires a world" }
        val entity = world.createEntity(location, Item::class.java).apply { itemStack = item.clone() }
        return FakeEntityImpl(entity, thread)
    }

    override fun createFallingBlock(location: Location, blockData: BlockData): FakeEntity<FallingBlock> {
        thread.requirePrimaryThread()
        val world = requireNotNull(location.world) { "A fake entity requires a world" }
        val entity = world.createEntity(location, FallingBlock::class.java).apply { this.blockData = blockData.clone() }
        return FakeEntityImpl(entity, thread)
    }
}

private class FakeEntityImpl<T : Entity>(
    private val bukkitEntity: T,
    private val thread: GlapServerThread
) : FakeEntity<T> {
    private val viewers = Collections.newSetFromMap(WeakHashMap<Player, Boolean>())
    private var removed = false
    private val nmsEntity get() = (bukkitEntity as CraftEntity).handle

    override fun entity(): T {
        thread.requirePrimaryThread()
        checkValid()
        return bukkitEntity
    }

    override fun uniqueId() = bukkitEntity.uniqueId

    override fun location(): Location {
        thread.requirePrimaryThread()
        return bukkitEntity.location
    }

    override fun isValid(): Boolean {
        thread.requirePrimaryThread()
        return !removed
    }

    override fun spawnTo(viewer: Player): Boolean {
        thread.requirePrimaryThread()
        checkValid()
        require(viewer.world === bukkitEntity.world) { "Viewer and fake entity must be in the same world" }
        require(viewer.isOnline) { "Viewer must be online" }
        if (viewer in viewers) return false

        val entity = nmsEntity
        val tracker = ServerEntity(
            entity.level() as ServerLevel,
            entity,
            UpdateInterval.NEVER,
            false,
            NoopSynchronizer,
            emptySet()
        )
        val craftViewer = viewer as CraftPlayer
        tracker.sendPairingData(craftViewer.handle) { packet -> craftViewer.handle.connection.send(packet) }
        viewers.add(viewer)
        return true
    }

    override fun hideFrom(viewer: Player): Boolean {
        thread.requirePrimaryThread()
        if (!viewers.remove(viewer)) return false
        send(viewer, ClientboundRemoveEntitiesPacket(nmsEntity.id))
        return true
    }

    override fun teleport(location: Location) {
        thread.requirePrimaryThread()
        checkValid()
        require(location.world === bukkitEntity.world) { "Fake entities cannot change worlds" }
        val entity = nmsEntity
        entity.setPos(location.x, location.y, location.z)
        entity.setYRot(location.yaw)
        entity.setXRot(location.pitch)
        entity.setYHeadRot(location.yaw)
        val packet = ClientboundTeleportEntityPacket(
            entity.id,
            PositionMoveRotation.of(entity),
            emptySet<Relative>(),
            entity.onGround()
        )
        viewers.toList().forEach { send(it, packet) }
    }

    override fun updateMetadata(update: Consumer<T>) {
        thread.requirePrimaryThread()
        checkValid()
        update.accept(bukkitEntity)
        viewers.toList().forEach(::sendMetadata)
    }

    override fun updateEquipment(update: Consumer<EntityEquipment>) {
        thread.requirePrimaryThread()
        checkValid()
        val equipment = (bukkitEntity as? org.bukkit.entity.LivingEntity)?.equipment
            ?: throw IllegalStateException("This fake entity has no living equipment")
        update.accept(equipment)
        viewers.toList().forEach(::sendEquipment)
    }

    override fun remove() {
        thread.requirePrimaryThread()
        if (removed) return
        val packet = ClientboundRemoveEntitiesPacket(nmsEntity.id)
        viewers.toList().forEach { send(it, packet) }
        viewers.clear()
        removed = true
    }

    private fun sendMetadata(viewer: Player) {
        send(viewer, ClientboundSetEntityDataPacket(nmsEntity.id, nmsEntity.entityData.packAll()))
    }

    private fun sendEquipment(viewer: Player) {
        val living = nmsEntity as? NmsLivingEntity ?: return
        val equipment = (bukkitEntity as org.bukkit.entity.LivingEntity).equipment ?: return
        val slots = Lists.newArrayList<Pair<net.minecraft.world.entity.EquipmentSlot, net.minecraft.world.item.ItemStack>>()
        org.bukkit.inventory.EquipmentSlot.values().forEach { slot ->
            val stack = equipment.getItem(slot)
            val converted = CraftItemStack.asNMSCopy(stack)
            if (!converted.isEmpty) slots += Pair.of(CraftEquipmentSlot.getNMS(slot), converted)
        }
        if (slots.isNotEmpty()) send(viewer, ClientboundSetEquipmentPacket(living.id, slots, true))
    }

    private fun send(viewer: Player, packet: net.minecraft.network.protocol.Packet<*>) {
        (viewer as CraftPlayer).handle.connection.send(packet)
    }

    private fun checkValid() {
        check(!removed) { "Fake entity has been removed" }
    }
}

private object NoopSynchronizer : ServerEntity.Synchronizer {
    override fun sendToTrackingPlayers(packet: Packet<in ClientGamePacketListener>) = Unit

    override fun sendToTrackingPlayersAndSelf(packet: Packet<in ClientGamePacketListener>) = Unit

    override fun sendToTrackingPlayersFiltered(
        packet: Packet<in ClientGamePacketListener>,
        predicate: Predicate<ServerPlayer>
    ) = Unit
}
