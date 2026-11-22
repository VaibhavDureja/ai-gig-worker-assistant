package com.parkcast.mirror

import java.security.SecureRandom
import java.util.Base64

/** Random, unguessable token required to view a single local casting session. */
object SessionToken {
    fun generate(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun matches(expected: String, submitted: String): Boolean {
        // Avoid a token-dependent early exit when the two strings are equal length.
        val a = expected.toByteArray(Charsets.UTF_8)
        val b = submitted.toByteArray(Charsets.UTF_8)
        return java.security.MessageDigest.isEqual(a, b)
    }
}
