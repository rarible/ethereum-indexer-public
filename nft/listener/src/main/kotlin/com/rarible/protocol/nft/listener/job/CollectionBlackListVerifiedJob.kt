package com.rarible.protocol.nft.listener.job

import com.rarible.core.task.Task
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.nft.core.repository.TempTaskRepository
import com.rarible.protocol.nft.core.service.CollectionFeatureProvider
import com.rarible.protocol.nft.core.service.token.TokenEventListener
import com.rarible.protocol.nft.core.service.token.TokenUpdateService
import com.rarible.protocol.nft.listener.admin.BlackListVerifiedTaskHandler
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled

class CollectionBlackListVerifiedJob(
    @Value("\${listener.collectionBlackListVerified.enabled:true}")
    private val enabled: Boolean,
    private val taskRepository: TempTaskRepository,
    private val tokenUpdateService: TokenUpdateService,
    private val tokenListener: TokenEventListener,
    private val collectionFeatureProvider: CollectionFeatureProvider
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        fixedDelayString = "\${listener.collectionBlackListVerifiedRefresh.rate:PT30S}",
        initialDelayString = "PT1M"
    )
    fun execute() = runBlocking<Unit> {
        logger.info("Starting CollectionBlackListVerifiedJob")
        if (enabled) {
            collectionFeatureProvider.refresh()

            val blacklist = collectionFeatureProvider.getBlacklisted().map {
                BlackListVerifiedTaskHandler.Param(it, BlackListVerifiedTaskHandler.Feature.BLACKLIST)
            }
            val verified = collectionFeatureProvider.getVerified().map {
                BlackListVerifiedTaskHandler.Param(it, BlackListVerifiedTaskHandler.Feature.VERIFIED)
            }
            (blacklist + verified).forEach { param ->
                val existedTask =
                    taskRepository.findByType(BlackListVerifiedTaskHandler.NAME, param.toString()).firstOrNull()
                if (null == existedTask) {
                    tokenUpdateService.getToken(param.address)?.let { token ->
                        when (param.feature) {
                            BlackListVerifiedTaskHandler.Feature.BLACKLIST -> {
                                tokenUpdateService.removeToken(token.id)
                            }
                            BlackListVerifiedTaskHandler.Feature.VERIFIED -> {
                                tokenListener.onTokenChanged(token)
                            }
                        }
                    }
                    logger.info("Creating task for ${param}")
                    val task = Task(
                        type = BlackListVerifiedTaskHandler.NAME,
                        param = param.toString(),
                        state = null,
                        running = false,
                        lastStatus = TaskStatus.NONE
                    )
                    taskRepository.save(task)
                }
            }
        }
        logger.info("Finished CollectionBlackListVerifiedJob")
    }
}
