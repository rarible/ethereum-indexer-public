package com.rarible.protocol.nft.core.service.token.filter

import com.rarible.protocol.nft.core.repository.token.TokenByteCodeRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.runBlocking
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class ScamByteCodeHashCache(private val tokenByteCodeRepository: TokenByteCodeRepository) {
    private val markers = mutableSetOf<Word>()

    @PostConstruct
    @Scheduled(initialDelayString = "PT12H", fixedRateString = "PT12H")
    fun init() = runBlocking {
        markers.addAll(tokenByteCodeRepository.allScamHashes())
    }

    fun add(hash: Word) {
        markers.add(hash)
    }

    fun markers(): Set<Word> = markers
}
