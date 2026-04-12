package com.tribe.domain.community

import com.tribe.domain.trip.core.Country
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface CommunityPostRepository : JpaRepository<CommunityPost, Long>, CommunityPostRepositoryCustom {
    @Query(
        value = "select cp from CommunityPost cp join fetch cp.author join fetch cp.trip where cp.trip.country = :country",
        countQuery = "select count(cp) from CommunityPost cp where cp.trip.country = :country",
    )
    fun findByTripCountry(@Param("country") country: Country, pageable: Pageable): Page<CommunityPost>

    @Query(
        value = "select cp from CommunityPost cp join fetch cp.author join fetch cp.trip",
        countQuery = "select count(cp) from CommunityPost cp",
    )
    override fun findAll(pageable: Pageable): Page<CommunityPost>

    @Query("select cp from CommunityPost cp join fetch cp.author join fetch cp.trip where cp.id = :postId")
    fun findByIdWithDetails(@Param("postId") postId: Long): CommunityPost?

    @Query(
        value = "select p from CommunityPost p join fetch p.author a join fetch p.trip t where a.id = :memberId order by p.createdAt desc",
        countQuery = "select count(p) from CommunityPost p where p.author.id = :memberId",
    )
    fun findByAuthorMemberIdWithDetails(@Param("memberId") memberId: Long, pageable: Pageable): Page<CommunityPost>
}
