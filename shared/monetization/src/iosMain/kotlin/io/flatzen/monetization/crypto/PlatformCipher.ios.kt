package io.flatzen.monetization.crypto

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.posix.arc4random_buf
import platform.posix.memcpy
import kotlin.experimental.xor

/**
 * iOS secure preference cipher: random key persisted in app sandbox (NSUserDefaults),
 * values XOR'd. Android uses Keystore AES-GCM. Sufficient for subscription flags;
 * escalate to Keychain/CryptoKit Swift bridge if threat model requires it.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun createPlatformCipher(alias: String): PlatformCipher = IosPlatformCipher(alias)

@OptIn(ExperimentalForeignApi::class)
private class IosPlatformCipher(private val alias: String) : PlatformCipher {

    private val key: ByteArray by lazy { loadOrCreateKey() }

    private fun loadOrCreateKey(): ByteArray {
        val defaults = NSUserDefaults.standardUserDefaults
        val storageKey = "flatzen.cipher.$alias"
        val existing = defaults.dataForKey(storageKey)
        if (existing != null) {
            return existing.toByteArray()
        }
        val generated = ByteArray(32)
        generated.usePinned { pinned ->
            arc4random_buf(pinned.addressOf(0), generated.size.toULong())
        }
        defaults.setObject(generated.toNSData(), storageKey)
        defaults.synchronize()
        return generated
    }

    override fun encrypt(plain: ByteArray): ByteArray = transform(plain)

    override fun decrypt(cipher: ByteArray): ByteArray = transform(cipher)

    private fun transform(input: ByteArray): ByteArray {
        val out = ByteArray(input.size)
        for (i in input.indices) {
            out[i] = input[i] xor key[i % key.size]
        }
        return out
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val out = ByteArray(size)
    out.usePinned { pinned ->
        val src = bytes
        if (src != null) {
            memcpy(pinned.addressOf(0), src, size.toULong())
        }
    }
    return out
}
