package dev.glap.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/** Main-thread boundary for GLAP APIs that touch Bukkit state or send entity packets. */
public interface GlapServerThread {
    boolean isPrimaryThread();

    /** Throws if called away from the server's single main thread. */
    default void requirePrimaryThread() {
        if (!this.isPrimaryThread()) {
            throw new IllegalStateException("This GLAP API must be used on the server thread");
        }
    }

    /** Runs immediately on the server thread or schedules the action there. */
    default void execute(final Plugin plugin, final Runnable action) {
        if (this.isPrimaryThread()) {
            action.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, action);
        }
    }
}
