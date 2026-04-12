package com.tribe.application.security

import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.core.OAuth2AuthenticationException

@ExtendWith(MockitoExtension::class)
class CustomOAuth2UserServiceTest {

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `existing same-email member is reused`() {
        val service = CustomOAuth2UserService(memberRepository, passwordEncoder)
        val existingMember = Member(
            id = 7L,
            email = "user@example.com",
            passwordHash = "hashed-password",
            role = "ROLE_USER"
        )
        val attributes = OAuthAttributes(
            email = "user@example.com",
            nickname = "tribe-user",
            avatar = null,
            providerId = "kakao-1",
            provider = "KAKAO",
            attributes = mapOf("id" to "kakao-1"),
            nameAttributeKey = "id"
        )

        `when`(memberRepository.findByEmail("user@example.com")).thenReturn(existingMember)

        val principal = service.syncMember(attributes)

        assertEquals(7L, principal.memberId)
        assertEquals("ROLE_USER", principal.role)
    }

    @Test
    fun `new oauth user creates member with generated password hash`() {
        val service = CustomOAuth2UserService(memberRepository, passwordEncoder)
        val savedMember = Member(
            id = 8L,
            email = "new@example.com",
            passwordHash = "encoded-random",
            role = "ROLE_USER"
        )
        val attributes = OAuthAttributes(
            email = "new@example.com",
            nickname = "newbie",
            avatar = null,
            providerId = "kakao-2",
            provider = "KAKAO",
            attributes = mapOf("id" to "kakao-2"),
            nameAttributeKey = "id"
        )

        `when`(memberRepository.findByEmail("new@example.com")).thenReturn(null)
        `when`(passwordEncoder.encode(org.mockito.Mockito.anyString())).thenReturn("encoded-random")
        `when`(memberRepository.save(org.mockito.Mockito.any(Member::class.java))).thenReturn(savedMember)

        val principal = service.syncMember(attributes)

        assertEquals(8L, principal.memberId)
        verify(memberRepository).save(org.mockito.Mockito.any(Member::class.java))
    }

    @Test
    fun `missing oauth email is surfaced as oauth authentication failure`() {
        val service = CustomOAuth2UserService(memberRepository, passwordEncoder)

        val exception = assertThrows(OAuth2AuthenticationException::class.java) {
            service.resolvePrincipal(
                "kakao",
                "id",
                mapOf(
                    "id" to "kakao-3",
                    "kakao_account" to emptyMap<String, Any>()
                )
            )
        }

        assertEquals("oauth_profile_invalid", exception.error.errorCode)
    }
}
