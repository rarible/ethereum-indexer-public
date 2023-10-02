package com.rarible.protocol.nft.api.controller.admin

import com.rarible.core.common.nowMillis
import com.rarible.core.task.Task
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.api.converter.ItemIdConverter
import com.rarible.protocol.nft.api.dto.AdminTaskDto
import com.rarible.protocol.nft.api.dto.ItemMaintenanceResultDto
import com.rarible.protocol.nft.api.dto.TokenDto
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.api.model.sorted
import com.rarible.protocol.nft.api.service.admin.MaintenanceService
import com.rarible.protocol.nft.core.model.ItemId
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.service.ReindexOwnerService
import com.rarible.protocol.nft.core.service.ReindexTokenService
import com.rarible.protocol.nft.core.service.item.ItemReduceService
import com.rarible.protocol.nft.core.service.token.TokenProvider
import com.rarible.protocol.nft.core.service.token.TokenService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import scalether.domain.Address

@ExperimentalCoroutinesApi
@RestController
class AdminController(
    private val reindexTokenService: ReindexTokenService,
    private val tokenService: TokenService,
    private val itemReduceService: ItemReduceService,
    private val maintenanceService: MaintenanceService,
    private val tokenProvider: TokenProvider,
    private val reindexOwnerService: ReindexOwnerService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping(
        value = ["/admin/nft/autoReduce"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    suspend fun autoReduce(): ResponseEntity<Void> {
        reindexTokenService.createAutoReduceTask()
        return ResponseEntity.noContent().build()
    }

    @PostMapping(
        value = ["/admin/nft/items/{itemId}/reduce"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    suspend fun reduceItemById(
        @PathVariable("itemId") itemId: String
    ): ResponseEntity<Void> {
        val id = ItemIdConverter.convert(itemId)
        itemReduceService.update(id.token, id.tokenId).subscribe()
        return ResponseEntity.noContent().build()
    }

    @PostMapping(
        value = ["/admin/nft/items/reduce"],
        produces = ["application/json"],
        consumes = ["application/json"]
    )
    suspend fun reduceItemsInRange(
        @RequestParam(value = "from", required = false) from: String?,
        @RequestParam(value = "to", required = false) to: String?,
        @RequestParam(value = "tasks", required = false) tasks: Int = 1
    ): ResponseEntity<Void> {

        val fromItemId = from?.let { ItemIdConverter.convert(it) }
            ?: ItemId(Address.ZERO(), EthUInt256.ZERO)

        val toItemId = to?.let { ItemIdConverter.convert(it) }
            ?: ItemId(
                Address.apply("0xffffffffffffffffffffffffffffffffffffffff"),
                EthUInt256.of("0xffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
            )

        reindexTokenService.createReduceTokenRangeTask(fromItemId, toItemId, tasks)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(
        value = ["/admin/nft/collections/{collectionId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun getTokenById(
        @PathVariable("collectionId") collectionId: Address
    ): ResponseEntity<TokenDto> {
        val token = tokenService.getToken(collectionId)
            ?: throw EntityNotFoundApiException("Collection", collectionId)
        return ResponseEntity.ok().body(convert(token))
    }

    @GetMapping(
        value = ["/admin/nft/collections/{collectionId}/update"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun updateToken(
        @PathVariable("collectionId") collectionId: Address,
        @RequestParam(value = "reduce", required = false, defaultValue = "false") reduce: Boolean,
    ): ResponseEntity<TokenDto> {
        logger.info("Attempting to refresh token id=$collectionId reduce=$reduce")
        // Just update in DB, events will be emitted after the reduce
        val token = tokenService.update(collectionId)
            ?: throw EntityNotFoundApiException("Collection", collectionId)
        if (reduce && token.standard.isNotIgnorable()) {
            reindexTokenService.createReindexAndReduceTokenTasks(listOf(collectionId))
        }
        return ResponseEntity.ok().body(convert(token))
    }

    @DeleteMapping(
        value = ["/admin/nft/collections/{collectionId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun deleteTokenById(
        @PathVariable("collectionId") collectionId: Address
    ): ResponseEntity<Void> {
        tokenService.removeToken(collectionId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reindexToken"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReindexTokenTask(
        @RequestParam(value = "collection", required = true) collection: List<Address>,
        @RequestParam(value = "fromBlock", required = false) fromBlock: Long?,
        @RequestParam(value = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexTokenService.createReindexTokenTask(collection, fromBlock, force ?: false)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reindexItems"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReindexTokenItemsTask(
        @RequestParam(value = "collection", required = true) collection: List<Address>,
        @RequestParam(value = "fromBlock", required = false) fromBlock: Long?,
        @RequestParam(value = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexTokenService.createReindexTokenItemsTask(collection, fromBlock, force ?: false)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reindexPunks"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReindexCryptoPunksTasks(
        @RequestParam(value = "currentBlock", required = true) currentBlock: Long
    ): ResponseEntity<List<AdminTaskDto>> {
        val tasks = reindexTokenService.createReindexCryptoPunksTasks(currentBlock)
        return ResponseEntity.ok().body(tasks.map { convert(it) })
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reduceToken"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReduceTokenTask(
        @RequestParam(value = "collection", required = true) collection: Address,
        @RequestParam(value = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexTokenService.createReduceTokenTask(collection, force ?: false)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reduceItems"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReduceTokenItemsTask(
        @RequestParam(value = "collection", required = true) collection: Address,
        @RequestParam(value = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexTokenService.createReduceTokenItemsTask(collection, force ?: false)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/owners/tasks/reduceItems"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReduceOwnerItemsTask(
        @RequestParam(value = "owner", required = true) owner: Address,
        @RequestParam(value = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexOwnerService.createReduceOwnerItemsTask(owner, force ?: false)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks/reindexItemsRoyalties"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun createReindexTokenItemRoyaltiesTask(
        @RequestParam(value = "collection", required = true) collection: Address,
        @RequestParam(value = "force", required = false) force: Boolean?
    ): ResponseEntity<AdminTaskDto> {
        val task = reindexTokenService.createReindexTokenItemRoyaltiesTask(collection, force ?: false)
        return ResponseEntity.ok().body(convert(task))
    }

    @GetMapping(
        value = ["/admin/nft/user/{user}/fixItems"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun fixUserItems(
        @PathVariable("user") user: String,
    ): ResponseEntity<ItemMaintenanceResultDto> {
        logger.info("Fixing user items for user $user")
        val fixResult = maintenanceService.fixUserItems(user).sorted()
        logger.info("fixItems result user $user is $fixResult")
        return ResponseEntity.ok().body(ItemMaintenanceResultDto(fixResult))
    }

    @GetMapping(
        value = ["/admin/nft/user/{user}/checkItems"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun checkUserItems(
        @PathVariable("user") user: String,
    ): ResponseEntity<ItemMaintenanceResultDto> {
        logger.info("Checking user items for user $user")
        val checkResult = maintenanceService.checkUserItems(user).sorted()
        logger.info("checkItems result user $user is $checkResult")
        return ResponseEntity.ok().body(ItemMaintenanceResultDto(checkResult))
    }

    @GetMapping(
        value = ["/admin/nft/collections/tasks"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun getTokenTasks(): ResponseEntity<List<AdminTaskDto>> {
        val tasks = reindexTokenService.getTokenTasks()
        return ResponseEntity.ok().body(tasks.map { convert(it) })
    }

    @GetMapping(
        value = ["/admin/nft/testLogging"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    suspend fun testLoggingIssue(): ResponseEntity<String> {
        repeat(50) {
            logger.info("(${nowMillis().toEpochMilli()}), test logging issue $it (1ms delay)")
            delay(1)
        }

        return ResponseEntity.ok("OK")
    }

    private fun convert(task: Task): AdminTaskDto {
        return AdminTaskDto(
            id = task.id.toHexString(),
            type = task.type,
            status = task.lastStatus.toString(),
            error = task.lastError,
            params = task.param,
            state = task.state.toString()
        )
    }

    private fun convert(token: Token): TokenDto {
        return TokenDto(
            id = token.id,
            standard = token.standard.name,
            owner = token.owner,
            name = token.name,
            symbol = token.symbol,
            features = token.features.map { it.name }
        )
    }
}
