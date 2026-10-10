package com.example.odac26.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.odac26.model.Participant
import com.example.odac26.repository.MarathonRepository

@Composable
fun DashboardScreen(
    onNavigateToUpload: () -> Unit = {},
    onNavigateToAttendance: () -> Unit = {},
    onNavigateToRaceStart: () -> Unit = {},
    onNavigateToScanner: () -> Unit = {}
) {
    val repository = remember { MarathonRepository() }
    val participants by repository.getParticipantsFlow().collectAsState(initial = emptyList())

    val finished = participants.filter { it.status == "finished" }
    val started = participants.filter { it.status == "started" }
    val absent = participants.filter { it.status == "absent" }

    var expandedSection by remember { mutableStateOf("Finished") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Finished Section
        AccordionHeader(
            title = "🏁 Finished (${finished.size})",
            isExpanded = expandedSection == "Finished",
            backgroundColor = Color(0xFFA5B8A6), // Greenish from mockup
            onClick = { expandedSection = if (expandedSection == "Finished") "" else "Finished" }
        )
        if (expandedSection == "Finished") {
            FinishedSectionContent(finished)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Started Section
        AccordionHeader(
            title = "🏃 Started (${started.size})",
            isExpanded = expandedSection == "Started",
            backgroundColor = Color(0xFFD4BBA5), // Orange/Brownish
            onClick = { expandedSection = if (expandedSection == "Started") "" else "Started" }
        )
        if (expandedSection == "Started") {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(started) { BasicParticipantRow(it) }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Absent Section
        AccordionHeader(
            title = "🚫 Absent (${absent.size} / ${participants.size})",
            isExpanded = expandedSection == "Absent",
            backgroundColor = Color(0xFFC7A2A2), // Redish
            onClick = { expandedSection = if (expandedSection == "Absent") "" else "Absent" }
        )
        if (expandedSection == "Absent") {
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(absent) { BasicParticipantRow(it) }
            }
        }
    }
}

@Composable
fun FinishedSectionContent(finishedParticipants: List<Participant>) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("5K", "11K")

    // Filter and sort by duration
    val sortedList = finishedParticipants
        .filter { it.category.equals(tabs[selectedTab], ignoreCase = true) }
        .mapNotNull { p ->
            val start = p.startedAt?.seconds
            val finish = p.finishedAt?.seconds
            if (start != null && finish != null) {
                p to (finish - start)
            } else null
        }
        .sortedBy { it.second }

    Column(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            itemsIndexed(sortedList) { index, pair ->
                val (participant, durationSec) = pair
                RankedParticipantRow(
                    rank = index + 1,
                    participant = participant,
                    durationFormatted = formatDuration(durationSec)
                )
            }
        }
    }
}

@Composable
fun AccordionHeader(title: String, isExpanded: Boolean, backgroundColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, color = Color.White)
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun RankedParticipantRow(rank: Int, participant: Participant, durationFormatted: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "$rank.", fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = participant.name, fontWeight = FontWeight.Bold)
            Text(text = "Jersey: ${participant.jersey}", fontSize = 12.sp, color = Color.Gray)
        }
        Text(text = durationFormatted, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
}

@Composable
fun BasicParticipantRow(participant: Participant) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = participant.name)
            Text(text = "Jersey: ${participant.jersey}", fontSize = 12.sp, color = Color.Gray)
        }
    }
    HorizontalDivider(color = Color.LightGray, thickness = 0.5.dp)
}

private fun formatDuration(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hrs > 0) String.format("%d:%02d:%02d", hrs, mins, secs)
    else String.format("%02d:%02d", mins, secs)
}
