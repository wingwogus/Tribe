package com.tribe.application.member

import com.tribe.domain.member.Member

/**
 * 회원 result 모델 경계.
 *
 * 도메인 상태를 API 응답 가능한 shape로 분리.
 */
object MemberResult {
    data class Profile(
        val memberId: Long,
        val nickname: String,
        val email: String,
        val avatar: String?,
        val isNewUser: Boolean,
    ) {
        companion object {
            fun from(member: Member): Profile {
                return Profile(
                    memberId = member.id,
                    nickname = member.nickname,
                    email = member.email,
                    avatar = member.avatar,
                    isNewUser = member.isFirstLogin,
                )
            }
        }
    }
}
