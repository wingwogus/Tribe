package com.tribe.config

import com.cloudinary.Cloudinary
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 설정 API 설정 경계.
 *
 * Spring wiring과 runtime 옵션 연결.
 */
@Configuration
class CloudinaryConfig(
    @Value("\${cloudinary.url}") private val cloudinaryUrl: String,
) {
    @Bean
    fun cloudinary(): Cloudinary = Cloudinary(cloudinaryUrl)
}
