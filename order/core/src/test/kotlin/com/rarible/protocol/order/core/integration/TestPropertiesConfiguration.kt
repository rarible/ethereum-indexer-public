package com.rarible.protocol.order.core.integration

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.core.contract.model.Erc20Token
import com.rarible.ethereum.contract.service.ContractService
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.BlockchainDto
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.erc20.api.client.Erc20BalanceControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.data.createErc20BalanceDto
import com.rarible.protocol.order.core.data.createNftOwnershipDto
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.daonomic.rpc.mono.WebClientTransport
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import scalether.core.MonoEthereum
import scalether.domain.Address
import scalether.transaction.MonoTransactionPoller
import scalether.transaction.ReadOnlyMonoTransactionSender
import java.time.Instant

@TestConfiguration
@EnableConfigurationProperties(OrderIndexerProperties::class)
class TestPropertiesConfiguration {
    @Bean
    @Primary
    fun mockOrderPublisher(): ProtocolOrderPublisher {
        return mockk {
            coEvery { publish(any<OrderEventDto>()) } returns Unit
            coEvery { publish(any<OrderActivityDto>()) } returns Unit
        }
    }

    @Bean
    fun blockchain(): Blockchain {
        return Blockchain.ETHEREUM
    }

    @Bean
    fun testEthereum(@Value("\${parityUrls}") url: String): MonoEthereum {
        return MonoEthereum(WebClientTransport(url, MonoEthereum.mapper(), 10000, 10000))
    }

    @Bean
    fun readOnlyMonoTransactionSender(ethereum: MonoEthereum): ReadOnlyMonoTransactionSender {
        return ReadOnlyMonoTransactionSender(ethereum, Address.ONE())
    }

    @Bean
    fun poller(ethereum: MonoEthereum): MonoTransactionPoller {
        return MonoTransactionPoller(ethereum)
    }

    @Bean
    @Primary
    fun mockedErc20BalanceApiClient(): Erc20BalanceControllerApi {
        return mockk {
            every { getErc20Balance(any(), any()) } returns Mono.just(createErc20BalanceDto())
        }
    }

    @Bean
    @Primary
    fun mockedNftOwnershipApi(): NftOwnershipControllerApi {
        return mockk {
            every { getNftOwnershipById(any(), any()) } returns Mono.just(createNftOwnershipDto())
        }
    }

    @Bean
    fun testContractService(): ContractService {
        val mock = mockk<ContractService>()
        coEvery {
            mock.get(any())
        } returns Erc20Token(
            Address.ONE(),
            "test",
            "TST",
            18
        )
        return mock
    }

    @Bean
    fun applicationEnvironment() = ApplicationEnvironmentInfo(
        "e2e", "localhost"
    )


    @Bean
    @Primary
    fun testCurrencyApi(): CurrencyControllerApi = object : CurrencyControllerApi() {
        override fun getCurrencyRate(blockchain: BlockchainDto, address: String?, at: Long?): Mono<CurrencyRateDto> {
            return CurrencyRateDto(
                "test",
                "usd",
                1.5.toBigDecimal(),
                Instant.ofEpochMilli(at!!)
            ).toMono()
        }
    }
}
