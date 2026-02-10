package ir.ayantech.sdk_ocr

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OcrResultAdapter(private var items: MutableList<OcrRowModel>) :
    RecyclerView.Adapter<OcrResultAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val ocr: TextView = view.findViewById(R.id.tvOcrValue)
        val cleaned: TextView = view.findViewById(R.id.tvCleanedValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ocr_result, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.ocr.text = item.ocrValue
        holder.cleaned.text = item.cleanedValue
    }

    override fun getItemCount() = items.size

     fun updateData(newItems: MutableList<OcrRowModel>) {
        this.items = newItems
        notifyDataSetChanged()
    }
}