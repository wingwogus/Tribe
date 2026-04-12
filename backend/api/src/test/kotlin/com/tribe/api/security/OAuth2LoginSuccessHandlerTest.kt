package com.tribe.api.security

import com.tribe.application.auth.AuthResult
import com.tribe.application.redis.RefreshTokenRepository
import com.tribe.application.security.OAuth2MemberPrincipal
import com.tribe.application.security.TokenProvider
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority

@ExtendWith(MockitoExtension::class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private lateinit var tokenProvider: TokenProvider

    @Mock
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Test
    fun `success handler stores refresh token and redirects with tokens`() {
        val handler = OAuth2LoginSuccessHandler(
            tokenProvider,
            refreshTokenRepository,
            "http://localhost:3000",
            false,
            "Lax"
        )
        val principal = OAuth2MemberPrincipal(
            memberId = 1L,
            role = "ROLE_USER",
            attributes = mapOf("id" to "kakao-1"),
            nameAttributeKey = "id"
        )
        val authentication = UsernamePasswordAuthenticationToken(
            principal,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        `when`(tokenProvider.generateToken(1L, "ROLE_USER"))
            .thenReturn(AuthResult.TokenPair("access-token", "refresh-token"))
        `when`(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L)

        handler.onAuthenticationSuccess(request, response, authentication)

        verify(refreshTokenRepository).save(1L, "refresh-token", 120L)
        assertTrue(response.redirectedUrl!!.contains("accessToken=access-token"))
        assertTrue(!response.redirectedUrl!!.contains("refreshToken="))
        assertTrue(response.getHeader("Set-Cookie")!!.contains("refreshToken=refresh-token"))
    }
}
