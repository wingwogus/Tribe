package com.tribe.api.controller

import com.tribe.api.exception.GlobalExceptionHandler
import com.tribe.application.auth.AuthCommand
import com.tribe.application.auth.AuthResult
import com.tribe.application.auth.AuthService
import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.security.TokenProvider
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.mock.web.MockCookie
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler::class)
class AuthControllerBusinessTest(
    @Autowired private val mockMvc: MockMvc
) {

    @MockBean
    private lateinit var authService: AuthService

    @MockBean
    private lateinit var tokenProvider: TokenProvider

    @Test
    fun `login returns token pair from service`() {
        `when`(authService.login(AuthCommand.Login("user@example.com", "password123")))
            .thenReturn(AuthResult.TokenPair("access-token", "refresh-token"))

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"user@example.com","password":"password123"}""")
        )
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=refresh-token")))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken", equalTo("access-token")))
            .andExpect(jsonPath("$.data.isFirstLogin", equalTo(false)))
    }

    @Test
    fun `send-code maps business exception to failure response`() {
        doThrow(object : BusinessException(ErrorCode.DUPLICATE_EMAIL) {})
            .`when`(authService)
            .sendVerificationCode(AuthCommand.SendVerificationCode("taken@example.com"))

        mockMvc.perform(
            post("/api/v1/auth/email/send-code")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"taken@example.com"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_003")))
    }

    @Test
    fun `reissue returns rotated token pair and refresh cookie`() {
        `when`(authService.reissue(AuthCommand.Reissue("refresh-token")))
            .thenReturn(AuthResult.TokenPair("new-access-token", "new-refresh-token"))
        `when`(tokenProvider.getRefreshTokenValiditySeconds()).thenReturn(120L)

        mockMvc.perform(
            post("/api/v1/auth/reissue")
                .cookie(MockCookie("refreshToken", "refresh-token"))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken", equalTo("new-access-token")))
            .andExpect(jsonPath("$.data.isFirstLogin", equalTo(false)))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("refreshToken=new-refresh-token")))
    }
}
