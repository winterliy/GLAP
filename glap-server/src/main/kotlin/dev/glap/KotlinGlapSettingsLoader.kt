package dev.glap

import com.mojang.logging.LogUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import org.bukkit.configuration.file.YamlConfiguration

/** Parses and persists GLAP settings before the Java bridge publishes the new snapshot. */
class KotlinGlapSettingsLoader : GlapSettingsLoader {
    override fun load(file: File): GlapSettings {
        val config = YamlConfiguration()
        if (file.exists()) config.load(file)

        val oldVersion = readVersion(config)
        require(oldVersion <= CURRENT_CONFIG_VERSION) {
            "glap.yml config-version $oldVersion is newer than supported version $CURRENT_CONFIG_VERSION"
        }

        val settings = GlapSettings(
            CURRENT_CONFIG_VERSION,
            boolean(config, "gameplay.enderman-picks-up-blocks", true, "false면 엔더맨이 블록을 집어 가지 않아요."),
            boolean(config, "gameplay.creeper-destroys-blocks", true, "false면 크리퍼 폭발로 블록이 부서지지 않아요."),
            boolean(config, "gameplay.farmland-trampling", true, "false면 플레이어와 몹이 밭을 밟아도 흙으로 변하지 않아요."),
            boolean(config, "gameplay.allow-water-placement-in-end", false, "true면 엔드에서도 물을 설치할 수 있어요."),
            boolean(config, "gameplay.dispensers-place-anvils", false, "true면 디스펜서가 모루를 블록으로 설치해요."),
            boolean(config, "gameplay.allow-infinity-and-mending-together", false, "true면 활에 무한과 수선을 함께 부여할 수 있어요."),
            boolean(config, "updates.enabled", true, "false면 시작 시 자동 확인과 /glap version의 원격 확인을 끄고 로컬 버전만 표시해요."),
            boolean(config, "privacy.paper-metrics-enabled", true, "false면 Paper 내장 Metrics 전송을 끄며 재시작 후 적용돼요.")
        )

        config.set("config-version", CURRENT_CONFIG_VERSION)
        config.options().setHeader(HEADER)
        if (file.exists() && oldVersion < CURRENT_CONFIG_VERSION) backup(file)
        config.save(file)
        return settings
    }

    private fun readVersion(config: YamlConfiguration): Int {
        if (!config.contains("config-version")) return 0
        require(config.isInt("config-version")) { "glap.yml config-version must be an integer" }
        val version = config.getInt("config-version")
        require(version >= 0) { "glap.yml config-version cannot be negative" }
        return version
    }

    private fun boolean(config: YamlConfiguration, path: String, default: Boolean, comment: String): Boolean {
        if (config.contains(path)) require(config.isBoolean(path)) { "glap.yml $path must be true or false" }
        else config.set(path, default)
        config.setComments(path, listOf(comment))
        return config.getBoolean(path, default)
    }

    private fun backup(file: File) {
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC).format(Instant.now())
        val backup = File(file.parentFile ?: File("."), "${file.name}.$timestamp.bak")
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        LOGGER.info("기존 glap.yml 설정을 {}에 백업했어요.", backup.name)
    }

    private companion object {
        const val CURRENT_CONFIG_VERSION = 1
        val LOGGER = LogUtils.getLogger()
        val HEADER = listOf(
            "GLAP1 설정 파일이에요.",
            "값을 바꾼 뒤 서버를 재시작하거나 /glap reload 를 입력하면 적용돼요.",
            "https://github.com/winterliy/GLAP"
        )
    }
}
