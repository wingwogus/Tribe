package com.tribe.application.community

import org.springframework.web.multipart.MultipartFile

/**
 * 커뮤니티 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
interface CommunityImageStorage {
    fun upload(file: MultipartFile, folder: String): String
    fun delete(imageUrl: String)
}
