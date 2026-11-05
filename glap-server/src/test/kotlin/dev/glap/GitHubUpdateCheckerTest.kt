package dev.glap

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GitHubUpdateCheckerTest {
    @Test
    fun `does not attempt private release lookup without a token`() {
        val checker = GitHubUpdateChecker(
            apiBaseUri = URI.create("http://127.0.0.1:1"),
            githubToken = null,
        )

        val status = checker.check("3.0.2")

        assertTrue(status is UpdateChecker.Status.Unknown)
        assertTrue((status as UpdateChecker.Status.Unknown).reason.contains("GLAP_GITHUB_TOKEN"))
    }

    @Test
    fun `uses a configured token to read the latest private release`() {
        var authorization: String? = null
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/repos/winterliy/GLAP/releases/latest") { exchange ->
            authorization = exchange.requestHeaders.getFirst("Authorization")
            val response = """{"tag_name":"v3.0.3"}""".toByteArray()
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()
        try {
            val checker = GitHubUpdateChecker(
                apiBaseUri = URI.create("http://127.0.0.1:${server.address.port}"),
                githubToken = "test-read-only-token",
                httpClient = HttpClient.newHttpClient(),
            )

            val status = checker.check("3.0.2")

            assertEquals(UpdateChecker.Status.Outdated("3.0.3"), status)
            assertEquals("Bearer test-read-only-token", authorization)
        } finally {
            server.stop(0)
        }
    }
}
