package com.rarible.protocol.order.migration.integration

import com.rarible.core.common.nowMillis
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import com.rarible.protocol.order.core.service.balance.AssetMakeBalanceProvider
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.coEvery
import io.mockk.mockk
import org.apache.commons.lang3.RandomUtils
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono
import scalether.core.MonoEthereum
import java.math.BigDecimal


@TestConfiguration
class TestPropertiesConfiguration {
    @Bean
    fun monoEthereum(): MonoEthereum {
        return MonoEthereum(WebClientTransport("https://dark-solitary-resonance.quiknode.pro/c0b7c629520de6c3d39261f6417084d71c3f8791/",
            MonoEthereum.mapper(), 10000, 10000))
    }

    @Bean
    @Primary
    fun mockedCurrencyApi(): CurrencyControllerApi {
        return mockk {
            coEvery { getCurrencyRate(any(), any(), any())
            } returns Mono.just(CurrencyRateDto("from", "usd", BigDecimal.valueOf(1), nowMillis()))
        }
    }

    @Bean
    @Primary
    fun mockAssetMakeBalanceProvider(): AssetMakeBalanceProvider = mockk()

    @Bean
    @Primary
    fun mockProducer(): ProtocolOrderPublisher = mockk()
}
