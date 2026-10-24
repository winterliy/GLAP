package dev.glap;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;

/**
 * Loads {@code glap.yml} from the server directory. Minecraft code reads the fields directly, so each
 * option stays a plain static field.
 */
public final class GlapConfig {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final File FILE = new File("glap.yml");
    private static final List<String> HEADER = List.of(
        "GLAP1 설정 파일이에요.",
        "값을 바꾼 뒤 서버를 재시작하거나 /glap reload 를 입력하면 적용돼요.",
        "https://github.com/winterliy/GLAP"
    );

    public static boolean endermanPicksUpBlocks = true;
    public static boolean creeperDestroysBlocks = true;
    public static boolean farmlandTrampling = true;

    private GlapConfig() {
    }

    /** Returns false and keeps the current values when the file cannot be read. */
    public static boolean load() {
        final YamlConfiguration config = new YamlConfiguration();
        if (FILE.exists()) {
            try {
                config.load(FILE);
            } catch (final Exception e) {
                LOGGER.error("glap.yml을 읽지 못해서 이전 설정을 그대로 써요.", e);
                return false;
            }
        }
        config.options().setHeader(HEADER);

        endermanPicksUpBlocks = option(config, "gameplay.enderman-picks-up-blocks", true,
            "false면 엔더맨이 블록을 집어 가지 않아요.");
        creeperDestroysBlocks = option(config, "gameplay.creeper-destroys-blocks", true,
            "false면 크리퍼가 터져도 블록은 부서지지 않아요. 주변 생명체는 그대로 피해를 입어요.");
        farmlandTrampling = option(config, "gameplay.farmland-trampling", true,
            "false면 플레이어와 몹이 밭을 밟아도 흙으로 변하지 않아요.");

        try {
            config.save(FILE);
        } catch (final Exception e) {
            LOGGER.error("glap.yml을 저장하지 못했어요.", e);
        }
        return true;
    }

    private static boolean option(final YamlConfiguration config, final String path, final boolean def, final String comment) {
        if (!config.contains(path)) {
            config.set(path, def);
        }
        config.setComments(path, List.of(comment));
        return config.getBoolean(path, def);
    }
}
