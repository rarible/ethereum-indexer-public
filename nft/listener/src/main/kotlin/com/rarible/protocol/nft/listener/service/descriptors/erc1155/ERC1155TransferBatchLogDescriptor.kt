package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc1155.TransferBatchEventWithFullData
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import com.rarible.protocol.nft.listener.service.resolver.IgnoredTokenResolver
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
class ERC1155TransferBatchLogDescriptor(
    private val tokenService: TokenService,
    ignoredTokenResolver: IgnoredTokenResolver,
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    private val skipContracts = ignoredTokenResolver.resolve()
    private val logger = LoggerFactory.getLogger(ERC1155TransferBatchLogDescriptor::class.java)

    init {
        logger.info("Creating ERC1155TransferBatchLogDescriptor, found ${skipContracts.size} skip tokens")
    }

    override fun convert(log: Log, transaction: Transaction, date: Instant): Publisher<ItemTransfer> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }
        return mono { tokenService.getTokenStandard(log.address()) }
            .flatMapMany { standard ->
                if (standard == TokenStandard.ERC1155) {
                    val e = when (log.topics().size()) {
                        1 -> TransferBatchEventWithFullData.apply(log)
                        else -> TransferBatchEvent.apply(log)
                    }
                    val ids = e._ids()
                    val values = if (e._values().size > ids.size) {
                        e._values().dropLastWhile { it == BigInteger.ZERO }
                    } else {
                        e._values().toList()
                    }
                    if (ids.size != values.size) {
                        Mono.error(IllegalStateException(
                            buildString {
                                appendLine("Invalid TransferBatchEvent for transaction ${log.transactionHash()} logIndex ${log.logIndex()}")
                                appendLine("ids (${e._ids().size}): ${e._ids().toList()}")
                                appendLine("values (${e._values().size}): ${e._values().toList()}")
                            }
                        ))
                    } else if (e._from() == Address.ZERO() && e._to() == Address.ZERO()) {
                        Mono.empty()
                    } else {
                        ids.zip(values)
                            .map {
                                ItemTransfer(
                                    from = e._from(),
                                    owner = e._to(),
                                    token = log.address(),
                                    tokenId = EthUInt256.of(it.first),
                                    date = date,
                                    value = EthUInt256.of(it.second)
                                )
                            }
                            .toFlux()
                    }
                } else {
                    Mono.empty()
                }
            }
    }

    override val topic: Word = TransferBatchEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
