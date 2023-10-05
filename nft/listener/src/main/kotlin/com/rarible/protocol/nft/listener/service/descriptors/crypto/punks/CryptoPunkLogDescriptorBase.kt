package com.rarible.protocol.nft.listener.service.descriptors.crypto.punks

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.ItemTransfer
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenService
import com.rarible.protocol.nft.listener.service.descriptors.ItemHistoryLogEventDescriptor
import kotlinx.coroutines.reactor.mono
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

abstract class CryptoPunkLogDescriptorBase(
    private val tokenService: TokenService,
    private val nftIndexerProperties: NftIndexerProperties
) : ItemHistoryLogEventDescriptor<ItemTransfer> {

    abstract fun convertItemTransfer(log: Log, date: Instant): Mono<ItemTransfer>

    override fun convert(log: Log, transaction: Transaction, date: Instant): Mono<ItemTransfer> =
        mono { tokenService.getTokenStandard(log.address()) }
            .flatMap { standard ->
                if (standard == TokenStandard.CRYPTO_PUNKS) {
                    convertItemTransfer(log, date)
                } else {
                    Mono.empty()
                }
            }

    protected val cryptoPunksContractAddress: Address get() = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)

    override fun getAddresses(): Mono<Collection<Address>> =
        Mono.just(listOf(cryptoPunksContractAddress))
}
