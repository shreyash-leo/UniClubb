package com.example.uniclubb

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class CreateEventActivity : AppCompatActivity() {

    companion object {
        private const val CLOUDINARY_UPLOAD_URL = "https://api.cloudinary.com/v1_1/ddzf4jpbm/image/upload"
        private const val CLOUDINARY_UPLOAD_PRESET = "android_upload"
        private const val PICK_IMAGE_REQUEST_CODE = 1001
    }

    private lateinit var imageFlyerPreview: ImageView
    private lateinit var btnUploadFlyer: Button
    private lateinit var editEventTitle: EditText
    private lateinit var editClubName: EditText
    private lateinit var editDate: EditText
    private lateinit var editVenue: EditText
    private lateinit var editCategory: EditText
    private lateinit var editDescription: EditText
    private lateinit var checkBoxRegistration: CheckBox
    private lateinit var layoutAdditionalOptions: LinearLayout
    private lateinit var checkBoxGroup: CheckBox
    private lateinit var checkBoxSong: CheckBox
    private lateinit var btnPreview: Button
    private lateinit var btnCreateEvent: Button

    private var selectedFlyerUri: Uri? = null
    private lateinit var clubId: String
    private lateinit var clubName: String
    private var clubLogoUrl: String? = null
    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_event)

        // Initialize Views
        imageFlyerPreview = findViewById(R.id.imageFlyerPreview)
        btnUploadFlyer = findViewById(R.id.btnUploadFlyer)
        editEventTitle = findViewById(R.id.editEventTitle)
        editClubName = findViewById(R.id.editClubName)
        editDate = findViewById(R.id.editDate)
        editVenue = findViewById(R.id.editVenue)
        editCategory = findViewById(R.id.editCategory)
        editDescription = findViewById(R.id.editDescription)
        checkBoxRegistration = findViewById(R.id.checkBoxRegistration)
        layoutAdditionalOptions = findViewById(R.id.layoutAdditionalOptions)
        checkBoxGroup = findViewById(R.id.checkBoxGroup)
        checkBoxSong = findViewById(R.id.checkBoxSong)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)

        // Get club data from intent
        clubId = intent.getStringExtra("clubId") ?: ""
        clubName = intent.getStringExtra("clubName") ?: ""
        clubLogoUrl = intent.getStringExtra("clubLogoUrl")
        editClubName.setText(clubName)

        setupListeners()
    }

    private fun setupListeners() {
        checkBoxRegistration.setOnCheckedChangeListener { _, isChecked ->
            layoutAdditionalOptions.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
        }

        editDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
                val formatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.time)
                editDate.setText(formatted)
            },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnUploadFlyer.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }

        btnCreateEvent.setOnClickListener {
            uploadFlyerAndCreateEvent()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedFlyerUri = data.data
            Glide.with(this).load(selectedFlyerUri).into(imageFlyerPreview)
        }
    }

    private fun showLoadingDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setView(ProgressBar(this))
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun hideLoadingDialog() {
        loadingDialog?.dismiss()
    }

    private fun uploadFlyerAndCreateEvent() {
        val flyerUri = selectedFlyerUri
        if (flyerUri == null) {
            Toast.makeText(this, "Please upload a flyer", Toast.LENGTH_SHORT).show()
            return
        }

        val title = editEventTitle.text.toString().trim()
        val date = editDate.text.toString().trim()
        val venue = editVenue.text.toString().trim()
        val category = editCategory.text.toString().trim()
        val description = editDescription.text.toString().trim()
        val registrationRequired = checkBoxRegistration.isChecked
        val allowGroup = checkBoxGroup.isChecked
        val allowSong = checkBoxSong.isChecked

        if (title.isEmpty() || date.isEmpty() || venue.isEmpty() || category.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        btnCreateEvent.isEnabled = false
        showLoadingDialog()

        val inputStream = contentResolver.openInputStream(flyerUri)
        val bytes = inputStream?.readBytes()
        val fileType = contentResolver.getType(flyerUri)?.substringAfter("/") ?: "jpg"

        if (bytes != null) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", "flyer.$fileType", bytes.toRequestBody("image/*".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                .build()

            val request = Request.Builder()
                .url(CLOUDINARY_UPLOAD_URL)
                .post(requestBody)
                .build()

            OkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        hideLoadingDialog()
                        btnCreateEvent.isEnabled = true
                        Toast.makeText(this@CreateEventActivity, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val responseData = response.body?.string()
                        val flyerUrl = JSONObject(responseData ?: "").getString("secure_url")

                        runOnUiThread {
                            hideLoadingDialog()
                            btnCreateEvent.isEnabled = true
                            saveEventToFirestore(
                                flyerUrl, title, date, venue, category,
                                description, registrationRequired, allowGroup, allowSong
                            )
                        }
                    } else {
                        runOnUiThread {
                            hideLoadingDialog()
                            btnCreateEvent.isEnabled = true
                            Toast.makeText(this@CreateEventActivity, "Cloudinary error", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } else {
            hideLoadingDialog()
            btnCreateEvent.isEnabled = true
            Toast.makeText(this, "Failed to read image data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveEventToFirestore(
        flyerUrl: String,
        title: String,
        date: String,
        venue: String,
        category: String,
        description: String,
        registrationRequired: Boolean,
        allowGroup: Boolean,
        allowSong: Boolean
    ) {
        val db = FirebaseFirestore.getInstance()
        val eventId = UUID.randomUUID().toString()

        val event = hashMapOf(
            "eventId" to eventId,
            "flyerUrl" to flyerUrl,
            "title" to title,
            "clubId" to clubId,
            "clubName" to clubName,
            "clubLogoUrl" to clubLogoUrl,
            "date" to date,
            "venue" to venue,
            "category" to category,
            "description" to description,
            "registrationRequired" to registrationRequired,
            "allowGroup" to allowGroup,
            "allowSong" to allowSong,
            "timestamp" to Timestamp.now()
        )

        db.collection("events").document(eventId).set(event)
            .addOnSuccessListener {
                Toast.makeText(this, "Event created successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save event", Toast.LENGTH_SHORT).show()
            }
    }
}
