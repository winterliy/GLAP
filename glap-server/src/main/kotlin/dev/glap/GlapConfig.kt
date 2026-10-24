package dev.glap

import com.mojang.logging.LogUtils
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration

/**
 * Loads `glap.yml` from the server directory. Minecraft code reads the fields directly, so each
 * option stays a plain static field.
 */
object GlapConfig {
    private val LOGGER = LogUtils.getLogger()
    private val FILE = File("glap.yml")
    private val HEADER = listOf(
        "GLAP1 설정 파일이에요.",
        "값을 바꾼 뒤 서버를 재시작하거나 /glap reload 를 입력하면 적용돼요.",
        "https://github.com/winterliy/GLAP",
    )

    @JvmField var endermanPicksUpBlocks = true
    @JvmField var creeperDestroysBlocks = true
    @JvmField var farmlandTrampling = true

    /** Returns false and keeps the current values when the file cannot be read. */
    @JvmStatic
    fun load(): Boolean {
        val config = YamlConfiguration()
        if (FILE.exists()) {
            try {
                config.load(FILE)
            } catch (e: Exception) {
                LOGGER.error("glap.yml을 읽지 못해서 이전 설정을 그대로 써요.", e)
                return false
            }
        }
        config.options().setHeader(HEADER)

        endermanPicksUpBlocks = config.option(
            "gameplay.enderman-picks-up-blocks", true,
            "false면 엔더맨이 블록을 집어 가지 않아요.",
        )
        creeperDestroysBlocks = config.option(
            "gameplay.creeper-destroys-blocks", true,
            "false면 크리퍼가 터져도 블록은 부서지지 않아요. 주변 생명체는 그대로 피해를 입어요.",
        )
        farmlandTrampling = config.option(
            "gameplay.farmland-trampling", true,
            "false면 플레이어와 몹이 밭을 밟아도 흙으로 변하지 않아요.",
        )

        try {
            config.save(FILE)
        } catch (e: Exception) {
            LOGGER.error("glap.yml을 저장하지 못했어요.", e)
        }
        return true
    }

    private fun YamlConfiguration.option(path: String, default: Boolean, comment: String): Boolean {
        if (!contains(path)) set(path, default)
        setComments(path, listOf(comment))
        return getBoolean(path, default)
    }
}
