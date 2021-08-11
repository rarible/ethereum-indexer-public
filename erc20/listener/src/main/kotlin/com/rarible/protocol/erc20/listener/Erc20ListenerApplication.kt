package com.rarible.protocol.erc20.listener

import com.rarible.ethereum.monitoring.EnableBlockchainMonitoring
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableBlockchainMonitoring
class Erc20ListenerApplication

fun main(args: Array<String>) {
    runApplication<Erc20ListenerApplication>(*args)
}