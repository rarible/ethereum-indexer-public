package com.rarible.protocol.order.core.model

import com.rarible.ethereum.listener.log.domain.EventData
import scalether.domain.Address

/**
 * History of Approve and ApproveForAll events
 *
 * @property collection     token contract who send event
 * @property owner          collection owner
 * @property operator       who has been permission granted
 * @property approved       permission sign
 */
data class ApprovalHistory(
    val collection: Address,
    val owner: Address,
    val operator: Address,
    val approved: Boolean,
): EventData
