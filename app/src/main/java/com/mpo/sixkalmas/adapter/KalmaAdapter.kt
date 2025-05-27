package com.mpo.sixkalmas.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mpo.sixkalmas.R
import com.mpo.sixkalmas.model.Kalma

class KalmaAdapter(private val kalmas: List<Kalma>) : RecyclerView.Adapter<KalmaAdapter.KalmaViewHolder>() {

    class KalmaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.kalmaTitle)
        val arabicTextView: TextView = view.findViewById(R.id.kalmaArabic)
        val translationTextView: TextView = view.findViewById(R.id.kalmaTranslation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KalmaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kalma, parent, false)
        return KalmaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KalmaViewHolder, position: Int) {
        val kalma = kalmas[position]
        holder.titleTextView.text = kalma.title
        holder.arabicTextView.text = kalma.arabicText
        holder.translationTextView.text = kalma.translation
    }

    override fun getItemCount() = kalmas.size
} 