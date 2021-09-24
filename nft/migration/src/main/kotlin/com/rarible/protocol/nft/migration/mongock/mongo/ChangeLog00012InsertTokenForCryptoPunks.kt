package com.rarible.protocol.nft.migration.mongock.mongo

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

@ChangeLog(order = "00012")
class ChangeLog00012InsertTokenForCryptoPunks {

    @ChangeSet(id = "ChangeLog00012UpdateTokeFeatures.insertTokenForCryptoPunks", order = "1", author = "protocol")
    fun insertTokenForCryptoPunks(
        @NonLockGuarded tokenRegistrationService: TokenRegistrationService,
        @NonLockGuarded nftIndexerProperties: NftIndexerProperties
    ) = runBlocking<Unit> {
        val address = Address.apply(nftIndexerProperties.cryptoPunksContractAddress)
        val token = Token(
            address,
            name = "CRYPTOPUNKS",
            symbol = "(Ͼ)",
            standard = TokenStandard.CRYPTO_PUNKS
        )
        logger.info("Inserting token for CryptoPunks: $token")
        tokenRegistrationService.getOrSaveToken(address) { token.toMono() }.awaitFirst()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(ChangeLog00012InsertTokenForCryptoPunks::class.java)
    }
}
