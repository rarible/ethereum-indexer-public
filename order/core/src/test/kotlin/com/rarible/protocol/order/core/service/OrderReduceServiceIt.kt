package com.rarible.protocol.order.core.service

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.order.core.data.createLogEvent
import com.rarible.protocol.order.core.data.createOrderBasicSeaportDataV1
import com.rarible.protocol.order.core.data.createOrderCancel
import com.rarible.protocol.order.core.data.createOrderDataLegacy
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV1
import com.rarible.protocol.order.core.data.createOrderLooksrareDataV2
import com.rarible.protocol.order.core.data.createOrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.data.createOrderRaribleV1DataV3Sell
import com.rarible.protocol.order.core.data.createOrderSideMatch
import com.rarible.protocol.order.core.data.createOrderSudoSwapAmmDataV1
import com.rarible.protocol.order.core.data.createOrderVersion
import com.rarible.protocol.order.core.data.createOrderX2Y2DataV1
import com.rarible.protocol.order.core.data.createSudoSwapPoolDataV1
import com.rarible.protocol.order.core.data.randomApproveHistory
import com.rarible.protocol.order.core.data.randomErc1155
import com.rarible.protocol.order.core.data.randomErc20
import com.rarible.protocol.order.core.data.randomErc721
import com.rarible.protocol.order.core.data.randomOrder
import com.rarible.protocol.order.core.data.randomPoolNftWithdraw
import com.rarible.protocol.order.core.data.randomSellOnChainAmmOrder
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.model.OrderCryptoPunksData
import com.rarible.protocol.order.core.model.OrderExchangeHistory
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderState
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.PoolHistory
import com.rarible.protocol.order.core.model.token
import com.rarible.protocol.order.core.service.curve.PoolCurve.Companion.eth
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.types.ObjectId
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration

@IntegrationTest
class OrderReduceServiceIt : AbstractIntegrationTest() {

    @Test
    fun `should calculate order for existed order`() = runBlocking<Unit> {
        val order = createOrderVersion()
        orderUpdateService.save(order)

        val sideMatchDate1 = nowMillis() + Duration.ofHours(2)
        val sideMatchDate2 = nowMillis() + Duration.ofHours(1)
        val cancelDate = nowMillis() + Duration.ofHours(3)

        prepareStorage(
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.of(1),
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                date = sideMatchDate1,
                source = HistorySource.RARIBLE
            ),
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.of(2),
                maker = order.maker,
                make = order.make,
                take = order.take,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                date = sideMatchDate2,
                source = HistorySource.RARIBLE
            ),
            OrderCancel(
                hash = order.hash,
                maker = order.maker,
                make = order.make,
                take = order.take,
                date = cancelDate,
                source = HistorySource.RARIBLE
            )
        )
        val result = orderReduceService.updateOrder(order.hash)!!

        assertThat(result.fill).isEqualTo(EthUInt256.of(3))
        assertThat(result.cancelled).isEqualTo(true)
        assertThat(result.lastUpdateAt).isEqualTo(cancelDate)
    }

    @Test
    fun `should not change order lastUpdateAt if reduce past events`() = runBlocking<Unit> {
        val orderVersion = createOrderVersion()
        val orderCreatedAt = orderVersion.createdAt

        orderUpdateService.save(orderVersion)

        prepareStorage(
            OrderCancel(
                hash = orderVersion.hash,
                maker = orderVersion.maker,
                make = orderVersion.make,
                take = orderVersion.take,
                date = orderVersion.createdAt - Duration.ofHours(1),
                source = HistorySource.RARIBLE
            )
        )
        orderReduceService.updateOrder(orderVersion.hash)

        val updatedOrder = orderRepository.findById(orderVersion.hash)
        assertThat(updatedOrder?.cancelled).isEqualTo(true)
        assertThat(updatedOrder?.lastUpdateAt).isEqualTo(orderCreatedAt)
    }

    @Test
    fun `should not change lastEventId with only orderVersions`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())

        prepareStorage(
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash)
        )

        val order = orderReduceService.updateOrder(hash)!!
        assertThat(order.lastEventId).isNotNull()

        val recalculatedOrder = orderReduceService.updateOrder(hash)!!
        assertThat(recalculatedOrder.lastEventId).isEqualTo(order.lastEventId)
    }

    @Test
    fun `should not change lastEventId with orderVersions and logEvents`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())

        prepareStorage(
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash)
        )
        prepareStorage(
            createOrderSideMatch().copy(hash = hash),
            createOrderCancel().copy(hash = hash)
        )

        val order = orderReduceService.updateOrder(hash)!!
        assertThat(order.lastEventId).isNotNull()

        val recalculatedOrder = orderReduceService.updateOrder(hash)!!
        assertThat(recalculatedOrder.lastEventId).isEqualTo(order.lastEventId)
    }

    @Test
    fun `should change lastEventId if a new event come`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())

        prepareStorage(
            createOrderVersion().copy(hash = hash)
        )
        prepareStorage(
            createOrderSideMatch().copy(hash = hash)
        )

        val order = orderReduceService.updateOrder(hash)!!
        assertThat(order.lastEventId).isNotNull()

        prepareStorage(
            createOrderCancel().copy(hash = hash)
        )

        val updatedOrder = orderReduceService.updateOrder(hash)!!
        assertThat(updatedOrder.lastEventId).isNotNull()
        assertThat(updatedOrder.lastEventId).isNotEqualTo(order.lastEventId)
    }

    @Test
    internal fun `take of order version was updated`() = runBlocking<Unit> {
        val make = Asset(Erc721AssetType(randomAddress(), EthUInt256.of(42)), EthUInt256.ONE)
        val take = Asset(Erc20AssetType(randomAddress()), EthUInt256.of(10))
        val orderVersion = createOrderVersion().copy(make = make, take = take)
        val hash = orderVersion.hash
        val saved = orderUpdateService.save(orderVersion)
        assertThat(saved.take.value).isEqualTo(take.value)
        assertThat(orderRepository.findById(hash)?.take?.value).isEqualTo(take.value)
        val newTakeValue = EthUInt256.Companion.of(5)
        val updated = orderUpdateService.save(
            orderVersion.copy(
                take = orderVersion.take.copy(value = newTakeValue)
            )
        )
        assertThat(updated.take.value).isEqualTo(newTakeValue)
        assertThat(orderRepository.findById(hash)?.take?.value).isEqualTo(newTakeValue)
    }

    @Test
    internal fun `should cancel OpenSea order if nonces are not matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderOpenSeaV1DataV1().copy(nonce = 0)
        val orderVersion = createOrderVersion().copy(
            type = OrderType.OPEN_SEA_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = nonce,
                address = data.exchange,
                topic = Word.apply(randomWord()),
                transactionHash = randomWord(),
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(nonce.date)
    }

    @Test
    internal fun `should cancel Seaport order if counters are not matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderBasicSeaportDataV1().copy(
            counterHex = EthUInt256.ZERO,
            counter = 0L
        )
        val orderVersion = createOrderVersion().copy(
            type = OrderType.SEAPORT_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = nonce,
                address = data.protocol,
                topic = Word.apply(randomWord()),
                transactionHash = randomWord(),
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(nonce.date)
    }

    @Test
    @Deprecated("Remove with OpenSea order specific code")
    internal fun `should not cancel OpenSea order if nonces are matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderOpenSeaV1DataV1().copy(nonce = 1)
        val orderVersion = createOrderVersion().copy(
            type = OrderType.OPEN_SEA_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = nonce,
                address = data.exchange,
                topic = Word.apply(randomWord()),
                transactionHash = randomWord(),
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(orderVersion.createdAt)
    }

    @Test
    internal fun `should not cancel Seaport order if counter are matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderBasicSeaportDataV1().copy(
            counterHex = EthUInt256.ONE,
            counter = 1L
        )
        val orderVersion = createOrderVersion().copy(
            type = OrderType.SEAPORT_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = nonce,
                address = data.protocol,
                topic = Word.apply(randomWord()),
                transactionHash = randomWord(),
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isNotEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(orderVersion.createdAt)
    }

    @Test
    internal fun `should  cancel Seaport order if price are so small`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderBasicSeaportDataV1().copy(
            counterHex = EthUInt256.ONE,
            counter = 1L
        )
        val orderVersion = createOrderVersion().copy(
            type = OrderType.SEAPORT_V1,
            data = data,
            createdAt = now,
            makePrice = BigDecimal.valueOf(1, 18)
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            ReversedEthereumLogRecord(
                id = ObjectId().toHexString(),
                data = nonce,
                address = data.protocol,
                topic = Word.apply(randomWord()),
                transactionHash = randomWord(),
                status = EthereumBlockStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now,
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(orderVersion.createdAt)
    }

    @Test
    internal fun `should cancel OpenSea order as it turned off on prod`() = runBlocking<Unit> {
        val now = nowMillis()
        val orderVersion = createOrderVersion().copy(
            data = createOrderOpenSeaV1DataV1(),
            type = OrderType.OPEN_SEA_V1,
            createdAt = now
        )
        val savedOrder = orderUpdateService.save(orderVersion)
        assertThat(savedOrder.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    internal fun `should not cancel order except OpenSea`() = runBlocking<Unit> {
        OrderType.values().filter { it != OrderType.OPEN_SEA_V1 }.forEach {
            val orderVersion = createOrderVersion().copy(
                type = it,
                data = when (it) {
                    OrderType.RARIBLE_V1 -> createOrderDataLegacy()
                    OrderType.RARIBLE_V2 -> createOrderRaribleV1DataV3Sell()
                    OrderType.SEAPORT_V1 -> createOrderBasicSeaportDataV1().copy(
                        counterHex = EthUInt256.ZERO,
                        counter = 0L
                    )
                    OrderType.CRYPTO_PUNKS -> OrderCryptoPunksData
                    OrderType.LOOKSRARE -> createOrderLooksrareDataV1()
                    OrderType.LOOKSRARE_V2 -> createOrderLooksrareDataV2()
                    OrderType.X2Y2 -> createOrderX2Y2DataV1()
                    OrderType.AMM -> createOrderSudoSwapAmmDataV1()
                    OrderType.OPEN_SEA_V1 -> throw IllegalArgumentException("Illegal order data for this test")
                },
                createdAt = nowMillis()
            )
            val savedOrder = orderUpdateService.save(orderVersion)
            assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
    }

    @Test
    fun `should set FILLED status for LooksRare erc721 sell order`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            hash = Word.apply(randomWord()),
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1(),
            make = randomErc721(),
            take = randomErc20(EthUInt256.TEN)
        )
        orderUpdateService.save(order)

        prepareStorage(
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.ONE,
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                source = HistorySource.LOOKSRARE
            )
        )
        val result = orderReduceService.updateOrder(order.hash)!!
        assertThat(result.fill).isEqualTo(EthUInt256.ONE)
        assertThat(result.status).isEqualTo(OrderStatus.FILLED)
    }

    @Test
    fun `should set FILLED status for LooksRare erc1155 sell order`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            hash = Word.apply(randomWord()),
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1(),
            make = randomErc1155(EthUInt256.ONE),
            take = randomErc20(EthUInt256.TEN)
        )
        orderUpdateService.save(order)

        prepareStorage(
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.ONE,
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                source = HistorySource.LOOKSRARE
            )
        )
        val result = orderReduceService.updateOrder(order.hash)!!
        assertThat(result.fill).isEqualTo(EthUInt256.ONE)
        assertThat(result.status).isEqualTo(OrderStatus.FILLED)
    }

    @Test
    fun `should create amm sell order from OnChainAmmOrder`() = runBlocking<Unit> {
        val poolData = createSudoSwapPoolDataV1()
            .copy(
                bondingCurve = sudoSwapAddresses.linearCurveV1,
                spotPrice = BigInteger("1").eth(),
                delta = BigInteger("3").eth(),
                fee = BigInteger.ZERO
            )
        val onChainAmmOrder = randomSellOnChainAmmOrder(poolData)
            .copy(currency = Address.ZERO(), source = HistorySource.SUDOSWAP)

        prepareStorage(onChainAmmOrder)
        val result = orderReduceService.updateOrder(onChainAmmOrder.hash)!!
        assertThat(result.hash).isEqualTo(onChainAmmOrder.hash)
        assertThat(result.fill).isEqualTo(EthUInt256.ZERO)
        assertThat(result.status).isEqualTo(OrderStatus.ACTIVE)
        assertThat(result.make.value).isEqualTo(EthUInt256.ONE)
        assertThat(result.makeStock).isEqualTo(EthUInt256.of(onChainAmmOrder.tokenIds.size))
        //With liner curve price should be 'spotPrice + delta' (all fee are zero)
        assertThat(result.makePrice).isEqualTo(BigDecimal("4.000000000000000000"))
    }

    @Test
    fun `should reduce amm sell order from several events`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())
        val poolData = createSudoSwapPoolDataV1()
            .copy(
                bondingCurve = sudoSwapAddresses.linearCurveV1,
                spotPrice = BigInteger("1").eth(),
                delta = BigInteger("3").eth(),
                fee = BigInteger.ZERO,
            )
        val createPool = randomSellOnChainAmmOrder(poolData).copy(hash = hash, currency = Address.ZERO())
        val nftOut = randomPoolNftWithdraw().copy(hash = hash, collection = createPool.collection, tokenIds = createPool.tokenIds)

        prepareStorage(createPool, nftOut)
        val result = orderReduceService.updateOrder(hash)!!
        assertThat(result.hash).isEqualTo(hash)
        assertThat(result.make.value).isEqualTo(EthUInt256.ONE)
        assertThat(result.makeStock).isEqualTo(EthUInt256.ZERO)
        assertThat(result.status).isEqualTo(OrderStatus.INACTIVE)
    }

    @Test
    fun `should remove amm order if history reverted`() = runBlocking<Unit> {
        val onChainAmmOrder = randomSellOnChainAmmOrder()
        orderRepository.save(randomOrder().copy(id = Order.Id(onChainAmmOrder.hash), hash = onChainAmmOrder.hash))
        prepareStorage(EthereumBlockStatus.REVERTED, onChainAmmOrder)

        val result = orderReduceService.updateOrder(onChainAmmOrder.hash)!!

        assertThat(result.hash).isEqualTo(OrderReduceService.EMPTY_ORDER_HASH)
        assertThat(orderRepository.findById(onChainAmmOrder.hash)).isNull()
    }

    @Test
    fun `should apply state as final order state`() = runBlocking<Unit> {
        val order = createOrderVersion()
        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isNotEqualTo(OrderStatus.CANCELLED)
        orderStateRepository.save(OrderState(saved.hash, canceled = true))
        val updated = orderReduceService.updateOrder(order.hash)
        assertThat(updated?.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `should make sell order inactive if no approval`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(make = randomErc721(), take = randomErc20(), platform = Platform.RARIBLE)

        val approvalTrue = randomApproveHistory(
            collection = order.make.type.token,
            owner = order.maker,
            operator = transferProxyAddresses.transferProxy,
            approved = true
        )
        approvalHistoryRepository.save(createLogEvent(approvalTrue, blockNumber = 1))

        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isEqualTo(OrderStatus.ACTIVE)

        val approvalFalse = randomApproveHistory(
            collection = order.make.type.token,
            owner = order.maker,
            operator = transferProxyAddresses.transferProxy,
            approved = false
        )
        approvalHistoryRepository.save(createLogEvent(approvalFalse, blockNumber = 2))

        val updated = orderReduceService.updateOrder(order.hash)
        assertThat(updated?.status).isEqualTo(OrderStatus.INACTIVE)
        assertThat(updated?.approved).isFalse
    }

    @Test
    fun `should use default approval from orderVersion`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            make = randomErc721(),
            take = randomErc20(),
            platform = Platform.RARIBLE,
            approved = false
        )
        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isEqualTo(OrderStatus.INACTIVE)
    }

    @Test
    fun `should become active after getting approval`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            make = randomErc721(), take = randomErc20(), platform = Platform.RARIBLE,
            approved = false)
        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isEqualTo(OrderStatus.INACTIVE)

        // we received approval after the order creation
        val approvalTrue = randomApproveHistory(
            collection = order.make.type.token,
            owner = order.maker,
            operator = transferProxyAddresses.transferProxy,
            approved = true
        )
        approvalHistoryRepository.save(createLogEvent(approvalTrue))

        val updated = orderReduceService.updateOrder(order.hash)
        assertThat(updated?.status).isEqualTo(OrderStatus.ACTIVE)
    }

    @Test
    fun `should cancel inactive order without end date`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            make = randomErc721(),
            take = randomErc20(),
            platform = Platform.RARIBLE,
            approved = false,
            end = null,
        )
        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    fun `should not cancel active order without end date`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            make = randomErc721(),
            take = randomErc20(),
            platform = Platform.RARIBLE,
            approved = true,
            end = null,
        )
        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isEqualTo(OrderStatus.ACTIVE)
    }

    @Test
    fun `should cancel active bid without end date`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            make = randomErc20(),
            take = randomErc721(),
            platform = Platform.RARIBLE,
            approved = true,
            end = null,
        )
        val saved = orderUpdateService.save(order)
        assertThat(saved.status).isEqualTo(OrderStatus.CANCELLED)
    }

    private suspend fun prepareStorage(status: EthereumBlockStatus, vararg histories: OrderExchangeHistory) {
        histories.forEachIndexed { index, history ->
            exchangeHistoryRepository.save(
                ReversedEthereumLogRecord(
                    id = ObjectId().toHexString(),
                    address = AddressFactory.create(),
                    topic = Word.apply(randomWord()),
                    transactionHash = randomWord(),
                    status = status,
                    blockNumber = 1,
                    logIndex = 0,
                    minorLogIndex = 0,
                    index = index,
                    createdAt = history.date,
                    updatedAt = history.date,
                    data = history,
                )
            ).awaitFirst()
        }
    }

    private suspend fun prepareStorage(status: EthereumBlockStatus, vararg histories: PoolHistory) {
        histories.forEachIndexed { index, history ->
            poolHistoryRepository.save(
                ReversedEthereumLogRecord(
                    id = ObjectId().toHexString(),
                    data = history,
                    address = AddressFactory.create(),
                    topic = Word.apply(randomWord()),
                    transactionHash = randomWord(),
                    status = status,
                    blockNumber = index.toLong(),
                    logIndex = 0,
                    minorLogIndex = 0,
                    index = index,
                    createdAt = history.date,
                    updatedAt = history.date
                )
            ).awaitFirst()
        }
    }

    private suspend fun prepareStorage(vararg histories: OrderExchangeHistory) {
        prepareStorage(EthereumBlockStatus.CONFIRMED, *histories)
    }

    private suspend fun prepareStorage(vararg histories: PoolHistory) {
        prepareStorage(EthereumBlockStatus.CONFIRMED, *histories)
    }

    private suspend fun prepareStorage(vararg orderVersions: OrderVersion) {
        orderVersions.forEach {
            orderVersionRepository.save(it).awaitFirst()
        }
    }
}
