package com.rarible.protocol.erc20.core.repository.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.math.BigInteger
import java.time.Instant
import java.util.Date

fun randomLogEvent(
    history: Erc20TokenHistory,
    blockNumber: Long = 1,
    logIndex: Int = 0,
    minorLogIndex: Int = 0,
    index: Int = 0
): LogEvent {
    return LogEvent(
        data = history,
        address = history.token,
        topic = Word.apply(randomWord()),
        transactionHash = Word.apply(randomWord()),
        status = LogEventStatus.CONFIRMED,
        blockNumber = blockNumber,
        logIndex = logIndex,
        minorLogIndex = minorLogIndex,
        index = index
    )
}

fun randomBalance(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    createdAt: Instant = nowMillis(),
    lastUpdatedAt: Instant = nowMillis(),
    balance: EthUInt256 = EthUInt256.of(randomBigInt())
): Erc20Balance {
    return Erc20Balance(
        token = token,
        owner = owner,
        balance = balance,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt,
        revertableEvents = emptyList()
    )
}

fun randomErc20OutcomeTransfer(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    value: BigInteger = BigInteger.ONE,
    date: Instant = nowMillis()
): Erc20OutcomeTransfer {
    return Erc20OutcomeTransfer(
        owner = owner,
        token = token,
        date = Date(date.toEpochMilli()),
        value = EthUInt256.of(value)
    )
}

fun randomErc20IncomeTransfer(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    value: BigInteger = BigInteger.ONE,
    date: Instant = nowMillis()
): Erc20IncomeTransfer {
    return Erc20IncomeTransfer(
        owner = owner,
        token = token,
        date = Date(date.toEpochMilli()),
        value = EthUInt256.of(value)
    )
}

fun randomErc20Deposit(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    value: BigInteger = BigInteger.ONE,
    date: Instant = nowMillis()
): Erc20Deposit {
    return Erc20Deposit(
        owner = owner,
        token = token,
        date = Date(date.toEpochMilli()),
        value = EthUInt256.of(value)
    )
}

fun randomErc20Withdrawal(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    value: BigInteger = BigInteger.ONE,
    date: Instant = nowMillis()
): Erc20Withdrawal {
    return Erc20Withdrawal(
        owner = owner,
        token = token,
        date = Date(date.toEpochMilli()),
        value = EthUInt256.of(value)
    )
}