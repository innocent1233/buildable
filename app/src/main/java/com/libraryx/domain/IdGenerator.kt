package com.libraryx.domain

import kotlin.random.Random

/**
 * Mirrors `generateId`, `hashPin`, and `generatePin` from src/lib/store.ts.
 */
object IdGenerator {

    /** Mirrors `generateId`: timestamp base36 + random base36 suffix. */
    fun generateId(): String {
        val time = System.currentTimeMillis().toString(36)
        val rand = Random.nextLong(0, 36L * 36 * 36 * 36 * 36 * 36).toString(36)
        return time + rand.padStart(6, '0').take(6)
    }

    /**
     * Mirrors `hashPin`: a tiny non-cryptographic 32-bit rolling hash, kept bit-for-bit
     * compatible with the original `((hash << 5) - hash) + char; hash |= 0` JS implementation
     * so PINs hashed by the original web app remain valid if a backup is imported.
     */
    fun hashPin(pin: String): String {
        var hash = 0
        for (ch in pin) {
            val code = ch.code
            hash = (hash shl 5) - hash + code
            // `hash |= 0` in JS forces a 32-bit signed integer; Kotlin Int already is 32-bit,
            // so plain overflow wraparound during the arithmetic above reproduces it exactly.
        }
        return kotlin.math.abs(hash).toString(36)
    }

    /** Mirrors `generatePin`: random 4-digit string. */
    fun generatePin(): String = Random.nextInt(1000, 10000).toString()
}
