package com.rarible.protocol.nft.listener.service.descriptors.royalty

import com.rarible.ethereum.common.toAddress
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.royalties.event.RoyaltiesSetForContractEvent
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Part
import com.rarible.protocol.nft.core.model.RoyaltiesEvent
import com.rarible.protocol.nft.core.model.SetRoyaltiesForContract
import com.rarible.protocol.nft.core.repository.history.RoyaltiesHistoryRepository
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction

@Service
class RoyaltiesSetForContractLogDescriptor(
    private val properties: NftIndexerProperties
) : LogEventDescriptor<RoyaltiesEvent> {

    override val collection: String = RoyaltiesHistoryRepository.COLLECTION

    override val topic: Word = RoyaltiesSetForContractEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Mono<RoyaltiesEvent> {
        val event = RoyaltiesSetForContractEvent.apply(log)
        return SetRoyaltiesForContract(
            token = event.token(),
            parts = event.royalties().map { Part(it._1, it._2.intValueExact()) },
        ).toMono()
    }

    override fun getAddresses(): Mono<Collection<Address>> {
        return listOf(properties.royaltyRegistryAddress.toAddress()).toMono()
    }
}
