package com.rarible.protocol.unlockable.test

import com.rarible.ethereum.common.generateNewKeys
import org.apache.commons.lang3.RandomStringUtils

object LockTestDataFactory {

    fun randomLockTestData(itemId: String): LockTestData {
        val (privateKey, publicKey, ownerAddress) = generateNewKeys()
        val nftItem = NftItemDtoFactory.randomItemDto(itemId, ownerAddress)
        val lockContent = RandomStringUtils.randomAlphanumeric(8)

        return LockTestData(nftItem, lockContent, ownerAddress, privateKey, publicKey)
    }

}