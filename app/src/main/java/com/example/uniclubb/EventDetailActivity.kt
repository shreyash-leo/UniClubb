package com.example.uniclubb

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class EventDetailActivity : AppCompatActivity() {

    private lateinit var imageFlyer: ImageView
    private lateinit var imageClubLogo: ImageView
    private lateinit var textClubName: TextView
    private lateinit var textTitle: TextView
    private lateinit var textDate: TextView
    private lateinit var textVenue: TextView
    private lateinit var textCategory: TextView
    private lateinit var textDescription: TextView
    private lateinit var btnRegister: Button

    private val PICK_AUDIO_REQUEST = 101
    private var selectedSongUri: Uri? = null

    private var eventId: String? = null
    private var clubId: String? = null
    private var registrationRequired = false
    private var allowGroup = false
    private var allowSong = false
    private var currentUserEmail: String? = null

    private var presidentEmail: String? = null
    private var supervisorEmail: String? = null
    private var isAdmin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        imageFlyer = findViewById(R.id.imageFlyer)
        imageClubLogo = findViewById(R.id.imageClubLogo)
        textClubName = findViewById(R.id.textClubName)
        textTitle = findViewById(R.id.textTitle)
        textDate = findViewById(R.id.textDate)
        textVenue = findViewById(R.id.textVenue)
        textCategory = findViewById(R.id.textCategory)
        textDescription = findViewById(R.id.textDescription)
        btnRegister = findViewById(R.id.btnRegister)

        eventId = intent.getStringExtra("eventId")
        if (eventId == null) {
            Toast.makeText(this, "Invalid Event", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        loadEventDetails()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_event_admin, menu)
        menu?.findItem(R.id.menu_edit_event)?.isVisible = isAdmin
        menu?.findItem(R.id.menu_cancel_event)?.isVisible = isAdmin
        menu?.findItem(R.id.menu_view_registrations)?.isVisible = isAdmin
        menu?.findItem(R.id.menu_send_notification)?.isVisible = isAdmin
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_event -> editEvent()
            R.id.menu_cancel_event -> cancelEvent()
            R.id.menu_view_registrations -> viewRegistrations()
            R.id.menu_send_notification -> sendNotification()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadEventDetails() {
        val db = FirebaseFirestore.getInstance()
        db.collection("events").document(eventId!!)
            .get()
            .addOnSuccessListener { doc ->
                val event = doc.toObject(Event::class.java)
                if (event != null) {
                    Glide.with(this).load(event.flyerUrl).into(imageFlyer)
                    Glide.with(this).load(event.clubLogoUrl).into(imageClubLogo)

                    textClubName.text = event.clubName
                    textTitle.text = event.title
                    textDate.text = event.date
                    textVenue.text = event.venue
                    textCategory.text = event.category
                    textDescription.text = event.description

                    registrationRequired = event.registrationRequired
                    allowGroup = event.allowGroup
                    allowSong = event.allowSong
                    clubId = event.clubId

                    if (registrationRequired) {
                        btnRegister.visibility = View.VISIBLE
                        btnRegister.setOnClickListener {
                            showRegistrationForm()
                        }
                    } else {
                        btnRegister.visibility = View.GONE
                    }

                    loadClubData()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadClubData() {
        if (clubId == null) return
        val db = FirebaseFirestore.getInstance()
        db.collection("clubs").document(clubId!!)
            .get()
            .addOnSuccessListener { doc ->
                val club = doc.toObject(Club::class.java)
                presidentEmail = club?.presidentEmail
                supervisorEmail = club?.supervisor?.supervisorEmail
                isAdmin = currentUserEmail == presidentEmail || currentUserEmail == supervisorEmail
                invalidateOptionsMenu()
            }
    }

    private fun showRegistrationForm() {
        val formView = layoutInflater.inflate(R.layout.activity_registration, null)
        val editName = formView.findViewById<EditText>(R.id.editName)
        val editContact = formView.findViewById<EditText>(R.id.editContact)
        val editDept = formView.findViewById<EditText>(R.id.editDept)
        val checkGroup = formView.findViewById<CheckBox>(R.id.checkGroup)
        val btnSelectSong = formView.findViewById<Button>(R.id.btnSelectSong)

        checkGroup.visibility = if (allowGroup) View.VISIBLE else View.GONE
        btnSelectSong.visibility = if (allowSong) View.VISIBLE else View.GONE

        btnSelectSong.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "audio/*"
            startActivityForResult(intent, PICK_AUDIO_REQUEST)
        }

        AlertDialog.Builder(this)
            .setTitle("Register for ${textTitle.text}")
            .setView(formView)
            .setPositiveButton("Submit") { _, _ ->
                val name = editName.text.toString().trim()
                val contact = editContact.text.toString().trim()
                val dept = editDept.text.toString().trim()
                val isGroup = checkGroup.isChecked

                if (name.isEmpty() || contact.isEmpty() || dept.isEmpty()) {
                    Toast.makeText(this, "Fill all required fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (allowSong && selectedSongUri != null) {
                    uploadSongToCloudinary(selectedSongUri!!) { songUrl ->
                        saveRegistration(name, contact, dept, isGroup, songUrl)
                    }
                } else {
                    saveRegistration(name, contact, dept, isGroup, null)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadSongToCloudinary(uri: Uri, onComplete: (String?) -> Unit) {
        val stream = contentResolver.openInputStream(uri) ?: return
        val requestBody = stream.readBytes()

        val request = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "song.mp3",
                RequestBody.create("audio/mpeg".toMediaTypeOrNull(), requestBody)
            )
            .addFormDataPart("upload_preset", "android_upload")
            .build()

        val cloudinaryUrl = "https://api.cloudinary.com/v1_1/ddzf4jpbm/raw/upload"

        val client = OkHttpClient()
        val req = Request.Builder().url(cloudinaryUrl).post(request).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EventDetailActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                    onComplete(null)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                val json = JSONObject(responseBody ?: "{}")
                val url = json.optString("secure_url", null)
                runOnUiThread {
                    onComplete(url)
                }
            }
        })
    }

    private fun saveRegistration(
        name: String,
        contact: String,
        dept: String,
        isGroup: Boolean,
        songUrl: String?
    ) {
        val registration = hashMapOf(
            "eventId" to eventId,
            "name" to name,
            "contact" to contact,
            "department" to dept,
            "isGroup" to isGroup.takeIf { allowGroup },
            "songUrl" to songUrl.takeIf { allowSong },
            "timestamp" to Timestamp.now()
        )

        FirebaseFirestore.getInstance()
            .collection("event_registrations")
            .add(registration)
            .addOnSuccessListener {
                Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to register", Toast.LENGTH_SHORT).show()
            }
    }

    private fun editEvent() {
        Toast.makeText(this, "Edit Event clicked", Toast.LENGTH_SHORT).show()
    }

    private fun cancelEvent() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Event")
            .setMessage("Are you sure you want to cancel this event?")
            .setPositiveButton("Yes") { _, _ ->
                FirebaseFirestore.getInstance()
                    .collection("events")
                    .document(eventId!!)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Event cancelled", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun viewRegistrations() {
        Toast.makeText(this, "View Registrations clicked", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK) {
            selectedSongUri = data?.data
            Toast.makeText(this, "Song selected!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendNotification() {
        if (clubId == null) {
            Toast.makeText(this, "Club ID not available", Toast.LENGTH_SHORT).show()
            return
        }
        showSendNotificationDialog()
    }

    private fun showSendNotificationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_send_notification, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editNotificationTitle)
        val editMessage = dialogView.findViewById<EditText>(R.id.editNotificationMessage)

        AlertDialog.Builder(this)
            .setTitle("Send Notification")
            .setView(dialogView)
            .setPositiveButton("Send") { _, _ ->
                val title = editTitle.text.toString().trim()
                val message = editMessage.text.toString().trim()

                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (clubId == null) {
                    Toast.makeText(this, "Club ID not found", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val notification = hashMapOf(
                    "clubId" to clubId!!,
                    "title" to title,
                    "message" to message,
                    "timestamp" to Timestamp.now()
                )

                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .add(notification)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Notification sent", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to send notification", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
