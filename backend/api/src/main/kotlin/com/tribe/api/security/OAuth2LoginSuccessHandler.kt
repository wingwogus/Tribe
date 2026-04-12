package com.tribe.api.security

import com.tribe.application.redis.RefreshTokenRepository
import com.tribe.application.security.OAuth2MemberPrincipal
import com.tribe.application.security.TokenProvider
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import org.springframework.http.ResponseCookie
import org.springframework.web.util.UriComponentsBuilder

@Component
class OAuth2LoginSuccessHandler(
    private val tokenProvider: TokenProvider,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${app.url:http://localhost:3000}") private val appUrl: String,
    @Value("\${app.auth.cookie.secure:false}") private val secureCookie: Boolean,
    @Value("\${app.auth.cookie.same-site:Lax}") private val sameSite: String,
) : SimpleUrlAuthenticationSuccessHandler() {

    override fun onAuthenticationSuccess(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authentication: Authentication
    ) {
        val principal = authentication.principal as OAuth2MemberPrincipal
        val tokenPair = tokenProvider.generateToken(principal.memberId, principal.role)

        refreshTokenRepository.save(
            principal.memberId,
            tokenPair.refreshToken,
            tokenProvider.getRefreshTokenValiditySeconds()
        )

        val refreshTokenCookie = ResponseCookie.from("refreshToken", tokenPair.refreshToken)
            .httpOnly(true)
            .secure(secureCookie)
            .path("/")
            .sameSite(sameSite)
            .maxAge(tokenProvider.getRefreshTokenValiditySeconds())
            .build()
        response.addHeader("Set-Cookie", refreshTokenCookie.toString())

        val targetUrl = UriComponentsBuilder.fromUriString("$appUrl/oauth/callback")
            .queryParam("accessToken", tokenPair.accessToken)
            .queryParam("isFirstLogin", principal.isFirstLogin)
            .build()
            .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
