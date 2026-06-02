package com.tribe.domain.community

/**
 * 커뮤니티 도메인 상태 모델.
 *
 * 영속성 identity와 업무 규칙의 기준점.
 */
data class PostSearchCondition(
    val country: String? = null,
    val authorId: Long? = null,
)
