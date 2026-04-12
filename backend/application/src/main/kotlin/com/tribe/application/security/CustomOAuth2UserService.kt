package com.tribe.application.security

import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@ConditionalOnProperty(name = ["tribe.auth.enabled"], havingValue = "true", matchIfMissing = true)
class CustomOAuth2UserService(
    private val memberRepository: MemberRepository,
    private val passwordEncoder: PasswordEncoder
) : DefaultOAuth2UserService() {

    @Transactional
    override fun loadUser(userRequest: OAuth2UserRequest): OAuth2User {
        val oauth2User = super.loadUser(userRequest)
        return resolvePrincipal(
            userRequest.clientRegistration.registrationId,
            userRequest.clientRegistration.providerDetails.userInfoEndpoint.userNameAttributeName,
            oauth2User.attributes
        )
    }

    @Transactional
    fun resolvePrincipal(
        registrationId: String,
        nameAttributeName: String,
        attributes: Map<String, Any>
    ): OAuth2MemberPrincipal {
        return try {
            syncMember(OAuthAttributes.of(registrationId, nameAttributeName, attributes))
        } catch (exception: IllegalArgumentException) {
            throw OAuth2AuthenticationException(
                OAuth2Error("oauth_profile_invalid", exception.message ?: "OAuth profile is invalid", null),
                exception.message
            )
        }
    }

    @Transactional
    fun syncMember(attributes: OAuthAttributes): OAuth2MemberPrincipal {
        val member = memberRepository.findByProviderAndProviderId(attributes.provider, attributes.providerId)
            ?: memberRepository.findByEmail(attributes.email)?.apply {
                linkOAuthAccount(
                    provider = attributes.provider,
                    providerId = attributes.providerId,
                    avatar = attributes.avatar
                )
            }
            ?: memberRepository.save(
                Member(
                    email = attributes.email,
                    passwordHash = passwordEncoder.encode(UUID.randomUUID().toString()),
                    nickname = resolveUniqueNickname(attributes.nickname),
                    avatar = attributes.avatar,
                    provider = attributes.provider,
                    providerId = attributes.providerId,
                    isFirstLogin = true,
                    role = Member.ROLE_USER
                )
            )

        return OAuth2MemberPrincipal(
            memberId = member.id,
            role = member.role,
            isFirstLogin = member.isFirstLogin,
            attributes = attributes.attributes,
            nameAttributeKey = attributes.nameAttributeKey
        )
    }

    private fun resolveUniqueNickname(rawNickname: String): String {
        val baseNickname = rawNickname.ifBlank { "member" }.take(20)
        if (!memberRepository.existsByNickname(baseNickname)) {
            return baseNickname
        }

        var suffix = 1
        while (suffix < 10_000) {
            val candidate = "$baseNickname$suffix".take(20)
            if (!memberRepository.existsByNickname(candidate)) {
                return candidate
            }
            suffix += 1
        }

        return "${baseNickname.take(15)}${UUID.randomUUID().toString().take(5)}"
    }
}
