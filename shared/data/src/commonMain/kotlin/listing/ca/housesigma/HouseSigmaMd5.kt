package listing.ca.housesigma

import org.kotlincrypto.hash.md.MD5

/** MD5 hex for HouseSigma request signing. */
internal object HouseSigmaMd5 {
    fun hex(input: String): String {
        val digest = MD5().digest(input.encodeToByteArray())
        return digest.joinToString("") { b ->
            ((b.toInt() and 0xff) + 0x100).toString(16).substring(1)
        }
    }
}
