package com.rarible.protocol.erc20.listener.test

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.every
import io.mockk.mockk
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import scalether.java.Lists
import java.math.BigInteger

fun log(
    topics: List<Word> = listOf(Word.apply(randomWord())),
    data: String = "0x",
    logIndex: Long = 0,
    transactionHash: Word = Word.apply(randomWord()),
    address: Address = randomAddress()
) = Log(
    BigInteger.valueOf(logIndex), // logIndex
    BigInteger.TEN, // transactionIndex
    transactionHash, // transactionHash
    Word.apply(ByteArray(32)), // blockHash
    BigInteger.ZERO, // blockNumber
    address, // address
    Binary.apply( // data
        data
    ),
    false, // removed
    Lists.toScala( // topics
        topics
    ),
    "" // type
)

fun transaction(from: Address = randomAddress(), to: Address = randomAddress()): Transaction {
    return mockk<Transaction> {
        every { input() } returns Binary.empty()
        every { hash() } returns Word.apply(randomWord())
        every { from() } returns from
        every { to() } returns to
    }
}
