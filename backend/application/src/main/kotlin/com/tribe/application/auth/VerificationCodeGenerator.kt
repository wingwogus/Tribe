package com.tribe.application.auth

interface VerificationCodeGenerator {
    fun generate(): String
}
