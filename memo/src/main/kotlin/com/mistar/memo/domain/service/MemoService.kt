package com.mistar.memo.domain.service

import com.mistar.memo.domain.exception.auth.UserNotFoundException
import com.mistar.memo.domain.exception.memo.InvalidPageException
import com.mistar.memo.domain.exception.memo.MemoNotFoundException
import com.mistar.memo.domain.exception.memo.PageOutOfBoundsException
import com.mistar.memo.domain.exception.memo.UserAndMemoNotMatchedException
import com.mistar.memo.domain.model.common.Page
import com.mistar.memo.domain.model.dto.MemoPatchDto
import com.mistar.memo.domain.model.dto.MemoPostDto
import com.mistar.memo.domain.model.entity.memo.Memo
import com.mistar.memo.domain.model.entity.memo.Tag
import com.mistar.memo.domain.model.repository.MemoRepository
import com.mistar.memo.domain.model.repository.TagRepository
import com.mistar.memo.domain.model.repository.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class MemoService(
    private val memoRepository: MemoRepository,
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository
) {
    private val defaultPageSize = 10

    fun createMemo(userId: Int, memoPostDto: MemoPostDto) {
        val user = userRepository.findById(userId).orElseThrow { UserNotFoundException() }
        val memo = Memo(
            title = memoPostDto.title,
            content = memoPostDto.content,
            isPublic = memoPostDto.isPublic,
            tags = memoPostDto.tags,
            user = user
        )
        memoRepository.save(memo)
        createTags(memo.id!!, memoPostDto.tags)
    }

    private fun createTags(memoId: Int, tags: Set<Tag>) {
        for (tag in tags) {
            tag.memoId = memoId
            tagRepository.save(tag)
        }
    }

    fun selectAllMemos(page: Int): List<Memo> {
        if (page < 1)
            throw InvalidPageException()
        val memoCnt = memoRepository.findAllByIsDeleted(false).size
        if (memoCnt < (page - 1) * 10)
            throw PageOutOfBoundsException()

        val requestedPage = Page(page - 1, defaultPageSize)
        return memoRepository.findAllByIsDeleted(requestedPage, false)
    }

    fun selectAllMemos(userId: Int, page: Int): List<Memo> {
        if (page < 1)
            throw InvalidPageException()
        val memoCnt = memoRepository.findAllByUserIdAndIsDeletedAndIsPublic(userId, false, true).size
        if (memoCnt < (page - 1) * 10)
            throw PageOutOfBoundsException()

        val requestedPage = Page(page - 1, defaultPageSize)
        return memoRepository.findAllByUserIdAndIsDeletedAndIsPublic(requestedPage, userId, false, true)
    }

    fun selectMemosById(userId: Int, memoId: Int): List<Memo> {
        val memo =
            memoRepository.findByIdAndIsDeletedAndIsPublic(memoId, false, true).orElseThrow { MemoNotFoundException() }
        if (memo.user.id != userId)
            throw UserAndMemoNotMatchedException()
        return listOf(memo)
    }

    fun selectMemosByTag(userId: Int, tag: String, page: Int): List<Memo> {
        if (page < 1)
            throw InvalidPageException()

        val memoIds = getMemoIds(userId, tag)
        return when {
            memoIds.size / 10 < page - 1 -> {
                throw PageOutOfBoundsException()
            }
            memoIds.size / 10 == page - 1 -> {
                memoRepository.findAllById(memoIds)
                    .subList((page - 1) * defaultPageSize, (page - 1) * defaultPageSize + (memoIds.size % 10))
            }
            else -> {
                memoRepository.findAllById(memoIds)
                    .subList((page - 1) * defaultPageSize, page * defaultPageSize)
            }
        }
    }

    private fun getMemoIds(userId: Int, content: String): LinkedHashSet<Int> {
        val memoIds = linkedSetOf<Int>()
        val tags = tagRepository.findByContentContaining(content)
        for (tag in tags) {
            val memoId = tag.memoId
            if (memoRepository.existsByUserIdAndId(userId, memoId!!))
                memoIds.add(memoId)
        }
        return memoIds
    }

    fun selectMemosByTag(tag: String, page: Int): List<Memo> {
        if (page < 1)
            throw InvalidPageException()

        val memoIds = getMemoIds(tag)
        return when {
            memoIds.size / 10 < page - 1 -> {
                throw PageOutOfBoundsException()
            }
            memoIds.size / 10 == page - 1 -> {
                memoRepository.findAllById(memoIds)
                    .subList((page - 1) * defaultPageSize, (page - 1) * defaultPageSize + (memoIds.size % 10))
            }
            else -> {
                memoRepository.findAllById(memoIds)
                    .subList((page - 1) * defaultPageSize, page * defaultPageSize)
            }
        }
    }

    private fun getMemoIds(content: String): LinkedHashSet<Int> {
        val memoIds = linkedSetOf<Int>()
        val tags = tagRepository.findByContentContaining(content)
        for (tag in tags) {
            val memoId = tag.memoId
            if (memoRepository.existsById(memoId!!))
                memoIds.add(memoId)
        }
        return memoIds
    }

    @Transactional
    fun patchMemo(userId: Int, memoId: Int, memoPatchDto: MemoPatchDto) {
        val memo = memoRepository.findById(memoId).orElseThrow { MemoNotFoundException() }
        if (memo.user.id != userId)
            throw UserAndMemoNotMatchedException()

        if (memoPatchDto.title != null)
            memo.title = memoPatchDto.title
        if (memoPatchDto.content != null)
            memo.content = memoPatchDto.content
        if (memoPatchDto.isPublic != null)
            memo.isPublic = memoPatchDto.isPublic

        deleteTags(memo, memoPatchDto)
        saveTags(memo, memoPatchDto)
    }

    private fun deleteTags(memo: Memo, memoPatchDto: MemoPatchDto) {
        val contents = arrayListOf<String>()
        val tagsToDelete = arrayListOf<Tag>()

        for (tag in memoPatchDto.tags)
            contents.add(tag.content)

        for (tag in memo.tags)
            if (!contents.contains(tag.content))
                tagsToDelete.add(tag)

        for (tag in tagsToDelete) {
            tagRepository.delete(tag)
            memo.tags.remove(tag)
        }
    }

    private fun saveTags(memo: Memo, memoPatchDto: MemoPatchDto) {
        for (tag in memoPatchDto.tags) {
            if (!tagRepository.existsByMemoIdAndContent(memo.id!!, tag.content)) {
                memo.tags.add(tag)
                tag.memoId = memo.id
                tagRepository.save(tag)
            }
        }
    }

    fun deleteMemo(userId: Int, memoId: Int) {
        val memo = memoRepository.findById(memoId).orElseThrow { MemoNotFoundException() }
        if (memo.user.id != userId)
            throw UserAndMemoNotMatchedException()

        memo.isDeleted = true
        memo.deletedAt = LocalDateTime.now()
        memoRepository.save(memo)
    }

    @Transactional
    @Scheduled(cron = "0 0 0 * * *")
    fun deleteMemosByCron(): Int {
        val now = LocalDateTime.now().minusDays(7)
        val memosToDelete = memoRepository.findAllByDeletedAtBeforeAndIsDeleted(now, true)
        for (memo in memosToDelete) {
            tagRepository.deleteByMemoId(memo.id!!)
            memoRepository.deleteById(memo.id)
        }
        return memosToDelete.size
    }
}