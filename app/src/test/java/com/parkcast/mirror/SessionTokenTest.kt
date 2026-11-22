package com.parkcast.mirror

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SessionTokenTest {
    @Test fun tokenIsRandomUrlSafeAndLongEnough() {
        val one = SessionToken.generate()
        val two = SessionToken.generate()
        assertNotEquals(one, two)
        assertTrue(one.matches(Regex("[A-Za-z0-9_-]{32}")))
    }

    @Test fun wrongTokenDoesNotPass() {
        val token = SessionToken.generate()
        assertTrue(SessionToken.matches(token, token))
        assertFalse(SessionToken.matches(token, ""))
        assertFalse(SessionToken.matches(token, "incorrect"))
    }
}
