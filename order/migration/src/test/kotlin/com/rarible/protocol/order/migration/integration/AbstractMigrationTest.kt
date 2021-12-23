package com.rarible.protocol.order.migration.integration

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftOrdersPriceUpdateEventDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.mockk.coEvery
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoOperations

abstract class AbstractMigrationTest {

    @Autowired
    protected lateinit var mongo: ReactiveMongoOperations

    @Autowired
    protected lateinit var assetMakeBalanceProvider: AssetMakeBalanceProvider

    @Autowired
    protected lateinit var protocolOrderPublisher: ProtocolOrderPublisher

    @BeforeEach
    fun clearMocks() {
        io.mockk.clearMocks(assetMakeBalanceProvider)
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns EthUInt256.ZERO
        io.mockk.clearMocks(protocolOrderPublisher)
        coEvery { protocolOrderPublisher.publish(any() as OrderActivityDto) } returns Unit
        coEvery { protocolOrderPublisher.publish(any() as OrderEventDto) } returns Unit
        coEvery { protocolOrderPublisher.publish(any() as NftOrdersPriceUpdateEventDto) } returns Unit
    }
}
