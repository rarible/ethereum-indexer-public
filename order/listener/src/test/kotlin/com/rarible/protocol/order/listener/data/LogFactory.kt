package com.rarible.protocol.order.listener.data

import com.rarible.core.test.data.randomWord
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.java.Lists
import java.math.BigInteger

fun log(
    topics: List<Word> = listOf(Word.apply(randomWord())),
    data: String = "0x"
) = Log(
    BigInteger.ONE, // logIndex
    BigInteger.TEN, // transactionIndex
    Word.apply(ByteArray(32)), // transactionHash
    Word.apply(ByteArray(32)), // blockHash
    BigInteger.ZERO, // blockNumber
    Address.ZERO(), // address
    Binary.apply( // data
        data
    ),
    false, // removed
    Lists.toScala( // topics
        topics
    ),
    "" // type
)
