package com.tribe

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Spring Boot 실행 batch 실행 경계.
 *
 * 스케줄러와 application use case 연결.
 */
@SpringBootApplication
@EnableScheduling
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}