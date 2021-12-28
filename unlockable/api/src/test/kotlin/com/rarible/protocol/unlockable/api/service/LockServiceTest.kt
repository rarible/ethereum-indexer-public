package com.rarible.protocol.unlockable.api.service

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.common.generateNewKeys
import com.rarible.ethereum.common.toBinary
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.LockFormDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.unlockable.api.exception.LockAlreadyExistsException
import com.rarible.protocol.unlockable.api.exception.LockOwnershipException
import com.rarible.protocol.unlockable.domain.Lock
import com.rarible.protocol.unlockable.repository.LockRepository
import com.rarible.protocol.unlockable.util.LockMessageUtil
import com.rarible.protocol.unlockable.util.SignUtil
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Sign
import scalether.domain.Address
import java.math.BigInteger

class LockServiceTest {

    lateinit var lockRepo: LockRepository
    lateinit var nftClientService: NftClientService
    lateinit var lockService: LockService

    @BeforeEach
    fun beforeEach() {
        lockRepo = mockk()
        nftClientService = mockk()
        lockService = LockService(lockRepo, nftClientService, emptyList())
    }

    @Test
    fun `should create lock for existing unlockable item`() = runBlocking {
        coEvery { lockRepo.save(any()) } returnsArgument 0
        coEvery { lockRepo.findByItemId(any()) } returns null

        val lock = createLock()

        assertEquals("ipfs://content", lock.content)
        assertEquals("itemId", lock.itemId)
    }


    @Test
    fun `should throw error on already locked item`() = runBlocking<Unit> {
        try {
            val (privateKey, publicKey, ownerAddress) = generateNewKeys()
            val itemDto = itemDto("itemId", ownerAddress)
            coEvery { lockRepo.findByItemId(any()) } returns Lock("1", "1", ownerAddress, null)

            createLock(itemDto, publicKey, privateKey)
        } catch (ex: Exception) {
            assertEquals(LockAlreadyExistsException().message, ex.message)
        }
    }

    @Test
    fun `should return correct isUnlockable`() = runBlocking {
        val capturedLock = slot<Lock>()
        val lockByItemId = mutableMapOf<String, Lock>()
        coEvery {
            lockRepo.save(capture(capturedLock))
        } answers {
            val lock = capturedLock.captured
            lockByItemId[lock.itemId] = lock
            lock
        }

        val capturedItemId = slot<String>()
        coEvery {
            lockRepo.findByItemId(capture(capturedItemId))
        } answers {
            lockByItemId[capturedItemId.captured]
        }

        val (privateKey, publicKey, ownerAddress) = generateNewKeys()
        val itemDto = itemDto("itemId", ownerAddress)
        createLock(itemDto, publicKey, privateKey)
        assertTrue(lockService.isUnlockable(itemDto))

        val itemDto2 = itemDto("itemId2", ownerAddress)
        assertFalse(lockService.isUnlockable(itemDto2))
    }

    @Test
    fun `should get content of owned token`() = runBlocking {
        val slot = slot<Lock>()
        coEvery { lockRepo.save(capture(slot)) } returnsArgument 0
        coEvery { lockRepo.findByItemId(any()) } returns null
        coEvery { nftClientService.hasItem(any(), any(), any()) } returns true

        val (privateKey, publicKey, ownerAddress) = generateNewKeys()
        val itemDto = itemDto("itemId", ownerAddress)
        val lock = createLock(itemDto, publicKey, privateKey)
        val unlockData = Sign.signMessage(
            SignUtil.addStart(LockMessageUtil.getUnlockMessage(itemDto.contract, EthUInt256(itemDto.tokenId))).bytes(),
            publicKey,
            privateKey
        )
        coEvery { lockRepo.findByItemId(any()) } returns lock
        coEvery { lockRepo.findById(any()) } returns lock

        val content = lockService.getContent(itemDto, unlockData.toBinary())

        assertEquals("ipfs://content", content)
    }

    @Test
    fun `should not get content of owned token`() = runBlocking<Unit> {
        val slot = slot<Lock>()
        coEvery { lockRepo.save(capture(slot)) } returnsArgument 0
        coEvery { nftClientService.hasItem(any(), any(), any()) } returns true

        val (privateKey, publicKey, ownerAddress) = generateNewKeys()
        val (privateKey2, publicKey2, _) = generateNewKeys()
        val itemDto = itemDto("itemId", ownerAddress)

        coEvery { lockRepo.findByItemId(any()) } returns null

        val lock = createLock(itemDto, publicKey, privateKey)
        val unlockData = Sign.signMessage(
            SignUtil.addStart(LockMessageUtil.getUnlockMessage(itemDto.contract, EthUInt256(itemDto.tokenId))).bytes(),
            publicKey2,
            privateKey2
        )
        coEvery { lockRepo.findByItemId(any()) } returns lock
        coEvery { lockRepo.findById(any()) } returns lock

        try {
            lockService.getContent(itemDto, unlockData.toBinary())
        } catch (e: Exception) {
            assertEquals(LockOwnershipException().message, e.cause?.message)
        }
    }

    private suspend fun createLock(itemDto: NftItemDto, publicKey: BigInteger, privateKey: BigInteger): Lock {
        val lockData = Sign.signMessage(
            SignUtil.addStart(
                LockMessageUtil.getLockMessage(
                    Address.ONE(),
                    EthUInt256.TEN,
                    "ipfs://content"
                )
            ).bytes(), publicKey, privateKey
        )
        return lockService.createLock(itemDto, LockFormDto(lockData.toBinary(), "ipfs://content"))
    }

    private suspend fun createLock(itemId: String = "itemId"): Lock {
        val (privateKey, publicKey, ownerAddress) = generateNewKeys()

        val itemDto = itemDto(itemId, ownerAddress)

        return createLock(itemDto, publicKey, privateKey)
    }

    private fun itemDto(id: String, ownerAddress: Address) = NftItemDto(
        id = id,
        contract = Address.ONE(),
        tokenId = BigInteger.TEN,
        creators = listOf(PartDto(Address.TWO(), 10)),
        supply = BigInteger.ONE,
        lazySupply = BigInteger.TEN,
        owners = listOf(ownerAddress),
        royalties = emptyList(),
        pending = emptyList(),
        lastUpdatedAt = nowMillis(),
        deleted = false,
        meta = NftItemMetaDto("Test", null, null, null, null)
    )
}
