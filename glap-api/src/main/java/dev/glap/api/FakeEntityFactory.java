package dev.glap.api;

import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Creates server-side entity objects that are sent only to explicitly selected viewers. */
public interface FakeEntityFactory {
    <T extends Entity> @NotNull FakeEntity<T> create(@NotNull Location location, @NotNull Class<T> entityClass);

    @NotNull FakeEntity<Item> createItem(@NotNull Location location, @NotNull ItemStack item);

    @NotNull FakeEntity<FallingBlock> createFallingBlock(@NotNull Location location, @NotNull BlockData blockData);
}
