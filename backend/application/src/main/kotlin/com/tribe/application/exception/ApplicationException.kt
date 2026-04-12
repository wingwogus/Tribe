package com.tribe.application.exception

abstract class ApplicationException(
    override val message: String
) : RuntimeException(message)
