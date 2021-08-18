package com.rarible.protocol.unlockable.api.controller

import com.rarible.core.logging.withMdc
import com.rarible.protocol.dto.LockDto
import com.rarible.protocol.dto.LockFormDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.SignatureFormDto
import com.rarible.protocol.unlockable.api.service.LockService
import com.rarible.protocol.unlockable.api.service.NftClientService
import org.slf4j.LoggerFactory
import org.springframework.core.convert.ConversionService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
// causes error with additional mystery param:
// com.rarible.protocol.unlockable.api.controller.LockController#createLock0(String, LockFormDto, Continuation) [DispatcherHandler]
//@Validated
class LockController(
    private val lockService: LockService,
    private val nftClientService: NftClientService,
    private val conversionService: ConversionService
) : LockControllerApi {

    val logger = LoggerFactory.getLogger(LockController::class.java)

    override suspend fun createLock(itemId: String, form: LockFormDto): ResponseEntity<LockDto> {
        logger.info("Create lock itemId=[{}], lockForm=[{}]", itemId, form)
        val lockDto = withItem(itemId) {
            val lock = lockService.createLock(it, form)
            conversionService.convert(lock, LockDto::class.java)
        }
        return ResponseEntity.ok(lockDto)
    }

    override suspend fun getLockContent(itemId: String, form: SignatureFormDto): ResponseEntity<String> {
        logger.info("Get lock content itemId=[{}], signature=[{}]", itemId, form)
        val content = withItem(itemId) { lockService.getContent(it, form.signature) }
        return ResponseEntity.ok(content)
    }

    override suspend fun isUnlockable(itemId: String): ResponseEntity<Boolean> {
        logger.info("isUnlockable itemId=[{}]", itemId)
        val isUnlockable = withItem(itemId) { lockService.isUnlockable(it) }
        return ResponseEntity.ok(isUnlockable)
    }

    private suspend fun <T> withItem(itemId: String, block: suspend (item: NftItemDto) -> T?): T? {
        val item = nftClientService.getItem(itemId)
        logger.info("Item by itemId=[{}] is {}", itemId, item)
        return item?.let { block(it) }
    }

    //TODO Remove when marketplace start to use Protocol API higher than 1.7.0
    //------------------- Deprecated ------------------//
    
    @Deprecated("Duplicated /unlockable in Gateway API")
    @PostMapping(
        value = ["/v0.1/unlockable/{itemId}/lock"],
        produces = ["application/json", "*/*"],
        consumes = ["application/json"]
    )
    suspend fun createLock_legacy(
        @PathVariable("itemId") itemId: String,
        @RequestBody lockFormDto: LockFormDto
    ): ResponseEntity<LockDto> {
        return withMdc { createLock(itemId, lockFormDto) }
    }

    @Deprecated("Duplicated /unlockable in Gateway API")
    @PostMapping(
        value = ["/v0.1/unlockable/{itemId}/getContent"],
        produces = ["application/json", "*/*"],
        consumes = ["application/json"]
    )
    suspend fun getLockContent_legacy(
        @PathVariable("itemId") itemId: String,
        @RequestBody signatureFormDto: SignatureFormDto
    ): ResponseEntity<String> {
        return withMdc { getLockContent(itemId, signatureFormDto) }
    }

    @Deprecated("Duplicated /unlockable in Gateway API")
    @GetMapping(
        value = ["/v0.1/unlockable/{itemId}/isUnlockable"],
        produces = ["application/json", "*/*"]
    )
    suspend fun isUnlockable_legacy(
        @PathVariable("itemId") itemId: String
    ): ResponseEntity<Boolean> {
        return withMdc { isUnlockable(itemId) }
    }
}
