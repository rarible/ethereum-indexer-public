package com.rarible.protocol.order.core.parser

import com.rarible.protocol.contracts.exchange.blur.exchange.v2.BlurExchangeV2
import com.rarible.protocol.contracts.exchange.blur.exchange.v2.Execution721MakerFeePackedEvent
import com.rarible.protocol.contracts.exchange.blur.exchange.v2.Execution721PackedEvent
import com.rarible.protocol.contracts.exchange.blur.exchange.v2.Execution721TakerFeePackedEvent
import com.rarible.protocol.contracts.exchange.blur.exchange.v2.ExecutionEvent
import com.rarible.protocol.order.core.misc.methodSignatureId
import com.rarible.protocol.order.core.model.BlurV2AssetType
import com.rarible.protocol.order.core.model.BlurV2Exchange
import com.rarible.protocol.order.core.model.BlurV2ExecutionEvent
import com.rarible.protocol.order.core.model.BlurV2FeeRate
import com.rarible.protocol.order.core.model.BlurV2Listing
import com.rarible.protocol.order.core.model.BlurV2Order
import com.rarible.protocol.order.core.model.BlurV2OrderType
import com.rarible.protocol.order.core.model.BlurV2Take
import com.rarible.protocol.order.core.model.BlurV2Taker
import com.rarible.protocol.order.core.model.BlurV2Transfer
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scala.Tuple2
import scala.Tuple4
import scala.Tuple8
import scalether.abi.Int32Type
import scalether.domain.Address
import java.math.BigInteger

object BlurV2Parser {
    fun parserBlurV2(input: Binary, tx: Word): BlurV2Take {
        return when (input.methodSignatureId()) {
            BlurExchangeV2.takeAskSignature().id() -> {
                val value = getDecodedValue(BlurExchangeV2.takeAskSignature(), input)
                BlurV2Take(
                    orders = parseBlurV2Order(value._1()._1()),
                    exchanges = parserBlurExchangeV2(value._1()._2()),
                    takerFee = parserBlurV2FeeRate(value._1()._3()),
                    signatures = Binary.apply(value._1()._4()),
                    tokenRecipient = value._1()._5(),
                )
            }
            BlurExchangeV2.takeAskSingleSignature().id() -> {
                val value = getDecodedValue(BlurExchangeV2.takeAskSingleSignature(), input)
                BlurV2Take(
                    orders = parseBlurV2Order(arrayOf(value._1()._1())),
                    exchanges = parserBlurExchangeV2(arrayOf(value._1()._2())),
                    takerFee = parserBlurV2FeeRate(value._1()._3()),
                    signatures = Binary.apply(value._1()._4()),
                    tokenRecipient = value._1()._5(),
                )
            }
            BlurExchangeV2.takeAskPoolSignature().id() -> {
                val value = getDecodedValue(BlurExchangeV2.takeAskPoolSignature(), input)
                BlurV2Take(
                    orders = parseBlurV2Order(value._1()._1()),
                    exchanges = parserBlurExchangeV2(value._1()._2()),
                    takerFee = parserBlurV2FeeRate(value._1()._3()),
                    signatures = Binary.apply(value._1()._4()),
                    tokenRecipient = value._1()._5(),
                )
            }
            BlurExchangeV2.takeAskSinglePoolSignature().id() -> {
                val value = getDecodedValue(BlurExchangeV2.takeAskSinglePoolSignature(), input)
                BlurV2Take(
                    orders = parseBlurV2Order(arrayOf(value._1()._1())),
                    exchanges = parserBlurExchangeV2(arrayOf(value._1()._2())),
                    takerFee = parserBlurV2FeeRate(value._1()._3()),
                    signatures = Binary.apply(value._1()._4()),
                    tokenRecipient = value._1()._5(),
                )
            }
            BlurExchangeV2.takeBidSignature().id() -> {
                val value = getDecodedValue(BlurExchangeV2.takeBidSignature(), input)
                BlurV2Take(
                    orders = parseBlurV2Order(value._1()._1()),
                    exchanges = parserBlurExchangeV2(value._1()._2()),
                    takerFee = parserBlurV2FeeRate(value._1()._3()),
                    signatures = Binary.apply(value._1()._4()),
                )
            }
            BlurExchangeV2.takeBidSingleSignature().id() -> {
                val value = getDecodedValue(BlurExchangeV2.takeBidSingleSignature(), input)
                BlurV2Take(
                    orders = parseBlurV2Order(arrayOf(value._1()._1())),
                    exchanges = parserBlurExchangeV2(arrayOf(value._1()._2())),
                    takerFee = parserBlurV2FeeRate(value._1()._3()),
                    signatures = Binary.apply(value._1()._4()),
                )
            }
            else -> throw IllegalArgumentException("Unsupported method in tx $tx")
        }
    }

    fun parse(event: Execution721TakerFeePackedEvent): BlurV2ExecutionEvent {
        val (tokenId, listingIndex, trader) = unpackPackTokenIdListingIndexTrader(Int32Type.encode(event.tokenIdListingIndexTrader()))
        val (type, price, collection) = unpackTypePriceCollection(Int32Type.encode(event.collectionPriceSide()))
        val (recipient, rate) = unpackFee(Int32Type.encode(event.takerFeeRecipientRate()))

        return BlurV2ExecutionEvent(
            transfer = BlurV2Transfer(
                trader = trader,
                id = tokenId,
                collection = collection,
                amount = BigInteger.ONE,
                assetType = BlurV2AssetType.ERC721,
            ),
            orderHash = Word.apply(event.orderHash()),
            listingIndex = listingIndex,
            price = price,
            takerFee = BlurV2FeeRate(
                recipient = recipient,
                rate = rate,
            ),
            orderType = type,
            makerFee = null,
        )
    }

    fun parse(event: Execution721MakerFeePackedEvent): BlurV2ExecutionEvent {
        val (tokenId, listingIndex, trader) = unpackPackTokenIdListingIndexTrader(Int32Type.encode(event.tokenIdListingIndexTrader()))
        val (type, price, collection) = unpackTypePriceCollection(Int32Type.encode(event.collectionPriceSide()))
        val (recipient, rate) = unpackFee(Int32Type.encode(event.makerFeeRecipientRate()))

        return BlurV2ExecutionEvent(
            transfer = BlurV2Transfer(
                trader = trader,
                id = tokenId,
                collection = collection,
                amount = BigInteger.ONE,
                assetType = BlurV2AssetType.ERC721,
            ),
            orderHash = Word.apply(event.orderHash()),
            listingIndex = listingIndex,
            price = price,
            makerFee = BlurV2FeeRate(
                recipient = recipient,
                rate = rate,
            ),
            orderType = type,
            takerFee = null,
        )
    }

    fun parse(event: ExecutionEvent): BlurV2ExecutionEvent {
        return BlurV2ExecutionEvent(
            transfer = BlurV2Transfer(
                trader = event.transfer()._1(),
                id = event.transfer()._2(),
                amount = event.transfer()._3(),
                collection = event.transfer()._4(),
                assetType = BlurV2AssetType.fromValue(event.transfer()._5()),
            ),
            orderHash = Word.apply(event.orderHash()),
            listingIndex = event.listingIndex(),
            price = event.price(),
            makerFee = BlurV2FeeRate(
                recipient = event.makerFee()._1,
                rate = event.makerFee()._2,
            ),
            takerFee = BlurV2FeeRate(
                recipient = event.fees()._2()._1(),
                rate = event.fees()._2()._2(),
            ),
            orderType = BlurV2OrderType.fromValue(event.orderType())
        )
    }

    fun parse(event: Execution721PackedEvent): BlurV2ExecutionEvent {
        val (tokenId, listingIndex, trader) = unpackPackTokenIdListingIndexTrader(Int32Type.encode(event.tokenIdListingIndexTrader()))
        val (type, price, collection) = unpackTypePriceCollection(Int32Type.encode(event.collectionPriceSide()))

        return BlurV2ExecutionEvent(
            transfer = BlurV2Transfer(
                trader = trader,
                id = tokenId,
                collection = collection,
                amount = BigInteger.ONE,
                assetType = BlurV2AssetType.ERC721,
            ),
            orderHash = Word.apply(event.orderHash()),
            listingIndex = listingIndex,
            price = price,
            orderType = type,
            makerFee = null,
            takerFee = null,
        )
    }

    private fun unpackPackTokenIdListingIndexTrader(packed: Binary): UnpackedTokenIdListingIndexTrader {
        return UnpackedTokenIdListingIndexTrader(
            tokenId = packed.slice(0, 11).toBigInteger(),
            listingIndex = packed.slice(11, 12).toBigInteger(),
            trader = Address.apply(packed.slice(12, 32))
        )
    }

    private fun unpackTypePriceCollection(packed: Binary): UnpackTypePriceCollection {
        return UnpackTypePriceCollection(
            orderType = BlurV2OrderType.fromValue(packed.slice(0, 1).toBigInteger()),
            price = packed.slice(1, 12).toBigInteger(),
            collection = Address.apply(packed.slice(12, 32))
        )
    }


    private fun unpackFee(packed: Binary): UnpackedFee {
        return UnpackedFee(
            rate = packed.slice(0, 19).toBigInteger(),
            recipient = Address.apply(packed.slice(11, 31)),
        )
    }

    private fun parserBlurExchangeV2(value: Array<Tuple4<BigInteger, Array<ByteArray>, Tuple4<BigInteger, BigInteger, BigInteger, BigInteger>, Tuple2<BigInteger, BigInteger>>>): List<BlurV2Exchange> {
        return value.map { exchange ->
            BlurV2Exchange(
                index = exchange._1(),
                listing = BlurV2Listing(
                    index = exchange._3()._1(),
                    tokenId = exchange._3()._2(),
                    amount = exchange._3()._3(),
                    price = exchange._3()._4(),
                ),
                taker = BlurV2Taker(
                    tokenId = exchange._3()._1(),
                    amount = exchange._3()._2(),
                ),
            )
        }
    }

    private fun parseBlurV2Order(value: Array<Tuple8<Address, Address, ByteArray, BigInteger, BigInteger, BigInteger, Tuple2<Address, BigInteger>, BigInteger>>): List<BlurV2Order> {
        return value.map { order ->
            BlurV2Order(
                trader = order._1(),
                collection = order._2(),
                numberOfListings = order._4(),
                expirationTime = order._5(),
                assetType = BlurV2AssetType.fromValue(order._6()),
                makerFee = parserBlurV2FeeRate(order._7()),
                salt = order._8()
            )
        }
    }

    private fun parserBlurV2FeeRate(value: Tuple2<Address, BigInteger>): BlurV2FeeRate {
        return BlurV2FeeRate(
            recipient = value._1(),
            rate = value._2()
        )
    }

    private data class UnpackedTokenIdListingIndexTrader(
        val tokenId: BigInteger,
        val listingIndex: BigInteger,
        val trader: Address
    )

    private data class UnpackTypePriceCollection(
        val orderType: BlurV2OrderType,
        val price: BigInteger,
        val collection: Address
    )

    private data class UnpackedFee(
        val recipient: Address,
        val rate: BigInteger
    )

    private fun <T> getDecodedValue(signature: scalether.abi.Signature<T,*>, input: Binary): T {
        return signature.`in`().decode(input, 4).value()
    }
}