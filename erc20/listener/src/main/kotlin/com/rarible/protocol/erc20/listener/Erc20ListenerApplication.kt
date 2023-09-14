package com.rarible.protocol.erc20.listener

import com.rarible.core.kafka.KafkaShutdownHook
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class Erc20ListenerApplication(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(Erc20ListenerApplication::class.java)
    app.setRegisterShutdownHook(false)
    val context = app.run(*args)
    Runtime.getRuntime().addShutdownHook(Thread(KafkaShutdownHook(context, context::close)))
}
