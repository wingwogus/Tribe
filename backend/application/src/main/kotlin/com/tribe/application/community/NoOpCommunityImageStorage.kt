package com.tribe.application.community

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * 커뮤니티 application port 경계.
 *
 * use case가 외부 구현 세부사항에 직접 의존하지 않는 계약.
 */
@Component
@ConditionalOnProperty(name = ["tribe.community.image.enabled"], havingValue = "false")
class NoOpCommunityImageStorage : CommunityImageStorage {
    override fun upload(file: MultipartFile, folder: String): String {
        return ""
    }

    override fun delete(imageUrl: String) {
        // 실제 media adapter 미연결 환경에서는 삭제 작업 없음.
    }
}
