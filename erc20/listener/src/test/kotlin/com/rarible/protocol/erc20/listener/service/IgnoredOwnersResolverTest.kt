package com.rarible.protocol.erc20.listener.service

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.erc20.core.configuration.Erc20IndexerProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.core.io.ClassPathResource
import scalether.domain.Address

class IgnoredOwnersResolverTest {

    @ParameterizedTest
    @CsvSource("dev,ETHEREUM", "staging,ETHEREUM", "dev,POLYGON", "prod,POLYGON")
    internal fun `should read ignored owners by env and blockchain`(env: String, blockchain: String) {
        val expected = ClassPathResource("ignored-owners/$env-${blockchain.lowercase()}.json").inputStream.use { stream ->
            val array = jacksonObjectMapper().readTree(stream).get("ignored") as ArrayNode
            array.map {
                Address.apply(it.asText())
            }.toSet()
        }

        val actual = IgnoredOwnersResolver(
            Erc20IndexerProperties(
                blockchain = Blockchain.valueOf(blockchain)
            ),
            ApplicationEnvironmentInfo(env, "host")
        ).resolve()
        assertThat(actual).containsAll(expected)
    }
}
