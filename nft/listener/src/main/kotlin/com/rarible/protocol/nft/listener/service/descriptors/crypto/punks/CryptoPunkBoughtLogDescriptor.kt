package com.rarible.protocol.nft.listener.service.descriptors.crypto.punks

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.test.crypto.punks.PunkBoughtEvent
import com.rarible.protocol.contracts.test.crypto.punks.TransferEvent
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.service.token.TokenService
import io.daonomic.rpc.domain.Word
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.domain.request.LogFilter
import scalether.domain.request.TopicFilter
import scalether.domain.response.Log
import java.math.BigInteger
import java.time.Instant

@Service
class CryptoPunkBoughtLogDescriptor(
    tokenService: TokenService,
    nftIndexerProperties: NftIndexerProperties,
    private val ethereum: MonoEthereum
) : CryptoPunkLogDescriptorBase(tokenService, nftIndexerProperties) {

    private val logger = LoggerFactory.getLogger(CryptoPunkBoughtLogDescriptor::class.java)

    override val topic: Word = PunkBoughtEvent.id()

    override fun convertItemTransfer(log: Log, date: Instant): Mono<ItemTransfer> {
        val event = PunkBoughtEvent.apply(log)
        val addresses = if (event.toAddress() != Address.ZERO() && event.toAddress() != event.fromAddress()) {
            (event.fromAddress() to event.toAddress()).toMono()
        } else {
            /*
                Workaround https://github.com/larvalabs/cryptopunks/issues/19.
                We have to find "Transfer" event going before "PunkBought"
                from the same function in order to extract correct value for "toAddress".
             */
            logger.info("trying workaround for PunkBought bug for $log")
            val filter = LogFilter
                .apply(TopicFilter.simple(TransferEvent.id()))
                .address(cryptoPunksContractAddress)
                .blockHash(log.blockHash())
            ethereum.ethGetLogsJava(filter)
                .doOnError { logger.warn("Unable to get logs for block ${log.blockHash()}", it) }
                .map { logs ->
                    logs.find {
                        it.topics().head() == TransferEvent.id() &&
                                it.transactionHash() == log.transactionHash() &&
                                (it.logIndex() == log.logIndex() - BigInteger.ONE || it.logIndex() == log.logIndex() - BigInteger("2"))
                    }
                        ?.let { TransferEvent.apply(it) }
                        ?.let { it.from() to it.to() }
                        ?: (event.fromAddress() to event.toAddress())
                }
        }
        return addresses.map { (from, to) ->
            ItemTransfer(
                from = from,
                owner = to,
                token = log.address(),
                tokenId = EthUInt256.of(event.punkIndex()),
                date = date,
                value = EthUInt256.of(1)
            )
        }
    }
}
