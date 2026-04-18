package com.tribe.api.trip.ai

import com.tribe.application.trip.ai.GeminiGateway
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
@ConditionalOnProperty(name = ["tribe.trip.review.enabled"], havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(name = ["trip.review.ai.provider"], havingValue = "ollama")
class OllamaWebClientGateway(
    webClientBuilder: WebClient.Builder,
    @Value("\${ollama.api.url}") private val apiUrl: String,
    @Value("\${ollama.model}") private val model: String,
) : GeminiGateway {
    private val webClient = webClientBuilder.build()

    override fun generate(prompt: String): String? {
        val response = webClient.post()
            .uri(apiUrl)
            .bodyValue(buildRequestBody(prompt))
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()
            ?: return null

        return extractResponseText(response)
    }

    internal fun buildRequestBody(prompt: String): Map<String, Any> {
        return mapOf(
            "model" to model,
            "prompt" to prompt,
            "stream" to false,
        )
    }

    internal fun extractResponseText(response: Map<*, *>): String? {
        return response["response"] as? String
    }
}
