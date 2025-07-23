
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.iconbiztechnologies1.mynetcape.R

class OnboardingAdapter(
    private val images: List<Int>,
    private val context: Context,
    descriptions: List<String>
) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.imageView.setImageResource(images[position])
    }

    override fun getItemCount() = images.size

    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val


                imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}
