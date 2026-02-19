package com.example.taskassistant.storage

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage

object StorageHelper {
    private val storage = FirebaseStorage.getInstance()

    fun uploadTaskPhoto(
        taskId: String,
        photoUri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val filename = "tasks/$taskId/${System.currentTimeMillis()}.jpg"
        val ref = storage.reference.child(filename)

        ref.putFile(photoUri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString())
                }.addOnFailureListener { exception ->
                    onFailure(exception)
                }
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }
}