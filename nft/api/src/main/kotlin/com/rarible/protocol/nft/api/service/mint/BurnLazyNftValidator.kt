package com.rarible.protocol.nft.api.service.mint

import com.rarible.ethereum.sign.service.ERC1271SignService
import com.rarible.protocol.dto.BurnLazyNftFormDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.exceptions.ValidationApiException
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.history.LazyNftItemHistoryRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.stereotype.Component
import scalether.util.Hash

@Component
class BurnLazyNftValidator(
    private val lazyNftItemHistoryRepository: LazyNftItemHistoryRepository,
    private val signService: ERC1271SignService
) {
    suspend fun validate(itemId: ItemId, msg: String, burnLazyNftDto: BurnLazyNftFormDto) {
        val lazyMint = lazyNftItemHistoryRepository.findLazyMintById(itemId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Item", itemId)

        val mintCreators = lazyMint.creators.map { it.account }
        val burnCreators = burnLazyNftDto.creators

        if (setOf(mintCreators) != setOf(burnCreators)) {
            throw ValidationApiException("Incorrect creators for $itemId")
        }

        burnCreators.indices.forEach { i ->
            val address = burnLazyNftDto.creators[i]
            val signature = burnLazyNftDto.signatures[i]
            val hash = Word(Hash.sha3(ERC1271SignService.addStart(msg).bytes()))
            val recovered = signService.recover(hash, signature)
            if (address != recovered) {
                throw ValidationApiException("Incorrect signature for $itemId")
            }
        }
    }
}
