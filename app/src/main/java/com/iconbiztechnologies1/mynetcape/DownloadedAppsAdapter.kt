package com.iconbiztechnologies1.mynetcape

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DownloadedAppsAdapter(
    private val appsList: List<AppInfo>,
    private val onToggleChanged: (packageName: String, isBlocked: Boolean) -> Unit
) : RecyclerView.Adapter<DownloadedAppsAdapter.AppViewHolder>() {

    private val blockedApps = mutableSetOf<String>()

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
        val appName: TextView = itemView.findViewById(R.id.tvAppName)
        val packageName: TextView = itemView.findViewById(R.id.tvPackageName)
        val toggleButton: Button = itemView.findViewById(R.id.btnToggleBlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_downloaded_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = appsList[position]
        val isBlocked = blockedApps.contains(app.packageName)

        holder.appName.text = app.appName
        holder.packageName.text = app.packageName

        // Simple icon handling
        try {
            holder.appIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        } catch (e: Exception) {
            // Ignore icon errors
        }

        // Update toggle button appearance and text
        updateToggleButton(holder.toggleButton, isBlocked)

        // Set click listener for toggle button
        holder.toggleButton.setOnClickListener {
            val newBlockedState = !isBlocked
            if (newBlockedState) {
                blockedApps.add(app.packageName)
            } else {
                blockedApps.remove(app.packageName)
            }

            updateToggleButton(holder.toggleButton, newBlockedState)
            onToggleChanged(app.packageName, newBlockedState)
        }
    }

    private fun updateToggleButton(button: Button, isBlocked: Boolean) {
        try {
            if (isBlocked) {
                button.text = "UNBLOCK"
                button.setBackgroundColor(ContextCompat.getColor(button.context, android.R.color.holo_green_dark))
                button.setTextColor(ContextCompat.getColor(button.context, android.R.color.white))
            } else {
                button.text = "BLOCK"
                button.setBackgroundColor(ContextCompat.getColor(button.context, android.R.color.holo_red_dark))
                button.setTextColor(ContextCompat.getColor(button.context, android.R.color.white))
            }
        } catch (e: Exception) {
            // Fallback styling
            button.text = if (isBlocked) "UNBLOCK" else "BLOCK"
        }
    }

    override fun getItemCount(): Int = appsList.size

    fun updateBlockedApps(newBlockedApps: Set<String>) {
        blockedApps.clear()
        blockedApps.addAll(newBlockedApps)
        notifyDataSetChanged()
    }
}