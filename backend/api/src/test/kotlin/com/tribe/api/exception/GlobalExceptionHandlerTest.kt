package com.tribe.api.exception

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class GlobalExceptionHandlerTest {
    private val handler = GlobalExceptionHandler()

    @Test
    fun `external api business exception preserves safe detail with bad gateway status`() {
        val detail = mapOf(
            "operation" to "google_place_details",
            "externalPlaceId" to "place-1",
            "status" to 500,
            "cause" to "http_status",
            "retryable" to true,
        )

        val response = handler.handleBusiness(
            BusinessException(ErrorCode.EXTERNAL_API_ERROR, detail = detail),
        )

        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
        val body = requireNotNull(response.body)
        assertFalse(body.success)
        assertNull(body.data)
        assertEquals("COMMON_011", body.error?.code)
        assertEquals("error.external_api_error", body.error?.message)
        assertEquals(detail, body.error?.detail)
    }
}
