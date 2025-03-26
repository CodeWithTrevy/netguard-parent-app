package com.iconbiztechnologies1.mynetcape

import AppUsageModel
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.util.Locale

class AppUsageAdapter(private val context: Context, private val appList: List<AppUsageModel>) :
    RecyclerView.Adapter<AppUsageAdapter.AppUsageViewHolder>() {

    class AppUsageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
        val appName: TextView = view.findViewById(R.id.app_name)
        val usageTime: TextView = view.findViewById(R.id.usage_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppUsageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return AppUsageViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppUsageViewHolder, position: Int) {
        val app = appList[position]
        holder.appName.text = app.appName
        holder.usageTime.text = formatUsageTime(app.usageTime)

        // Try to get the app icon using the package manager
        val appIcon = getAppIcon(app.appName)

        if (appIcon != null) {
            holder.appIcon.setImageDrawable(appIcon)
        } else {
            // If not found, try using the predefined URL
            val iconUrl = getAppIconUrl(app.appName)
            if (iconUrl.isNotEmpty()) {
                Picasso.get().load(iconUrl).error(R.mipmap.ic_launcher).into(holder.appIcon)
            } else {
                // For WhatsApp, use the local drawable resource
                if (app.appName.lowercase(Locale.getDefault()) == "com.whatsapp") {
                    holder.appIcon.setImageResource(R.drawable.download1) // Set local drawable
                } else {
                    holder.appIcon.setImageResource(R.mipmap.ic_launcher) // Default icon
                }
            }
        }
    }

    override fun getItemCount(): Int = appList.size

    // Format the usage time to display as hours and minutes
    private fun formatUsageTime(ms: Long): String {
        val minutes = (ms / 1000) / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> String.format(Locale.getDefault(), "%dh %02dm", hours, minutes % 60)
            minutes > 0 -> String.format(Locale.getDefault(), "%dm", minutes)
            else -> "<1m"
        }
    }

    // Function to get the app icon from the package name
    private fun getAppIcon(appName: String): Drawable? {
        return try {
            val packageManager: PackageManager = context.packageManager
            // Use the app's package name to retrieve the app icon
            val appInfo = packageManager.getApplicationInfo(appName, 0)
            packageManager.getApplicationIcon(appInfo)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null
        }
    }

    // Function to get the app icon URL for some known apps
    private fun getAppIconUrl(appName: String): String {
        return when (appName.lowercase(Locale.getDefault())) {
            "youtube" -> "https://upload.wikimedia.org/wikipedia/commons/b/b8/YouTube_App_Logo.svg"
            "whatsapp" -> "" // This is handled with local drawable above
            "google go" -> "https://upload.wikimedia.org/wikipedia/commons/5/53/Google_%22G%22_Logo.svg"
            else -> ""
        }
    }
}
