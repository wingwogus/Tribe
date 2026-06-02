package com.tribe.domain.community

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

/**
 * 커뮤니티 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface CommunityPostRepositoryCustom {
    fun searchPost(condition: PostSearchCondition, pageable: Pageable): Page<CommunityPost>
}
