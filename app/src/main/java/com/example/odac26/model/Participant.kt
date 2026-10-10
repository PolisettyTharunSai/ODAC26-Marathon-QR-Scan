package com.example.odac26.model

import com.google.firebase.Timestamp

data class Participant(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val jersey: String = "",
    val category: String = "", // "5k" or "11k"
    val status: String = "absent", // "absent", "present", "started", "finished"
    val startedAt: Timestamp? = null,
    val finishedAt: Timestamp? = null
)
