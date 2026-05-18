package org.multipaz.mdoc.connectionmethod

import org.multipaz.cbor.Cbor
import org.multipaz.cbor.addCborMap
import org.multipaz.cbor.buildCborArray
import org.multipaz.mdoc.role.MdocRole
import org.multipaz.nfc.NdefRecord
import org.multipaz.util.Logger

/**
 * Connection method for NFCv2.
 *
 * The [apduResponseMaxSize] parameter is used to tell the mdoc that it needs to send its responses in APDUs no
 * larger than this size and this value must be smaller or equal to 65536, the maximum size of an Extended APDU.
 * Since this class is usually not used in an environment where a 64 KiB buffer is a showstopper we default to the
 * maximum size.
 *
 * @property apduResponseMaxSize the maximum length of the response data field.
 */
data class MdocConnectionMethodNfcV2(
    val apduResponseMaxSize: Long = 65536L
): MdocConnectionMethod() {
    override fun toString(): String =
        "nfcv2:apdu_response_max_size=$apduResponseMaxSize"

    override fun toDeviceEngagement(): ByteArray {
        return Cbor.encode(
            buildCborArray {
                add(METHOD_TYPE)
                add(METHOD_MAX_VERSION)
                addCborMap {
                    put(OPTION_KEY_APDU_RESPONSE_MAX_LENGTH, apduResponseMaxSize)
                }
            }
        )
    }

    override fun toNdefRecord(
        auxiliaryReferences: List<String>,
        role: MdocRole,
        skipUuids: Boolean
    ): Pair<NdefRecord, NdefRecord>? {
        Logger.w(TAG, "MdocConnectionMethodNfcV2 should never appear in a NDEF record")
        return null
    }

    companion object {
        private const val TAG = "MdocConnectionMethodNfcV2"

        /**
         * The device retrieval method type for NFC according to ISO/IEC 18013-5 Second Edition
         */
        const val METHOD_TYPE = 5L

        /**
         * The supported version of the device retrieval method type for NFC.
         */
        const val METHOD_MAX_VERSION = 1L

        private const val OPTION_KEY_APDU_RESPONSE_MAX_LENGTH = 0L

        internal fun fromDeviceEngagement(encodedDeviceRetrievalMethod: ByteArray): MdocConnectionMethodNfcV2? {
            val array = Cbor.decode(encodedDeviceRetrievalMethod)
            val type = array[0].asNumber
            val version = array[1].asNumber
            require(type == METHOD_TYPE)
            if (version > METHOD_MAX_VERSION) {
                return null
            }
            val map = array[2]
            return MdocConnectionMethodNfcV2(
                map[OPTION_KEY_APDU_RESPONSE_MAX_LENGTH].asNumber
            )
        }
    }
}