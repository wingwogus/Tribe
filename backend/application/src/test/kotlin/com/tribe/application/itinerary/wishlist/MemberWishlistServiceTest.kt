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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MemberWishlistServiceTest {
    @Mock private lateinit var memberWishlistItemRepository: MemberWishlistItemRepository
    @Mock private lateinit var placeCatalogService: PlaceCatalogService
    @Mock private lateinit var memberRepository: MemberRepository
    @Mock private lateinit var currentActor: CurrentActor

    private lateinit var service: MemberWishlistService

    @BeforeEach
    fun setUp() {
        service = MemberWishlistService(
            memberWishlistItemRepository = memberWishlistItemRepository,
            placeCatalogService = placeCatalogService,
            memberRepository = memberRepository,
            currentActor = currentActor,
            placeResultAssembler = PlaceResultAssembler(),
        )
    }

    @Test
    fun `addWishlistItem creates canonical place and member wishlist item`() {
        val member = member()
        val place = place("tokyo-tower", "도쿄타워")
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberRepository.findById(member.id)).thenReturn(Optional.of(member))
        `when`(memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(member.id, "tokyo-tower"))
            .thenReturn(false)
        `when`(
            placeCatalogService.getOrCreateAndEnrich(
                "tokyo-tower",
                "도쿄타워",
                "도쿄",
                BigDecimal.ONE,
                BigDecimal.TEN,
                "ko",
            ),
        ).thenReturn(place)
        `when`(memberWishlistItemRepository.saveAndFlush(any(MemberWishlistItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as MemberWishlistItem
            ReflectionTestUtils.setField(saved, "id", 20L)
            saved
        }

        val result = service.addWishlistItem(
            MemberWishlistCommand.Add(
                externalPlaceId = "tokyo-tower",
                placeName = "도쿄타워",
                address = "도쿄",
                latitude = BigDecimal.ONE,
                longitude = BigDecimal.TEN,
            ),
        )

        assertEquals(20L, result.memberWishlistItemId)
        assertEquals(place.id, result.placeId)
        assertEquals("tokyo-tower", result.externalPlaceId)
        assertEquals("도쿄타워", result.name)
    }

    @Test
    fun `addWishlistItem rejects duplicate place for same member`() {
        val member = member()
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberRepository.findById(member.id)).thenReturn(Optional.of(member))
        `when`(memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(member.id, "same-place"))
            .thenReturn(true)

        val ex = assertThrows(BusinessException::class.java) {
            service.addWishlistItem(
                MemberWishlistCommand.Add(
                    externalPlaceId = "same-place",
                    placeName = "다른 이름",
                    address = "다른 주소",
                    latitude = BigDecimal.ONE,
                    longitude = BigDecimal.TEN,
                ),
            )
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS, ex.errorCode)
        verify(memberWishlistItemRepository, never()).saveAndFlush(any(MemberWishlistItem::class.java))
    }

    @Test
    fun `addWishlistItem scopes duplicate check by current member`() {
        val member = member(id = 7L)
        val place = place("same-place", "도쿄타워")
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberRepository.findById(member.id)).thenReturn(Optional.of(member))
        `when`(memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(member.id, "same-place"))
            .thenReturn(false)
        `when`(
            placeCatalogService.getOrCreateAndEnrich(
                "same-place",
                "도쿄타워",
                "도쿄",
                BigDecimal.ONE,
                BigDecimal.TEN,
                "ko",
            ),
        ).thenReturn(place)
        `when`(memberWishlistItemRepository.saveAndFlush(any(MemberWishlistItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as MemberWishlistItem
            ReflectionTestUtils.setField(saved, "id", 21L)
            saved
        }

        service.addWishlistItem(
            MemberWishlistCommand.Add(
                externalPlaceId = "same-place",
                placeName = "도쿄타워",
                address = "도쿄",
                latitude = BigDecimal.ONE,
                longitude = BigDecimal.TEN,
            ),
        )

        verify(memberWishlistItemRepository).existsByMember_IdAndPlace_ExternalPlaceId(member.id, "same-place")
    }

    @Test
    fun `addWishlistItem allows same place for different members`() {
        val firstMember = member(id = 2L)
        val secondMember = member(id = 7L)
        val place = place("shared-place", "도쿄타워")
        var savedId = 30L

        `when`(currentActor.requireUserId()).thenReturn(firstMember.id, secondMember.id)
        `when`(memberRepository.findById(firstMember.id)).thenReturn(Optional.of(firstMember))
        `when`(memberRepository.findById(secondMember.id)).thenReturn(Optional.of(secondMember))
        `when`(memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(firstMember.id, "shared-place"))
            .thenReturn(false)
        `when`(memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(secondMember.id, "shared-place"))
            .thenReturn(false)
        `when`(
            placeCatalogService.getOrCreateAndEnrich(
                "shared-place",
                "도쿄타워",
                "도쿄",
                BigDecimal.ONE,
                BigDecimal.TEN,
                "ko",
            ),
        ).thenReturn(place)
        `when`(memberWishlistItemRepository.saveAndFlush(any(MemberWishlistItem::class.java))).thenAnswer { invocation ->
            val saved = invocation.arguments[0] as MemberWishlistItem
            ReflectionTestUtils.setField(saved, "id", savedId++)
            saved
        }

        val firstResult = service.addWishlistItem(sharedPlaceCommand())
        val secondResult = service.addWishlistItem(sharedPlaceCommand())

        assertEquals(30L, firstResult.memberWishlistItemId)
        assertEquals(31L, secondResult.memberWishlistItemId)
        verify(memberWishlistItemRepository).existsByMember_IdAndPlace_ExternalPlaceId(firstMember.id, "shared-place")
        verify(memberWishlistItemRepository).existsByMember_IdAndPlace_ExternalPlaceId(secondMember.id, "shared-place")
    }

    @Test
    fun `addWishlistItem translates save time duplicate into wishlist conflict`() {
        val member = member()
        val place = place("race-place", "도쿄타워")
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberRepository.findById(member.id)).thenReturn(Optional.of(member))
        `when`(memberWishlistItemRepository.existsByMember_IdAndPlace_ExternalPlaceId(member.id, "race-place"))
            .thenReturn(false)
        `when`(
            placeCatalogService.getOrCreateAndEnrich(
                "race-place",
                "도쿄타워",
                "도쿄",
                BigDecimal.ONE,
                BigDecimal.TEN,
                "ko",
            ),
        ).thenReturn(place)
        `when`(memberWishlistItemRepository.saveAndFlush(any(MemberWishlistItem::class.java)))
            .thenThrow(DataIntegrityViolationException("duplicate wishlist item"))

        val ex = assertThrows(BusinessException::class.java) {
            service.addWishlistItem(
                MemberWishlistCommand.Add(
                    externalPlaceId = "race-place",
                    placeName = "도쿄타워",
                    address = "도쿄",
                    latitude = BigDecimal.ONE,
                    longitude = BigDecimal.TEN,
                ),
            )
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTS, ex.errorCode)
    }

    @Test
    fun `getWishlist returns current member page`() {
        val member = member()
        val pageable = PageRequest.of(0, 10)
        val item = memberWishlistItem(member, place("osaka-castle", "오사카성"), id = 30L)
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberWishlistItemRepository.findAllByMember_Id(member.id, pageable))
            .thenReturn(PageImpl(listOf(item), pageable, 1))

        val result = service.getWishlist(pageable)

        assertEquals(1, result.totalElements)
        assertEquals("오사카성", result.content.first().name)
    }

    @Test
    fun `searchWishlist filters by current member and place name`() {
        val member = member()
        val pageable = PageRequest.of(0, 10)
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberWishlistItemRepository.findAllByMember_IdAndPlace_NameContainingIgnoreCase(member.id, "도쿄", pageable))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = service.searchWishlist("도쿄", pageable)

        assertEquals(0, result.totalElements)
        verify(memberWishlistItemRepository).findAllByMember_IdAndPlace_NameContainingIgnoreCase(member.id, "도쿄", pageable)
    }

    @Test
    fun `deleteWishlistItems no-ops on empty id list`() {
        val member = member()
        `when`(currentActor.requireUserId()).thenReturn(member.id)

        service.deleteWishlistItems(MemberWishlistCommand.Delete(emptyList()))

        verify(memberWishlistItemRepository, never()).findIdsByMemberIdAndIdIn(member.id, emptyList())
    }

    @Test
    fun `deleteWishlistItems rejects when any id is missing or owned by another member`() {
        val member = member()
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberWishlistItemRepository.findIdsByMemberIdAndIdIn(member.id, listOf(1L, 2L))).thenReturn(listOf(1L))

        val ex = assertThrows(BusinessException::class.java) {
            service.deleteWishlistItems(MemberWishlistCommand.Delete(listOf(1L, 2L)))
        }

        assertEquals(ErrorCode.WISHLIST_ITEM_NOT_FOUND, ex.errorCode)
        verify(memberWishlistItemRepository, never()).deleteAllByIdInBatch(listOf(1L))
    }

    @Test
    fun `deleteWishlistItems deletes distinct owned requested ids`() {
        val member = member()
        `when`(currentActor.requireUserId()).thenReturn(member.id)
        `when`(memberWishlistItemRepository.findIdsByMemberIdAndIdIn(member.id, listOf(1L, 2L))).thenReturn(listOf(1L, 2L))

        service.deleteWishlistItems(MemberWishlistCommand.Delete(listOf(1L, 1L, 2L)))

        verify(memberWishlistItemRepository).deleteAllByIdInBatch(listOf(1L, 2L))
    }

    private fun member(id: Long = 2L): Member =
        Member(id = id, email = "member$id@test.com", passwordHash = "pw", nickname = "member$id")

    private fun place(externalPlaceId: String, name: String): Place {
        val place = Place(externalPlaceId, name, "도쿄", BigDecimal.ONE, BigDecimal.TEN)
        ReflectionTestUtils.setField(place, "id", 10L)
        return place
    }

    private fun memberWishlistItem(member: Member, place: Place, id: Long): MemberWishlistItem {
        val item = MemberWishlistItem(member, place)
        ReflectionTestUtils.setField(item, "id", id)
        return item
    }

    private fun sharedPlaceCommand(): MemberWishlistCommand.Add =
        MemberWishlistCommand.Add(
            externalPlaceId = "shared-place",
            placeName = "도쿄타워",
            address = "도쿄",
            latitude = BigDecimal.ONE,
            longitude = BigDecimal.TEN,
        )
}
