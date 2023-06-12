package com.rarible.protocol.nft.api.controller

import com.rarible.protocol.dto.DomainResolveResultDto
import com.rarible.protocol.nft.api.converter.DomainResultDtoConverter
import com.rarible.protocol.nft.api.service.domain.CompositeDomainResolver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class DomainController(
    private val domainResolver: CompositeDomainResolver
) : NftDomainControllerApi {

    override suspend fun resolveDomainByName(name: String): ResponseEntity<DomainResolveResultDto> {
        val result = domainResolver.resolve(name)
        return ResponseEntity.ok(DomainResultDtoConverter.convert(result))
    }
}
