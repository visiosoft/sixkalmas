package com.mpo.sixkalmas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.mpo.sixkalmas.model.Kalma
import com.mpo.sixkalmas.R

class KalmaPagerAdapter(
    private val kalmas: List<Kalma>,
    private val onLanguageSwitch: (Int) -> Unit,
    private val onSpeak: (String) -> Unit
) : RecyclerView.Adapter<KalmaPagerAdapter.KalmaViewHolder>() {

    inner class KalmaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.kalmaTitle)
        val arabicText: TextView = itemView.findViewById(R.id.kalmaArabic)
        val translation: TextView = itemView.findViewById(R.id.kalmaTranslation)
        val switchLanguageButton: MaterialButton = itemView.findViewById(R.id.switchLanguageButton)
        val speakerButton: MaterialButton = itemView.findViewById(R.id.speakerButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KalmaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kalma, parent, false)
        return KalmaViewHolder(view)
    }

    override fun onBindViewHolder(holder: KalmaViewHolder, position: Int) {
        val kalma = kalmas[position]
        holder.title.text = kalma.title
        holder.arabicText.text = kalma.arabicText
        holder.translation.text = kalma.translation

        holder.switchLanguageButton.setOnClickListener {
            onLanguageSwitch(position)
        }

        holder.speakerButton.setOnClickListener {
            onSpeak(kalma.arabicText)
        }
    }

    override fun getItemCount() = kalmas.size
} 