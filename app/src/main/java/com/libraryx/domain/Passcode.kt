package com.libraryx.domain

import java.security.MessageDigest
import kotlin.random.Random

/**
 * Mirrors src/lib/passcode.ts. Uses SHA-256 (java.security.MessageDigest) instead of the
 * browser's `crypto.subtle.digest`, but with the identical salt prefix and hex encoding so
 * passcodes hashed by the original web/PWA build remain valid after migrating to this app
 * (e.g. when importing a Firestore-backed lab created by the TS version).
 */
object Passcode {
    private const val SALT_PREFIX = "studylab::"

    fun hashPasscode(passcode: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SALT_PREFIX + passcode).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPasscode(passcode: String, hash: String): Boolean = hashPasscode(passcode) == hash

    /** Mirrors `generatePasscode`: random 4-digit string. */
    fun generatePasscode(): String = Random.nextInt(1000, 10000).toString()
}
