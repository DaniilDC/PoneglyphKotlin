package com.technifysoft.poneglyph

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.technifysoft.poneglyph.databinding.ActivityPdfAddBinding

class PdfAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPdfAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog
    private lateinit var categoryArrayList: ArrayList<ModelCategory>
    private var pdfUri: Uri? = null
    private val TAG = "PDF_ADD_TAG"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        loadPdfCategories()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.categoryTv.setOnClickListener {
            categoryPickDialog()
        }
        binding.attachBtn.setOnClickListener {
            pdfPickIntent()
        }
        binding.submitBtn.setOnClickListener {
            validateData()
        }
        binding.backBtn.setOnClickListener {
            onBackPressed()
        }
    }

    private var title = ""
    private var description = ""
    private var category = ""
    private fun validateData() {
        Log.d(TAG, "validateData: validating data")
        title = binding.titleEt.toString().trim()
        description = binding.descriptionEt.toString().trim()
        category = binding.categoryTv.toString().trim()

        if (title.isEmpty()) {
            Toast.makeText(this, "Enter Title..", Toast.LENGTH_SHORT).show()
        }
        else if (description.isEmpty()){
            Toast.makeText(this, "Enter Description..", Toast.LENGTH_SHORT).show()
        }
        else if (category.isEmpty()){
            Toast.makeText(this, "Pick Category..", Toast.LENGTH_SHORT).show()
        }
        else if (pdfUri == null) {
            Toast.makeText(this, "Pick PDF..", Toast.LENGTH_SHORT).show()
        }
        else {
            uploadPdfToStorage()
        }
    }

    private fun uploadPdfToStorage() {
        Log.d(TAG, "uploadPdfToStorage: uploading to storage..")
        progressDialog.setMessage("Uploading PDF..")
        progressDialog.show()

        val timestamp = System.currentTimeMillis()
        val filePathAndName = "Articles/$timestamp"
        val storageReference = FirebaseStorage.getInstance().getReference(filePathAndName)
        storageReference.putFile(pdfUri!!)
            .addOnSuccessListener {taskSnapshot ->
                Log.d(TAG, "uploadPdfToStorage: PDF uploaded now getting url..")

                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful);
                val uploadedPdfUrl = "${uriTask.result}"

                uploadPdfInfoToDb(uploadedPdfUrl, timestamp)
            }
            .addOnFailureListener {e ->
                Log.d(TAG, "uploadPdfToStorage: failed to upload due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadPdfInfoToDb(uploadedPdfUrl: String, timestamp: Long) {
        Log.d(TAG, "uploadPdfInfoToDb: uploading to db")
        progressDialog.setMessage("Uploading PDF info..")
        val uid = firebaseAuth.uid
        val hashMap: HashMap<String, Any> = HashMap()
        hashMap["uid"] = "$uid"
        hashMap["id"] = "$timestamp"
        hashMap["title"] = title
        hashMap["description"] = description
        hashMap["categoryId"] = "$selectedCategoryId"
        hashMap["categoryTitle"] = "$selectedCategoryTitle"
        hashMap["url"] = "$uploadedPdfUrl"
        hashMap["timestamp"] = timestamp
        hashMap["viewsCount"] = 0
        hashMap["downloadsCount"] = 0

        val ref = FirebaseDatabase.getInstance().getReference("Articles")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                Log.d(TAG, "uploadPdfInfoToDb: uploaded to db")
                progressDialog.dismiss()
                Toast.makeText(this, "Uploaded Successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {e->
                Log.d(TAG, "uploadPdfInfoToDb: failed to upload due to ${e.message}")
                progressDialog.dismiss()
                Toast.makeText(this, "Failed to upload due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPdfCategories() {
        Log.d(TAG, "loadPdfCategories: Loading pdf categories")
        categoryArrayList = ArrayList()

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                categoryArrayList.clear()
                for (ds in snapshot.children) {
                    val model = ds.getValue(ModelCategory::class.java)
                    categoryArrayList.add(model!!)
                    Log.d(TAG, "onDataChange: ${model.category}")
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private var selectedCategoryId = ""
    private var selectedCategoryTitle = ""
    private fun categoryPickDialog() {
        val categoriesArray = arrayOfNulls<String>(categoryArrayList.size)
        for (i in categoryArrayList.indices) {
            categoriesArray[i] = categoryArrayList[i].category

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Pick Category")
                .setItems(categoriesArray) {dialog, which ->
                    selectedCategoryTitle = categoryArrayList[which].category
                    selectedCategoryId = categoryArrayList[which].id
                    binding.categoryTv.text = selectedCategoryTitle

                    Log.d(TAG, "categoryPickDialog: Selected Category ID: $selectedCategoryId")
                    Log.d(TAG, "categoryPickDialog: Selected Category Title: $selectedCategoryTitle")
                }
                .show()
        }
    }
    private fun pdfPickIntent() {
        Log.d(TAG, "pdfPickIntent: starting pdf pick intent")
        val intent = Intent()
        intent.type = "application/pdf"
        intent.action = Intent.ACTION_GET_CONTENT
        pdfActivityResultLauncher.launch(intent)
    }
    val pdfActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                Log.d(TAG, "PDF Picked")
                pdfUri = result.data!!.data
            }
            else {
                Log.d(TAG, "PDF Pick cancelled")
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    )
}