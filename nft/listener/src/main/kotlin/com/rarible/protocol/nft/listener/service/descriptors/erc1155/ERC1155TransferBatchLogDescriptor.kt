package com.rarible.protocol.nft.listener.service.descriptors.erc1155

import com.rarible.contracts.erc1155.TransferBatchEvent
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.contracts.erc1155.TransferBatchEventWithFullData
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.listener.configuration.NftListenerProperties
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import io.daonomic.rpc.domain.Word
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import java.math.BigInteger
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class ERC1155TransferBatchLogDescriptor(
    private val tokenRegistrationService: TokenRegistrationService,
    properties: NftListenerProperties,
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    private val skipContracts = properties.skipTransferContracts.map { Address.apply(it) }
    private val logger = LoggerFactory.getLogger(ERC1155TransferBatchLogDescriptor::class.java)

    init {
        logger.info("Creating ERC1155TransferBatchLogDescriptor with config: $properties")
    }

    override fun convert(log: Log, date: Instant): Publisher<ItemTransfer> {
        if (log.address() in skipContracts) {
            return Mono.empty()
        }

        return tokenRegistrationService.getTokenStandard(log.address())
            .flatMapMany { standard ->
                if (standard == TokenStandard.ERC1155) {
                    val e = when (log.topics().size()) {
                        1 -> TransferBatchEventWithFullData.apply(log)
                        else -> TransferBatchEvent.apply(log)
                    }
                    if (e._ids().isEmpty() && e._values().all { value -> value == BigInteger.ZERO }) {
                        Mono.empty()
                    } else if (e._ids().size != e._values().size) {
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
                        e._ids().zip(e._values())
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
                    Mono.empty<ItemTransfer>()
                }
            }
    }

    override val topic: Word = TransferBatchEvent.id()

    override fun getAddresses(): Mono<Collection<Address>> {
        return Mono.just(emptyList())
    }
}
