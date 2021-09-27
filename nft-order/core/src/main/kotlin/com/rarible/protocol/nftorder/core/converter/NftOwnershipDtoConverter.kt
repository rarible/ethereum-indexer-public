package com.rarible.protocol.nftorder.core.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nftorder.core.model.Ownership

object NftOwnershipDtoConverter {

    fun convert(dto: NftOwnershipDto): Ownership {
        return Ownership(
            dto.contract,
            EthUInt256(dto.tokenId),
            PartDtoConverter.convert(dto.creators),
            dto.owner,
            EthUInt256(dto.value),
            EthUInt256(dto.lazyValue),
            dto.date,
            ItemHistoryDtoToTransferConverter.convert(dto.pending),
            null
        )
    }
}