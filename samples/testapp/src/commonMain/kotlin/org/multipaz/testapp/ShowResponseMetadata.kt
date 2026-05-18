package org.multipaz.testapp

import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.mdoc.transport.NfcHybridTransportStats

@CborSerializable
data class ShowResponseMetadata(
    val engagementType: String,
    val transferProtocol: String,
    val requestSize: Long,
    val responseSize: Long,
    val durationMsecNfcTapToEngagement: Long?,
    val durationMsecEngagementReceivedToRequestSent: Long?,
    val durationMsecRequestSentToResponseReceived: Long,
    val nfcHybridTransportStats: NfcHybridTransportStats?
) {
    companion object
}