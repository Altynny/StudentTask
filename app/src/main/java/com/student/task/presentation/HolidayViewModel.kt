package com.student.task.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.student.task.domain.usecase.GetHolidaysPageUseCase
import com.student.task.presentation.model.CardState
import com.student.task.presentation.model.HolidayUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HolidayViewModel @Inject constructor(
    private val getHolidaysPageUseCase: GetHolidaysPageUseCase
) : ViewModel() {

    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Loading)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    private var currentPage = 0
    private var totalCount = 0
    private val allHolidays = mutableListOf<HolidayUiModel>()
    private var currentFilter: String? = null

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories = _categories.asStateFlow()

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            _screenState.value = ScreenState.Loading
            totalCount = getHolidaysPageUseCase.getTotalCount()

            getHolidaysPageUseCase(page = 0).fold(
                onSuccess = { holidays ->
                    val mapped = holidays.map { HolidayUiModel(it) }
                    allHolidays.clear()
                    allHolidays.addAll(mapped)
                    currentPage = 0
                    updateCategories()

                    val display = if (currentFilter.isNullOrEmpty()) {
                        allHolidays.toList()
                    } else {
                        allHolidays.filter { it.holiday.category.displayName == currentFilter }
                    }

                    val hasMore = if (currentFilter.isNullOrEmpty()) {
                        allHolidays.size < totalCount
                    } else {
                        mapped.any { it.holiday.category.displayName == currentFilter }
                    }

                    _screenState.value = ScreenState.Data(
                        holidays = display,
                        isLoadingMore = false,
                        hasMorePages = hasMore,
                        currentPage = currentPage
                    )
                },
                onFailure = { error ->
                    _screenState.value = ScreenState.Error(
                        message = error.message ?: "Неизвестная ошибка"
                    )
                }
            )
        }
    }

    fun loadNextPage() {
        val current = _screenState.value
        if (current !is ScreenState.Data || current.isLoadingMore || !current.hasMorePages) return

        viewModelScope.launch {
            _screenState.value = current.copy(isLoadingMore = true)

            getHolidaysPageUseCase(page = currentPage + 1).fold(
                onSuccess = { holidays ->
                    currentPage++

                    val mapped = holidays.map { HolidayUiModel(it) }
                    allHolidays.addAll(mapped)
                    updateCategories()

                    val display = if (currentFilter.isNullOrEmpty()) {
                        allHolidays.toList()
                    } else {
                        allHolidays.filter { it.holiday.category.displayName == currentFilter }
                    }

                    val toAdd = if (currentFilter.isNullOrEmpty()) mapped else mapped.filter { it.holiday.category.displayName == currentFilter }

                    val hasMore = if (currentFilter.isNullOrEmpty()) {
                        allHolidays.size < totalCount
                    } else {
                        toAdd.isNotEmpty()
                    }

                    _screenState.value = ScreenState.Data(
                        holidays = display,
                        isLoadingMore = false,
                        hasMorePages = hasMore,
                        currentPage = currentPage
                    )
                },
                onFailure = { error ->
                    _screenState.value = current.copy(isLoadingMore = false)
                }
            )
        }
    }

    private fun updateCategories() {
        val cats = allHolidays.map { it.holiday.category.displayName }.distinct().sorted()
        _categories.value = cats
    }

    fun filterByCategory(category: String?) {
        currentFilter = category
        val current = _screenState.value
        val filtered = if (category.isNullOrEmpty()) {
            allHolidays.toList()
        } else {
            allHolidays.filter { it.holiday.category.displayName == category }
        }

        if (current is ScreenState.Data) {
            _screenState.value = current.copy(holidays = filtered)
        } else {
            _screenState.value = ScreenState.Data(
                holidays = filtered,
                isLoadingMore = false,
                hasMorePages = allHolidays.size < totalCount,
                currentPage = currentPage
            )
        }
    }

    fun retry() {
        loadInitial()
    }

    fun toggleCardState(holidayId: Int) {
        val current = _screenState.value
        if (current !is ScreenState.Data) return

        val updatedList = allHolidays.map { uiModel ->
            if (uiModel.holiday.id == holidayId) {
                val newState = when (uiModel.cardState) {
                    CardState.Default -> CardState.Expanded
                    CardState.Expanded -> CardState.Default
                    CardState.Favorite -> CardState.Favorite
                }
                uiModel.copy(cardState = newState)
            } else {
                uiModel
            }
        }
        allHolidays.clear()
        allHolidays.addAll(updatedList)
        val display = if (currentFilter.isNullOrEmpty()) updatedList else updatedList.filter { it.holiday.category.displayName == currentFilter }
        _screenState.value = current.copy(holidays = display)
    }

    fun toggleFavorite(holidayId: Int) {
        val current = _screenState.value
        if (current !is ScreenState.Data) return

        val updatedList = allHolidays.map { uiModel ->
            if (uiModel.holiday.id == holidayId) {
                val newState = when (uiModel.cardState) {
                    CardState.Favorite -> CardState.Default
                    else -> CardState.Favorite
                }
                uiModel.copy(cardState = newState)
            } else {
                uiModel
            }
        }
        allHolidays.clear()
        allHolidays.addAll(updatedList)
        val display = if (currentFilter.isNullOrEmpty()) updatedList else updatedList.filter { it.holiday.category.displayName == currentFilter }
        _screenState.value = current.copy(holidays = display)
    }
}
