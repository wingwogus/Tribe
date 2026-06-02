package com.tribe.api.trip.ai

import com.tribe.application.trip.ai.GeminiGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

/**
 * 여행 외부 adapter 경계.
 *
 * 외부 SDK/API 응답을 application port shape로 변환.
 */
@Component
@ConditionalOnProperty(name = ["tribe.trip.review.enabled"], havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = ["trip.review.ai.provider"], havingValue = "gemini", matchIfMissing = true)
class GeminiWebClientGateway(
    webClientBuilder: WebClient.Builder,
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.api.url}") private val apiUrl: String,
) : GeminiGateway {
    private val webClient = webClientBuilder.build()

    override fun generate(prompt: String): String? {
        val response = webClient.post()
            .uri("$apiUrl?key=$apiKey")
            .bodyValue(buildRequestBody(prompt))
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()
            ?: return null

        return extractResponseText(response)
    }

    internal fun buildRequestBody(prompt: String): Map<String, Any> {
        return mapOf(
            "contents" to listOf(
                mapOf(
                    "parts" to listOf(
                        mapOf("text" to prompt)
                    )
                )
            )
        )
    }

    internal fun extractResponseText(response: Map<*, *>): String? {
        val candidates = response["candidates"] as? List<*> ?: return null
        val first = candidates.firstOrNull() as? Map<*, *> ?: return null
        val content = first["content"] as? Map<*, *> ?: return null
        val parts = content["parts"] as? List<*> ?: return null
        val firstPart = parts.firstOrNull() as? Map<*, *> ?: return null
        return firstPart["text"] as? String
    }
}
