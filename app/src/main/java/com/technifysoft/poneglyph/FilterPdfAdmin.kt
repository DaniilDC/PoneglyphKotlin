package com.technifysoft.poneglyph

import android.widget.Filter

class FilterPdfAdmin : Filter {
    var filterList: ArrayList<ModelPdf>
    var adapterPdfAdmin: AdapterPdfAdmin

    constructor(filterList: ArrayList<ModelPdf>, adapterPdfAdmin: AdapterPdfAdmin) {
        this.filterList = filterList
        this.adapterPdfAdmin = adapterPdfAdmin
    }

    override fun performFiltering(constraint: CharSequence?): FilterResults {
        var constraint: CharSequence? = constraint
        val results = FilterResults()

        if (constraint != null && constraint.isNotEmpty()) {
            constraint = constraint.toString().lowercase()
            val filteredModles = ArrayList<ModelPdf>()
            for (i in filterList.indices) {
                if (filterList[i].title.lowercase().contains(constraint)) {
                    filteredModles.add(filterList[i])
                }
            }
            results.count = filteredModles.size
            results.values = filteredModles
        }
        else {
            results.count = filterList.size
            results.values = filterList
        }
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
        adapterPdfAdmin.pdfArrayList = results!!.values as ArrayList<ModelPdf>
        adapterPdfAdmin.notifyDataSetChanged()
    }
}