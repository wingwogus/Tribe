package com.tribe.application.exception

/**
 * application 예외 최상위 경계.
 *
 * use case 오류를 HTTP transport와 분리하는 기준.
 */
abstract class ApplicationException(
    override val message: String
) : RuntimeException(message)
