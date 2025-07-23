package com.iconbiztechnologies1.mynetcape

import AppUsageModel
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class AppUsageAdapter(
    private val context: Context,
    private val appUsageList: List<AppUsageModel>
) : RecyclerView.Adapter<AppUsageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appUsage = appUsageList[position]

        holder.tvAppName.text = appUsage.appName

        val hours = TimeUnit.MILLISECONDS.toHours(appUsage.usageTime)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(appUsage.usageTime) % 60
        holder.tvUsageTime.text = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

        if (appUsage.appIcon != null) {
            holder.ivAppIcon.setImageDrawable(appUsage.appIcon)
        } else {
            holder.ivAppIcon.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_generic_app)
            )
        }

        // --- CORRECTED: No conversion needed, it's already an Int ---
        holder.progressBar.progress = appUsage.usagePercentage

        // --- CORRECTED: Removed all unnecessary .toInt() conversions ---
        val colorRes = when {
            appUsage.usagePercentage > 75 -> R.color.progress_high
            appUsage.usagePercentage > 50 -> R.color.progress_medium
            appUsage.usagePercentage > 25 -> R.color.progress_low
            else -> R.color.progress_minimal
        }

        holder.progressBar.progressTintList = ContextCompat.getColorStateList(context, colorRes)
    }

    override fun getItemCount(): Int = appUsageList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAppIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val tvAppName: TextView = itemView.findViewById(R.id.tvAppName)
        val tvUsageTime: TextView = itemView.findViewById(R.id.tvUsageTime)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }
}