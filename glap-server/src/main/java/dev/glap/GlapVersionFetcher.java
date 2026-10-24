package dev.glap;

import com.destroystokyo.paper.util.VersionFetcher;
import com.mojang.logging.LogUtils;
import io.papermc.paper.util.JarManifests;
import java.util.ServiceLoader;
import java.util.jar.Manifest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import static net.kyori.adventure.text.Component.text;

/** Paper's version check hook, backed by the {@link UpdateChecker} service. */
public final class GlapVersionFetcher implements VersionFetcher {
    public static final String REPOSITORY = "winterliy/GLAP";
    public static final String RELEASES_PAGE = "https://github.com/" + REPOSITORY + "/releases";
    public static final String CURRENT_VERSION = readVersion();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UpdateChecker CHECKER = ServiceLoader.load(UpdateChecker.class, GlapVersionFetcher.class.getClassLoader())
        .findFirst()
        .orElseGet(() -> {
            LOGGER.warn("GLAP1 업데이트 확인 기능(UpdateChecker)을 찾지 못했어요. jar가 잘못 빌드됐을 수 있어요.");
            return current -> new UpdateChecker.Status.Unknown();
        });

    @Override
    public long getCacheTime() {
        return 720_000;
    }

    @Override
    public Component getVersionMessage() {
        if (!GlapConfig.updatesEnabled()) {
            return text("GLAP1 " + CURRENT_VERSION + " — 원격 업데이트 확인이 꺼져 있어요. ", NamedTextColor.GRAY)
                .append(text(RELEASES_PAGE, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(RELEASES_PAGE)));
        }
        return switch (CHECKER.check(CURRENT_VERSION)) {
            case UpdateChecker.Status.Outdated(final String latest) ->
                text("GLAP1 " + CURRENT_VERSION + " — 새 버전 " + latest + "이 있어요. ", NamedTextColor.YELLOW)
                    .append(text(RELEASES_PAGE, NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(RELEASES_PAGE)));
            case UpdateChecker.Status.UpToDate() -> text("GLAP1 " + CURRENT_VERSION + " — 최신 버전이에요.", NamedTextColor.GREEN);
            case UpdateChecker.Status.Unknown() -> text("GLAP1 " + CURRENT_VERSION + " — 업데이트를 확인하지 못했어요.", NamedTextColor.GRAY);
        };
    }

    /** Called once on startup in place of Paper's update check. */
    public static void startupCheck() {
        if (!GlapConfig.updatesEnabled()) {
            LOGGER.info("GLAP1 원격 업데이트 확인이 설정으로 꺼져 있어요 (지금 {}).", CURRENT_VERSION);
            return;
        }
        switch (CHECKER.check(CURRENT_VERSION)) {
            case UpdateChecker.Status.Outdated(final String latest) -> {
                LOGGER.warn("*** GLAP1 새 버전 {}이 나왔어요 (지금 {}). ***", latest, CURRENT_VERSION);
                LOGGER.warn("*** {} 에서 받을 수 있어요. ***", RELEASES_PAGE);
            }
            case UpdateChecker.Status.UpToDate() -> LOGGER.info("GLAP1 {}은 최신 버전이에요.", CURRENT_VERSION);
            case UpdateChecker.Status.Unknown() -> LOGGER.info("GLAP1 업데이트를 확인하지 못했어요 (지금 {}).", CURRENT_VERSION);
        }
    }

    private static String readVersion() {
        final Manifest manifest = JarManifests.manifest(GlapVersionFetcher.class);
        final String version = manifest == null ? null : manifest.getMainAttributes().getValue("GLAP-Version");
        return version == null ? "dev" : version;
    }
}
