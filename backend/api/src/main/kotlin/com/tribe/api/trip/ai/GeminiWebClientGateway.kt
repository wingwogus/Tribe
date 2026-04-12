package com.tribe.api.trip.ai

import com.tribe.application.trip.ai.GeminiGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
@ConditionalOnProperty(name = ["tribe.trip.review.enabled"], havingValue = "true", matchIfMissing = true)
class GeminiWebClientGateway(
    webClientBuilder: WebClient.Builder,
    @Value("\${gemini.api.key}") private val apiKey: String,
    @Value("\${gemini.api.url}") private val apiUrl: String,
) : GeminiGateway {
    private val webClient = webClientBuilder.build()

    override fun generate(prompt: String): String? {
        val response = webClient.post()
            .uri("$apiUrl?key=$apiKey")
            .bodyValue(
                mapOf(
                    "contents" to listOf(
                        mapOf(
                            "parts" to listOf(
                                mapOf("text" to prompt)
                            )
                        )
                    )
                )
            )
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()
            ?: return null

        val candidates = response["candidates"] as? List<*> ?: return null
        val first = candidates.firstOrNull() as? Map<*, *> ?: return null
        val content = first["content"] as? Map<*, *> ?: return null
        val parts = content["parts"] as? List<*> ?: return null
        val firstPart = parts.firstOrNull() as? Map<*, *> ?: return null
        return firstPart["text"] as? String
    }
}
