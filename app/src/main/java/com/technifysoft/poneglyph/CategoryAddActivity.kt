package com.technifysoft.poneglyph

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.technifysoft.poneglyph.databinding.ActivityCategoryAddBinding

class CategoryAddActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCategoryAddBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryAddBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait..")
        progressDialog.setCanceledOnTouchOutside(false)

        binding.backBtn.setOnClickListener {
            onBackPressed()
            finish()
        }

        binding.submitBtn.setOnClickListener {
            validateData()
        }
    }

    private var category = ""

    private fun validateData() {
        category = binding.categoryEt.text.toString().trim()

        if (category.isEmpty()) {
            Toast.makeText(this, "Enter Category..", Toast.LENGTH_SHORT).show()
        } else {
            addCategoryFirebase()
        }
    }

    private fun addCategoryFirebase() {
        progressDialog.show()
        val timestamp = System.currentTimeMillis()

        val hashMap = HashMap<String, Any>()
        hashMap["id"] = "$timestamp"
        hashMap["category"] = category
        hashMap["timestamp"] = timestamp
        hashMap["uid"] = "${firebaseAuth.uid}"

        val ref = FirebaseDatabase.getInstance().getReference("Categories")
        ref.child("$timestamp")
            .setValue(hashMap)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Toast.makeText(this, "Added successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e->
                progressDialog.dismiss()
                Toast.makeText(this, "Failed adding category due to ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}