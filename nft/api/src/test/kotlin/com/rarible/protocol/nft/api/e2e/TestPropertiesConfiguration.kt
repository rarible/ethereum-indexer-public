package com.rarible.protocol.nft.api.e2e

import com.rarible.ethereum.nft.validation.LazyNftValidator
import com.rarible.ethereum.sign.service.ERC1271SignService
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import scalether.transaction.MonoTransactionSender

@TestConfiguration
class TestPropertiesConfiguration {

    @Autowired
    private lateinit var sender: MonoTransactionSender

    @Bean
    fun erc1271SignService(): ERC1271SignService {
        return ERC1271SignService(sender)
    }
}
