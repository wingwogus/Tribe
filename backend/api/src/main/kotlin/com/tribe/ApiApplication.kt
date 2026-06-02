package com.tribe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Spring Boot 실행 API 계층 경계.
 *
 * HTTP transport와 application 계층 분리.
 */
@SpringBootApplication(scanBasePackages = ["com.tribe"])
class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
