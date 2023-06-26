package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.subscriber.EthereumTransactionEventSubscriber
import com.rarible.protocol.nft.core.misc.methodSignatureId
import com.rarible.protocol.nft.core.model.SetBaseUriRecord
import com.rarible.protocol.nft.core.model.SubscriberGroups
import io.daonomic.rpc.domain.Binary
import org.springframework.stereotype.Component
import scala.jdk.javaapi.CollectionConverters

@Component
class SetBaseUriSubscriber : EthereumTransactionEventSubscriber {
    override suspend fun getEventRecords(block: EthereumBlockchainBlock): List<SetBaseUriRecord> {
        val transactions = CollectionConverters.asJava(block.ethBlock.transactions())
        return transactions.filter {
            it.input().methodSignatureId() == SET_BASE_URI_METHOD_ID_SIGNATURE
        }.map {
            SetBaseUriRecord(
                hash = it.hash().toString(),
                address = it.to(),
            )
        }
    }

    override fun getGroup(): String = SubscriberGroups.SET_BASE_URI

    companion object {
        internal val SET_BASE_URI_METHOD_ID_SIGNATURE: Binary = Binary.apply("0x55f804b3")
    }
}
