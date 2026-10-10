package com.example.odac26.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.odac26.model.Participant
import com.example.odac26.repository.MarathonRepository
import com.opencsv.CSVReaderBuilder
import kotlinx.coroutines.launch
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUploadScreen(
    onNavigateBack: () -> Unit
) {
    val repository = remember { MarathonRepository() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var pendingParticipantsToUpload by remember { mutableStateOf<List<Participant>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isProcessing = true
            statusMessage = "Parsing CSV..."
            scope.launch {
                try {
                    val participants = parseCsv(context, it)
                    pendingParticipantsToUpload = participants
                    statusMessage = "Parsed ${participants.size} participants. Awaiting confirmation."
                } catch (e: Exception) {
                    statusMessage = "Error parsing file: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin CSV Upload") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Upload Participants CSV",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Required columns: name, jersey, category (5k or 11k)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            if (isProcessing) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Button(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Select CSV File")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { showDeleteConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Clear All Data")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(text = statusMessage)
        }

        pendingParticipantsToUpload?.let { participants ->
            AlertDialog(
                onDismissRequest = { pendingParticipantsToUpload = null },
                title = { Text("Confirm CSV Upload") },
                text = { Text("Found ${participants.size} participants in the CSV. Are you sure you want to upload these records into Firebase?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val list = participants
                            pendingParticipantsToUpload = null
                            isProcessing = true
                            statusMessage = "Uploading ${list.size} participants..."
                            scope.launch {
                                val result = repository.uploadParticipants(list)
                                isProcessing = false
                                statusMessage = if (result.isSuccess) {
                                    "Successfully uploaded ${result.getOrNull()} participants."
                                } else {
                                    "Upload failed: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        }
                    ) {
                        Text("Upload")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingParticipantsToUpload = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Confirm Data Deletion") },
                text = { Text("Are you sure you want to permanently delete ALL participant data from the database? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            isProcessing = true
                            statusMessage = "Deleting all data..."
                            scope.launch {
                                val result = repository.deleteAllParticipants()
                                isProcessing = false
                                statusMessage = if (result.isSuccess) {
                                    "Successfully deleted ${result.getOrNull()} participants."
                                } else {
                                    "Deletion failed: ${result.exceptionOrNull()?.message}"
                                }
                            }
                        }
                    ) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

private fun parseCsv(context: android.content.Context, uri: Uri): List<Participant> {
    val participants = mutableListOf<Participant>()
    val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Could not open file")
    
    inputStream.use { stream ->
        val reader = CSVReaderBuilder(InputStreamReader(stream)).build()
        val allRows = reader.readAll()
        
        if (allRows.isEmpty()) throw Exception("CSV is empty")
        
        // Find column indices
        val header = allRows[0].map { it.lowercase().trim() }
        val nameIdx = header.indexOf("name")
        val jerseyIdx = header.indexOf("jersey")
        val categoryIdx = header.indexOf("category")
        val emailIdx = header.indexOf("email")
        
        if (nameIdx == -1 || jerseyIdx == -1 || categoryIdx == -1) {
            throw Exception("Missing required columns. Found: $header")
        }

        // Parse data rows
        for (i in 1 until allRows.size) {
            val row = allRows[i]
            if (row.size <= maxOf(nameIdx, jerseyIdx, categoryIdx)) continue
            
            val name = row[nameIdx].trim()
            val jersey = row[jerseyIdx].trim()
            val category = row[categoryIdx].trim().lowercase()
            val email = if (emailIdx != -1 && row.size > emailIdx) row[emailIdx].trim() else ""
            
            if (name.isNotEmpty() && jersey.isNotEmpty()) {
                participants.add(
                    Participant(
                        name = name,
                        jersey = jersey,
                        category = category,
                        email = email,
                        status = "absent"
                    )
                )
            }
        }
    }
    return participants
}
