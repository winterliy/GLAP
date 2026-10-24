package dev.glap

import com.google.gson.JsonParser
import com.mojang.logging.LogUtils
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Looks up the latest GLAP release on GitHub and compares it with this build. */
object GlapReleases {
    const val REPOSITORY = "winterliy/GLAP"
    const val PAGE = "https://github.com/$REPOSITORY/releases"
    private val LOGGER = LogUtils.getLogger()
    private val HTTP: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    sealed interface Status {
        data object UpToDate : Status
        data class Outdated(val latest: String) : Status
        data object Unknown : Status
    }

    @JvmStatic
    fun check(current: String): Status {
        val latest = fetchLatestVersion(current) ?: return Status.Unknown
        val local = parseVersion(current) ?: return Status.Unknown
        val remote = parseVersion(latest) ?: return Status.Unknown
        return if (compareVersions(remote, local) > 0) Status.Outdated(latest) else Status.UpToDate
    }

    private fun fetchLatestVersion(current: String): String? = try {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/$REPOSITORY/releases/latest"))
            .timeout(Duration.ofSeconds(5))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "GLAP1/$current")
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
