package com.tribe.application.member

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.CurrentActor
import com.tribe.domain.member.MemberRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 use case 경계.
 *
 * 검증, 도메인 조회, 결과 조립 순서 보관.
 */
@Service
@Transactional
class MemberService(
    private val currentActor: CurrentActor,
    private val memberRepository: MemberRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getMyProfile(): MemberResult.Profile {
        val member = memberRepository.findById(currentActor.requireUserId())
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val profile = MemberResult.Profile.from(member)
        if (member.isFirstLogin) {
            member.isFirstLogin = false
        }
        return profile
    }

    @Transactional(readOnly = true)
    fun getMemberProfile(command: MemberCommand.GetMemberProfile): MemberResult.Profile {
        val member = memberRepository.findById(command.memberId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        return MemberResult.Profile.from(member)
    }

    fun updateNickname(command: MemberCommand.UpdateNickname): MemberResult.Profile {
        val member = memberRepository.findById(currentActor.requireUserId())
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
        val nextNickname = command.nickname.trim()
        if (nextNickname.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_INPUT)
        }

        if (member.nickname != nextNickname && memberRepository.existsByNickname(nextNickname)) {
            throw BusinessException(
                errorCode = ErrorCode.USER_ALREADY_EXISTS,
                detail = mapOf("nickname" to nextNickname),
                customMessage = "error.duplicate_nickname",
            )
        }

        member.nickname = nextNickname
        logger.info("Nickname updated. memberId={}", member.id)
        return MemberResult.Profile.from(member)
    }
}
