package com.rarible.protocol.unlockable.api.controller

import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import java.lang.RuntimeException

@RestController
@RequestMapping(
    value = ["/v0.1/errors"],
    produces = [MediaType.APPLICATION_JSON_VALUE]
)
@Validated
class TestErrorController {

    @GetMapping
    fun getError(): Mono<String> {
        throw RuntimeException("Error")
    }
}