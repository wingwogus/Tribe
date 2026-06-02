package com.tribe.api.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

/**
 * 보안 framework callback 경계.
 *
 * Spring 실행 흐름과 Tribe 오류/인증 규칙 연결.
 */
@Component
class OAuth2LoginFailureHandler(
    @Value("\${app.url:http://localhost:3000}") private val appUrl: String
) : SimpleUrlAuthenticationFailureHandler() {

    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException
    ) {
        val errorCode = (exception as? OAuth2AuthenticationException)?.error?.errorCode ?: "oauth_login_failed"
        val targetUrl = UriComponentsBuilder.fromUriString("$appUrl/oauth/callback")
            .queryParam("error", errorCode)
            .build()
            .toUriString()

        redirectStrategy.sendRedirect(request, response, targetUrl)
    }
}
