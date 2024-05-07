package com.technifysoft.poneglyph

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.technifysoft.poneglyph.databinding.ActivityPdfAddBinding
import com.technifysoft.poneglyph.databinding.ActivityPdfListAdminBinding
import java.lang.Exception

class PdfListAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfListAdminBinding

    private companion object {
        const val TAG = "PDF_LIST_ADMIN_TAG"
    }

    private var categoryId = ""
    private var category = ""
    private lateinit var pdfArrayList: ArrayList<ModelPdf>
    private lateinit var adapterPdfAdmin: AdapterPdfAdmin

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfListAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        categoryId = intent.getStringExtra("categoryId")!!
        Log.d(TAG, "onCreate: $categoryId")
        category = intent.getStringExtra("category")!!

        binding.subtitleTv.text = category

        loadPdfList()

        binding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //не требуется реализация
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    adapterPdfAdmin.filter!!.filter(s)
                }
                catch (e: Exception) {
                    Log.d(TAG, "onTextChanged: ${e.message}")
                }
            }
            override fun afterTextChanged(s: Editable?) {
                //не требуется реализация
            }
        })
        binding.backBtn.setOnClickListener { onBackPressed() }
    }

    private fun loadPdfList() {
        pdfArrayList = ArrayList()
        val ref = FirebaseDatabase.getInstance().getReference("Articles")
        ref.orderByChild("categoryId").equalTo(categoryId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    pdfArrayList.clear()
                    for (ds in snapshot.children) {
                        Log.d(TAG, ds.toString())
                        val model = ds.getValue(ModelPdf::class.java)
                        if (model!= null) {
                            pdfArrayList.add(model)
                            Log.d(TAG, "onDataChange: ${model.title} ${model.categoryId}")
                        }
                    }
                    // Убедитесь, что pdfArrayList не пуст
                    if (pdfArrayList.isNotEmpty()) {
                        adapterPdfAdmin = AdapterPdfAdmin(this@PdfListAdminActivity, pdfArrayList)
                        binding.bookRv.adapter = adapterPdfAdmin
                        adapterPdfAdmin.notifyDataSetChanged() // Уведомляем адаптер о изменении данных
                    } else {
                        Log.d(TAG, "No data to display")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // не требуется реализация
                }
            })
    }

}