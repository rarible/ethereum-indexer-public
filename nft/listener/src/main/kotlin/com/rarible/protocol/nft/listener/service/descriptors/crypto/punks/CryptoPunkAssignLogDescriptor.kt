package com.rarible.protocol.nft.listener.service.descriptors.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.test.crypto.punks.AssignEvent
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.service.token.TokenService
import io.daonomic.rpc.domain.Word
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address
import scalether.domain.response.Log
import java.time.Instant

@Service
class CryptoPunkAssignLogDescriptor(
    tokenService: TokenService,
    nftIndexerProperties: NftIndexerProperties
) : CryptoPunkLogDescriptorBase(tokenService, nftIndexerProperties) {

    override val topic: Word = AssignEvent.id()

    override fun convertItemTransfer(log: Log, date: Instant): Mono<ItemTransfer> {
        val event = AssignEvent.apply(log)
        return ItemTransfer(
            from = Address.ZERO(),
            owner = event.to(),
            token = log.address(),
            tokenId = EthUInt256.of(event.punkIndex()),
            date = date,
            value = EthUInt256.of(1)
        ).toMono()
    }
}