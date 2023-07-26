package com.rarible.protocol.erc20.core

import com.rarible.blockchain.scanner.ethereum.model.EthereumBlockStatus
import com.rarible.blockchain.scanner.ethereum.model.EthereumLog
import com.rarible.blockchain.scanner.ethereum.model.EventData
import com.rarible.blockchain.scanner.ethereum.model.ReversedEthereumLogRecord
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.erc20.core.model.BalanceId
import com.rarible.protocol.erc20.core.model.Erc20Event
import io.daonomic.rpc.domain.Word
import scalether.domain.Address
import java.time.Instant
import java.util.Date

fun randomEthereumLog(
    transactionSender: Address = randomAddress()
): EthereumLog =
    EthereumLog(
        transactionHash = randomWord(),
        status = EthereumBlockStatus.values().random(),
        address = randomAddress(),
        topic = Word.apply(randomWord()),
        blockHash = Word.apply(randomWord()),
        blockNumber = randomLong(),
        logIndex = randomInt(),
        minorLogIndex = randomInt(),
        index = randomInt(),
        from = transactionSender,
        blockTimestamp = nowMillis().epochSecond,
        createdAt = nowMillis()
    )

fun randomReversedEthereumLogRecord(data: EventData): ReversedEthereumLogRecord {
    return ReversedEthereumLogRecord(
        id = randomString(),
        log = randomEthereumLog(),
        data = data
    )
}

fun randomIncomeTransferEvent(): Erc20Event.Erc20IncomeTransferEvent {
    val token = randomAddress()
    val owner = randomAddress()

    return Erc20Event.Erc20IncomeTransferEvent(
        token = token,
        owner = owner,
        entityId = BalanceId(token, owner).stringValue,
        date = Date(randomLong()),
        value = EthUInt256.Companion.of(randomInt()),
        log = randomEthereumLog()
    )
}

fun randomDepositEvent(): Erc20Event.Erc20DepositEvent {
    val token = randomAddress()
    val owner = randomAddress()

    return Erc20Event.Erc20DepositEvent(
        token = token,
        owner = owner,
        entityId = BalanceId(token, owner).stringValue,
        date = Date(randomLong()),
        value = EthUInt256.Companion.of(randomInt()),
        log = randomEthereumLog()
    )
}

fun randomOutcomeTransferEvent(): Erc20Event.Erc20OutcomeTransferEvent {
    val token = randomAddress()
    val owner = randomAddress()

    return Erc20Event.Erc20OutcomeTransferEvent(
        token = token,
        owner = owner,
        entityId = BalanceId(token, owner).stringValue,
        date = Date(randomLong()),
        value = EthUInt256.Companion.of(randomInt()),
        log = randomEthereumLog()
    )
}

fun randomWithdrawalEvent(): Erc20Event.Erc20WithdrawalEvent {
    val token = randomAddress()
    val owner = randomAddress()

    return Erc20Event.Erc20WithdrawalEvent(
        token = token,
        owner = owner,
        entityId = BalanceId(token, owner).stringValue,
        date = Date(randomLong()),
        value = EthUInt256.Companion.of(randomInt()),
        log = randomEthereumLog()
    )
}

fun randomTokenApprovalEvent(): Erc20Event.Erc20TokenApprovalEvent {
    val token = randomAddress()
    val owner = randomAddress()

    return Erc20Event.Erc20TokenApprovalEvent(
        token = token,
        owner = owner,
        entityId = BalanceId(token, owner).stringValue,
        date = Date(randomLong()),
        value = EthUInt256.Companion.of(randomInt()),
        log = randomEthereumLog(),

        spender = randomAddress()
    )
}

fun EthereumLog.withNewValues(
    status: EthereumBlockStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    from: Address? = null,
    index: Int? = null
) = copy(
    status = status ?: this.status,
    createdAt = createdAt ?: this.createdAt,
    blockNumber = blockNumber ?: if (this.blockNumber != null) null else this.blockNumber,
    logIndex = logIndex ?: if (this.logIndex != null) null else this.logIndex,
    index = index ?: this.index,
    minorLogIndex = minorLogIndex ?: this.minorLogIndex,
    address = address ?: this.address,
    from = from ?: this.from
)

fun Erc20Event.Erc20IncomeTransferEvent.withNewValues(
    status: EthereumBlockStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    index: Int? = null,
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex, index = index))

fun Erc20Event.Erc20OutcomeTransferEvent.withNewValues(
    status: EthereumBlockStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    index: Int? = null,
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex, index = index))

fun Erc20Event.Erc20DepositEvent.withNewValues(
    status: EthereumBlockStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    index: Int? = null,
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex, index = index))

fun Erc20Event.Erc20WithdrawalEvent.withNewValues(
    status: EthereumBlockStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    index: Int? = null,
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex, index = index))

fun Erc20Event.Erc20TokenApprovalEvent.withNewValues(
    status: EthereumBlockStatus? = null,
    createdAt: Instant? = null,
    blockNumber: Long? = null,
    logIndex: Int? = null,
    minorLogIndex: Int? = null,
    address: Address? = null,
    index: Int? = null,
) = copy(log = log.withNewValues(status, createdAt, blockNumber, logIndex, minorLogIndex, index = index))
