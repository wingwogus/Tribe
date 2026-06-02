package com.tribe.api.member

import com.tribe.application.member.MemberResult

/**
 * 회원 HTTP response 모델 경계.
 *
 * application result를 클라이언트 응답 shape로 조립.
 */
object MemberResponses {
    data class ProfileResponse(
        val memberId: Long,
        val nickname: String,
        val email: String,
        val avatar: String?,
        val isNewUser: Boolean,
    ) {
        companion object {
            fun from(profile: MemberResult.Profile): ProfileResponse {
                return ProfileResponse(
                    memberId = profile.memberId,
                    nickname = profile.nickname,
                    email = profile.email,
                    avatar = profile.avatar,
                    isNewUser = profile.isNewUser,
                )
            }
        }
    }
}
