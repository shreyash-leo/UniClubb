package com.example.uniclubb

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CreateClubActivity : AppCompatActivity() {

    private lateinit var bannerImageView: ImageView
    private lateinit var logoImageView: ImageView
    private lateinit var clubNameEditText: EditText
    private lateinit var clubCategoryEditText: EditText
    private lateinit var clubDescriptionEditText: EditText
    private lateinit var presidentNameEditText: EditText
    private lateinit var presidentEmailEditText: EditText
    private lateinit var presidentContactEditText: EditText
    private lateinit var membersContainer: LinearLayout
    private lateinit var addMemberButton: Button
    private lateinit var supervisorCheckBox: CheckBox
    private lateinit var supervisorDetailsLayout: LinearLayout
    private lateinit var supervisorNameEditText: EditText
    private lateinit var supervisorEmailEditText: EditText
    private lateinit var supervisorContactEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private var memberCount = 0
    private var selectedBannerUri: Uri? = null
    private var selectedLogoUri: Uri? = null

    private val firestore = FirebaseFirestore.getInstance()
    private val clubsCollection = firestore.collection("clubs")
    private var bannerCloudinaryUrl: String? = null
    private var logoCloudinaryUrl: String? = null

    companion object {
        private const val TAG = "CreateClubActivity"
        private const val PICK_BANNER_IMAGE_REQUEST = 1
        private const val PICK_LOGO_IMAGE_REQUEST = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_create_club)

        // Initialize views
        bannerImageView = findViewById(R.id.clubBannerImageView)
        logoImageView = findViewById(R.id.clubLogoImageView)
        clubNameEditText = findViewById(R.id.clubNameEditText)
        clubCategoryEditText = findViewById(R.id.clubCategoryEditText)
        clubDescriptionEditText = findViewById(R.id.clubDescriptionEditText)
        presidentNameEditText = findViewById(R.id.presidentNameEditText)
        presidentEmailEditText = findViewById(R.id.presidentEmailEditText)
        presidentContactEditText = findViewById(R.id.presidentContactEditText)
        membersContainer = findViewById(R.id.membersContainer)
        addMemberButton = findViewById(R.id.addMemberButton)
        supervisorCheckBox = findViewById(R.id.supervisorCheckBox)
        supervisorDetailsLayout = findViewById(R.id.supervisorDetailsLayout)
        supervisorNameEditText = findViewById(R.id.supervisorNameEditText)
        supervisorEmailEditText = findViewById(R.id.supervisorEmailEditText)
        supervisorContactEditText = findViewById(R.id.supervisorContactEditText)
        submitButton = findViewById(R.id.submitButton)
        progressBar = findViewById(R.id.progressBar)

        // Image Selection Logic
        bannerImageView.setOnClickListener {
            openImageChooser(PICK_BANNER_IMAGE_REQUEST)
        }
        logoImageView.setOnClickListener {
            openImageChooser(PICK_LOGO_IMAGE_REQUEST)
        }

        // Add Member Button Logic
        addMemberButton.setOnClickListener {
            addMemberField()
        }

        // Supervisor Checkbox Logic
        supervisorCheckBox.setOnCheckedChangeListener { _, isChecked ->
            supervisorDetailsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Submit Button Logic
        submitButton.setOnClickListener {
            collectAndSubmitClubData()
        }

        // Back Button Logic
        val backButton: ImageButton? = findViewById(R.id.backButton)
        backButton?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
            // Consider adding a confirmation dialog here
        }

        // Initialize Cloudinary using the separate file
        CloudinaryConfig.initialize(applicationContext)
    }

    private fun openImageChooser(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            imageUri?.let { uri -> // Use let to safely handle nullable imageUri
                when (requestCode) {
                    PICK_BANNER_IMAGE_REQUEST -> {
                        selectedBannerUri = uri
                        loadImage(uri, bannerImageView)
                    }
                    PICK_LOGO_IMAGE_REQUEST -> {
                        selectedLogoUri = uri
                        loadImage(uri, logoImageView)
                    }
                }
            }
        }
    }

    private fun loadImage(imageUri: Uri, imageView: ImageView) {
        Glide.with(this)
            .load(imageUri)
            .into(imageView)
    }

    private fun addMemberField() {
        memberCount++
        val memberView = LayoutInflater.from(this).inflate(R.layout.item_member_input, membersContainer, false)

        membersContainer.addView(memberView)
    }

    private fun collectAndSubmitClubData() {
        // 1. Collect Data from all fields
        val clubName = clubNameEditText.text.toString().trim()
        val clubCategory = clubCategoryEditText.text.toString().trim()
        val clubDescription = clubDescriptionEditText.text.toString().trim()
        val presidentName = presidentNameEditText.text.toString().trim()
        val presidentEmail = presidentEmailEditText.text.toString().trim()
        val presidentContact = presidentContactEditText.text.toString().trim()

        val membersData = mutableListOf<Member>()
        for (i in 0 until membersContainer.childCount) {
            val memberView = membersContainer.getChildAt(i)
            val nameEditText = memberView.findViewById<EditText>(R.id.memberNameEditText)
            val positionEditText = memberView.findViewById<EditText>(R.id.memberPositionEditText)
            val emailEditText = memberView.findViewById<EditText>(R.id.memberEmailEditText)
            val contactEditText = memberView.findViewById<EditText>(R.id.memberContactEditText)

            membersData.add(
                Member(
                    memberName = nameEditText.text.toString().trim(),
                    memberPosition = positionEditText.text.toString().trim(),
                    memberEmail = emailEditText.text.toString().trim(),
                    memberContact = contactEditText.text.toString().trim()
                )
            )
        }

        val supervisorData: Supervisor? = if (supervisorCheckBox.isChecked) {
            Supervisor(
                supervisorName = supervisorNameEditText.text.toString().trim(),
                supervisorEmail = supervisorEmailEditText.text.toString().trim(),
                supervisorContact = supervisorContactEditText.text.toString().trim()
            )
        } else {
            null
        }

        // 2. Validate Data
        if (!isDataValid(
                clubName,
                clubCategory,
                clubDescription,
                presidentName,
                presidentEmail,
                presidentContact,
                membersData,
                supervisorData
            )
        ) {
            return // Stop submission if validation fails
        }

        // 3. Prepare Club Data Object
        val club = Club(
            clubname = clubName,
            category = clubCategory,
            description = clubDescription,
            presidentName = presidentName,
            presidentEmail = presidentEmail,
            presidentContact = presidentContact,
            members = membersData,
            supervisor = supervisorData
        )

        // 4. Send Data to Backend
        uploadImagesAndSaveClubData(club)
    }

    private fun isDataValid(
        clubName: String,
        clubCategory: String,
        clubDescription: String,
        presidentName: String,
        presidentEmail: String,
        presidentContact: String,
        membersData: List<Member>,
        supervisorData: Supervisor?
    ): Boolean {
        var isValid = true

        if (clubName.isEmpty()) {
            clubNameEditText.error = "Club name is required"
            isValid = false
        }
        if (clubCategory.isEmpty()) {
            clubCategoryEditText.error = "Club category is required"
            isValid = false
        }
        if (clubDescription.isEmpty()) {
            clubDescriptionEditText.error = "Club description is required"
            isValid = false
        }
        if (presidentName.isEmpty()) {
            presidentNameEditText.error = "President name is required"
            isValid = false
        }
        if (!isValidEmail(presidentEmail)) {
            presidentEmailEditText.error = "Invalid email format"
            isValid = false
        }
        if (presidentContact.isEmpty()) {
            presidentContactEditText.error = "President contact is required"
            isValid = false
        } else if (!isValidPhoneNumber(presidentContact)) {
            presidentContactEditText.error = "Invalid phone number"
            isValid = false
        }

        // Validate members data
        membersData.forEachIndexed { index, member ->
            if (member.memberName.isNullOrEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), "Member ${index + 1}: Name is required", Snackbar.LENGTH_SHORT).show()
                isValid = false
            }
            if (!member.memberEmail.isNullOrEmpty() && !isValidEmail(member.memberEmail!!)) {
                Snackbar.make(findViewById(android.R.id.content), "Member ${index + 1}: Invalid email format", Snackbar.LENGTH_SHORT).show()
                isValid = false
            }
            if (!member.memberContact.isNullOrEmpty() && !isValidPhoneNumber(member.memberContact!!)) {
                Snackbar.make(findViewById(android.R.id.content), "Member ${index + 1}: Invalid phone number", Snackbar.LENGTH_SHORT).show()
                isValid = false
            }
            if (member.memberPosition.isNullOrEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), "Member ${index + 1}: Position is required", Snackbar.LENGTH_SHORT).show()
                isValid = false
            }
        }

        // Validate supervisor data if present
        supervisorData?.let {
            if (it.supervisorName.isNullOrEmpty()) {
                supervisorNameEditText.error = "Supervisor name is required"
                isValid = false
            }
            if (!it.supervisorEmail.isNullOrEmpty() && !isValidEmail(it.supervisorEmail!!)) {
                supervisorEmailEditText.error = "Invalid email format"
                isValid = false
            }
            if (!it.supervisorContact.isNullOrEmpty() && !isValidPhoneNumber(it.supervisorContact!!)) {
                supervisorContactEditText.error = "Invalid phone number"
                isValid = false
            }
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^\\d{10}$")) // Checks for 10 digits
    }

    private fun uploadImagesAndSaveClubData(club: Club) {
        progressBar.visibility = View.VISIBLE
        submitButton.isEnabled = false

        var bannerUploadId: String? = null
        var logoUploadId: String? = null
        var bannerUploaded = selectedBannerUri == null // If no banner, consider it uploaded
        var logoUploaded = selectedLogoUri == null     // If no logo, consider it uploaded

        val uploadCallback = object : UploadCallback {
            override fun onStart(requestId: String) {
                Log.d(TAG, "Upload started for ID: $requestId")
            }

            override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                Log.d(TAG, "Upload progress for ID: $requestId - $bytes/$totalBytes")
            }

            override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                Log.d(TAG, "Upload success for ID: $requestId, Result: $resultData")
                val secureUrl = resultData["secure_url"] as? String
                when (requestId) {
                    bannerUploadId -> {
                        bannerCloudinaryUrl = secureUrl
                        bannerUploaded = true
                    }
                    logoUploadId -> {
                        logoCloudinaryUrl = secureUrl
                        logoUploaded = true
                    }
                }
                if (bannerUploaded && logoUploaded) {
                    saveClubDataToFirestore(club.copy(bannerImageUrl = bannerCloudinaryUrl, logoImageUrl = logoCloudinaryUrl))
                }
            }

            override fun onError(requestId: String, error: ErrorInfo) {
                Log.e(TAG, "Upload error for ID: $requestId, Error: ${error.description}")
                Snackbar.make(findViewById(android.R.id.content), "Failed to upload image: ${error.description}", Snackbar.LENGTH_LONG).show()
                // Proceed to save even if an image upload fails, but inform the user
                if (requestId == bannerUploadId) bannerUploaded = true
                if (requestId == logoUploadId) logoUploaded = true
                if (bannerUploaded && logoUploaded) {
                    saveClubDataToFirestore(club.copy(bannerImageUrl = bannerCloudinaryUrl, logoImageUrl = logoCloudinaryUrl))
                }
            }

            override fun onReschedule(requestId: String, error: ErrorInfo) {
                Log.w(TAG, "Upload rescheduled for ID: $requestId, Error: ${error.description}")
            }
        }

        selectedBannerUri?.let { uri ->
            bannerUploadId = MediaManager.get().upload(uri)
                .unsigned("android_upload") // Replace with your unsigned preset if needed
                .callback(uploadCallback)
                .dispatch()
        }

        selectedLogoUri?.let { uri ->
            logoUploadId = MediaManager.get().upload(uri)
                .unsigned("android_upload") // Replace with your unsigned preset if needed
                .callback(uploadCallback)
                .dispatch()
        }

        // If no images were selected, save immediately
        if (selectedBannerUri == null && selectedLogoUri == null) {
            saveClubDataToFirestore(club)
        }
    }

    private fun saveClubDataToFirestore(club: Club) {
        clubsCollection.add(club)
            .addOnSuccessListener { documentReference ->
                val clubId = documentReference.id
                Log.d(TAG, "Club added with ID: $clubId")

                // Auto-follow for current user
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                currentUserId?.let { uid ->
                    val userDocRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    userDocRef.update("followedClubIds", FieldValue.arrayUnion(clubId))
                        .addOnSuccessListener {
                            Log.d(TAG, "Club auto-followed by creator")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed to auto-follow club: ${e.message}")
                        }
                }

                Snackbar.make(findViewById(android.R.id.content), "Club created successfully!", Snackbar.LENGTH_LONG).show()

                // Send result back to ClubsFragment
                val resultIntent = Intent().apply {
                    putExtra("club_id", clubId)
                }
                setResult(Activity.RESULT_OK, resultIntent)

                finish()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding club", e)
                Snackbar.make(findViewById(android.R.id.content), "Failed to create club: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                progressBar.visibility = View.GONE
                submitButton.isEnabled = true
            }
    }


}