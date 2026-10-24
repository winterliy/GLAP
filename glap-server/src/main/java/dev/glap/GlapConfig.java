package dev.glap;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/** Java-facing read-only bridge to the immutable GLAP configuration snapshot. */
public final class GlapConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File FILE = new File("glap.yml");
    private static final GlapSettingsLoader LOADER = ServiceLoader.load(GlapSettingsLoader.class, GlapConfig.class.getClassLoader())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("GLAP settings loader is missing from the server jar"));
    private static final AtomicReference<GlapSettings> SETTINGS = new AtomicReference<>(new GlapSettings(0, true, true, true, false, false, false, true, true));
    private static volatile boolean metricsEnabledAtStartup = true;
    private static volatile String lastLoadError;

    private GlapConfig() {
    }

    /** Parses, validates, and persists before atomically publishing a complete snapshot. */
    public static boolean load() {
        try {
            SETTINGS.set(LOADER.load(FILE));
            lastLoadError = null;
            return true;
        } catch (final Exception e) {
            lastLoadError = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            LOGGER.error("glap.yml 설정을 적용하지 못했어요. 기존 설정을 그대로 사용해요.", e);
            return false;
        }
    }

    /** Called once after initial config load to record the restart-only metrics state. */
    public static void captureStartupSettings() {
        metricsEnabledAtStartup = settings().paperMetricsEnabled();
    }

    public static GlapSettings settings() {
        return SETTINGS.get();
    }

    public static boolean endermanPicksUpBlocks() {
        return settings().endermanPicksUpBlocks();
    }

    public static boolean creeperDestroysBlocks() {
        return settings().creeperDestroysBlocks();
    }

    public static boolean farmlandTrampling() {
        return settings().farmlandTrampling();
    }

    public static boolean allowWaterPlacementInEnd() {
        return settings().allowWaterPlacementInEnd();
    }

    public static boolean dispensersPlaceAnvils() {
        return settings().dispensersPlaceAnvils();
    }

    public static boolean allowInfinityAndMendingTogether() {
        return settings().allowInfinityAndMendingTogether();
    }

    public static boolean updatesEnabled() {
        return settings().updatesEnabled();
    }

    public static boolean paperMetricsEnabled() {
        return settings().paperMetricsEnabled();
    }

    public static boolean metricsEnabledAtStartup() {
        return metricsEnabledAtStartup;
    }

    public static String lastLoadError() {
        return lastLoadError;
    }
}
