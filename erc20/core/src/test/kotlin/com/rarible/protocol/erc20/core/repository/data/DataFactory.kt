package com.rarible.protocol.erc20.core.repository.data

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Allowance
import com.rarible.protocol.erc20.core.model.Erc20Balance
import com.rarible.protocol.erc20.core.model.Erc20Deposit
import com.rarible.protocol.erc20.core.model.Erc20IncomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20OutcomeTransfer
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import com.rarible.protocol.erc20.core.model.Erc20Withdrawal
import io.daonomic.rpc.domain.Word
import org.bson.types.ObjectId
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
): ReversedEthereumLogRecord {
    return ReversedEthereumLogRecord(
        id = ObjectId().toHexString(),
        data = history,
        address = history.token,
        topic = Word.apply(randomWord()),
        transactionHash = randomWord(),
        status = EthereumBlockStatus.CONFIRMED,
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
    balance: EthUInt256 = EthUInt256.of(randomBigInt()),
    blockNumber: Long? = null
): Erc20Balance {
    return Erc20Balance(
        token = token,
        owner = owner,
        balance = balance,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt,
        revertableEvents = emptyList(),
        blockNumber = blockNumber
    )
}

fun randomAllowance(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    createdAt: Instant = nowMillis(),
    lastUpdatedAt: Instant = nowMillis(),
    allowance: EthUInt256 = EthUInt256.of(randomBigInt()),
): Erc20Allowance {
    return Erc20Allowance(
        token = token,
        owner = owner,
        allowance = allowance,
        createdAt = createdAt,
        lastUpdatedAt = lastUpdatedAt,
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

fun randomErc20TokenApproval(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
    spender: Address = randomAddress(),
    value: BigInteger = BigInteger.ONE,
    date: Instant = nowMillis()
): Erc20TokenApproval {
    return Erc20TokenApproval(
        owner = owner,
        spender = spender,
        token = token,
        date = Date(date.toEpochMilli()),
        value = EthUInt256.of(value)
    )
}

fun randomBalanceId(
    token: Address = randomAddress(),
    owner: Address = randomAddress(),
): BalanceId {
    return BalanceId(token = token, owner = owner)
}
