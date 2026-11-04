package dev.ledge.kode;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class KodePlugin extends JavaPlugin {
    private static final List<String> REQUIRED_RUNTIME_CLASSES = List.of(
        "kotlin.Unit",
        "kotlin.reflect.KClass",
        "kotlinx.coroutines.CoroutineScope",
        "kotlinx.serialization.json.Json"
    );

    @Override
    public void onEnable() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        for (final String className : REQUIRED_RUNTIME_CLASSES) {
            try {
                Class.forName(className, false, classLoader);
            } catch (final ClassNotFoundException exception) {
                throw new IllegalStateException("Kode is missing required runtime class: " + className, exception);
            }
        }
        getLogger().info("Kotlin runtime provider enabled (stdlib, reflection, coroutines, serialization).");
    }
}
