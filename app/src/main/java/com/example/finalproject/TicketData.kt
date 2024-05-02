package com.example.finalproject

import android.os.Build
import android.provider.MediaStore.Audio.Genres
import androidx.annotation.RequiresApi
import java.time.LocalDate

data class TicketData(

    val _embedded: EmbeddedData
)
data class EmbeddedData(
    val events: List<EventData>
)

data class EventData(
    val name: String,
    val url: String,
    val type: String,
    val images:  List<Images>,
    val dates: Dates,
    val _embedded: EmbeddedVenue,
    val priceRanges: List<PriceRangeData>,
    val id: String,
    val distance: Double,
    val classifications: List<Classifications>,
    var isEventPassed: Boolean = false

)
data class Images(
    val width: Int,
    val height: Int,
    val url: String
)
data class Dates(
    val start: Start
)
data class Classifications(
    val genre: Genre,
    val segment: Segment
)
data class Genre (
    val name: String
)
data class Segment(
    val name: String
)

data class Start(
    val localDate: String,
    val localTime: String
)

data class EmbeddedVenue(
    val venues: List<VenueData>
)

data class VenueData(
    val name: String,
    val city: City,
    val state: State,
    val address: Address
)
data class Address(
    val line1: String
)
data class State(
    val stateCode: String
)
data class City(
    val name: String
)

data class PriceRangeData(
    val min: Double,
    val max: Double,
    val currency: String
)

@RequiresApi(Build.VERSION_CODES.O)
fun eventPassed(eventData: EventData) : Boolean {
    val curDate = LocalDate.now()
    val eventDate = LocalDate.parse(eventData.dates.start.localDate)
    eventData.isEventPassed = eventDate.isBefore(curDate)
    return eventDate.isBefore(curDate)


}

