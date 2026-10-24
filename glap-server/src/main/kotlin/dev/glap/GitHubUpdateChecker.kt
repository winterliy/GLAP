package dev.glap

import com.google.gson.JsonParser
import com.mojang.logging.LogUtils
import dev.glap.UpdateChecker.Status
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Compares this build with the latest GLAP release on GitHub. Registered in `META-INF/services`. */
class GitHubUpdateChecker : UpdateChecker {
    override fun check(currentVersion: String): Status {
        val latest = fetchLatestVersion(currentVersion) ?: return Status.Unknown()
        val local = parseVersion(currentVersion) ?: return Status.Unknown()
        val remote = parseVersion(latest) ?: return Status.Unknown()
        return if (compareVersions(remote, local) > 0) Status.Outdated(latest) else Status.UpToDate()
    }

    private fun fetchLatestVersion(currentVersion: String): String? = try {
        val request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/${GlapVersionFetcher.REPOSITORY}/releases/latest"))
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

    private companion object {
        val LOGGER = LogUtils.getLogger()
        val HTTP: HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
    }
}
