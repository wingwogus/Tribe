package com.tribe.application.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

/**
 * 보안 application 계층 경계.
 *
 * 도메인 조작과 외부 adapter 의존성 분리.
 */
class OAuth2MemberPrincipal(
    val memberId: Long,
    val role: String,
    val isFirstLogin: Boolean = false,
    private val attributes: Map<String, Any>,
    private val nameAttributeKey: String
) : OAuth2User {

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority(role))
    }

    override fun getName(): String {
        return attributes[nameAttributeKey]?.toString() ?: memberId.toString()
    }
}
