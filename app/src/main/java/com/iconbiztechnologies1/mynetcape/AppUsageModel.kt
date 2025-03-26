import android.graphics.drawable.Drawable

data class AppUsageModel(
    val appName: String,
    val usageTime: Long,
    val timestamp: Long,
    val appIcon: Drawable? // Add appIcon as Drawable
)
