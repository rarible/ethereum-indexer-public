package com.rarible.protocol.nft.api.e2e.activity

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.*
import com.rarible.protocol.nft.api.e2e.End2EndTest
import com.rarible.protocol.nft.api.e2e.SpringContainerBaseTest
import com.rarible.protocol.nft.api.e2e.data.*
import com.rarible.protocol.nft.core.repository.history.NftItemHistoryRepository
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.AddressFactory
import java.time.Duration
import java.time.Instant
import java.util.stream.Stream

@End2EndTest
class ActivityFt : SpringContainerBaseTest() {
    @Autowired
    private lateinit var historyRepository: NftItemHistoryRepository

    @BeforeEach
    override fun setupDatabase() = runBlocking {
        super.setupDatabase()
        historyRepository.createIndexes()
        historyRepository.dropIndexes()
    }

    companion object {
        private val now = Instant.ofEpochSecond(Instant.now().epochSecond)

        @JvmStatic
        private fun allFilter() = Stream.of(
            Arguments.of(
                listOf(
                    createItemMint().withTransferDate(now + Duration.ofMinutes(4)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(3)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(2)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(1)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(0))
                ),
                listOf(createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                NftActivityFilterAllDto(listOf(NftActivityFilterAllDto.Types.MINT)),
                ActivitySortDto.LATEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createItemMint().withTransferDate(now + Duration.ofMinutes(0)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(1)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(2)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(3)),
                    createItemMint().withTransferDate(now + Duration.ofMinutes(4))
                ),
                listOf(createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                NftActivityFilterAllDto(listOf(NftActivityFilterAllDto.Types.MINT)),
                ActivitySortDto.EARLIEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createItemBurn().withTransferDate(now + Duration.ofMinutes(4)),
                    createItemBurn().withTransferDate(now + Duration.ofMinutes(3)),
                    createItemBurn().withTransferDate(now + Duration.ofMinutes(2)),
                    createItemBurn().withTransferDate(now + Duration.ofMinutes(1)),
                    createItemBurn().withTransferDate(now + Duration.ofMinutes(0))
                ),
                listOf(createItemMint(), createItemMint(), createItemTransfer(), createItemTransfer()),
                NftActivityFilterAllDto(listOf(NftActivityFilterAllDto.Types.BURN)),
                ActivitySortDto.LATEST_FIRST
            ),
            Arguments.of(
                listOf(
                    createItemTransfer().withTransferDate(now + Duration.ofMinutes(4)),
                    createItemTransfer().withTransferDate(now + Duration.ofMinutes(3)),
                    createItemTransfer().withTransferDate(now + Duration.ofMinutes(2)),
                    createItemTransfer().withTransferDate(now + Duration.ofMinutes(1)),
                    createItemTransfer().withTransferDate(now + Duration.ofMinutes(0))
                ),
                listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn()),
                NftActivityFilterAllDto(listOf(NftActivityFilterAllDto.Types.TRANSFER)),
                ActivitySortDto.LATEST_FIRST
            ),
            run {
                val owner = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByUserDto(listOf(owner), listOf(NftActivityFilterByUserDto.Types.MINT)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val owner = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(6)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(5)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now - Duration.ofMinutes(1)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now - Duration.ofMinutes(2))
                    ),
                    NftActivityFilterByUserDto(
                        listOf(owner),
                        listOf(NftActivityFilterByUserDto.Types.MINT),
                        from = (now + Duration.ofMinutes(0)).epochSecond,
                        to = (now + Duration.ofMinutes(4)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val owner = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(0)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(4))
                    ),
                    listOf(
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(6)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(5)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now - Duration.ofMinutes(1)),
                        createItemMint()
                            .withTransferOwner(owner)
                            .withTransferDate(now - Duration.ofMinutes(2))
                    ),
                    NftActivityFilterByUserDto(
                        listOf(owner),
                        listOf(NftActivityFilterByUserDto.Types.MINT),
                        from = (now + Duration.ofMinutes(0)).epochSecond,
                        to = (now + Duration.ofMinutes(4)).epochSecond
                    ),
                    ActivitySortDto.EARLIEST_FIRST
                )
            },
            run {
                val from = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByUserDto(listOf(from), listOf(NftActivityFilterByUserDto.Types.BURN)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val from = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(2))
                    ),
                    listOf(
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(5)),
                        createItemBurn()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(1))
                    ),
                    NftActivityFilterByUserDto(
                        listOf(from),
                        listOf(NftActivityFilterByUserDto.Types.BURN),
                        from = (now + Duration.ofMinutes(2)).epochSecond,
                        to = (now + Duration.ofMinutes(4)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val from = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByUserDto(listOf(from), listOf(NftActivityFilterByUserDto.Types.TRANSFER_FROM)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val from = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(2))
                    ),
                    listOf(
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(5)),
                        createItemTransfer()
                            .withTransferFrom(from)
                            .withTransferDate(now + Duration.ofMinutes(1))
                    ),
                    NftActivityFilterByUserDto(
                        listOf(from),
                        listOf(NftActivityFilterByUserDto.Types.TRANSFER_FROM),
                        from = (now + Duration.ofMinutes(2)).epochSecond,
                        to = (now + Duration.ofMinutes(4)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val owner = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByUserDto(listOf(owner), listOf(NftActivityFilterByUserDto.Types.TRANSFER_TO)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val owner = AddressFactory.create()
                Arguments.of(
                    listOf(
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(2))
                    ),
                    listOf(
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(5)),
                        createItemTransfer()
                            .withTransferOwner(owner)
                            .withTransferDate(now + Duration.ofMinutes(1))
                    ),
                    NftActivityFilterByUserDto(
                        listOf(owner),
                        listOf(NftActivityFilterByUserDto.Types.TRANSFER_TO),
                        from = (now + Duration.ofMinutes(2)).epochSecond,
                        to = (now + Duration.ofMinutes(4)).epochSecond
                    ),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createItemMint()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemMint()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemMint()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemMint()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemMint()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByItemDto(token, tokenId.value, listOf(NftActivityFilterByItemDto.Types.MINT)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createItemBurn()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemBurn()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemBurn()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemBurn()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemBurn()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByItemDto(token, tokenId.value, listOf(NftActivityFilterByItemDto.Types.BURN)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()
                val tokenId = EthUInt256.of((1L..1000L).random())

                Arguments.of(
                    listOf(
                        createItemTransfer()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemTransfer()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemTransfer()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemTransfer()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemTransfer()
                            .withToken(token)
                            .withTokenId(tokenId)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByItemDto(token, tokenId.value, listOf(NftActivityFilterByItemDto.Types.TRANSFER)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createItemMint()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemMint()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemMint()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemMint()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemMint()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByCollectionDto(token, listOf(NftActivityFilterByCollectionDto.Types.MINT)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createItemBurn()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemBurn()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemBurn()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemBurn()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemBurn()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByCollectionDto(token, listOf(NftActivityFilterByCollectionDto.Types.BURN)),
                    ActivitySortDto.LATEST_FIRST
                )
            },
            run {
                val token = AddressFactory.create()

                Arguments.of(
                    listOf(
                        createItemTransfer()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(4)),
                        createItemTransfer()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(3)),
                        createItemTransfer()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(2)),
                        createItemTransfer()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(1)),
                        createItemTransfer()
                            .withToken(token)
                            .withTransferDate(now + Duration.ofMinutes(0))
                    ),
                    listOf(createItemMint(), createItemMint(), createItemBurn(), createItemBurn(), createItemTransfer(), createItemTransfer()),
                    NftActivityFilterByCollectionDto(token, listOf(NftActivityFilterByCollectionDto.Types.TRANSFER)),
                    ActivitySortDto.LATEST_FIRST
                )
            }
        )
    }

    @ParameterizedTest
    @MethodSource("allFilter")
    fun `should get all for several requests`(
        logs: List<LogEvent>,
        otherTypes: List<LogEvent>,
        filter: NftActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {

        save(*logs.shuffled().toTypedArray())
        save(*otherTypes.toTypedArray())

        val allActivities = mutableListOf<NftActivityDto>()

        var continuation: String? = null
        do {
            val activities = nftActivityApiClient.getNftActivities(filter, continuation, 2, sort).awaitFirst()
            assertThat(activities.items).hasSizeLessThanOrEqualTo(2)

            allActivities.addAll(activities.items)
            continuation = activities.continuation
        } while (continuation != null)

        assertThat(allActivities).hasSize(logs.size)

        allActivities.forEachIndexed { index, nftActivity ->
            checkItem(nftActivity, logs[index])
        }
    }


    @ParameterizedTest
    @MethodSource("allFilter")
    fun `should get all for single requests`(
        logs: List<LogEvent>,
        otherTypes: List<LogEvent>,
        filter: NftActivityFilterDto,
        sort: ActivitySortDto
    ) = runBlocking {

        save(*logs.shuffled().toTypedArray())
        save(*otherTypes.toTypedArray())

        val activities = nftActivityApiClient.getNftActivities(filter, null, null, sort).awaitFirst()

        assertThat(activities.items).hasSize(logs.size)

        activities.items.forEachIndexed { index, nftActivity ->
            checkItem(nftActivity, logs[index])
        }
    }

    private suspend fun save(vararg history: LogEvent) {
        history.forEach { historyRepository.save(it).awaitFirst()  }
    }

    private fun checkItem(activity: NftActivityDto, history: LogEvent) {
        assertThat(activity.id).isEqualTo(history.id.toString())
    }
}
