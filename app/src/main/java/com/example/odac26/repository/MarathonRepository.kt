package com.example.odac26.repository

import com.example.odac26.model.Participant
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MarathonRepository {
    // Delay initialization until the object is actually used
    private val db by lazy { FirebaseFirestore.getInstance() }
    private val participantsCollection by lazy { db.collection("participants") }

    // Real-time stream of all participants
    fun getParticipantsFlow(): Flow<List<Participant>> = callbackFlow {
        val subscription = participantsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val participants = snapshot.documents.mapNotNull { it.toObject(Participant::class.java) }
                trySend(participants)
            }
        }
        awaitClose { subscription.remove() }
    }

    suspend fun getParticipants(): List<Participant> {
        return participantsCollection.get().await().toObjects(Participant::class.java)
    }

    suspend fun markAttendance(jersey: String): Result<Unit> {
        return try {
            val querySnapshot = participantsCollection.whereEqualTo("jersey", jersey).get().await()
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Participant not found"))
            }
            
            val doc = querySnapshot.documents.first()
            val currentStatus = doc.getString("status")
            if (currentStatus == "absent") {
                participantsCollection.document(doc.id).update("status", "present").await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Participant already marked as $currentStatus"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startRace(category: String): Result<Int> {
        return try {
            val querySnapshot = participantsCollection
                .whereEqualTo("category", category)
                .whereEqualTo("status", "present")
                .get()
                .await()

            if (querySnapshot.isEmpty) {
                return Result.success(0)
            }

            db.runBatch { batch ->
                for (doc in querySnapshot.documents) {
                    batch.update(
                        participantsCollection.document(doc.id),
                        mapOf(
                            "status" to "started",
                            "startedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(querySnapshot.size())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markFinished(jersey: String): Result<Participant> {
        return try {
            val querySnapshot = participantsCollection.whereEqualTo("jersey", jersey).get().await()
            if (querySnapshot.isEmpty) {
                return Result.failure(Exception("Participant not found"))
            }

            val doc = querySnapshot.documents.first()
            val participant = doc.toObject(Participant::class.java)!!

            when (participant.status) {
                "finished" -> return Result.failure(Exception("Already finished"))
                "started" -> {
                    participantsCollection.document(doc.id).update(
                        mapOf(
                            "status" to "finished",
                            "finishedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    
                    // Fetch updated doc to get the exact timestamp
                    val updatedDoc = participantsCollection.document(doc.id).get().await()
                    Result.success(updatedDoc.toObject(Participant::class.java)!!)
                }
                else -> Result.failure(Exception("Participant must be 'started' to finish (Current: ${participant.status})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadParticipants(participants: List<Participant>): Result<Int> {
        return try {
            var count = 0
            // Firestore batches can hold up to 500 writes
            participants.chunked(500).forEach { chunk ->
                db.runBatch { batch ->
                    chunk.forEach { participant ->
                        // Use the jersey number as the Document ID to enforce uniqueness
                        val docRef = participantsCollection.document(participant.jersey)
                        batch.set(docRef, participant.copy(id = docRef.id))
                        count++
                    }
                }.await()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllParticipants(): Result<Int> {
        return try {
            val querySnapshot = participantsCollection.get().await()
            if (querySnapshot.isEmpty) {
                return Result.success(0)
            }

            var count = 0
            querySnapshot.documents.chunked(500).forEach { chunk ->
                db.runBatch { batch ->
                    chunk.forEach { doc ->
                        batch.delete(doc.reference)
                        count++
                    }
                }.await()
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAdminPassword(inputPassword: String): Boolean {
        return try {
            val doc = db.collection("config").document("admin").get().await()
            val remotePassword = doc.getString("password")
            val validPassword = if (!remotePassword.isNullOrBlank()) remotePassword else "odac@26"
            inputPassword == validPassword
        } catch (e: Exception) {
            inputPassword == "odac@26"
        }
    }
}
