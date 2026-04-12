package com.tribe.application.chat.event

interface ChatEventPublisher {
    fun publish(event: ChatEvent)
}
