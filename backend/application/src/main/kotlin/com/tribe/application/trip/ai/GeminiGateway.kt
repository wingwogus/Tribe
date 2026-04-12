package com.tribe.application.trip.ai

interface GeminiGateway {
    fun generate(prompt: String): String?
}
