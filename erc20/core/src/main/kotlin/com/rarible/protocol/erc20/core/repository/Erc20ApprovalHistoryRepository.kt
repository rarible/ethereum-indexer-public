package com.rarible.protocol.erc20.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.common.mapNotNull
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.protocol.erc20.core.model.Erc20HistoryLog
import com.rarible.protocol.erc20.core.model.Erc20TokenApproval
import com.rarible.protocol.erc20.core.model.Erc20TokenApprovalHistoryLog
import com.rarible.protocol.erc20.core.model.Erc20TokenHistory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import scalether.domain.Address

@Component
@CaptureSpan(type = "db", subtype = "erc20-approval-history")
class Erc20ApprovalHistoryRepository(
    private val template: ReactiveMongoTemplate
) {
    fun save(event: LogEvent): Mono<LogEvent> {
        return template.save(event, COLLECTION)
    }

    fun findOwnerLogEvents(token: Address?, owner: Address?): Flux<Erc20HistoryLog> {
        val criteria = Criteria()
            .let { if (token != null) it.and("${LogEvent::data.name}.${Erc20TokenHistory::token.name}").isEqualTo(token) else it }
            .let { if (owner != null) it.and("${LogEvent::data.name}.${Erc20TokenHistory::owner.name}").isEqualTo(owner) else it }

        val query = Query(criteria).with(LOG_SORT_ASC)
        return template
            .find(query, LogEvent::class.java, COLLECTION)
            .mapNotNull {
                when (val logData = it.data) {
                    is Erc20TokenApproval -> Erc20TokenApprovalHistoryLog(it, logData)
                    else -> null
                }
            }
    }

    companion object {
        const val COLLECTION = "erc20_approval_history"

        val LOG_SORT_ASC: Sort = Sort
            .by(
                "${LogEvent::data.name}.${Erc20TokenHistory::token.name}",
                "${LogEvent::data.name}.${Erc20TokenHistory::owner.name}",
                LogEvent::blockNumber.name,
                LogEvent::logIndex.name
            )
    }
}

