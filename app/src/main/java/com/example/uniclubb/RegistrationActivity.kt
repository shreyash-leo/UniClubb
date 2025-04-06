package com.example.uniclubb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.uniclubb.Event
import com.example.uniclubb.model.EventRegistration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationActivity : AppCompatActivity() {

    private lateinit var editName: EditText
    private lateinit var editContact: EditText
    private lateinit var editDepartment: EditText
    private lateinit var checkGroup: CheckBox
    private lateinit var editGroupName: EditText
    private lateinit var editSongName: EditText
    private lateinit var btnUploadSong: Button
    private lateinit var textSongFile: TextView
    private lateinit var btnSubmit: Button

    private var songUri: Uri? = null
    private var allowGroup = false
    private var allowSong = false
    private lateinit var eventId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize Cloudinary (make sure it's configured in your app startup)
        CloudinaryConfig.initialize(this)

        // Bind Views
        editName = findViewById(R.id.editName)
        editContact = findViewById(R.id.editContact)
        editDepartment = findViewById(R.id.editDept)
        checkGroup = findViewById(R.id.checkGroup)
        editGroupName = findViewById(R.id.editGroupName)
        editSongName = findViewById(R.id.editSong)
        btnUploadSong = findViewById(R.id.btnSelectSong)
        textSongFile = findViewById(R.id.textSongFile)
        btnSubmit = findViewById(R.id.btnSubmit)

        eventId = intent.getStringExtra("eventId") ?: return

        // Fetch event to check feature toggles
        FirebaseFirestore.getInstance().collection("events").document(eventId)
            .get()
            .addOnSuccessListener { document ->
                val event = document.toObject(Event::class.java)
                allowGroup = event?.allowGroup == true
                allowSong = event?.allowSong == true

                checkGroup.visibility = if (allowGroup) View.VISIBLE else View.GONE
                editGroupName.visibility = View.GONE // initially hidden

                editSongName.visibility = if (allowSong) View.VISIBLE else View.GONE
                btnUploadSong.visibility = if (allowSong) View.VISIBLE else View.GONE
                textSongFile.visibility = if (allowSong) View.VISIBLE else View.GONE
            }

        // Toggle group name field when checkbox is clicked
        checkGroup.setOnCheckedChangeListener { _, isChecked ->
            editGroupName.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Song upload intent
        btnUploadSong.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, 2001)
        }

        btnSubmit.setOnClickListener {
            submitRegistration()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2001 && resultCode == Activity.RESULT_OK && data != null) {
            songUri = data.data
            textSongFile.text = "Selected: ${songUri?.lastPathSegment}"
        }
    }

    private fun submitRegistration() {
        val name = editName.text.toString().trim()
        val contact = editContact.text.toString().trim()
        val dept = editDepartment.text.toString().trim()
        val isGroup = checkGroup.isChecked
        val groupName = editGroupName.text.toString().trim()
        val songName = editSongName.text.toString().trim()

        if (name.isEmpty() || contact.isEmpty() || dept.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (allowGroup && isGroup && groupName.isEmpty()) {
            Toast.makeText(this, "Enter group name", Toast.LENGTH_SHORT).show()
            return
        }

        fun saveToFirestore(songUrl: String?) {
            val registration = EventRegistration(
                userId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                userName = name,
                contactNumber = contact,
                department = dept,
                isGroup = isGroup,
                groupName = if (isGroup) groupName else null,
                songUrl = songUrl,
                timestamp = System.currentTimeMillis()
            )

            FirebaseFirestore.getInstance()
                .collection("events")
                .document(eventId)
                .collection("registrations")
                .add(registration)
                .addOnSuccessListener {
                    Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
                }
        }

        if (allowSong && songUri != null) {
            Toast.makeText(this, "Uploading song...", Toast.LENGTH_SHORT).show()
            uploadSongToCloudinary(songUri!!,
                onSuccess = { url -> saveToFirestore(url) },
                onFailure = {
                    Toast.makeText(this, "Song upload failed", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            saveToFirestore(null)
        }
    }

    private fun uploadSongToCloudinary(
        uri: Uri,
        onSuccess: (String) -> Unit,
        onFailure: () -> Unit
    ) {
        MediaManager.get().upload(uri)
            .option("resource_type", "auto")
            .unsigned("android_upload") // ✅ Your unsigned preset name
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                override fun onSuccess(requestId: String?, resultData: Map<*, *>?) {
                    val url = resultData?.get("secure_url") as? String
                    if (url != null) {
                        onSuccess(url)
                    } else {
                        onFailure()
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    onFailure()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }
}
