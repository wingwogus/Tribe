package com.tribe.application.itinerary.place

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["tribe.itinerary.place-search.enabled"], havingValue = "false")
class NoOpPlaceSearchGateway : PlaceSearchGateway {
    override fun search(query: String?, language: String, context: PlaceSearchContext): List<PlaceSearchGateway.SearchHit> = emptyList()
    override fun searchNearby(request: PlaceSearchGateway.NearbySearchRequest): List<PlaceSearchGateway.SearchHit> = emptyList()
    override fun getPlaceSummary(externalPlaceId: String, language: String): PlaceSearchGateway.SearchHit? = null
    override fun getPlaceDetails(externalPlaceId: String, language: String): PlaceSearchGateway.DetailsPayload? = null
    override fun getPhoto(photoName: String, maxWidthPx: Int): PlacePhotoMedia? = null
    override fun directions(originPlaceId: String, destinationPlaceId: String, travelMode: String): RouteDetails? = null
}
