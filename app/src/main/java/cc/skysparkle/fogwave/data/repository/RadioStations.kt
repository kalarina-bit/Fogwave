package cc.skysparkle.fogwave.data.repository

import cc.skysparkle.fogwave.R
import cc.skysparkle.fogwave.data.model.Genre
import cc.skysparkle.fogwave.data.model.RadioStation

// Built once per process and shared by the UI and the playback service.
val RadioStations: List<RadioStation> = listOf(
    RadioStation(
        id = "zip",
        name = "ZIP FM",
        descriptionRes = R.string.station_desc_zip,
        streamUrl = "https://stream4.radijas.lt/zipfm",
        iconResId = R.drawable.ic_station_zipfm,
        genres = listOf(Genre.HITS, Genre.POP, Genre.LITHUANIAN),
        bitrate = "MP3"
    ),
    RadioStation(
        id = "powerhit",
        name = "Power Hit Radio",
        descriptionRes = R.string.station_desc_powerhit,
        // Stable revma entry point: it redirects to a fresh node URL with a short-lived token.
        streamUrl = "https://stream.rcs.revma.com/f31w7e0fveuvv",
        iconResId = R.drawable.ic_station_powerhit,
        genres = listOf(Genre.DANCE, Genre.POP, Genre.HITS),
        bitrate = "AAC"
    ),
    RadioStation(
        id = "powerhitgold",
        name = "Power Hit Radio Gold",
        descriptionRes = R.string.station_desc_powerhitgold,
        streamUrl = "https://stream.rcs.revma.com/asdpz3qfbhuvv",
        iconResId = R.drawable.ic_station_powerhitgold,
        genres = listOf(Genre.DANCE, Genre.OLDIES, Genre.HITS),
        bitrate = "AAC"
    ),
    RadioStation(
        id = "rock",
        name = "Rock FM",
        descriptionRes = R.string.station_desc_rock,
        streamUrl = "https://stream4.radijas.lt/rockfm",
        iconResId = R.drawable.ic_station_rockfm,
        genres = listOf(Genre.ROCK, Genre.LITHUANIAN),
        bitrate = "MP3",
        lightBackdrop = true
    ),
    RadioStation(
        id = "relax",
        name = "Relax FM",
        descriptionRes = R.string.station_desc_relax,
        streamUrl = "https://stream4.radijas.lt/relaxfm",
        iconResId = R.drawable.ic_station_relaxfm,
        genres = listOf(Genre.EASY_LISTENING),
        bitrate = "MP3"
    ),
    RadioStation(
        id = "lietus",
        name = "Lietus",
        descriptionRes = R.string.station_desc_lietus,
        streamUrl = "https://stream.m-1.fm/lietus/aacp64",
        iconResId = R.drawable.ic_station_lietus,
        genres = listOf(Genre.LITHUANIAN, Genre.POP),
        bitrate = "AAC 64 kbps",
        lightBackdrop = true
    ),
    RadioStation(
        id = "m1",
        name = "M-1",
        descriptionRes = R.string.station_desc_m1,
        streamUrl = "https://stream.m-1.fm/m1/aacp64",
        iconResId = R.drawable.ic_station_m1,
        genres = listOf(Genre.POP, Genre.HITS),
        bitrate = "AAC 64 kbps"
    ),
    RadioStation(
        id = "plus",
        name = "M-1 Plius",
        descriptionRes = R.string.station_desc_plus,
        streamUrl = "https://radio.m-1.fm/m1plius/aacp64",
        iconResId = R.drawable.ic_station_m1plus,
        genres = listOf(Genre.POP, Genre.OLDIES),
        bitrate = "AAC 64 kbps",
        lightBackdrop = true
    ),
    RadioStation(
        id = "goldfm",
        name = "Gold FM",
        descriptionRes = R.string.station_desc_goldfm,
        streamUrl = "https://stream.goldfm.lt/goldfm.aac",
        iconResId = R.drawable.ic_station_goldfm,
        genres = listOf(Genre.OLDIES, Genre.HITS, Genre.LITHUANIAN),
        bitrate = "AAC"
    ),
    RadioStation(
        id = "rc",
        name = "RC",
        descriptionRes = R.string.station_desc_rc,
        streamUrl = "https://stream1.rc.lt/rc128.mp3",
        iconResId = R.drawable.ic_station_rc,
        genres = listOf(Genre.HITS, Genre.LITHUANIAN),
        bitrate = "MP3 128 kbps",
        lightBackdrop = true
    )
)
