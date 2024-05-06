package com.technifysoft.poneglyph

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import com.technifysoft.poneglyph.databinding.RowPdfAdminBinding

class AdapterPdfAdmin : RecyclerView.Adapter<AdapterPdfAdmin.HolderPdfAdmin>, Filterable {

    private var context: Context
    public var pdfArrayList: ArrayList<ModelPdf>
    private val filterList: ArrayList<ModelPdf>

    var filter: FilterPdfAdmin? = null

    private lateinit var binding: RowPdfAdminBinding

    constructor(context: Context, pdfArrayList: ArrayList<ModelPdf>) : super() {
        this.context = context
        this.pdfArrayList = pdfArrayList
        this.filterList = pdfArrayList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HolderPdfAdmin {
        binding = RowPdfAdminBinding.inflate(LayoutInflater.from(context), parent, false)
        return HolderPdfAdmin(binding.root)
    }

    override fun onBindViewHolder(holder: HolderPdfAdmin, position: Int) {

        val model = pdfArrayList[position]
        val pdfId = model.id
        val categoryId = model.categoryId
        val title = model.title
        val description = model.description
        val pdfUrl = model.url
        val timestanp = model.timestamp
        //требуется сконвертировать таймстемп в формат даты и времени
        val formattedDate = MyApplication.formatTimeStamp(timestanp)

        holder.titleTv.text = title
        holder.descriptionTv.text = description
        holder.dateTv.text = formattedDate

        MyApplication.loadCategory(categoryId, holder.categoryTv)
        MyApplication.loadPdfFromUrlSinglePage(
            pdfUrl,
            title,
            holder.pdfView,
            holder.progressBar,
            null
        )
        MyApplication.loadPdfSize(pdfUrl, title, holder.sizeTv)
    }

    override fun getItemCount(): Int {
        return pdfArrayList.size
    }

    override fun getFilter(): Filter {
        if (filter == null) {
            filter = FilterPdfAdmin(filterList, this)
        }
        return  filter as FilterPdfAdmin
    }

    inner class HolderPdfAdmin(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pdfView = binding.pdfView
        val progressBar = binding.progressBar
        val titleTv = binding.titleTv
        val descriptionTv = binding.descriptionTv
        val categoryTv = binding.categoryTv
        val sizeTv = binding.sizeTv
        val dateTv = binding.dateTv
        val moreBtn = binding.moreBtn
    }
}