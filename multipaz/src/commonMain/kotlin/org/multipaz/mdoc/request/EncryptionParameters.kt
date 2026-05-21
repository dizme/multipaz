package org.multipaz.mdoc.request

import kotlinx.io.bytestring.ByteString
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.putCborArray
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.X509Cert

/**
 * Parameters to use when requesting to encrypt a document response.
 *
 * @param dataItem a [DataItem] which is a map with keys `recipientPublicKey`, `recipientCertificate`, and `nonce`
 * as defined in ISO/IEC 18013-5 Second Edition.
 */
data class EncryptionParameters(
    val dataItem: DataItem
) {

    /**
     * @param recipientPublicKey the public key to encrypt the response against.
     */
    val recipientPublicKey: EcPublicKey
        get() = dataItem["recipientPublicKey"].asCoseKey.ecPublicKey

    /**
     * @param recipientCertificates zero or more certificates for [recipientPublicKey].
     */
    val recipientCertificates: List<X509Cert>
        get() = dataItem.getOrNull("recipientCertificate")?.asArray?.map {
            X509Cert(ByteString(it.asBstr))
        } ?: emptyList()

    /**
     * Optional nonce to use.
     */
    val nonce: ByteString?
        get() = dataItem.getOrNull("nonce")?.asBstr?.let { ByteString(it) }

    companion object {
        /**
         * Creates a new [EncryptionParameters] from values.
         *
         * @param recipientPublicKey the public key to encrypt the response against.
         * @param recipientCertificates zero or more certificates for [recipientPublicKey].
         * @param nonce optional nonce to use.
         * @return a [EncryptionParameters] instance.
         */
        fun fromValues(
            recipientPublicKey: EcPublicKey,
            recipientCertificates: List<X509Cert> = emptyList(),
            nonce: ByteString? = null
        ) = EncryptionParameters(buildCborMap {
            put("recipientPublicKey", recipientPublicKey.toCoseKey().toDataItem())
            if (recipientCertificates.isNotEmpty()) {
                putCborArray("recipientCertificate") {
                    for (cert in recipientCertificates) {
                        add(cert.toDataItem())
                    }
                }
            }
            nonce?.let {
                put("nonce", nonce.toByteArray())
            }
        })
    }
}