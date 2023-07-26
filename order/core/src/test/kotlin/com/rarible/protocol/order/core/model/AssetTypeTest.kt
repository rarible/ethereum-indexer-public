package com.rarible.protocol.order.core.model

import com.rarible.protocol.order.core.data.randomAmmNftType
import com.rarible.protocol.order.core.data.randomCollectionType
import com.rarible.protocol.order.core.data.randomCryptoPunksAssetType
import com.rarible.protocol.order.core.data.randomErc1155LazyAssetType
import com.rarible.protocol.order.core.data.randomErc1155Type
import com.rarible.protocol.order.core.data.randomErc20Type
import com.rarible.protocol.order.core.data.randomErc721LazyAssetType
import com.rarible.protocol.order.core.data.randomErc721Type
import com.rarible.protocol.order.core.data.randomGenerativeArtAssetType
import com.rarible.protocol.order.core.misc.MAPPER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class AssetTypeTest {

    private companion object {
        private val assetTypes = listOf<Pair<AssetType, Class<*>>>(
            EthAssetType to EthAssetType::class.java,
            randomErc20Type() to Erc20AssetType::class.java,
            randomErc721Type() to Erc721AssetType::class.java,
            randomErc721LazyAssetType() to Erc721LazyAssetType::class.java,
            randomErc1155Type() to Erc1155AssetType::class.java,
            randomErc1155LazyAssetType() to Erc1155LazyAssetType::class.java,
            randomCryptoPunksAssetType() to CryptoPunksAssetType::class.java,
            randomCollectionType() to CollectionAssetType::class.java,
            randomAmmNftType() to AmmNftAssetType::class.java,
            randomGenerativeArtAssetType() to GenerativeArtAssetType::class.java
        )

        @JvmStatic
        fun assetTypesStream(): Stream<Arguments> = run {
            require(
                assetTypes
                    .map { it.first.type }
                    .containsAll(AssetType.Companion.Type.values().toList())
            )
            assetTypes.stream().map { Arguments.of(it.first, it.second) }
        }
    }

    @ParameterizedTest
    @MethodSource("assetTypesStream")
    fun `serialize and deserialize - ok`(assetType: AssetType, assetTypeClass: Class<*>) {
        val jsonAssetType = MAPPER.writeValueAsString(assetType)
        val deserializedAssetType = MAPPER.readValue(jsonAssetType, assetTypeClass)
        assertThat(deserializedAssetType).isEqualTo(assetType)
    }
}
