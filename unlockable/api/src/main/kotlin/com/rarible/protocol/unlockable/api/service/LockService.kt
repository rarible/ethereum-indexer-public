package com.rarible.protocol.unlockable.api.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.LockFormDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.unlockable.api.exception.LockAlreadyExistsException
import com.rarible.protocol.unlockable.api.exception.LockOwnershipException
import com.rarible.protocol.unlockable.domain.Lock
import com.rarible.protocol.unlockable.event.LockEvent
import com.rarible.protocol.unlockable.event.LockEventListener
import com.rarible.protocol.unlockable.event.LockEventType
import com.rarible.protocol.unlockable.repository.LockRepository
import com.rarible.protocol.unlockable.util.LockMessageUtil
import com.rarible.protocol.unlockable.util.SignUtil
import io.daonomic.rpc.domain.Binary
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.SignatureException
import java.util.*

@Component
class LockService(
    private val lockRepository: LockRepository,
    val nftClientService: NftClientService,
    val lockEventListeners: List<LockEventListener>
) {
    private val logger = LoggerFactory.getLogger(LockService::class.java)

    suspend fun createLock(item: NftItemDto, form: LockFormDto): Lock {
        val existingLock = lockRepository.findByItemId(item.id)
        if (existingLock != null) {
            logger.warn("Lock exists [{}] for item [{}]", existingLock, item.id)
            throw LockAlreadyExistsException()
        }

        val lockMessage = LockMessageUtil.getLockMessage(item.contract, EthUInt256(item.tokenId), form.content)

        val signerAddress = try {
            SignUtil.recover(lockMessage, form.signature)
        } catch (e: SignatureException) {
            logger.warn(
                "Failed to recover address for message [{}] with signature [{}]",
                lockMessage, form.signature, e
            )
            throw LockOwnershipException()
        }

        logger.info("Signer address [{}] for item [{}]", signerAddress, item.id)
        if (item.owners.all { it == signerAddress }) {
            val lock = Lock(item.id, form.content, signerAddress, form.signature)
            onLockCreated(lock)
            return lockRepository.save(lock)
        } else {
            logger.info("Item owners [{}] do not contain signer address", item.owners)
            throw LockOwnershipException()
        }
    }

    suspend fun isUnlockable(item: NftItemDto): Boolean {
        val existingLock = lockRepository.findByItemId(item.id)
        logger.info("Item [{}] has lock [{}]", item.id, existingLock?.id)
        return existingLock != null
    }

    suspend fun getContent(item: NftItemDto, signature: Binary?): String {
        val signerAddress = SignUtil.recover(
            LockMessageUtil.getUnlockMessage(item.contract, EthUInt256(item.tokenId)),
            signature
        )
        val lock = lockRepository.findByItemId(item.id)
        if (lock == null) {
            logger.info("Lock for item [{}] not found", item.id)
            throw LockOwnershipException()
        }

        val hasItem = nftClientService.hasItem(item.contract, item.tokenId, signerAddress)
        if (!hasItem) {
            logger.info("Nft item with id [{}] not found", item.id)
            throw LockOwnershipException()
        }

        val content = lockRepository
            .save(lock.copy(unlockDate = nowMillis()))
            .content

        onLockUnlocked(lock)
        return content
    }

    private suspend fun onLockCreated(lock: Lock) {
        val event = LockEvent(
            UUID.randomUUID().toString(),
            lock.itemId,
            LockEventType.LOCK_CREATED
        )
        onEvent(event)
    }

    private suspend fun onLockUnlocked(lock: Lock) {
        val event = LockEvent(
            UUID.randomUUID().toString(),
            lock.itemId,
            LockEventType.LOCK_UNLOCKED
        )
        onEvent(event)
    }

    private suspend fun onEvent(event: LockEvent) {
        lockEventListeners.forEach { it.onEvent(event) }
    }
}
