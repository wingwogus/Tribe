package com.tribe.api.websocket

import com.tribe.application.security.TokenProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder

class WebSocketAuthChannelInterceptorTest {
    private val tokenProvider = mockk<TokenProvider>()
    private val interceptor = WebSocketAuthChannelInterceptor(tokenProvider)
    private val channel = mockk<MessageChannel>(relaxed = true)

    @Test
    fun `refresh token is rejected on connect`() {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        accessor.setLeaveMutable(true)
        accessor.addNativeHeader("Authorization", "Bearer refresh-token")
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)

        every { tokenProvider.validateToken("refresh-token") } returns true
        every { tokenProvider.isAccessToken("refresh-token") } returns false

        assertNull(interceptor.preSend(message, channel))
    }

    @Test
    fun `access token is accepted on connect`() {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        accessor.setLeaveMutable(true)
        accessor.addNativeHeader("Authorization", "Bearer access-token")
        val message = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        val authentication = mockk<org.springframework.security.core.Authentication>(relaxed = true)

        every { tokenProvider.validateToken("access-token") } returns true
        every { tokenProvider.isAccessToken("access-token") } returns true
        every { tokenProvider.getAuthentication("access-token") } returns authentication

        assertNotNull(interceptor.preSend(message, channel))
    }
}
