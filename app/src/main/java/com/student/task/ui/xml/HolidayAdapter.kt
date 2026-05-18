package com.student.task.ui.xml

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import com.student.task.databinding.ItemHolidayCardBinding
import com.student.task.databinding.ItemLoadingMoreBinding
import com.student.task.presentation.model.CardState
import com.student.task.presentation.model.HolidayUiModel
import androidx.core.graphics.toColorInt
import com.student.task.R

class HolidayAdapter(
    private val onCardClick: (Int) -> Unit,
    private val onFavoriteClick: (Int) -> Unit
) : ListAdapter<HolidayAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    private var isLoadingMore = false

    sealed class ListItem {
        data class HolidayItem(val uiModel: HolidayUiModel) : ListItem()
        data object LoadingItem : ListItem()
    }

    fun submitHolidays(holidays: List<HolidayUiModel>, loadingMore: Boolean) {
        isLoadingMore = loadingMore
        val items = mutableListOf<ListItem>()
        items.addAll(holidays.map { ListItem.HolidayItem(it) })
        if (loadingMore) {
            items.add(ListItem.LoadingItem)
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.HolidayItem -> VIEW_TYPE_HOLIDAY
            is ListItem.LoadingItem -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HOLIDAY -> {
                val binding = ItemHolidayCardBinding.inflate(inflater, parent, false)
                HolidayViewHolder(binding)
            }
            VIEW_TYPE_LOADING -> {
                val binding = ItemLoadingMoreBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.HolidayItem -> (holder as HolidayViewHolder).bind(item.uiModel)
            is ListItem.LoadingItem -> { }
        }
    }

    inner class HolidayViewHolder(
        private val binding: ItemHolidayCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(uiModel: HolidayUiModel) {
            val holiday = uiModel.holiday

            binding.emojiText.text = holiday.category.emoji
            binding.categoryBadge.text = holiday.category.displayName
            binding.holidayName.text = holiday.name
            binding.holidayDate.text = holiday.date
            binding.officialBadge.visibility = if (holiday.isOfficial) View.VISIBLE else View.GONE

            binding.cardRoot.setOnClickListener {
                onCardClick(holiday.id)
            }
            binding.favoriteIcon.visibility = View.VISIBLE

            val favoriteClickListener = View.OnClickListener { v ->
                v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                }.start()
                onFavoriteClick(holiday.id)
            }
            binding.favoriteIcon.setOnClickListener(favoriteClickListener)

            when (uiModel.cardState) {
                CardState.Default, CardState.Expanded -> bindDefault()
                CardState.Favorite -> bindFavorite()
            }
        }

        private fun bindDefault() {
            binding.cardRoot.setCardBackgroundColor(Color.WHITE)
            binding.favoriteIcon.setImageResource(R.drawable.ic_baseline_star_border_24)
            binding.cardRoot.outlineAmbientShadowColor = Color.BLACK
            binding.cardRoot.outlineSpotShadowColor = Color.BLACK
        }

        private fun bindFavorite() {
            binding.cardRoot.setCardBackgroundColor("#FFF7E0".toColorInt())
            binding.favoriteIcon.setImageResource(R.drawable.ic_baseline_star_24)
            val shadowColor = "#FF4500".toColorInt()
            binding.cardRoot.outlineAmbientShadowColor = shadowColor
            binding.cardRoot.outlineSpotShadowColor = shadowColor
        }

    }

    class LoadingViewHolder(binding: ItemLoadingMoreBinding) : RecyclerView.ViewHolder(binding.root)

    private class DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return when {
                oldItem is ListItem.HolidayItem && newItem is ListItem.HolidayItem ->
                    oldItem.uiModel.holiday.id == newItem.uiModel.holiday.id
                oldItem is ListItem.LoadingItem && newItem is ListItem.LoadingItem -> true
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem): Boolean {
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TYPE_HOLIDAY = 0
        private const val VIEW_TYPE_LOADING = 1
    }
}
