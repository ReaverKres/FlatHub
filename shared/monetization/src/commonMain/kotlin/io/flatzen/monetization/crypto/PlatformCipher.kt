package io.flatzen.monetization.crypto

/**
 * Platform AES-GCM cipher. Key material lives in Android Keystore / iOS Keychain.
 */
interface PlatformCipher {
    fun encrypt(plain: ByteArray): ByteArray
    fun decrypt(cipher: ByteArray): ByteArray
}

expect fun createPlatformCipher(alias: String = "flatzen_secure_store"): PlatformCipher
