package listing.ca.housesigma

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.SecureRandom
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal actual object HouseSigmaCrypto {
    actual val rsaPublicKeyPem: String =
        "-----BEGIN PUBLIC KEY-----\n" +
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDQlOcjEbqprurl2xjoEP0QdjGI\n" +
                "rZhLVn5vzwCorG4+2AtSi4AAHjghSXM//ljqE5rA13gfTc58JvM6I75Dmqr5r5Vv\n" +
                "o57CAbxBXHsXu5ojtgvb5rOd2lrZeckwJL0Z7euvRsA/FjbFdGMcGeSJ8JoePq+H\n" +
                "0RFOt285bSb8hVq0LQIDAQAB\n" +
                "-----END PUBLIC KEY-----"

    actual fun encryptPayload(payloadJson: String, secretKey: String): EncryptedPayload {
        val ts = (System.currentTimeMillis() / 1000L).toString()
        val withTs = insertTimestamp(payloadJson, ts)
        val aesKeyStr = secretKey.padEnd(16, '*').take(16)
        val aesKey = aesKeyStr.toByteArray(Charsets.UTF_8)
        val counter = ByteArray(16).also { SecureRandom().nextBytes(it) }

        val aesCipher = Cipher.getInstance("AES/CTR/NoPadding")
        aesCipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(counter))
        val encrypted = aesCipher.doFinal(withTs.toByteArray(Charsets.UTF_8))

        val rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        rsaCipher.init(Cipher.ENCRYPT_MODE, loadPublicKey(rsaPublicKeyPem))
        val ctrEncrypted = rsaCipher.doFinal(counter)

        val body = buildJsonObject {
            put("ctr", Base64.getEncoder().encodeToString(ctrEncrypted))
            put("et_payload", Base64.getEncoder().encodeToString(encrypted))
        }
        return EncryptedPayload(body, ts, aesKey, counter)
    }

    actual fun decryptResponse(b64Data: String, aesKey: ByteArray, counter: ByteArray): String {
        val encrypted = Base64.getDecoder().decode(b64Data)
        val aesCipher = Cipher.getInstance("AES/CTR/NoPadding")
        aesCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(counter))
        val decrypted = aesCipher.doFinal(encrypted)
        return GZIPInputStream(ByteArrayInputStream(decrypted)).use { it.readBytes() }
            .decodeToString()
    }

    private fun loadPublicKey(pem: String) = KeyFactory.getInstance("RSA").generatePublic(
        X509EncodedKeySpec(
            Base64.getDecoder().decode(
                pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("\\s".toRegex(), ""),
            ),
        ),
    )

    private fun insertTimestamp(payloadJson: String, ts: String): String {
        val trimmed = payloadJson.trimEnd()
        return if (trimmed.endsWith("}")) {
            trimmed.dropLast(1) + ""","hs_request_timestamp":"$ts"}"""
        } else {
            payloadJson
        }
    }
}
