package com.mpo.sixkalmas

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.button.MaterialButton
import com.mpo.sixkalmas.model.Kalma
import com.mpo.sixkalmas.R

class KalmaPagerAdapter(
    private val kalmas: List<Kalma>,
    private val onLanguageSwitch: (Int) -> Unit,
    private val onSpeak: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_KALMA = 0
        private const val VIEW_TYPE_AD = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            2, 4 -> VIEW_TYPE_AD  // Show ad after 2nd and 4th Kalmas
            else -> VIEW_TYPE_KALMA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_ad, parent, false)
                AdViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_kalma, parent, false)
                KalmaViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is KalmaViewHolder -> {
                val kalmaPosition = when {
                    position < 2 -> position
                    position < 4 -> position - 1
                    else -> position - 2
                }
                val kalma = kalmas[kalmaPosition]
                holder.title.text = kalma.title
                holder.arabicText.text = kalma.arabicText
                holder.translation.text = kalma.translation

                holder.switchLanguageButton.setOnClickListener {
                    onLanguageSwitch(kalmaPosition)
                }

                holder.speakerButton.setOnClickListener {
                    onSpeak(kalma.arabicText)
                }
            }
            is AdViewHolder -> {
                holder.loadAd()
            }
        }
    }

    override fun getItemCount(): Int = kalmas.size + 2  // Add 2 for ads

    inner class KalmaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.kalmaTitle)
        val arabicText: TextView = itemView.findViewById(R.id.kalmaArabic)
        val translation: TextView = itemView.findViewById(R.id.kalmaTranslation)
        val switchLanguageButton: MaterialButton = itemView.findViewById(R.id.switchLanguageButton)
        val speakerButton: MaterialButton = itemView.findViewById(R.id.speakerButton)
    }

    inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val adView: AdView = itemView.findViewById(R.id.adView)

        fun loadAd() {
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
        }
    }
} 