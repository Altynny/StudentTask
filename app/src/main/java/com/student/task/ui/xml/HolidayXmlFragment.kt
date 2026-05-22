package com.student.task.ui.xml

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.student.task.databinding.FragmentHolidayXmlBinding
import com.student.task.presentation.HolidayViewModel
import com.student.task.presentation.ScreenState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.isNotEmpty

@AndroidEntryPoint
class HolidayXmlFragment : Fragment() {

    private var _binding: FragmentHolidayXmlBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HolidayViewModel by viewModels()

    private val adapter = HolidayAdapter(
        onCardClick = { holidayId -> viewModel.toggleCardState(holidayId) },
        onFavoriteClick = { holidayId -> viewModel.toggleFavorite(holidayId) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidayXmlBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupCategoryChips()
        observeState()
    }

    private fun setupCategoryChips() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    populateChips(categories)
                }
            }
        }
    }

    private fun populateChips(categories: List<String>) {
        val container = binding.chipsGroup as LinearLayout
        container.removeAllViews()

        val defaultTextColor = ContextCompat.getColor(requireContext(), com.student.task.R.color.black)

        val marginPx = (8 * resources.displayMetrics.density).toInt()
        val paddingH = (12 * resources.displayMetrics.density).toInt()
        val paddingV = (6 * resources.displayMetrics.density).toInt()

        fun highlightSelected(tag: Any?) {
            for (i in 0 until container.childCount) {
                val v = container.getChildAt(i) as Button
                if (v.tag == tag) {
                    v.setBackgroundResource(com.student.task.R.drawable.chip_bg_selected)
                    v.setTextColor(Color.WHITE)
                } else {
                    v.setBackgroundResource(com.student.task.R.drawable.chip_bg_unselected)
                    v.setTextColor(defaultTextColor)
                }
            }
        }

        val allButton = Button(requireContext())
        allButton.text = "Все"
        allButton.isAllCaps = false
        allButton.tag = "ALL"
        allButton.setOnClickListener {
            viewModel.filterByCategory(null)
            highlightSelected(it.tag)
        }
        allButton.setBackgroundResource(com.student.task.R.drawable.chip_bg_unselected)
        allButton.setPadding(paddingH, paddingV, paddingH, paddingV)
        val lpAll = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lpAll.marginEnd = marginPx
        allButton.layoutParams = lpAll
        container.addView(allButton)

        for (cat in categories) {
            val btn = Button(requireContext())
            btn.text = cat
            btn.isAllCaps = false
            btn.tag = cat
            btn.setOnClickListener {
                viewModel.filterByCategory(cat)
                highlightSelected(it.tag)
            }
            btn.setBackgroundResource(com.student.task.R.drawable.chip_bg_unselected)
            btn.setPadding(paddingH, paddingV, paddingH, paddingV)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = marginPx
            btn.layoutParams = lp
            container.addView(btn)
        }

        if (container.childCount > 0) {
            highlightSelected("ALL")
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.layoutManager = layoutManager
        binding.recyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { viewModel.retry() }

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()
                if (lastVisibleItem >= totalItemCount - 2) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.screenState.collect { state ->
                    when (state) {
                        is ScreenState.Loading -> showLoading()
                        is ScreenState.Error -> showError(state.message)
                        is ScreenState.Data -> showData(state)
                    }
                }
            }
        }
    }

    private fun showLoading() {
        binding.recyclerView.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.loadingLayout.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        binding.recyclerView.visibility = View.GONE
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.VISIBLE

        binding.errorMessage.text = message
        binding.retryButton.setOnClickListener { viewModel.retry() }
        binding.swipeRefresh.isRefreshing = false

        binding.errorLayout.alpha = 0f
        binding.errorLayout.translationY = 24f
        binding.errorLayout.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()
    }

    private fun showData(state: ScreenState.Data) {
        binding.loadingLayout.visibility = View.GONE
        binding.errorLayout.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.swipeRefresh.isRefreshing = false

        adapter.submitHolidays(state.holidays, state.isLoadingMore)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
