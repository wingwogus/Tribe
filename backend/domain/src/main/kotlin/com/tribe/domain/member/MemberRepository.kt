package com.tribe.domain.member

import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository: JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?
    fun findByProviderAndProviderId(provider: String, providerId: String): Member?
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
}
