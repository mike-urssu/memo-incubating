package com.mistar.memo.domain.model.dto

import com.mistar.memo.domain.model.entity.memo.Tag

class MemoPatchDto(
    val title: String?,

    val content: String?,

    val isPublic: Boolean?,

    val tags: MutableSet<Tag>
)