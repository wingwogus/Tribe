package com.tribe.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, unique = true)
    val email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(nullable = false, unique = true)
    var nickname: String = email.substringBefore("@"),

    @Column
    var avatar: String? = null,

    @Column(nullable = false)
    var provider: String = PROVIDER_LOCAL,

    @Column
    var providerId: String? = null,

    @Column(nullable = false)
    var isFirstLogin: Boolean = false,

    @Column(nullable = false)
    var role: String = ROLE_USER,
) {
    fun linkOAuthAccount(
        provider: String,
        providerId: String,
        avatar: String?
    ) {
        this.provider = provider
        this.providerId = providerId
        if (!avatar.isNullOrBlank()) {
            this.avatar = avatar
        }
    }

    companion object {
        const val ROLE_USER = "ROLE_USER"
        const val PROVIDER_LOCAL = "LOCAL"
    }
}
