package com.rarible.protocol.nft.core.converters.dto

import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import org.slf4j.LoggerFactory

object CollectionDtoConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(token: Token): NftCollectionDto {
        try {
            return NftCollectionDto(
                id = token.id,
                type = CollectionTypeDtoConverter.convert(token.standard),
                status = convertStatus(token.status),
                owner = token.owner,
                name = token.name,
                symbol = token.symbol,
                features = token.features.map { CollectionFeatureDtoConverter.convert(it) },
                flags = CollectionFlagDtoConverter.convert(token.flags),
                supportsLazyMint = token.features.contains(TokenFeature.MINT_AND_TRANSFER),
                minters = if (token.isRaribleContract) listOfNotNull(token.owner) else emptyList(),
                isRaribleContract = token.isRaribleContract,
                scam = token.scam,
            )
        } catch (e: Throwable) {
            logger.error("Failed to convert collection [{}]: {}", token.id, e.message)
            throw e
        }
    }

    private fun convertStatus(tokenStatus: ContractStatus): NftCollectionDto.Status =
        when (tokenStatus) {
            ContractStatus.PENDING -> NftCollectionDto.Status.PENDING
            ContractStatus.ERROR -> NftCollectionDto.Status.ERROR
            ContractStatus.CONFIRMED -> NftCollectionDto.Status.CONFIRMED
        }
}
