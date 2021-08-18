package com.rarible.protocol.unlockable.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.rarible.protocol.unlockable.api"])
class UnlockableServiceApp

fun main(args: Array<String>) {
    runApplication<UnlockableServiceApp>(*args)
}
