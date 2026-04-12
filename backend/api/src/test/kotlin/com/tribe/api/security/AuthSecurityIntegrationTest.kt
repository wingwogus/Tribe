package com.tribe.api.security

import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(
    properties = [
        "spring.mail.username=test@example.com",
        "spring.mail.password=test-password"
    ]
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val tokenProvider: TokenProvider
) {

    @Test
    fun `auth endpoints are publicly accessible`() {
        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"invalid","password":"password123"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `protected endpoint rejects missing token`() {
        mockMvc.perform(get("/api/v1/test/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint accepts valid jwt`() {
        val accessToken = tokenProvider.createAccessToken(42L, "ROLE_USER")

        mockMvc.perform(
            get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(content().string(containsString("42")))
    }

    @Test
    fun `refresh token cannot authenticate protected endpoint`() {
        val refreshToken = tokenProvider.createRefreshToken(42L)

        mockMvc.perform(
            get("/api/v1/test/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $refreshToken")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `logout requires authentication`() {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isUnauthorized)
    }
}
