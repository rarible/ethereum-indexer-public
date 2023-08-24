package com.rarible.protocol.erc20.listener.listener

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomWord
import com.rarible.protocol.dto.AssetDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.EthActivityEventDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.EventTimeMarksDto
import com.rarible.protocol.dto.OrderActivityCancelListDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.OrderActivityMatchSideDto
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.service.Erc20AllowanceService
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import scalether.domain.Address
import java.math.BigDecimal
import java.math.BigInteger

@ExtendWith(MockitoExtension::class)
internal class OrderActivityEventHandlerTest {
    @InjectMocks
    private lateinit var orderActivityEventHandler: OrderActivityEventHandler

    @Mock
    private lateinit var erc20AllowanceService: Erc20AllowanceService

    @Test
    fun handle() = runBlocking<Unit> {
        orderActivityEventHandler.handle(
            EthActivityEventDto(
                activity = OrderActivityMatchDto(
                    id = "id",
                    blockHash = Word.apply(randomWord()),
                    blockNumber = 1,
                    date = nowMillis(),
                    logIndex = 1,
                    price = BigDecimal.ONE,
                    transactionHash = Word.apply(randomWord()),
                    source = OrderActivityDto.Source.RARIBLE,
                    type = OrderActivityMatchDto.Type.ACCEPT_BID,
                    left = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.ONE(),
                        asset = AssetDto(
                            assetType = Erc20AssetTypeDto(
                                contract = Address.TWO(),
                            ),
                            value = BigInteger.ONE,
                        ),
                        type = OrderActivityMatchSideDto.Type.BID,
                    ),
                    right = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.THREE(),
                        asset = AssetDto(
                            assetType = Erc721AssetTypeDto(
                                contract = Address.FOUR(),
                                tokenId = BigInteger.ONE,
                            ),
                            value = BigInteger.ONE,
                        ),
                        type = OrderActivityMatchSideDto.Type.SELL,
                    )

                ),
                eventTimeMarks = EventTimeMarksDto("source", marks = emptyList())
            )
        )

        verify(erc20AllowanceService).onChainUpdate(
            balanceId = eq(
                BalanceId(
                    owner = Address.ONE(),
                    token = Address.TWO()
                )
            ),
            eventTimeMarks = argThat {
                source == "source" && marks.size == 1 && marks[0].name == "indexer-in_erc20"
            },
            event = isNull(),
        )
    }

    @Test
    fun `skip BLUR`() = runBlocking<Unit> {
        orderActivityEventHandler.handle(
            EthActivityEventDto(
                activity = OrderActivityMatchDto(
                    id = "id",
                    blockHash = Word.apply(randomWord()),
                    blockNumber = 1,
                    date = nowMillis(),
                    logIndex = 1,
                    price = BigDecimal.ONE,
                    transactionHash = Word.apply(randomWord()),
                    source = OrderActivityDto.Source.BLUR,
                    type = OrderActivityMatchDto.Type.ACCEPT_BID,
                    left = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.ONE(),
                        asset = AssetDto(
                            assetType = Erc20AssetTypeDto(
                                contract = Address.TWO(),
                            ),
                            value = BigInteger.ONE,
                        ),
                        type = OrderActivityMatchSideDto.Type.BID,
                    ),
                    right = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.THREE(),
                        asset = AssetDto(
                            assetType = Erc721AssetTypeDto(
                                contract = Address.FOUR(),
                                tokenId = BigInteger.ONE,
                            ),
                            value = BigInteger.ONE,
                        ),
                        type = OrderActivityMatchSideDto.Type.SELL,
                    )

                ),
                eventTimeMarks = EventTimeMarksDto("source", marks = emptyList())
            )
        )
        verifyNoInteractions(erc20AllowanceService)
    }

    @Test
    fun `handle not ACCEPT_BID`() = runBlocking<Unit> {
        orderActivityEventHandler.handle(
            EthActivityEventDto(
                activity = OrderActivityMatchDto(
                    id = "id",
                    blockHash = Word.apply(randomWord()),
                    blockNumber = 1,
                    date = nowMillis(),
                    logIndex = 1,
                    price = BigDecimal.ONE,
                    transactionHash = Word.apply(randomWord()),
                    source = OrderActivityDto.Source.RARIBLE,
                    type = OrderActivityMatchDto.Type.SELL,
                    right = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.ONE(),
                        asset = AssetDto(
                            assetType = Erc20AssetTypeDto(
                                contract = Address.TWO(),
                            ),
                            value = BigInteger.ONE,
                        )
                    ),
                    left = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.THREE(),
                        asset = AssetDto(
                            assetType = Erc721AssetTypeDto(
                                contract = Address.FOUR(),
                                tokenId = BigInteger.ONE,
                            ),
                            value = BigInteger.ONE,
                        )
                    )

                ),
                eventTimeMarks = EventTimeMarksDto("source", marks = emptyList())
            )
        )

        verifyNoInteractions(erc20AllowanceService)
    }

    @Test
    fun `handle not match`() = runBlocking<Unit> {
        orderActivityEventHandler.handle(
            EthActivityEventDto(
                activity = OrderActivityCancelListDto(
                    id = "id",
                    blockHash = Word.apply(randomWord()),
                    blockNumber = 1,
                    date = nowMillis(),
                    logIndex = 1,
                    transactionHash = Word.apply(randomWord()),
                    source = OrderActivityDto.Source.RARIBLE,
                    take = Erc20AssetTypeDto(
                        contract = Address.TWO(),
                    ),
                    make = Erc721AssetTypeDto(
                        contract = Address.FOUR(),
                        tokenId = BigInteger.ONE,
                    ),
                    maker = Address.ONE(),
                    hash = Word.apply(randomWord())
                ),
                eventTimeMarks = EventTimeMarksDto("source", marks = emptyList())
            )
        )

        verifyNoInteractions(erc20AllowanceService)
    }

    @Test
    fun `handle not erc20`() = runBlocking<Unit> {
        orderActivityEventHandler.handle(
            EthActivityEventDto(
                activity = OrderActivityMatchDto(
                    id = "id",
                    blockHash = Word.apply(randomWord()),
                    blockNumber = 1,
                    date = nowMillis(),
                    logIndex = 1,
                    price = BigDecimal.ONE,
                    transactionHash = Word.apply(randomWord()),
                    source = OrderActivityDto.Source.RARIBLE,
                    type = OrderActivityMatchDto.Type.ACCEPT_BID,
                    left = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.ONE(),
                        asset = AssetDto(
                            assetType = EthAssetTypeDto(),
                            value = BigInteger.ONE,
                        )
                    ),
                    right = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.THREE(),
                        asset = AssetDto(
                            assetType = Erc721AssetTypeDto(
                                contract = Address.FOUR(),
                                tokenId = BigInteger.ONE,
                            ),
                            value = BigInteger.ONE,
                        )
                    )

                ),
                eventTimeMarks = EventTimeMarksDto("source", marks = emptyList())
            )
        )

        verifyNoInteractions(erc20AllowanceService)
    }

    @Test
    fun `handle switch left right`() = runBlocking<Unit> {
        orderActivityEventHandler.handle(
            EthActivityEventDto(
                activity = OrderActivityMatchDto(
                    id = "id",
                    blockHash = Word.apply(randomWord()),
                    blockNumber = 1,
                    date = nowMillis(),
                    logIndex = 1,
                    price = BigDecimal.ONE,
                    transactionHash = Word.apply(randomWord()),
                    source = OrderActivityDto.Source.RARIBLE,
                    type = OrderActivityMatchDto.Type.ACCEPT_BID,
                    right = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.THREE(),
                        asset = AssetDto(
                            assetType = Erc20AssetTypeDto(
                                contract = Address.FOUR(),
                            ),
                            value = BigInteger.ONE,
                        ),
                        type = OrderActivityMatchSideDto.Type.BID,
                    ),
                    left = OrderActivityMatchSideDto(
                        hash = Word.apply(randomWord()),
                        maker = Address.ONE(),
                        asset = AssetDto(
                            assetType = Erc721AssetTypeDto(
                                contract = Address.TWO(),
                                tokenId = BigInteger.ONE,
                            ),
                            value = BigInteger.ONE,
                        ),
                        type = OrderActivityMatchSideDto.Type.SELL,
                    )

                ),
                eventTimeMarks = EventTimeMarksDto("source", marks = emptyList())
            )
        )

        verify(erc20AllowanceService).onChainUpdate(
            balanceId = eq(
                BalanceId(
                    owner = Address.THREE(),
                    token = Address.FOUR()
                )
            ),
            eventTimeMarks = argThat {
                source == "source" && marks.size == 1 && marks[0].name == "indexer-in_erc20"
            },
            event = isNull(),
        )
    }
}
