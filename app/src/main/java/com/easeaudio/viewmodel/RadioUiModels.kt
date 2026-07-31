package com.easeaudio.viewmodel

import androidx.annotation.StringRes

data class GenreDisplay(
    val key: String,
    @StringRes val labelResId: Int
)

data class EqPresetDisplay(
    val key: String,
    @StringRes val labelResId: Int
)

data class CountryDisplay(
    val name: String,
    val flag: String,
    val code: String = "",
    val stationCountText: String = ""
)
