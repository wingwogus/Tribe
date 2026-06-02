package com.tribe.domain.member

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 회원 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface MemberRepository: JpaRepository<Member, Long> {
    fun findByEmail(email: String): Member?
    fun findByProviderAndProviderId(provider: String, providerId: String): Member?
    fun existsByNickname(nickname: String): Boolean
    fun existsByEmail(email: String): Boolean
}
