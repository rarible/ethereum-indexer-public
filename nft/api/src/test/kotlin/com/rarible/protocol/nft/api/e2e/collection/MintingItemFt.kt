package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.nft.api.client.NftLazyMintControllerApi
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.createPartDto
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenRepository
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.web3j.crypto.Keys
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger
import java.util.*

@End2EndTest
class MintingItemFt : SpringContainerBaseTest() {

    @Autowired
    private lateinit var tokenRepository: TokenRepository

    @Test
    fun `shouldn't lazy mint to unsupported collection`() = runBlocking<Unit> {
        val privateKey = BigInteger.valueOf(100)
        val creator = Address.apply(Keys.getAddressFromPrivateKey(privateKey))

        val contract = createToken().copy(
            standard = TokenStandard.ERC721,
            features = setOf(TokenFeature.APPROVE_FOR_ALL, TokenFeature.BURN)
        )
        tokenRepository.save(contract).awaitFirst()

        val collectionDto = nftCollectionApiClient.getNftCollectionById(contract.id.hex()).awaitFirst()
        assertThat(collectionDto.supportsLazyMint).isFalse()

        val bs = Binary.apply(creator.bytes().plus(ByteArray(12)))
        val tokenId = Uint256Type.decode(bs, 0).value()
        val lazyItemDto = createNft(contract.id, tokenId, listOf(PartDto(creator, 10000)))

        assertThrows(NftLazyMintControllerApi.ErrorMintNftAsset::class.java) {
            runBlocking {
                nftLazyMintApiClient.mintNftAsset(lazyItemDto).awaitSingle()
            }
        }
    }

    private fun createNft(contract: Address, tokenId: BigInteger, creators: List<PartDto>): LazyErc721Dto {
        return LazyErc721Dto(
            contract = contract,
            tokenId = tokenId,
            uri = UUID.randomUUID().toString(),
            royalties = listOf(createPartDto()),
            creators = creators,
            signatures = listOf(Binary.empty())
        )
    }
}
