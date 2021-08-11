package com.rarible.protocol.nftorder.listener.test.data

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nftorder.core.converter.ItemHistoryDtoToTransferConverter
import com.rarible.protocol.nftorder.core.converter.PartDtoConverter
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import org.assertj.core.api.Assertions.assertThat

fun assertOwnershipAndNftDtoEquals(ownership: Ownership, nftOwnershipDto: NftOwnershipDto) {
    assertThat(ownership.contract).isEqualTo(nftOwnershipDto.contract)
    assertThat(ownership.tokenId.value).isEqualTo(nftOwnershipDto.tokenId)
    assertThat(ownership.creators).isEqualTo(PartDtoConverter.convert(nftOwnershipDto.creators))
    assertThat(ownership.value.value).isEqualTo(nftOwnershipDto.value)
    assertThat(ownership.lazyValue.value).isEqualTo(nftOwnershipDto.lazyValue)
    assertThat(ownership.date).isEqualTo(nftOwnershipDto.date)
    assertThat(ownership.pending).isEqualTo(ItemHistoryDtoToTransferConverter.convert(nftOwnershipDto.pending))
}

fun assertItemAndDtoEquals(item: Item, nftItemDto: NftItemDto) {
    assertThat(item.token).isEqualTo(nftItemDto.contract)
    assertThat(item.tokenId.value).isEqualTo(nftItemDto.tokenId)
    assertThat(item.creators).isEqualTo(PartDtoConverter.convert(nftItemDto.creators))
    assertThat(item.supply.value).isEqualTo(nftItemDto.supply)
    assertThat(item.lazySupply.value).isEqualTo(nftItemDto.lazySupply)
    assertThat(item.date).isEqualTo(nftItemDto.date)
    assertThat(item.pending).isEqualTo(nftItemDto.pending)
    assertThat(item.royalties).isEqualTo(PartDtoConverter.convert(nftItemDto.royalties))
}