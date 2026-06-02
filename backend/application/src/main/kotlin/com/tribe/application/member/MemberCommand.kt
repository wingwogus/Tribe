package com.tribe.application.member

/**
 * 회원 command 모델 경계.
 *
 * controller 입력을 use case 의도로 정규화.
 */
object MemberCommand {
    data object GetMyProfile

    data class GetMemberProfile(
        val memberId: Long,
    )

    data class UpdateNickname(
        val nickname: String,
    )
}
