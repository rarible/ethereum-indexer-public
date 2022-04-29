package com.rarible.protocol.order.core.misc

import com.rarible.protocol.order.api.misc.indexName
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes.BIDS_BY_ITEM_PLATFORM_DEFINITION
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes.BIDS_BY_MAKER_PLATFORM_DEFINITION
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IndexExtensionTest {
    @Test
    fun `should get valid index name`() {
        assertThat(BIDS_BY_ITEM_PLATFORM_DEFINITION.indexName).isEqualTo("take.type.token_1_take.type.tokenId_1_platform_1_takePriceUsd_1__id_1")
        assertThat(BIDS_BY_MAKER_PLATFORM_DEFINITION.indexName).isEqualTo("take.type.nft_1_maker_1_platform_1_lastUpdateAt_1__id_1")
    }
}