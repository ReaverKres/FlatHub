package listing.ca.housesigma

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import platform.CoreFoundation.CFDictionaryCreate
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.NSData
import platform.Foundation.NSDataBase64DecodingIgnoreUnknownCharacters
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create
import platform.Security.SecKeyCreateEncryptedData
import platform.Security.SecKeyCreateWithData
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmRSAEncryptionOAEPSHA1
import platform.Security.kSecRandomDefault
import platform.posix.memcpy
import platform.zlib.Z_OK
import platform.zlib.Z_STREAM_END
import platform.zlib.inflate
import platform.zlib.inflateEnd
import platform.zlib.inflateInit2_
import platform.zlib.z_stream

@OptIn(ExperimentalForeignApi::class)
internal actual object HouseSigmaCrypto {
    actual val rsaPublicKeyPem: String =
        "-----BEGIN PUBLIC KEY-----\n" +
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDQlOcjEbqprurl2xjoEP0QdjGI\n" +
                "rZhLVn5vzwCorG4+2AtSi4AAHjghSXM//ljqE5rA13gfTc58JvM6I75Dmqr5r5Vv\n" +
                "o57CAbxBXHsXu5ojtgvb5rOd2lrZeckwJL0Z7euvRsA/FjbFdGMcGeSJ8JoePq+H\n" +
                "0RFOt285bSb8hVq0LQIDAQAB\n" +
                "-----END PUBLIC KEY-----"

    actual fun encryptPayload(payloadJson: String, secretKey: String): EncryptedPayload {
        val ts = (platform.Foundation.NSDate().timeIntervalSince1970.toLong()).toString()
        val withTs = insertTimestamp(payloadJson, ts)
        val aesKeyStr = secretKey.padEnd(16, '*').take(16)
        val aesKey = aesKeyStr.encodeToByteArray()
        val counter = randomBytes(16)

        val encrypted = aesCtr(withTs.encodeToByteArray(), aesKey, counter)
        val ctrEncrypted = rsaEncrypt(counter)

        val body = buildJsonObject {
            put("ctr", ctrEncrypted.toBase64())
            put("et_payload", encrypted.toBase64())
        }
        return EncryptedPayload(body, ts, aesKey, counter)
    }

    actual fun decryptResponse(b64Data: String, aesKey: ByteArray, counter: ByteArray): String {
        val encrypted = b64Data.fromBase64()
        val decrypted = aesCtr(encrypted, aesKey, counter)
        return gunzip(decrypted).decodeToString()
    }

    private fun insertTimestamp(payloadJson: String, ts: String): String {
        val trimmed = payloadJson.trimEnd()
        return if (trimmed.endsWith("}")) {
            trimmed.dropLast(1) + ""","hs_request_timestamp":"$ts"}"""
        } else {
            payloadJson
        }
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { bytes ->
        bytes.usePinned { pinned ->
            SecRandomCopyBytes(kSecRandomDefault, size.convert(), pinned.addressOf(0))
        }
    }

    private fun rsaEncrypt(data: ByteArray): ByteArray {
        val der = pemToDer(rsaPublicKeyPem)
        val keyData = der.toNSData()
        val attrs = mapOf<platform.CoreFoundation.CFTypeRef?, Any?>(
            kSecAttrKeyType to kSecAttrKeyTypeRSA,
            kSecAttrKeyClass to kSecAttrKeyClassPublic,
        )
        memScoped {
            val keys = allocArrayOf(*attrs.keys.toTypedArray())
            val values = allocArrayOf(*attrs.values.toTypedArray())
            val dict = CFDictionaryCreate(
                null,
                keys,
                values,
                attrs.size.convert(),
                kCFTypeDictionaryKeyCallBacks.ptr,
                kCFTypeDictionaryValueCallBacks.ptr,
            )
            val key = SecKeyCreateWithData(keyData, dict, null)
                ?: error("HouseSigma: failed to load RSA public key")
            val encrypted = SecKeyCreateEncryptedData(
                key,
                kSecKeyAlgorithmRSAEncryptionOAEPSHA1,
                data.toNSData(),
                null,
            ) ?: error("HouseSigma: RSA encrypt failed")
            return encrypted.toByteArray()
        }
    }

    private fun pemToDer(pem: String): ByteArray {
        val b64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")
        return b64.fromBase64()
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun aesCtr(input: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
    val result = ByteArray(input.size)
    val blockSize = 16
    var counter = iv.copyOf()
    var offset = 0
    while (offset < input.size) {
        val keystream = aesEcbBlock(key, counter)
        val chunk = minOf(blockSize, input.size - offset)
        for (i in 0 until chunk) {
            result[offset + i] = (input[offset + i].toInt() xor keystream[i].toInt()).toByte()
        }
        incrementCounter(counter)
        offset += chunk
    }
    return result
}

@OptIn(ExperimentalForeignApi::class)
private fun aesEcbBlock(key: ByteArray, block: ByteArray): ByteArray {
    val out = ByteArray(16)
    key.usePinned { keyPinned ->
        block.usePinned { blockPinned ->
            out.usePinned { outPinned ->
                platform.CommonCrypto.CCCrypt(
                    platform.CommonCrypto.kCCEncrypt,
                    platform.CommonCrypto.kCCAlgorithmAES,
                    platform.CommonCrypto.kCCOptionECBMode,
                    keyPinned.addressOf(0),
                    key.size.convert(),
                    null,
                    blockPinned.addressOf(0),
                    block.size.convert(),
                    outPinned.addressOf(0),
                    out.size.convert(),
                    null,
                )
            }
        }
    }
    return out
}

private fun incrementCounter(counter: ByteArray) {
    for (i in counter.indices.reversed()) {
        val next = (counter[i].toInt() and 0xFF) + 1
        counter[i] = next.and(0xFF).toByte()
        if (next <= 0xFF) break
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun gunzip(input: ByteArray): ByteArray = memScoped {
    val stream = alloc<z_stream>()
    check(inflateInit2_(stream.ptr, 15 + 32, "1.2.11", 112.convert()) == Z_OK)
    val out = ByteArray(input.size * 8)
    input.usePinned { inPinned ->
        out.usePinned { outPinned ->
            stream.next_in = inPinned.addressOf(0).reinterpret()
            stream.avail_in = input.size.convert()
            stream.next_out = outPinned.addressOf(0).reinterpret()
            stream.avail_out = out.size.convert()
            check(inflate(stream.ptr, 4) == Z_STREAM_END)
            inflateEnd(stream.ptr)
            out.copyOf(out.size - stream.avail_out.toInt())
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.convert())
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    if (len == 0) return ByteArray(0)
    return ByteArray(len).also { dest ->
        dest.usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, len.convert())
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toBase64(): String =
    NSData.create(this).base64EncodedStringWithOptions(0u)

@OptIn(ExperimentalForeignApi::class)
private fun String.fromBase64(): ByteArray =
    NSData.create(base64EncodedString = this, options = NSDataBase64DecodingIgnoreUnknownCharacters)
        ?.toByteArray()
        ?: ByteArray(0)

@OptIn(ExperimentalForeignApi::class)
private fun allocArrayOf(vararg refs: platform.CoreFoundation.CFTypeRef?): platform.CoreFoundation.CFTypeRef?Array =
platform.posix.calloc(refs.size.toULong(), platform.posix.sizeof<platform.CoreFoundation.CFTypeRefVar>().toULong())
?.reinterpret()
?.also {
    array ->
    refs.forEachIndexed { index, ref ->
        array[index] = ref
    }
}
