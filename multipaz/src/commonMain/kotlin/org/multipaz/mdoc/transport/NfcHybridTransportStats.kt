package org.multipaz.mdoc.transport

import org.multipaz.cbor.annotation.CborSerializable

/**
 * Count of messages sent and received on [NfcHybridTransportMdoc] and [NfcHybridTransportMdocReader] transports.
 *
 * @property numSent number of messages sent.
 * @property numSentViaNfc number of messages sent via NFC channel.
 * @property numSentViaTransport number of messages sent via Transport channel.
 * @property numReceived number of messages received.
 * @property numReceivedFirstOnNfc number of messages received on NFC before receiving on Transport.
 * @property numReceivedFirstOnTransport number of messages received on Transport before receiving on NFC.
 * @property nfcDisconnectedDuringTransaction true if NFC disconnected during the transaction.
 */
@CborSerializable
data class NfcHybridTransportStats(
    val numSent: Int,
    val numSentViaNfc: Int,
    val numSentViaTransport: Int,
    val numReceived: Int,
    val numReceivedFirstOnNfc: Int,
    val numReceivedFirstOnTransport: Int,
    val nfcDisconnectedDuringTransaction: Boolean
) {
    companion object
}