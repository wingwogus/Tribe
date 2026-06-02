package com.tribe.application.itinerary.wishlist

import com.tribe.application.exception.ErrorCode
import com.tribe.application.exception.business.BusinessException
import com.tribe.application.itinerary.place.PlaceCatalogService
import com.tribe.application.itinerary.place.PlaceResultAssembler
import com.tribe.application.security.CurrentActor
import com.tribe.domain.itinerary.place.Place
import com.tribe.domain.itinerary.wishlist.MemberWishlistItem
import com.tribe.domain.itinerary.wishlist.MemberWishlistItemRepository
import com.tribe.domain.member.Member
import com.tribe.domain.member.MemberRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 개인 위시리스트 use case.
 *
 * 멤버 개인 장소 저장소와 여행 위시리스트 승격의 출발점.
 */
@Service
@Transactional
class MemberWishlistService(
    private val memberWishlistItemRepository: MemberWishlistItemRepository,
    private val placeCatalogService: PlaceCatalogService,
    private val memberRepository: MemberRepository,
    private val currentActor: CurrentActor,
    private val placeResultAssembler: PlaceResultAssembler,
) {
    fun addWishlistItem(command: MemberWishlistCommand.Add): MemberWishlistResult.Item {
        // 흐름: 현재 멤버 확인 -> 중복 차단 -> canonical Place 확보 -> 개인 위시 저장.
        val memberId = currentActor.requireUserId()
        val member = memberRepository.findById(memberId).orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        // 개인 위시리스트도 Google placeId 기준 중복 저장 차단.
        if (memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(memberId, command.externalPlaceId)) {
            throw BusinessException(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS)
        }

        // 검색 후보 payload를 내부 Place로 저장/보강한 뒤 개인 항목과 연결.
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
    fun searchWishlist(query: String, pageable: Pageable): MemberWishlistResult.SearchPage {
        val memberId = currentActor.requireUserId()
        val page = memberWishlistItemRepository.findAllByMember_IdAndPlace_NameContainingIgnoreCase(
            memberId,
            query,
            pageable,
        )
        return toSearchPage(page)
    }

    @Transactional(readOnly = true)
    fun getWishlist(pageable: Pageable): MemberWishlistResult.SearchPage {
        val memberId = currentActor.requireUserId()
        val page = memberWishlistItemRepository.findAllByMember_Id(memberId, pageable)
        return toSearchPage(page)
    }

    fun deleteWishlistItems(command: MemberWishlistCommand.Delete) {
        // 현재 멤버 소유 항목만 삭제 대상, 일부 누락도 오류로 처리.
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

    private fun saveWishlistItem(
        member: Member,
        place: Place,
    ): MemberWishlistItem =
        try {
            // DB unique 경합까지 같은 중복 오류로 수렴.
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
        // 개인 위시 목록에서도 저장된 Place 상세 요약을 함께 노출.
        val placeTypeSummary = placeResultAssembler.toPlaceTypeSummary(item.place)
        val photoHint = placeResultAssembler.toPhotoHint(item.place)
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
        )
    }
}
