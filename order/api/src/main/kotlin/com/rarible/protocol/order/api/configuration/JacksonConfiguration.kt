package com.rarible.protocol.order.api.configuration

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.rarible.protocol.order.api.converter.WordDeserializer
import io.daonomic.rpc.domain.Word
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfiguration {

    /**
     * Word has StdDeserializer defined via class annotation, we need to override it.
     */
    @JsonDeserialize(using = WordDeserializer::class)
    class WordDeserializerMixIn

    @Bean
    fun jsonCustomizer(): Jackson2ObjectMapperBuilderCustomizer? = Jackson2ObjectMapperBuilderCustomizer {
        it.mixIn(Word::class.java, WordDeserializerMixIn::class.java)
    }
}
