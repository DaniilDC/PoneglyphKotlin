package com.technifysoft.poneglyph

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.technifysoft.poneglyph.databinding.ActivityPdfDetailBinding
import java.io.FileOutputStream
import java.lang.Exception

class PdfDetailActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "ARTICLE_DETAILS_TAG"
    }

    private lateinit var binding: ActivityPdfDetailBinding
    private var articleId = ""
    private var articleTitle = ""
    private var articleUrl = ""
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        articleId = intent.getStringExtra("articleId")!!

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Please wait..")
        progressDialog.setCanceledOnTouchOutside(false)

        MyApplication.incrementArticleViewCount(articleId)
        loadArticleDetails()

        binding.backBtn.setOnClickListener { onBackPressed() }
        binding.readBookBtn.setOnClickListener {
            val intent = Intent(this, PdfViewActivity::class.java)
            intent.putExtra("articleId", articleId)
            startActivity(intent)
        }

        binding.downloadBookBtn.setOnClickListener {
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                Log.d(TAG, "onCreate: STORAGE PERMISSION GRANTED")
                downloadArticle()
//            } else {
//                Log.d(TAG, "onCreate: STORAGE PERMISSION DENIED, lets request")
//                requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//            }
        }
    }

    private val requestStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d(TAG, "Storage permission is granted")
                downloadArticle()
            } else {
                Log.d(TAG, "Storage permission is not granted")
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private fun loadArticleDetails() {
        val ref = FirebaseDatabase.getInstance().getReference("Articles")
        ref.child(articleId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val categoryId = "${snapshot.child("categoryId").value}"
                    val description = "${snapshot.child("description").value}"
                    val downloadsCount = "${snapshot.child("downloadsCount").value}"
                    val timestamp = "${snapshot.child("timestamp").value}"
                    articleTitle = "${snapshot.child("title").value}"
                    val uid = "${snapshot.child("uid").value}"
                    articleUrl = "${snapshot.child("url").value}"
                    val viewsCount = "${snapshot.child("viewsCount").value}"

                    val date = MyApplication.formatTimeStamp(timestamp.toLong())
                    MyApplication.loadCategory(categoryId, binding.categoryTv)
                    MyApplication.loadPdfFromUrlSinglePage(
                        "$articleUrl",
                        "$articleTitle",
                        binding.pdfView,
                        binding.progressBar,
                        binding.pagesTv
                    )
                    MyApplication.loadPdfSize("$articleUrl", "$articleTitle", binding.sizeTv)

                    binding.titleTv.text = articleTitle
                    binding.descriptionTv.text = description
                    binding.viewsTv.text = viewsCount
                    binding.downloadsTv.text = downloadsCount
                    binding.dateTv.text = date
                    binding.dateTv.text = date
                }

                override fun onCancelled(error: DatabaseError) {
                    //не требуется реализация
                }
            })
    }

    private fun downloadArticle() {
        Log.d(TAG, "downloadArticle: Downloading Article")
        progressDialog.setMessage("Downloading Article")
        progressDialog.show()

        val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(articleUrl)
        storageReference.getBytes(Constants.MAX_BYTES_PDF)
            .addOnSuccessListener { bytes ->
                Log.d(TAG, "downloadArticle: Article downloaded")
                saveToDownloadsFolder(bytes)
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.d(TAG, "downloadArticle: Failed to download due to ${e.message}")
                Toast.makeText(this, "Failed to download due to ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveToDownloadsFolder(bytes: ByteArray?) {
        Log.d(TAG, "saveToDownloadsFolder: Saving to downloads folder")
        val nameWithExtension = "$articleTitle.pdf"

        try {
            val downloadsFolder =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsFolder.mkdirs()

            val filePath = downloadsFolder.path + "/" + nameWithExtension
            val out = FileOutputStream(filePath)
            out.write(bytes)
            out.close()
            Toast.makeText(this, "Saved to Downloads folder", Toast.LENGTH_SHORT)
                .show()
            Log.d(TAG, "saveToDownloadsFolder: Saved to Downloads folder")
            progressDialog.dismiss()
            incrementDownloadCount()
        } catch (e: Exception) {
            Log.d(
                TAG,
                "saveToDownloadsFolder: Failed to save to downloads folder due to ${e.message}"
            )
            Toast.makeText(
                this,
                "Failed to save to downloads folder due to ${e.message}",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

    private fun incrementDownloadCount() {
        val ref = FirebaseDatabase.getInstance().getReference("Articles")
        ref.child(articleId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var downloadsCount = "${snapshot.child("downloadsCount").value}"
                    Log.d(TAG, "onDataChange: Current downloads count: $downloadsCount")
                    if (downloadsCount == "" || downloadsCount == "null") {
                        downloadsCount = "0"
                    }
                    val newDownloadCount: Long = downloadsCount.toLong() + 1
                    Log.d(TAG, "onDataChange: New downloads count: $newDownloadCount")
                    val hashMap: HashMap<String, Any> = HashMap()
                    hashMap["downloadsCount"] = newDownloadCount

                    val dbRef = FirebaseDatabase.getInstance().getReference("Articles")
                    dbRef.child(articleId)
                        .updateChildren(hashMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "onDataChange: incremented successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.d(TAG, "onDataChange: failed to increment due to ${e.message}")
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                }
            })
    }
}