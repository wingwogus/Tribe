package com.tribe.api.itinerary.item

import com.tribe.application.itinerary.place.RouteDetails
import com.tribe.application.itinerary.item.ItemResult
import com.tribe.api.itinerary.place.PlaceRequests
import java.time.LocalDateTime

object ItemResponses {
    data class PlaceTypeSummaryResponse(
        val primaryType: String?,
        val types: List<String>,
        val localizedPrimaryLabel: String?,
    ) {
        companion object {
            fun from(summary: ItemResult.PlaceTypeSummary) = PlaceTypeSummaryResponse(
                primaryType = summary.primaryType,
                types = summary.types,
                localizedPrimaryLabel = summary.localizedPrimaryLabel,
            )
        }
    }

    data class LocationResponse(
        val lat: Double,
        val lng: Double,
        val address: String?,
    ) {
        companion object {
            fun from(location: ItemResult.LocationInfo) = LocationResponse(
                lat = location.lat,
                lng = location.lng,
                address = location.address,
            )
        }
    }

    data class ItemResponse(
        val itemId: Long,
        val tripId: Long,
        val visitDay: Int,
        val itemOrder: Int,
        val placeId: Long?,
        val externalPlaceId: String?,
        val name: String,
        val title: String?,
        val time: LocalDateTime?,
        val memo: String?,
        val location: LocationResponse?,
        val placeTypeSummary: PlaceTypeSummaryResponse?,
        val openingStatusWarning: String?,
    ) {
        companion object {
            fun from(view: ItemResult.ItemView) = ItemResponse(
                itemId = view.itemId,
                tripId = view.tripId,
                visitDay = view.visitDay,
                itemOrder = view.itemOrder,
                placeId = view.placeId,
                externalPlaceId = view.externalPlaceId,
                name = view.name,
                title = view.title,
                time = view.time,
                memo = view.memo,
                location = view.location?.let(LocationResponse::from),
                placeTypeSummary = view.placeTypeSummary?.let(PlaceTypeSummaryResponse::from),
                openingStatusWarning = view.openingStatusWarning,
            )
        }
    }

    data class RouteDetailsResponse(
        val travelMode: String,
        val originPlace: PlaceRequests.SearchResponse,
        val destinationPlace: PlaceRequests.SearchResponse,
        val totalDuration: String,
        val totalDistance: String,
        val steps: List<RouteStepResponse>,
    ) {
        companion object {
            fun from(route: RouteDetails) = RouteDetailsResponse(
                travelMode = route.travelMode,
                originPlace = PlaceRequests.SearchResponse(
                    externalPlaceId = route.originPlace.externalPlaceId,
                    placeName = route.originPlace.placeName,
                    address = route.originPlace.address,
                    latitude = route.originPlace.latitude,
                    longitude = route.originPlace.longitude,
                ),
                destinationPlace = PlaceRequests.SearchResponse(
                    externalPlaceId = route.destinationPlace.externalPlaceId,
                    placeName = route.destinationPlace.placeName,
                    address = route.destinationPlace.address,
                    latitude = route.destinationPlace.latitude,
                    longitude = route.destinationPlace.longitude,
                ),
                totalDuration = route.totalDuration,
                totalDistance = route.totalDistance,
                steps = route.steps.map(RouteStepResponse::from),
            )
        }
    }

    data class RouteStepResponse(
        val travelMode: String,
        val instructions: String,
        val duration: String,
        val distance: String,
        val transitDetails: TransitDetailsResponse?,
    ) {
        companion object {
            fun from(step: RouteDetails.RouteStep) = RouteStepResponse(
                travelMode = step.travelMode,
                instructions = step.instructions,
                duration = step.duration,
                distance = step.distance,
                transitDetails = step.transitDetails?.let(TransitDetailsResponse::from),
            )
        }
    }

    data class TransitDetailsResponse(
        val lineName: String,
        val vehicleType: String,
        val vehicleIconUrl: String?,
        val numStops: Int,
        val departureStop: String,
        val arrivalStop: String,
    ) {
        companion object {
            fun from(details: RouteDetails.TransitDetails) = TransitDetailsResponse(
                lineName = details.lineName,
                vehicleType = details.vehicleType,
                vehicleIconUrl = details.vehicleIconUrl,
                numStops = details.numStops,
                departureStop = details.departureStop,
                arrivalStop = details.arrivalStop,
            )
        }
    }
}
