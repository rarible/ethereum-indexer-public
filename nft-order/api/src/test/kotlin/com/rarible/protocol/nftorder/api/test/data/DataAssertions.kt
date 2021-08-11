package com.rarible.protocol.nftorder.core.test.data

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftOrderItemDto
import com.rarible.protocol.dto.NftOrderOwnershipDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.nftorder.core.converter.ItemHistoryDtoToTransferConverter
import com.rarible.protocol.nftorder.core.converter.PartDtoConverter
import com.rarible.protocol.nftorder.core.model.Item
import com.rarible.protocol.nftorder.core.model.Ownership
import org.assertj.core.api.Assertions.assertThat

fun assertOwnershipDtoAndNftDtoEquals(ownership: NftOrderOwnershipDto, nftOwnershipDto: NftOwnershipDto) {
    assertThat(ownership.id).isEqualTo(nftOwnershipDto.id)
    assertThat(ownership.contract).isEqualTo(nftOwnershipDto.contract)
    assertThat(ownership.tokenId).isEqualTo(nftOwnershipDto.tokenId)
    assertThat(ownership.creators).isEqualTo(nftOwnershipDto.creators)
    assertThat(ownership.value).isEqualTo(nftOwnershipDto.value)
    assertThat(ownership.lazyValue).isEqualTo(nftOwnershipDto.lazyValue)
    assertThat(ownership.date).isEqualTo(nftOwnershipDto.date)
    assertThat(ownership.pending).isEqualTo(nftOwnershipDto.pending)
}

fun assertOwnershipAndDtoEquals(ownership: Ownership, dto: NftOrderOwnershipDto) {
    assertThat(ownership.contract).isEqualTo(dto.contract)
    assertThat(ownership.tokenId.value).isEqualTo(dto.tokenId)
    assertThat(ownership.creators).isEqualTo(PartDtoConverter.convert(dto.creators!!))
    assertThat(ownership.value.value).isEqualTo(dto.value)
    assertThat(ownership.lazyValue.value).isEqualTo(dto.lazyValue)
    assertThat(ownership.date).isEqualTo(dto.date)
    assertThat(ownership.pending).isEqualTo(ItemHistoryDtoToTransferConverter.convert(dto.pending))
}

fun assertItemDtoAndNftDtoEquals(item: NftOrderItemDto, nftItemDto: NftItemDto) {
    assertThat(item.contract).isEqualTo(nftItemDto.contract)
    assertThat(item.tokenId).isEqualTo(nftItemDto.tokenId)
    assertThat(item.creators).isEqualTo(nftItemDto.creators)
    assertThat(item.supply).isEqualTo(nftItemDto.supply)
    assertThat(item.lazySupply).isEqualTo(nftItemDto.lazySupply)
    assertThat(item.date).isEqualTo(nftItemDto.date)
    assertThat(item.pending).isEqualTo(nftItemDto.pending)
    assertThat(item.royalties).isEqualTo(PartDtoConverter.convert(nftItemDto.royalties))
}

fun assertItemAndDtoEquals(item: Item, itemDto: NftOrderItemDto) {
    assertThat(item.token).isEqualTo(itemDto.contract)
    assertThat(item.tokenId.value).isEqualTo(itemDto.tokenId)
    assertThat(item.creators).isEqualTo(PartDtoConverter.convert(itemDto.creators))
    assertThat(item.supply.value).isEqualTo(itemDto.supply)
    assertThat(item.lazySupply.value).isEqualTo(itemDto.lazySupply)
    assertThat(item.date).isEqualTo(itemDto.date)
    assertThat(item.pending).isEqualTo(itemDto.pending)
    assertThat(item.royalties).isEqualTo(PartDtoConverter.convert(itemDto.royalties))
}


