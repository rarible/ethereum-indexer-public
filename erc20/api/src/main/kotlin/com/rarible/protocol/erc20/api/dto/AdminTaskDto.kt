package com.rarible.protocol.erc20.api.dto

data class AdminTaskDto(
    val id: String,
    val type: String,
    val status: String,
    val error: String?,
    val params: String,
    val state: String
)
