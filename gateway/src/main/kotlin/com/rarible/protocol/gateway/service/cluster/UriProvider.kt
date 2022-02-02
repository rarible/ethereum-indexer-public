package com.rarible.protocol.gateway.service.cluster

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.erc20.api.client.Erc20IndexerApiServiceUriProvider
import com.rarible.protocol.nft.api.client.NftIndexerApiServiceUriProvider
import com.rarible.protocol.order.api.client.OrderIndexerApiServiceUriProvider
import com.rarible.protocol.unlockable.api.client.UnlockableApiServiceUriProvider
import org.springframework.stereotype.Component
import java.net.URI

@Component
class UriProvider(
    private val nftIndexerApiServiceUriProvider: NftIndexerApiServiceUriProvider,
    private val erc20IndexerApiServiceUriProvider: Erc20IndexerApiServiceUriProvider,
    private val orderIndexerApiServiceUriProvider: OrderIndexerApiServiceUriProvider,
    private val unlockableApiServiceUriProvider: UnlockableApiServiceUriProvider
) {
    fun getNftIndexerApiUri(blockchain: Blockchain): URI {
        return nftIndexerApiServiceUriProvider.getUri(blockchain.value)
    }

    fun getErc20IndexerApiUri(blockchain: Blockchain): URI {
        return erc20IndexerApiServiceUriProvider.getUri(blockchain.value)
    }

    fun getOrderIndexerApiUri(blockchain: Blockchain): URI {
        return orderIndexerApiServiceUriProvider.getUri(blockchain.value)
    }

    fun getUnlockableApiUri(blockchain: Blockchain): URI {
        return unlockableApiServiceUriProvider.getUri(blockchain.value)
    }
}

