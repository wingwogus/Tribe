package com.tribe.application.security

data class OAuthAttributes(
    val email: String,
    val nickname: String,
    val avatar: String?,
    val providerId: String,
    val provider: String,
    val attributes: Map<String, Any>,
    val nameAttributeKey: String
) {
    companion object {
        fun of(registrationId: String, userNameAttributeName: String, attributes: Map<String, Any>): OAuthAttributes {
            return when (registrationId) {
                "kakao" -> ofKakao(userNameAttributeName, attributes)
                else -> throw IllegalArgumentException("Unsupported OAuth2 provider: $registrationId")
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun ofKakao(userNameAttributeName: String, attributes: Map<String, Any>): OAuthAttributes {
            val kakaoAccount = attributes["kakao_account"] as? Map<String, Any>
                ?: throw IllegalArgumentException("카카오 계정 정보가 없습니다.")
            val profile = kakaoAccount["profile"] as? Map<String, Any>
                ?: throw IllegalArgumentException("카카오 프로필 정보가 없습니다.")
            val email = kakaoAccount["email"] as? String
                ?: throw IllegalArgumentException("카카오 계정 이메일이 없습니다.")
            val providerId = attributes[userNameAttributeName]?.toString()
                ?: throw IllegalArgumentException("카카오 사용자 식별자가 없습니다.")
            val nickname = profile["nickname"] as? String
                ?: throw IllegalArgumentException("카카오 닉네임 정보가 없습니다.")

            return OAuthAttributes(
                email = email,
                nickname = nickname,
                avatar = profile["profile_image_url"] as? String,
                providerId = providerId,
                provider = "KAKAO",
                attributes = attributes,
                nameAttributeKey = userNameAttributeName
            )
        }
    }
}
