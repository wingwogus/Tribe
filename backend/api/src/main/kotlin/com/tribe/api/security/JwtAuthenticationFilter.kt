package com.tribe.api.security

import com.tribe.application.security.TokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 보안 framework callback 경계.
 *
 * Spring 실행 흐름과 Tribe 오류/인증 규칙 연결.
 */
@Component
class JwtAuthenticationFilter(
    private val tokenProvider: TokenProvider
) : OncePerRequestFilter() {


    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token != null && tokenProvider.validateToken(token) && tokenProvider.isAccessToken(token)) {


            val auth = tokenProvider.getAuthentication(token)
            SecurityContextHolder.getContext().authentication = auth

            val userId = tokenProvider.getUserId(token)
            MDC.put("userId", userId.toString())
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) {
            bearer.substring(7)
        } else null
    }
}
