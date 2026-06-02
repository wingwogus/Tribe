package com.tribe.domain.chat

import org.springframework.data.jpa.repository.JpaRepository

/**
 * 채팅 repository 경계.
 *
 * 도메인 조회 의도와 persistence query 이름 분리.
 */
interface ChatMessageRepository : JpaRepository<ChatMessage, Long>, ChatMessageRepositoryCustom
