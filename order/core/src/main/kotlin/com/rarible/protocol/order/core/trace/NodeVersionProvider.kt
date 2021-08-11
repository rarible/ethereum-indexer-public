package com.rarible.protocol.order.core.trace

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rarible.protocol.order.core.model.BlockchainClientVersion
import com.rarible.protocol.order.core.model.NodeType
import io.daonomic.rpc.domain.Request
import kotlinx.coroutines.reactive.awaitFirst
import org.slf4j.LoggerFactory
import scalether.core.MonoEthereum
import scalether.java.Lists

class NodeVersionProvider(
    private val ethereum: MonoEthereum
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    suspend fun getClientVersion(): BlockchainClientVersion? {
        val result = ethereum.executeRaw(Request(67, "web3_clientVersion", Lists.toScala(), "2.0")).awaitFirst()

        return result.result()
            .map { mapper.treeToValue(it, String::class.java) }
            .map { version ->
                val type = when {
                    version.startsWith(NodeType.GETH.prefix, true) -> NodeType.GETH
                    version.startsWith(NodeType.OPEN_ETHEREUM.prefix, true) -> NodeType.OPEN_ETHEREUM
                    else -> NodeType.UNKNOWN
                }
                BlockchainClientVersion(type, version)
            }
            .getOrElse {
                if (result.error().isEmpty.not()) {
                    logger.error("Can't fetch trace: {}", result.error().get())
                }
                null
            }
    }
}

