package com.tribe.api.trip.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class GeminiWebClientGatewayTest {
    private val gateway = GeminiWebClientGateway(
        webClientBuilder = WebClient.builder(),
        apiKey = "test-key",
        apiUrl = "https://example.com/generate",
    )

    @Test
    fun `buildRequestBody wraps prompt in gemini contents structure`() {
        val body = gateway.buildRequestBody("hello")

        val contents = body["contents"] as List<*>
        val first = contents.first() as Map<*, *>
        val parts = first["parts"] as List<*>
        val firstPart = parts.first() as Map<*, *>

        assertEquals("hello", firstPart["text"])
    }

    @Test
    fun `extractResponseText returns first text part`() {
        val response = mapOf(
            "candidates" to listOf(
                mapOf(
                    "content" to mapOf(
                        "parts" to listOf(
                            mapOf("text" to "gemini-output")
                        )
                    )
                )
            )
        )

        assertEquals("gemini-output", gateway.extractResponseText(response))
    }

    @Test
    fun `extractResponseText returns null when structure is missing`() {
        assertNull(gateway.extractResponseText(emptyMap<String, Any>()))
    }
}
