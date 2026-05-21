package org.multipaz.mdoc.response

import org.multipaz.cbor.DataItem
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.putCborArray
import org.multipaz.mdoc.zkp.ZkDocument

/**
 * Structure containing decrypted documents from a [EncryptedDocuments] structure.
 *
 * @property documents a list of returned documents.
 * @property zkDocuments a list of returned documents with ZKP.
 * @property otherDocuments a list of returned documents in other formats, such as SD-JWT VC.
 */
@ConsistentCopyVisibility
data class EncryptedDocumentsPlaintext internal constructor(
    val documents: List<MdocDocument>,
    val zkDocuments: List<ZkDocument>,
    val otherDocuments: List<OtherDocument>,
) {

    internal fun toDataItem() = buildCborMap {
        if (documents.isNotEmpty()) {
            putCborArray("documents") {
                documents.forEach {
                    add(it.toDataItem())
                }
            }
        }
        if (zkDocuments.isNotEmpty()) {
            putCborArray("zkDocuments") {
                zkDocuments.forEach {
                    add(it.toDataItem())
                }
            }
        }
        if (otherDocuments.isNotEmpty()) {
            putCborArray("otherDocuments") {
                otherDocuments.forEach {
                    add(it.toDataItem())
                }
            }
        }
    }

    companion object {

        internal suspend fun fromDataItem(dataItem: DataItem): EncryptedDocumentsPlaintext {
            val documents = dataItem.getOrNull("documents")?.asArray?.map {
                MdocDocument.fromDataItem(it)
            }
            val zkDocuments = dataItem.getOrNull("zkDocuments")?.asArray?.map {
                ZkDocument.fromDataItem(it)
            }
            val otherDocuments = dataItem.getOrNull("otherDocuments")?.asArray?.map {
                OtherDocument.fromDataItem(it)
            }
            return EncryptedDocumentsPlaintext(
                documents = documents ?: emptyList(),
                zkDocuments = zkDocuments ?: emptyList(),
                otherDocuments = otherDocuments ?: emptyList(),
            )
        }
    }
}
