import android.graphics.drawable.Drawable

data class AppUsageModel(
    val appName: String,
    val usageTime: Long,
    val timestamp: Long,
    val appIcon: Drawable? = null,
    // --- CORRECTED: Changed type from String to Int ---
    var usagePercentage: Int = 0,
    val packageName: String = ""
)