package com.iconbiztechnologies1.mynetcape

data class AppInfo(
    val packageName: String,
    val appName: String,
    val iconUrl: String = "",
    var isBlocked: Boolean = false
)