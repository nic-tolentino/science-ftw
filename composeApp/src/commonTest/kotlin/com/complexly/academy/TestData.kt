package com.complexly.academy

import com.complexly.academy.data.JsonDataSource
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Option
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Question
import com.complexly.academy.model.Video

object TestData {
    val crashcourse = Channel(
        id = "crashcourse",
        title = "CrashCourse",
        description = "Tons of awesome courses on sciences, history, and more.",
        primaryColor = "#E67E22",
        accentColor = "#D35400"
    )

    val scishow = Channel(
        id = "scishow",
        title = "SciShow",
        description = "Exploring the unexpected, the strange, and the beautiful in science.",
        primaryColor = "#3498DB",
        accentColor = "#2980B9"
    )

    val pbseons = Channel(
        id = "pbseons",
        title = "PBS Eons",
        description = "A journey through the history of life on Earth.",
        primaryColor = "#1ABC9C",
        accentColor = "#16A085"
    )

    val allChannels = listOf(crashcourse, scishow, pbseons)

    // CrashCourse playlists
    val ccBiology = Playlist(
        id = "cc_biology",
        channelId = "crashcourse",
        title = "Biology",
        description = "A 50-episode journey through the science of life.",
        videoIds = listOf("cc_bio_01", "cc_bio_02", "cc_bio_03")
    )

    val ccChemistry = Playlist(
        id = "cc_chemistry",
        channelId = "crashcourse",
        title = "Chemistry",
        description = "46 episodes covering atoms, reactions, and the building blocks of matter.",
        videoIds = listOf("cc_chem_01", "cc_chem_02")
    )

    // SciShow playlists
    val ssSpace = Playlist(
        id = "ss_space",
        channelId = "scishow",
        title = "Space",
        description = "Exploring the mysteries of our solar system, galaxy, and the universe.",
        videoIds = listOf("ss_space_01", "ss_space_02")
    )

    // PBS Eons playlists
    val eoDeepTime = Playlist(
        id = "eo_deep_time",
        channelId = "pbseons",
        title = "Deep Time",
        description = "A chronological journey through Earth's history.",
        videoIds = listOf("eo_dt_01", "eo_dt_02")
    )

    val allPlaylists = listOf(ccBiology, ccChemistry, ssSpace, eoDeepTime)

    val question = Question(
        id = "q1",
        text = "What is the powerhouse of the cell?",
        options = listOf(
            Option("Nucleus", false, "Controls the cell"),
            Option("Mitochondria", true, "Generates energy"),
            Option("Ribosome", false, "Makes proteins")
        )
    )

    // CrashCourse Biology videos
    val ccBio01 = Video(
        id = "cc_bio_01",
        playlistIds = listOf("cc_biology"),
        title = "Intro to Biology: Crash Course Biology #1",
        description = "What is biology? How does life work?",
        durationSeconds = 680,
        youtubeId = "tZE_fQFK8EY",
        thumbnailUrl = "https://img.youtube.com/vi/tZE_fQFK8EY/hqdefault.jpg",
        questions = listOf(question),
        viewCount = 474493,
        releaseDate = "2024-01-15"
    )

    val ccBio02 = Video(
        id = "cc_bio_02",
        playlistIds = listOf("cc_biology"),
        title = "Water - Liquid Awesome: Crash Course Biology #2",
        description = "The awesome properties of water and why it makes life possible.",
        durationSeconds = 700,
        youtubeId = "HVT3Y3_gHGg",
        thumbnailUrl = "https://img.youtube.com/vi/HVT3Y3_gHGg/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 1231709,
        releaseDate = "2024-01-22"
    )

    val ccBio03 = Video(
        id = "cc_bio_03",
        playlistIds = listOf("cc_biology"),
        title = "Biological Molecules: Crash Course Biology #3",
        description = "Carbohydrates, lipids, proteins, and nucleic acids.",
        durationSeconds = 720,
        youtubeId = "UrRM6i6M5fE",
        thumbnailUrl = "https://img.youtube.com/vi/UrRM6i6M5fE/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 610026,
        releaseDate = "2024-01-29"
    )

    // CrashCourse Chemistry videos
    val ccChem01 = Video(
        id = "cc_chem_01",
        playlistIds = listOf("cc_chemistry"),
        title = "The Nucleus: Crash Course Chemistry #1",
        description = "A recap of what we know about the atom and the nucleus.",
        durationSeconds = 710,
        youtubeId = "FSyAehMdpyI",
        thumbnailUrl = "https://img.youtube.com/vi/FSyAehMdpyI/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 892000,
        releaseDate = "2024-02-01"
    )

    val ccChem02 = Video(
        id = "cc_chem_02",
        playlistIds = listOf("cc_chemistry"),
        title = "Orbitals: Crash Course Chemistry #2",
        description = "Atomic orbitals show where electrons are most likely to be found.",
        durationSeconds = 685,
        youtubeId = "btDtfxK2Xo0",
        thumbnailUrl = "https://img.youtube.com/vi/btDtfxK2Xo0/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 567000,
        releaseDate = "2024-02-08"
    )

    // SciShow Space videos
    val ssSpace01 = Video(
        id = "ss_space_01",
        playlistIds = listOf("ss_space"),
        title = "The Weirdest Star in the Universe",
        description = "SciShow discusses the mysterious dips in light from KIC 8462852.",
        durationSeconds = 520,
        youtubeId = "Y7S3Xk0v7Yc",
        thumbnailUrl = "https://img.youtube.com/vi/Y7S3Xk0v7Yc/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 2340000,
        releaseDate = "2024-03-01"
    )

    val ssSpace02 = Video(
        id = "ss_space_02",
        playlistIds = listOf("ss_space"),
        title = "What Is Dark Matter?",
        description = "SciShow explains what dark matter is and why we know it exists.",
        durationSeconds = 480,
        youtubeId = "6lHjGvRwNkE",
        thumbnailUrl = "https://img.youtube.com/vi/6lHjGvRwNkE/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 3100000,
        releaseDate = "2024-03-10"
    )

    // PBS Eons videos
    val eoDt01 = Video(
        id = "eo_dt_01",
        playlistIds = listOf("eo_deep_time"),
        title = "The First Animals of the Ediacaran",
        description = "Eons explores the mysterious Ediacaran biota.",
        durationSeconds = 600,
        youtubeId = "Nf4ms3jwi0Q",
        thumbnailUrl = "https://img.youtube.com/vi/Nf4ms3jwi0Q/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 890000,
        releaseDate = "2024-04-01"
    )

    val eoDt02 = Video(
        id = "eo_dt_02",
        playlistIds = listOf("eo_deep_time"),
        title = "When Life Exploded: The Cambrian Explosion",
        description = "Eons examines the rapid diversification of animal life.",
        durationSeconds = 620,
        youtubeId = "lJ9uRJJBNrQ",
        thumbnailUrl = "https://img.youtube.com/vi/lJ9uRJJBNrQ/hqdefault.jpg",
        questions = emptyList(),
        viewCount = 1500000,
        releaseDate = "2024-04-15"
    )

    val allVideos = listOf(
        ccBio01, ccBio02, ccBio03,
        ccChem01, ccChem02,
        ssSpace01, ssSpace02,
        eoDt01, eoDt02
    )
}

/**
 * Full multi-channel fake data source for testing search, filtering, and sorting.
 */
class FullFakeJsonDataSource : JsonDataSource {
    override suspend fun getChannels(): List<Channel> = TestData.allChannels
    override suspend fun getPlaylists(): List<Playlist> = TestData.allPlaylists
    override suspend fun getVideos(): List<Video> = TestData.allVideos
}
