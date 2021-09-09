package com.rarible.protocol.nftorder.listener

import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.dto.UnlockableEventDto
import com.rarible.protocol.nftorder.listener.configuration.BatchedConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NftOrderListenerApplication(
    private val itemChangeWorker: ConsumerWorker<NftItemEventDto>,
    private val ownershipChangeWorker: BatchedConsumerWorker<NftOwnershipEventDto>,
    private val unlockableChangeWorker: ConsumerWorker<UnlockableEventDto>,
    private val orderChangeWorker: BatchedConsumerWorker<OrderEventDto>
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        itemChangeWorker.start()
        ownershipChangeWorker.start()
        unlockableChangeWorker.start()
        orderChangeWorker.start()
    }
}

fun main(args: Array<String>) {
    runApplication<NftOrderListenerApplication>(*args)
}