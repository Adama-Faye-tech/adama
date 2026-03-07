package com.android.system.update.core

import android.util.Base64

object SecurityUtils {
    private const val K = "v3ry_s3cur3_k3y_123" // Encryption key

    /**
     * Simple XOR decryption. While simple, it breaks heuristic string scanning.
     */
    fun d(input: String): String {
        val decoded = Base64.decode(input, Base64.DEFAULT)
        val result = ByteArray(decoded.size)
        val keyBytes = K.toByteArray()
        for (i in decoded.indices) {
            result[i] = (decoded[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return String(result)
    }

    /**
     * Used once to generate the encrypted strings for the code.
     */
    fun e(input: String): String {
        val bytes = input.toByteArray()
        val result = ByteArray(bytes.size)
        val keyBytes = K.toByteArray()
        for (i in bytes.indices) {
            result[i] = (bytes[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
        return Base64.encodeToString(result, Base64.DEFAULT)
    }
}
