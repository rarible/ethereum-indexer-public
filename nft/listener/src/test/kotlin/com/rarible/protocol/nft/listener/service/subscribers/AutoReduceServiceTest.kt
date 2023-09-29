package com.rarible.protocol.nft.listener.service.subscribers

import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.nft.core.data.createRandomItemTransfer
import com.rarible.protocol.nft.core.data.createRandomReversedEthereumLogRecord
import com.rarible.protocol.nft.core.model.AutoReduce
import com.rarible.protocol.nft.core.model.CreateCollection
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.repository.AutoReduceRepository
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class AutoReduceServiceTest {
    @InjectMockKs
    private lateinit var autoReduceService: AutoReduceService

    @MockK
    private lateinit var autoReduceRepository: AutoReduceRepository

    @Test
    fun autoReduce() = runBlocking<Unit> {
        val transfer1 = createRandomItemTransfer()
        val transfer2 = createRandomItemTransfer()
        val token1 = randomAddress()
        val token2 = randomAddress()

        coEvery {
            autoReduceRepository.saveItems(
                setOf(
                    AutoReduce(
                        ItemId(
                            token = transfer1.token,
                            tokenId = transfer1.tokenId
                        ).toString()
                    ),
                    AutoReduce(
                        ItemId(
                            token = transfer2.token,
                            tokenId = transfer2.tokenId
                        ).toString()
                    ),
                )
            )
        } returns Unit
        coEvery {
            autoReduceRepository.saveTokens(setOf(AutoReduce(token1.toString()), AutoReduce(token2.toString())))
        } returns Unit

        autoReduceService.autoReduce(
            listOf(
                createRandomReversedEthereumLogRecord(transfer1),
                createRandomReversedEthereumLogRecord(transfer2),
                createRandomReversedEthereumLogRecord(
                    CreateCollection(
                        id = token1,
                        name = "1",
                        symbol = "1",
                        owner = randomAddress()
                    )
                ),
                createRandomReversedEthereumLogRecord(
                    CreateCollection(
                        id = token2,
                        name = "1",
                        symbol = "1",
                        owner = randomAddress()
                    )
                ),
            )
        )
    }
}
