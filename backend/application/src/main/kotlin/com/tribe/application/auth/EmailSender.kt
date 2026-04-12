package com.tribe.application.auth

interface EmailSender {
    fun sendVerificationCode(email: String, code: String)
}
