package dev.glap;

import com.destroystokyo.paper.util.VersionFetcher;
import com.mojang.logging.LogUtils;
import io.papermc.paper.util.JarManifests;
import java.util.jar.Manifest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import static net.kyori.adventure.text.Component.text;

/** Paper's version check hook. The GitHub lookup itself lives in {@link GlapReleases}. */
public final class GlapVersionFetcher implements VersionFetcher {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String CURRENT_VERSION = readVersion();

    @Override
    public long getCacheTime() {
        return 720_000;
    }

    @Override
    public Component getVersionMessage() {
        final GlapReleases.Status status = GlapReleases.check(CURRENT_VERSION);
        if (status instanceof GlapReleases.Status.Outdated outdated) {
            return text("GLAP1 " + CURRENT_VERSION + " — 새 버전 " + outdated.getLatest() + "이 있어요. ", NamedTextColor.YELLOW)
                .append(text(GlapReleases.PAGE, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(GlapReleases.PAGE)));
        }
        if (status instanceof GlapReleases.Status.UpToDate) {
            return text("GLAP1 " + CURRENT_VERSION + " — 최신 버전이에요.", NamedTextColor.GREEN);
        }
        return text("GLAP1 " + CURRENT_VERSION + " — 업데이트를 확인하지 못했어요.", NamedTextColor.GRAY);
    }

    /** Called once on startup in place of Paper's update check. */
    public static void startupCheck() {
        final GlapReleases.Status status = GlapReleases.check(CURRENT_VERSION);
        if (status instanceof GlapReleases.Status.Outdated outdated) {
            LOGGER.warn("*** GLAP1 새 버전 {}이 나왔어요 (지금 {}). ***", outdated.getLatest(), CURRENT_VERSION);
            LOGGER.warn("*** {} 에서 받을 수 있어요. ***", GlapReleases.PAGE);
        } else if (status instanceof GlapReleases.Status.UpToDate) {
            LOGGER.info("GLAP1 {}은 최신 버전이에요.", CURRENT_VERSION);
        } else {
            LOGGER.info("GLAP1 업데이트를 확인하지 못했어요 (지금 {}).", CURRENT_VERSION);
        }
    }

    private static String readVersion() {
        final Manifest manifest = JarManifests.manifest(GlapVersionFetcher.class);
        final String version = manifest == null ? null : manifest.getMainAttributes().getValue("GLAP-Version");
        return version == null ? "dev" : version;
    }
}
