package dev.glap

import com.destroystokyo.paper.util.VersionFetcher
import com.google.gson.JsonParser
import com.mojang.logging.LogUtils
import io.papermc.paper.util.JarManifests
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor

/** Compares this build's `GLAP-Version` with the latest GitHub release of GLAP. */
class GlapVersionFetcher : VersionFetcher {
    override fun getCacheTime(): Long = 720_000

    override fun getVersionMessage(): Component = when (val status = checkStatus()) {
        UpdateStatus.UpToDate -> text("GLAP1 $currentVersion — 최신 버전이에요.", NamedTextColor.GREEN)
        is UpdateStatus.Outdated -> text("GLAP1 $currentVersion — 새 버전 ${status.latest}이 있어요. ", NamedTextColor.YELLOW)
            .append(
                text(RELEASES_PAGE, NamedTextColor.AQUA)
                    .clickEvent(ClickEvent.openUrl(RELEASES_PAGE)),
            )
        UpdateStatus.Unknown -> text("GLAP1 $currentVersion — 업데이트를 확인하지 못했어요.", NamedTextColor.GRAY)
    }

    sealed interface UpdateStatus {
        data object UpToDate : UpdateStatus
        data class Outdated(val latest: String) : UpdateStatus
        data object Unknown : UpdateStatus
    }

    companion object {
        const val REPOSITORY = "winterliy/GLAP"
        const val RELEASES_PAGE = "https://github.com/$REPOSITORY/releases"
        private val LOGGER = LogUtils.getLogger()
        private val HTTP: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()

        @JvmStatic
        val currentVersion: String by lazy {
            JarManifests.manifest(GlapVersionFetcher::class.java)
                ?.mainAttributes?.getValue("GLAP-Version") ?: "dev"
        }

        /** Called once on startup in place of Paper's update check. */
        @JvmStatic
        fun startupCheck() {
            when (val status = checkStatus()) {
                UpdateStatus.UpToDate -> LOGGER.info("GLAP1 {}은 최신 버전이에요.", currentVersion)
                is UpdateStatus.Outdated -> {
                    LOGGER.warn("*** GLAP1 새 버전 {}이 나왔어요 (지금 {}). ***", status.latest, currentVersion)
                    LOGGER.warn("*** {} 에서 받을 수 있어요. ***", RELEASES_PAGE)
                }
                UpdateStatus.Unknown -> LOGGER.info("GLAP1 업데이트를 확인하지 못했어요 (지금 {}).", currentVersion)
            }
        }

        @JvmStatic
        fun checkStatus(): UpdateStatus {
            val latest = fetchLatestVersion() ?: return UpdateStatus.Unknown
            val current = parseVersion(currentVersion) ?: return UpdateStatus.Unknown
            val remote = parseVersion(latest) ?: return UpdateStatus.Unknown
            return if (compareVersions(remote, current) > 0) UpdateStatus.Outdated(latest) else UpdateStatus.UpToDate
        }

        private fun fetchLatestVersion(): String? = try {
            val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/$REPOSITORY/releases/latest"))
                .timeout(Duration.ofSeconds(5))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "GLAP1/$currentVersion")
                .GET()
                .build()
            val response = HTTP.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                null
            } else {
                JsonParser.parseString(response.body()).asJsonObject.get("tag_name")?.asString?.removePrefix("v")
            }
        } catch (e: Exception) {
            LOGGER.debug("Could not fetch the latest GLAP release", e)
            null
        }

        private fun parseVersion(version: String): List<Int>? =
            version.split('.').map { it.toIntOrNull() ?: return null }

        private fun compareVersions(a: List<Int>, b: List<Int>): Int {
            for (i in 0 until maxOf(a.size, b.size)) {
                val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
                if (diff != 0) return diff
            }
            return 0
        }
    }
}
