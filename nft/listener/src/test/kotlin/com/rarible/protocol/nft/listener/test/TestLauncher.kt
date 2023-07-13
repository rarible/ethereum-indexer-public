package com.rarible.protocol.nft.listener.test

import com.rarible.blockchain.scanner.block.Block
import com.rarible.blockchain.scanner.block.BlockRepository
import com.rarible.blockchain.scanner.block.BlockStatus
import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.NftCollectionEventDto
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import scalether.core.MonoEthereum

@Component
class TestLauncher(
    private val activityWorker: ConsumerWorker<EthActivityEventDto>,
    private val collectionWorker: ConsumerWorker<NftCollectionEventDto>,
    private val blockRepository: BlockRepository,
    private val ethereum: MonoEthereum
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(vararg args: String?) = runBlocking<Unit> {
        logger.info("Test context started, launching test consumers")
        activityWorker.start();
        collectionWorker.start();

        // Only for initial case, otherwise we don't need it
        val currentBlock = blockRepository.getLastBlock()
        if (currentBlock != null) {
            return@runBlocking
        }

        val currentBlockNumber = ethereum.ethBlockNumber().awaitFirst()
        val currentEthBlock = ethereum.ethGetBlockByNumber(currentBlockNumber).awaitFirst()
        logger.info("NFT-Listener tests will start from block: {}", currentBlockNumber)

        blockRepository.save(
            Block(
                id = currentEthBlock.blockNumber.toLong(),
                hash = currentEthBlock.blockHash.prefixed(),
                parentHash = currentEthBlock.parentHash()?.prefixed(),
                timestamp = currentEthBlock.timestamp().toLong(),
                status = BlockStatus.SUCCESS
            )
        )
    }
}