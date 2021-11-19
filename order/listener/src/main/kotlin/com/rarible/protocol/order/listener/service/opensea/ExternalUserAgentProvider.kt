package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.agent.UserAgentProvider
import java.util.*

class ExternalUserAgentProvider(
    openSeaClientUserAgents: String
) : UserAgentProvider {
    private val agents = openSeaClientUserAgents.split("#")

    override fun get(): String {
        return agents.random().takeIf { agent -> agent.isNotBlank() } ?: UUID.randomUUID().toString().replace("-", "")
    }
}
