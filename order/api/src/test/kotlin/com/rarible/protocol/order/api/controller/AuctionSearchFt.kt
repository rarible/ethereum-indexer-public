package com.rarible.protocol.order.api.controller

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.dto.AuctionBidDto
import com.rarible.protocol.dto.AuctionDto
import com.rarible.protocol.dto.AuctionIdsDto
import com.rarible.protocol.dto.AuctionSortDto
import com.rarible.protocol.dto.AuctionStatusDto
import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.api.integration.AbstractIntegrationTest
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.data.createAuctionLogEvent
import com.rarible.protocol.order.core.data.randomAuction
import com.rarible.protocol.order.core.data.randomAuctionV1DataV1
import com.rarible.protocol.order.core.data.randomBid
import com.rarible.protocol.order.core.data.randomBidPlaced
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Auction
import com.rarible.protocol.order.core.model.BidPlaced
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.Part
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import scalether.domain.Address
import java.math.BigDecimal
import java.time.Duration
import java.util.stream.Stream
import com.rarible.protocol.order.api.client.AuctionControllerApi as AuctionClient


@IntegrationTest
class AuctionSearchFt : AbstractIntegrationTest() {
    companion object {
        interface FetchMethod {
            suspend fun fetch(
                client: AuctionClient,
                fetchParams: FetchParams,
                size: Int,
                continuation: String?
            ): AuctionsPaginationDto
        }

        private val fetchAllMethod = object : FetchMethod {
            override suspend fun fetch(
                client: AuctionClient,
                fetchParams: FetchParams,
                size: Int,
                continuation: String?
            ) =
                client.getAuctionsAll(
                    fetchParams.sort,
                    fetchParams.status,
                    fetchParams.origin,
                    fetchParams.platform,
                    continuation,
                    size
                ).awaitFirst()
        }
        private val fetchByItemMethod = object : FetchMethod {
            override suspend fun fetch(
                client: AuctionClient,
                fetchParams: FetchParams,
                size: Int,
                continuation: String?
            ) =
                client.getAuctionsByItem(
                    fetchParams.contract,
                    fetchParams.tokenId,
                    fetchParams.origin,
                    fetchParams.sort,
                    fetchParams.origin,
                    fetchParams.status,
                    fetchParams.currencyId,
                    fetchParams.platform,
                    continuation,
                    size
                ).awaitFirst()
        }
        private val fetchByCollectionMethod = object : FetchMethod {
            override suspend fun fetch(
                client: AuctionClient,
                fetchParams: FetchParams,
                size: Int,
                continuation: String?
            ) =
                client.getAuctionsByCollection(
                    fetchParams.contract,
                    fetchParams.seller,
                    fetchParams.origin,
                    fetchParams.status,
                    fetchParams.platform,
                    continuation,
                    size
                ).awaitFirst()
        }
        private val fetchBySellerMethod = object : FetchMethod {
            override suspend fun fetch(
                client: AuctionClient,
                fetchParams: FetchParams,
                size: Int,
                continuation: String?
            ) =
                client.getAuctionsBySeller(
                    fetchParams.seller,
                    fetchParams.status,
                    fetchParams.origin,
                    fetchParams.platform,
                    continuation,
                    size
                ).awaitFirst()
        }

        @JvmStatic
        fun auctions(): Stream<Arguments> = run {
            val now = nowMillis()

            Stream.of(
                Arguments.of(
                    FetchParams(sort = AuctionSortDto.LAST_UPDATE_ASC),
                    fetchAllMethod,
                    listOf(
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(0)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4))
                    ),
                    emptyList<Auction>()
                ),
                Arguments.of(
                    FetchParams(sort = AuctionSortDto.LAST_UPDATE_DESC),
                    fetchAllMethod,
                    listOf(
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(2)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(1)),
                        randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(0))
                    ),
                    emptyList<Auction>()
                ),
                run {
                    val origin = randomAddress()
                    val data = randomAuctionV1DataV1().copy(originFees = listOf(Part(origin, EthUInt256.ONE)))

                    Arguments.of(
                        FetchParams(origin = origin.prefixed(), sort = AuctionSortDto.LAST_UPDATE_DESC),
                        fetchAllMethod,
                        listOf(
                            randomAuction().copy(data = data, lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(data = data, lastUpdateAt = now + Duration.ofMinutes(3)),
                            randomAuction().copy(data = data, lastUpdateAt = now + Duration.ofMinutes(2)),
                            randomAuction().copy(data = data, lastUpdateAt = now + Duration.ofMinutes(1)),
                            randomAuction().copy(data = data, lastUpdateAt = now + Duration.ofMinutes(0))
                        ),
                        listOf(
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4))
                        )
                    )
                },
                run {
                    val contract = randomAddress()
                    val tokenId = randomBigInt()
                    val sell = Asset(Erc721AssetType(contract, EthUInt256.of(tokenId)), EthUInt256.ONE)
                    Arguments.of(
                        FetchParams(
                            contract = contract.prefixed(),
                            tokenId = tokenId.toString(),
                            sort = AuctionSortDto.LAST_UPDATE_DESC
                        ),
                        fetchByItemMethod,
                        listOf(
                            randomAuction().copy(sell = sell, lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(sell = sell, lastUpdateAt = now + Duration.ofMinutes(3)),
                            randomAuction().copy(sell = sell, lastUpdateAt = now + Duration.ofMinutes(2)),
                            randomAuction().copy(sell = sell, lastUpdateAt = now + Duration.ofMinutes(1)),
                            randomAuction().copy(sell = sell, lastUpdateAt = now + Duration.ofMinutes(0))
                        ),
                        listOf(
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3))
                        )
                    )
                },
                run {
                    val contract = randomAddress()
                    val tokenId = randomBigInt()
                    val sell = Asset(Erc721AssetType(contract, EthUInt256.of(tokenId)), EthUInt256.ONE)
                    Arguments.of(
                        FetchParams(
                            contract = contract.prefixed(),
                            tokenId = tokenId.toString(),
                            sort = AuctionSortDto.BUY_PRICE_ASC
                        ),
                        fetchByItemMethod,
                        listOf(
                            randomAuction().copy(sell = sell, buyPriceUsd = BigDecimal.valueOf(1)),
                            randomAuction().copy(sell = sell, buyPriceUsd = BigDecimal.valueOf(2)),
                            randomAuction().copy(sell = sell, buyPriceUsd = BigDecimal.valueOf(3)),
                            randomAuction().copy(sell = sell, buyPriceUsd = BigDecimal.valueOf(4)),
                            randomAuction().copy(sell = sell, buyPriceUsd = BigDecimal.valueOf(5))
                        ),
                        listOf(
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3))
                        )
                    )
                },
                run {
                    val contract = randomAddress()
                    val tokenId = randomBigInt()
                    val buyAsset = Erc20AssetType(randomAddress())
                    val sell = Asset(Erc721AssetType(contract, EthUInt256.of(tokenId)), EthUInt256.ONE)
                    Arguments.of(
                        FetchParams(
                            contract = contract.prefixed(),
                            tokenId = tokenId.toString(),
                            currencyId = buyAsset.token.prefixed(),
                            sort = AuctionSortDto.BUY_PRICE_ASC
                        ),
                        fetchByItemMethod,
                        listOf(
                            randomAuction().copy(sell = sell, buy = buyAsset, buyPrice = BigDecimal.valueOf(1)),
                            randomAuction().copy(sell = sell, buy = buyAsset, buyPrice = BigDecimal.valueOf(2)),
                            randomAuction().copy(sell = sell, buy = buyAsset, buyPrice = BigDecimal.valueOf(3)),
                            randomAuction().copy(sell = sell, buy = buyAsset, buyPrice = BigDecimal.valueOf(4)),
                            randomAuction().copy(sell = sell, buy = buyAsset, buyPrice = BigDecimal.valueOf(5))
                        ),
                        listOf(
                            randomAuction().copy(sell = sell, buyPrice = BigDecimal.valueOf(1)),
                            randomAuction().copy(sell = sell, buyPrice = BigDecimal.valueOf(2))
                        )
                    )
                },
                run {
                    val contract = randomAddress()
                    val sell = Erc721AssetType(contract, EthUInt256.of(randomBigInt()))
                    Arguments.of(
                        FetchParams(contract = contract.prefixed()),
                        fetchByCollectionMethod,
                        listOf(
                            randomAuction().copy(
                                sell = Asset(
                                    sell.copy(tokenId = EthUInt256.of(randomBigInt())),
                                    EthUInt256.ONE
                                ), lastUpdateAt = now + Duration.ofMinutes(4)
                            ),
                            randomAuction().copy(
                                sell = Asset(
                                    sell.copy(tokenId = EthUInt256.of(randomBigInt())),
                                    EthUInt256.ONE
                                ), lastUpdateAt = now + Duration.ofMinutes(3)
                            ),
                            randomAuction().copy(
                                sell = Asset(
                                    sell.copy(tokenId = EthUInt256.of(randomBigInt())),
                                    EthUInt256.ONE
                                ), lastUpdateAt = now + Duration.ofMinutes(2)
                            ),
                            randomAuction().copy(
                                sell = Asset(
                                    sell.copy(tokenId = EthUInt256.of(randomBigInt())),
                                    EthUInt256.ONE
                                ), lastUpdateAt = now + Duration.ofMinutes(1)
                            ),
                            randomAuction().copy(
                                sell = Asset(
                                    sell.copy(tokenId = EthUInt256.of(randomBigInt())),
                                    EthUInt256.ONE
                                ), lastUpdateAt = now + Duration.ofMinutes(0)
                            )
                        ),
                        listOf(
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3))
                        )
                    )
                },
                run {
                    val seller = randomAddress()
                    Arguments.of(
                        FetchParams(seller = seller.prefixed()),
                        fetchBySellerMethod,
                        listOf(
                            randomAuction().copy(seller = seller, lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(seller = seller, lastUpdateAt = now + Duration.ofMinutes(3)),
                            randomAuction().copy(seller = seller, lastUpdateAt = now + Duration.ofMinutes(2)),
                            randomAuction().copy(seller = seller, lastUpdateAt = now + Duration.ofMinutes(1)),
                            randomAuction().copy(seller = seller, lastUpdateAt = now + Duration.ofMinutes(0))
                        ),
                        listOf(
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(4)),
                            randomAuction().copy(lastUpdateAt = now + Duration.ofMinutes(3))
                        )
                    )
                }
            )
        }

        @JvmStatic
        fun auctionBids(): Stream<Arguments> = Stream.of(
            run {
                val contract = randomAddress()
                val auctionId = EthUInt256.ONE
                val hash = Auction.raribleV1HashKey(contract, auctionId)

                Arguments.of(
                    contract,
                    auctionId,
                    listOf(
                        createAuctionLogEvent(
                            randomBidPlaced().copy(
                                hash = hash,
                                bidValue = BigDecimal.valueOf(5),
                                bid = randomBid().copy(amount = EthUInt256.of(5))
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced().copy(
                                hash = hash,
                                bidValue = BigDecimal.valueOf(4),
                                bid = randomBid().copy(amount = EthUInt256.of(4))
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced().copy(
                                hash = hash,
                                bidValue = BigDecimal.valueOf(3),
                                bid = randomBid().copy(amount = EthUInt256.of(3))
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced().copy(
                                hash = hash,
                                bidValue = BigDecimal.valueOf(2),
                                bid = randomBid().copy(amount = EthUInt256.of(2))
                            )
                        ),
                        createAuctionLogEvent(
                            randomBidPlaced().copy(
                                hash = hash,
                                bidValue = BigDecimal.valueOf(1),
                                bid = randomBid().copy(amount = EthUInt256.of(1))
                            )
                        )
                    )
                )
            }
        )
    }

    @Test
    fun `should find auctions by id`() = runBlocking<Unit> {
        val auction = randomAuction()
        saveAuction(auction)

        val result = auctionClient.getAuctionByHash(auction.hash.prefixed()).awaitFirst()
        checkAuctionDto(result, auction)
    }

    @ParameterizedTest
    @MethodSource("auctions")
    fun `should find auctions with continuation`(
        fetchParams: FetchParams,
        fetchMethod: FetchMethod,
        auctions: List<Auction>,
        otherAuctions: List<Auction>
    ) = runBlocking<Unit> {
        saveAuction(*(auctions + otherAuctions).shuffled().toTypedArray())

        Wait.waitAssert {
            val allAuctions = mutableListOf<AuctionDto>()

            var continuation: String? = null
            do {
                val result = fetchMethod.fetch(auctionClient, fetchParams, 2, continuation)
                assertThat(result.auctions).hasSizeLessThanOrEqualTo(2)

                allAuctions.addAll(result.auctions)
                continuation = result.continuation
            } while (continuation != null)

            assertThat(allAuctions).hasSize(auctions.size)

            allAuctions.forEachIndexed { index, orderDto ->
                checkAuctionDto(orderDto, auctions[index])
            }
        }
    }

    @ParameterizedTest
    @MethodSource("auctions")
    fun `should find auctions`(
        fetchParams: FetchParams,
        fetchMethod: FetchMethod,
        auctions: List<Auction>,
        otherAuctions: List<Auction>
    ) = runBlocking<Unit> {
        saveAuction(*(auctions + otherAuctions).shuffled().toTypedArray())

        Wait.waitAssert {
            val result = fetchMethod.fetch(auctionClient, fetchParams, auctions.size, null)
            assertThat(result.auctions).hasSize(auctions.size)

            result.auctions.forEachIndexed { index, orderDto ->
                checkAuctionDto(orderDto, auctions[index])
            }
        }
    }

    @Test
    fun `should get auctions by ids`() = runBlocking<Unit> {
        val auction1 = randomAuction()
        val auction2 = randomAuction()
        saveAuction(auction1, auction2)

        val auctions = auctionClient.getAuctionsByIds(AuctionIdsDto(listOf(auction1.hash, auction2.hash))).collectList()
            .awaitFirst()

        assertThat(auctions).hasSize(2)
        assertThat(auctions).anySatisfy { assertThat(it.hash).isEqualTo(auction1.hash) }
        assertThat(auctions).anySatisfy { assertThat(it.hash).isEqualTo(auction2.hash) }
    }

    @ParameterizedTest
    @MethodSource("auctionBids")
    fun `should find auctions bids`(
        contract: Address,
        auctionId: EthUInt256,
        eventLogs: List<LogEvent>
    ) = runBlocking<Unit> {
        val auction = randomAuction().copy(contract = contract, auctionId = auctionId)
        saveAuction(auction)
        saveHistory(eventLogs)

        Wait.waitAssert {
            val result = auctionClient.getAuctionBidsByHash(auction.hash.prefixed(), null, eventLogs.size).awaitFirst()

            result.bids.forEachIndexed { index, orderDto ->
                checkAuctionBidDto(orderDto, eventLogs[index].data as BidPlaced)
            }
        }
    }

    @Test
    fun `should find active and historical bids`() = runBlocking<Unit> {
        val contract = randomAddress()
        val auctionId = EthUInt256.ONE
        val bid1 = randomBid().copy(amount = EthUInt256.of(5))
        val bid2 = randomBid().copy(amount = EthUInt256.of(10))
        val hash = Auction.raribleV1HashKey(contract, auctionId)

        val auction = randomAuction().copy(contract = contract, auctionId = auctionId, lastBid = bid2)
        saveAuction(auction)
        saveHistory(
            listOf(
                createAuctionLogEvent(
                    randomBidPlaced().copy(
                        hash = hash,
                        bidValue = BigDecimal.valueOf(5),
                        bid = bid1
                    )
                ),
                createAuctionLogEvent(
                    randomBidPlaced().copy(
                        hash = hash,
                        bidValue = BigDecimal.valueOf(10),
                        bid = bid2
                    )
                )
            )
        )

        Wait.waitAssert {
            val result = auctionClient.getAuctionBidsByHash(hash.prefixed(), null, 2).awaitFirst()

            assertThat(result.bids[0].status).isEqualTo(AuctionBidDto.Status.ACTIVE)
            assertThat(result.bids[0].date).isNotNull()

            assertThat(result.bids[1].status).isEqualTo(AuctionBidDto.Status.HISTORICAL)
            assertThat(result.bids[1].date).isNotNull()
        }
    }

    private fun checkAuctionDto(auctionDto: AuctionDto, auction: Auction) {
        assertThat(auctionDto.hash).isEqualTo(auction.hash)
    }

    private fun checkAuctionBidDto(auctionBidDto: AuctionBidDto, bidPlaced: BidPlaced) {
        assertThat(auctionBidDto.buyer).isEqualTo(bidPlaced.buyer)
        assertThat(auctionBidDto.date).isNotNull
    }

    private suspend fun saveAuction(vararg auction: Auction) {
        auction.forEach { auctionRepository.save(it) }
    }

    private suspend fun saveHistory(history: List<LogEvent>) {
        history.forEach { auctionHistoryRepository.save(it).awaitFirst() }
    }

    data class FetchParams(
        val contract: String? = null,
        val tokenId: String? = null,
        val seller: String? = null,
        val sort: AuctionSortDto? = null,
        val origin: String? = null,
        val status: List<AuctionStatusDto>? = null,
        val currencyId: String? = null,
        val platform: PlatformDto? = null
    )
}
