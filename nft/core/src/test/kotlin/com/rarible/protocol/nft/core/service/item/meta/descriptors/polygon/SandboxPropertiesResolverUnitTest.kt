package com.rarible.protocol.nft.core.service.item.meta.descriptors.polygon

import com.rarible.protocol.dto.MetaContentDto
import com.rarible.protocol.nft.core.data.createRandomItemId
import com.rarible.protocol.nft.core.model.ItemMetaContent
import com.rarible.protocol.nft.core.model.ItemProperties
import com.rarible.protocol.nft.core.model.meta.EthMetaContent
import com.rarible.protocol.nft.core.service.item.meta.descriptors.RariblePropertiesResolver
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class SandboxPropertiesResolverUnitTest {

    private val raribleResolver = mockk<RariblePropertiesResolver> {
        coEvery {
            resolve(any())
        } returns ItemProperties(
            "name", "descritpion", emptyList(), null, null, content = ItemMetaContent(
                imageOriginal = EthMetaContent(
                    "https://api.sandbox.game/lands/cb61dd01-f82a-4081-a3dd-8a9d22360ecb/v3/preview-560px-x-560px.webp",
                    representation = MetaContentDto.Representation.ORIGINAL
                )
            )
        )
    }

    @Test
    fun `should replace images urls`() = runBlocking<Unit> {
        val meta = SandboxPropertiesResolver(raribleResolver).resolve(
            createRandomItemId().copy(token = SandboxPropertiesResolver.SANDBOX_NFT_ADDRESS)
        )!!

        Assertions.assertThat(meta.content.imageOriginal!!.url).isEqualTo(
            "https://api.sandbox.game/lands/cb61dd01-f82a-4081-a3dd-8a9d22360ecb/preview"
        )
    }
}
