package com.rarible.protocol.erc20.core.admin.model

data class Erc20DuplicatedLogRecordsTaskParam(
    val update: Boolean
) {

    companion object {
        const val ERC20_DUPLICATED_LOG_RECORDS_TASK = "ERC20_DUPLICATED_LOG_RECORDS_TASK"
    }
}
