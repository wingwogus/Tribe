package com.tribe.application.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.OAuth2User

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
