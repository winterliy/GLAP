package dev.glap.api;

import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.jetbrains.annotations.NotNull;

/** A client-only entity. All methods that touch Bukkit state or send packets require the server thread. */
public interface FakeEntity<T extends Entity> {
    @NotNull T entity();

    @NotNull UUID uniqueId();

    @NotNull Location location();

    boolean isValid();

    boolean spawnTo(@NotNull Player viewer);

    boolean hideFrom(@NotNull Player viewer);

    void teleport(@NotNull Location location);

    void updateMetadata(@NotNull Consumer<T> update);

    void updateEquipment(@NotNull Consumer<EntityEquipment> update);

    void remove();
}
