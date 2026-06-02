package com.tribe.api.community

import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils
import com.tribe.application.community.CommunityImageStorage
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile

/**
 * 커뮤니티 외부 adapter 경계.
 *
 * 외부 SDK/API 응답을 application port shape로 변환.
 */
@Component
@ConditionalOnProperty(name = ["tribe.community.image.enabled"], havingValue = "true", matchIfMissing = true)
class CloudinaryCommunityImageStorage(
    private val cloudinary: Cloudinary,
) : CommunityImageStorage {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun upload(file: MultipartFile, folder: String): String {
        try {
            val options = ObjectUtils.asMap(
                "folder", folder,
                "resource_type", "image",
            )
            val result = cloudinary.uploader().upload(file.bytes, options)
            return result["secure_url"] as String
        } catch (e: Exception) {
            log.error("Cloudinary upload failed: {}", e.message, e)
            throw BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED)
        }
    }

    override fun delete(imageUrl: String) {
        try {
            val publicId = extractPublicIdFromUrl(imageUrl)
            if (publicId.isNotBlank()) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"))
            }
        } catch (e: Exception) {
            log.error("Cloudinary delete failed: {}", e.message, e)
        }
    }

    private fun extractPublicIdFromUrl(imageUrl: String): String {
        return try {
            val uploadMarker = "/upload/"
            val markerIndex = imageUrl.indexOf(uploadMarker)
            if (markerIndex == -1) return ""
            val publicIdStartIndex = imageUrl.indexOf('/', markerIndex + uploadMarker.length)
            if (publicIdStartIndex == -1) return ""
            val publicIdEndIndex = imageUrl.lastIndexOf('.')
            if (publicIdEndIndex == -1 || publicIdEndIndex < publicIdStartIndex) return ""
            imageUrl.substring(publicIdStartIndex + 1, publicIdEndIndex)
        } catch (_: Exception) {
            ""
        }
    }
}
