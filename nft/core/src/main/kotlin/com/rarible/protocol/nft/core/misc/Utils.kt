package com.rarible.protocol.nft.core.misc

import com.rarible.core.entity.reducer.chain.ReducersChain
import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.ItemId
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.util.Bytes
import java.lang.RuntimeException
import java.math.BigInteger

fun <T : Any> List<T>.ifNotEmpty(): List<T>? {
    return if (isNotEmpty()) this else null
}

fun <Event, Entity> combineIntoChain(
    vararg reducers: Reducer<Event, Entity>,
): Reducer<Event, Entity> {
    return ReducersChain(reducers.asList())
}

fun trimToLength(str: String, maxLength: Int, suffix: String? = null): String {
    if (str.length < maxLength) {
        return str
    }
    val safeSuffix = suffix ?: ""
    val trimmed = StringBuilder(maxLength + safeSuffix.length)
        .append(str.substring(0, maxLength))
        .append(suffix)

    return trimmed.toString()
}

fun toAddressSet(configStr: String?): Set<Address> {
    return configStr?.split(",")?.mapNotNull {
        val trimmed = it.trim()
        if (trimmed.isNotEmpty()) {
            Address.apply(trimmed)
        } else {
            null
        }
    }?.toSet() ?: emptySet()
}

fun splitToRanges(from: Address, to: Address, count: Int): List<Address> {
    if (count < 1) {
        throw IllegalArgumentException(
            "Can't split interval $from..$to, " +
                "specified range count should be positive, but $count provided"
        )
    }

    if (from == to) {
        return listOf(from)
    }

    val fromInt = from.toBigInteger()
    val toInt = to.toBigInteger()
    val delta = toInt.minus(fromInt).divide(count.toBigInteger())

    if (delta < BigInteger("1")) {
        throw IllegalArgumentException(
            "Can't split interval $from..$to, " +
                "specified range count $count is too big (delta = $delta)"
        )
    }

    val result = ArrayList<Address>(count)
    result.add(from)

    var current = fromInt
    for (i in 1 until count) {
        current = current.plus(delta)
        val bytes = Bytes.trimLeadingZeroes(current.toByteArray())
        val addressBytes = bytes.copyInto(ByteArray(20), 20 - bytes.size)
        result.add(Address.apply(addressBytes))
    }
    result.add(to)
    return result
}

fun splitToRanges(from: ItemId, to: ItemId, count: Int): List<Pair<ItemId, ItemId>> {
    val ranges = splitToRanges(from.token, to.token, count)
    val withoutFirstAndLast = ranges.subList(1, ranges.size - 1)
    var left = from
    val result = ArrayList<Pair<ItemId, ItemId>>(count)
    withoutFirstAndLast.forEach {
        val right = ItemId(it, EthUInt256.ZERO)
        result.add(left to right)
        left = right
    }
    result.add(left to to)
    return result
}

fun <T> applyLog(log: Log, applyMethod: (Log) -> T): T {
    return try {
        applyMethod(log)
    } catch (ex: Throwable) {
        throw RuntimeException("Can't apply log: tx=${log.transactionHash()}, logIndex=${log.logIndex()}", ex)
    }
}
