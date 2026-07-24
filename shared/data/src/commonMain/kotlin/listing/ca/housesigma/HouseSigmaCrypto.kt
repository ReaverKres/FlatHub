package listing.ca.housesigma

import kotlinx.serialization.json.JsonObject

/**
 * RSA-OAEP-SHA1 + AES-128-CTR payload encryption for HouseSigma `/list` and `/detail_v2`.
 * See tmp/ca/probe_encrypt.mjs and tmp/ca/api/housesigma/NOTES.md.
 */
internal expect object HouseSigmaCrypto {
    /** Public key from site bundle — rotate when encrypted calls start failing. */
    val rsaPublicKeyPem: String

    fun encryptPayload(payloadJson: String, secretKey: String): EncryptedPayload

    fun decryptResponse(b64Data: String, aesKey: ByteArray, counter: ByteArray): String
}

internal data class EncryptedPayload(
    val body: JsonObject,
    val requestTimestamp: String,
    val aesKey: ByteArray,
    val counter: ByteArray,
)
