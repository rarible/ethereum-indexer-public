package com.rarible.protocol.erc20.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.mapNotNull
import com.rarible.core.mongo.util.div
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.erc20.core.model.*
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.*
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = "db", subtype = "erc20-transfer-history")
class Erc20TransferHistoryRepository(
    private val template: ReactiveMongoTemplate
) {
    fun save(event: LogEvent): Mono<LogEvent> {
        return template.save(event, COLLECTION)
    }

    fun findBalanceLogEvents(
        balanceId: BalanceId?,
        fromBlockNumber: Long?
    ): Flux<LogEvent> {
        val criteria = Criteria()
            .run {
                balanceId?.let {
                    and(LogEvent::data / Erc20TokenHistory::token).isEqualTo(it.token).and(LogEvent::data / Erc20TokenHistory::owner).isEqualTo(it.owner)
                } ?: this
            }
            .run {
                fromBlockNumber?.let {
                    and(LogEvent::blockNumber).gt(it)
                } ?: this
            }

        val query = Query(criteria).with(LOG_SORT_ASC)
        return template.find(query, LogEvent::class.java, COLLECTION)
    }

    fun findOwnerLogEvents(
        token: Address? = null,
        owner: Address? = null,
        from: Wallet? = null
    ): Flux<Erc20HistoryLog> {
        val criteria = when {
            token != null && owner != null -> {
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_OWNER).`is`(owner)
            }
            token != null && from != null -> {
                Criteria.where(DATA_TOKEN).`is`(token).and(DATA_OWNER).gt(from.owner)
            }
            token != null -> {
                Criteria.where(DATA_TOKEN).`is`(token)
            }
            from != null -> {
                Criteria().orOperator(
                    Criteria.where(DATA_TOKEN).`is`(from.token).and(DATA_OWNER).gt(from.owner),
                    Criteria.where(DATA_TOKEN).gt(from.token)
                )
            }
            else -> {
                Criteria()
            }
        }

        val query = Query(criteria).with(LOG_SORT_ASC)
        return template
            .find(query, LogEvent::class.java, COLLECTION)
            .mapNotNull {
                when (val logData = it.data) {
                    is Erc20IncomeTransfer -> Erc20IncomeTransferHistoryLog(it, logData)
                    is Erc20OutcomeTransfer -> Erc20OutcomeTransferHistoryLog(it, logData)
                    is Erc20Deposit -> Erc20DepositHistoryLog(it, logData)
                    is Erc20Withdrawal -> Erc20WithdrawalHistoryLog(it, logData)
                    else -> null
                }
            }
    }

    companion object {
        const val COLLECTION = "erc20_history"

        val DATA_TOKEN = "${LogEvent::data.name}.${Erc20TokenHistory::token.name}"
        val DATA_OWNER = "${LogEvent::data.name}.${Erc20TokenHistory::owner.name}"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                DATA_TOKEN,
                DATA_OWNER,
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name
            )
    }
}

