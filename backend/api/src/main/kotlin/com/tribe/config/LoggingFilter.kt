package com.tribe.config


import com.tribe.application.common.LoggingUtil
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * 설정 framework callback 경계.
 *
 * Spring 실행 흐름과 Tribe 오류/인증 규칙 연결.
 */
@Component
class LoggingFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        try {
            MDC.put("traceId", UUID.randomUUID().toString())
            MDC.put("eventId", LoggingUtil.generateEventId())
            MDC.put("clientIp", request.remoteAddr)

            logger.info("Incoming request: ${request.method} ${request.requestURI}")

            chain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
