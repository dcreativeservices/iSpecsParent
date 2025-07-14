package com.ispecs.parent.model

data class ChildConfig(
    val macAddress: String = "",
    val screenTimeLimit: Int = 60,
    val blurType: String = "popup",
    val batteryAlertLevel: Int = 20,
    val allowedSites: List<String> = listOf(
        "https://www.youtube.com/kids",
        "https://kids.nationalgeographic.com",
        "https://www.sesamestreet.org",
        "https://www.funbrain.com",
        "https://www.pbskids.org"
    )
)
