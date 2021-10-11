package com.rarible.protocol.order.migration.integration

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations

@FlowPreview
abstract class AbstractMigrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var assetMakeBalanceProvider: AssetMakeBalanceProvider

    @BeforeEach
    fun clearMocks() {
        io.mockk.clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.ZERO
    }
}
