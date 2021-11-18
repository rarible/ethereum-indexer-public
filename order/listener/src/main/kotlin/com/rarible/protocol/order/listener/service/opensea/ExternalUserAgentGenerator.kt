package com.rarible.protocol.order.listener.service.opensea

import com.rarible.opensea.client.agent.UserAgentGenerator
import java.util.*

class ExternalUserAgentGenerator(
    openSeaClientUserAgents: String
) : UserAgentGenerator {
    private val agents = openSeaClientUserAgents.split("#")

    override fun generateUserAgent(): String {
        return agents.random().takeIf { agent -> agent.isNotBlank() } ?: UUID.randomUUID().toString().replace("-", "")
    }
}
