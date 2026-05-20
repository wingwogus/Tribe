package com.tribe.application.itinerary.wishlist

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.OpeningSummaryAssembler
import com.tribe.application.itinerary.place.PlaceCatalogService
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.security.CurrentActor
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.wishlist.AccountWishlistSort
import com.tribe.domain.itinerary.wishlist.MemberWishlistItem
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MemberWishlistService(
    private val memberWishlistItemRepository: MemberWishlistItemRepository,
    private val placeCatalogService: PlaceCatalogService,
    private val memberRepository: MemberRepository,
    private val currentActor: CurrentActor,
    private val placeResultAssembler: PlaceResultAssembler,
) {
    private val openingSummaryAssembler = OpeningSummaryAssembler()

    fun addWishlistItem(command: MemberWishlistCommand.Add): MemberWishlistResult.Item {
        val memberId = currentActor.requireUserId()
        val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        if (memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(memberId, command.externalPlaceId)) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS)
        }

        val place = placeCatalogService.getOrCreateAndEnrich(
            externalPlaceId = command.externalPlaceId,
            placeName = command.placeName,
            address = command.address,
            latitude = command.latitude,
            longitude = command.longitude,
        )

        val saved = saveWishlistItem(member, place)
        return toItem(saved)
    }

    @Transactional(readOnly = true)
    fun searchWishlist(
        query: String,
        pageable: Pageable,
        sort: String? = null,
    ): MemberWishlistResult.SearchPage {
        val memberId = currentActor.requireUserId()
        val page = memberWishlistItemRepository.findPageByMember(memberId, query, parseSort(sort), pageable)
        return toSearchPage(page)
    }

    @Transactional(readOnly = true)
    fun getWishlist(
        pageable: Pageable,
        sort: String? = null,
    ): MemberWishlistResult.SearchPage {
        val memberId = currentActor.requireUserId()
        val page = memberWishlistItemRepository.findPageByMember(memberId, null, parseSort(sort), pageable)
        return toSearchPage(page)
    }

    fun deleteWishlistItems(command: MemberWishlistCommand.Delete) {
        val memberId = currentActor.requireUserId()
        val ids = command.memberWishlistItemIds.distinct()
        if (ids.isEmpty()) return

        val existingIds = memberWishlistItemRepository.findIdsByMemberIdAndIdIn(memberId, ids)
        val existingIdSet = existingIds.toSet()
        val missing = ids.filterNot { it in existingIdSet }
        if (missing.isNotEmpty()) throw BusinessException(ErrorCode.WISHLIST_ITEM_NOT_FOUND)

        memberWishlistItemRepository.deleteAllByIdInBatch(existingIds)
    }

    private fun toSearchPage(page: Page<MemberWishlistItem>): MemberWishlistResult.SearchPage =
        MemberWishlistResult.SearchPage(
            content = page.content.map(::toItem),
            pageNumber = page.number,
            pageSize = page.size,
            totalPages = page.totalPages,
            totalElements = page.totalElements,
            isLast = page.isLast,
        )

    private fun parseSort(sort: String?): AccountWishlistSort? =
        AccountWishlistSort.fromApiValue(sort)
            ?: sort?.trim()?.takeIf { it.isNotEmpty() }?.let {
                throw BusinessException(
                    errorCode = ErrorCode.INVALID_INPUT,
                    detail = mapOf("field" to "sort", "rejectedValue" to sort),
                )
            }

    private fun saveWishlistItem(
        member: Member,
        place: Place,
    ): MemberWishlistItem =
        try {
            memberWishlistItemRepository.saveAndFlush(
                MemberWishlistItem(
                    member = member,
                    place = place,
                ),
            )
        } catch (ex: DataIntegrityViolationException) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS)
        }

    private fun toItem(item: MemberWishlistItem): MemberWishlistResult.Item {
        val placeTypeSummary = placeResultAssembler.toPlaceTypeSummary(item.place)
        val photoHint = placeResultAssembler.toPhotoHint(item.place)
        val openingSummary = openingSummaryAssembler.toOpeningSummary(item.place)
        return MemberWishlistResult.Item(
            memberWishlistItemId = item.id,
            placeId = item.place.id,
            externalPlaceId = item.place.externalPlaceId,
            name = item.place.name,
            address = item.place.address,
            latitude = item.place.latitude,
            longitude = item.place.longitude,
            placeTypeSummary = placeTypeSummary,
            normalizedCategoryKey = PlaceResultAssembler.toNormalizedCategoryKey(placeTypeSummary),
            photoHint = photoHint?.let { MemberWishlistResult.PhotoHint(it.name, it.photoUri) },
            placeDetailSummary = placeResultAssembler.toDetailSummary(item.place),
            openingSummary = openingSummary,
        )
    }
}
