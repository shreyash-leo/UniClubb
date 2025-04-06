package com.example.uniclubb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore

class EditClubInfoActivity : AppCompatActivity() {

    private lateinit var editClubName: EditText
    private lateinit var editDescription: EditText
    private lateinit var editCategory: EditText
    private lateinit var imageLogo: ImageView
    private lateinit var imageBanner: ImageView
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()
    private var clubId: String? = null
    private var logoUri: Uri? = null
    private var bannerUri: Uri? = null

    private var existingLogoUrl: String? = null
    private var existingBannerUrl: String? = null

    companion object {
        const val PICK_LOGO = 1001
        const val PICK_BANNER = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_club_info)

        editClubName = findViewById(R.id.editClubName)
        editDescription = findViewById(R.id.editDescription)
        editCategory = findViewById(R.id.editCategory)
        imageLogo = findViewById(R.id.imageLogo)
        imageBanner = findViewById(R.id.imageBanner)
        btnSave = findViewById(R.id.btnSaveChanges)

        clubId = intent.getStringExtra("clubId")
        if (clubId == null) {
            Toast.makeText(this, "Club ID missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadClubData()

        imageLogo.setOnClickListener {
            pickImage(PICK_LOGO)
        }

        imageBanner.setOnClickListener {
            pickImage(PICK_BANNER)
        }

        btnSave.setOnClickListener {
            saveChanges()
        }
    }

    private fun loadClubData() {
        db.collection("clubs").document(clubId!!)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("clubname")
                val desc = doc.getString("description")
                val category = doc.getString("category")
                existingLogoUrl = doc.getString("logoImageUrl")
                existingBannerUrl = doc.getString("bannerImageUrl")

                editClubName.setText(name)
                editDescription.setText(desc)
                editCategory.setText(category)

                Glide.with(this).load(existingLogoUrl).into(imageLogo)
                Glide.with(this).load(existingBannerUrl).into(imageBanner)
            }
    }

    private fun pickImage(requestCode: Int) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            when (requestCode) {
                PICK_LOGO -> {
                    logoUri = imageUri
                    imageLogo.setImageURI(logoUri)
                }
                PICK_BANNER -> {
                    bannerUri = imageUri
                    imageBanner.setImageURI(bannerUri)
                }
            }
        }
    }

    private fun saveChanges() {
        val updatedName = editClubName.text.toString().trim()
        val updatedDesc = editDescription.text.toString().trim()
        val updatedCategory = editCategory.text.toString().trim()

        if (updatedName.isEmpty() || updatedDesc.isEmpty() || updatedCategory.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Saving changes...", Toast.LENGTH_SHORT).show()

        uploadImagesAndSave(updatedName, updatedDesc, updatedCategory)
    }

    private fun uploadImagesAndSave(name: String, desc: String, category: String) {
        var uploadedLogoUrl = existingLogoUrl
        var uploadedBannerUrl = existingBannerUrl

        val uploadTasks = mutableListOf<Unit>()

        if (logoUri != null) {
            MediaManager.get().upload(logoUri)
                .unsigned("your_unsigned_preset")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {}

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        uploadedLogoUrl = resultData?.get("secure_url") as? String
                        checkUploadAndSave(name, desc, category, uploadedLogoUrl, uploadedBannerUrl)
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Toast.makeText(this@EditClubInfoActivity, "Logo upload failed", Toast.LENGTH_SHORT).show()
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                })
                .dispatch()
        }

        if (bannerUri != null) {
            MediaManager.get().upload(bannerUri)
                .unsigned("your_unsigned_preset") // Replace with your preset
                .callback(object : com.cloudinary.android.callback.UploadCallback {
                    override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                        uploadedBannerUrl = resultData?.get("secure_url") as? String
                        checkUploadAndSave(name, desc, category, uploadedLogoUrl, uploadedBannerUrl)
                    }
                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Toast.makeText(this@EditClubInfoActivity, "Banner upload failed", Toast.LENGTH_SHORT).show()
                    }
                    override fun onStart(requestId: String?) {}
                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}
                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
                }).dispatch()
        }

        if (logoUri == null && bannerUri == null) {
            checkUploadAndSave(name, desc, category, uploadedLogoUrl, uploadedBannerUrl)
        }
    }

    private fun checkUploadAndSave(
        name: String,
        desc: String,
        category: String,
        logoUrl: String?,
        bannerUrl: String?
    ) {
        val updateMap = hashMapOf(
            "clubname" to name,
            "description" to desc,
            "category" to category,
            "logoImageUrl" to logoUrl,
            "bannerImageUrl" to bannerUrl
        )

        db.collection("clubs").document(clubId!!)
            .update(updateMap as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(this, "Club info updated!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
            }
    }
}
