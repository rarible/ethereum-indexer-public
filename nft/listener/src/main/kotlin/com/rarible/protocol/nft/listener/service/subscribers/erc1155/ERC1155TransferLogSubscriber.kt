package com.rarible.protocol.nft.listener.service.subscribers.erc1155

import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainBlock
import com.rarible.blockchain.scanner.ethereum.client.EthereumBlockchainLog
import com.rarible.blockchain.scanner.ethereum.model.EthereumLogRecord
import com.rarible.blockchain.scanner.framework.data.FullBlock
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.SubscriberGroups
import com.rarible.protocol.nft.listener.service.descriptors.erc1155.ERC1155TransferLogDescriptor
import com.rarible.protocol.nft.listener.service.descriptors.mints.TransferLogsPostProcessor
import com.rarible.protocol.nft.listener.service.subscribers.AbstractItemLogEventSubscriber
import org.springframework.stereotype.Component

@Component
class ERC1155TransferLogSubscriber(
    private val transferLogsPostProcessor: TransferLogsPostProcessor,
    descriptor: ERC1155TransferLogDescriptor
) : AbstractItemLogEventSubscriber<ItemTransfer>(SubscriberGroups.ITEM_HISTORY, descriptor) {

    override suspend fun postProcess(
        block: FullBlock<EthereumBlockchainBlock, EthereumBlockchainLog>,
        logs: List<EthereumLogRecord>
    ) = transferLogsPostProcessor.process(block, logs)
}
