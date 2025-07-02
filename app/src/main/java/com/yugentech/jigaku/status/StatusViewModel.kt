package com.yugentech.jigaku.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yugentech.jigaku.status.statusRepository.StatusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatusViewModel(
    private val repository: StatusRepository
) : ViewModel() {

    private val _statuses = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val statuses: StateFlow<Map<String, Boolean>> = _statuses

    fun loadAllStatuses() {
        viewModelScope.launch {
            _statuses.value = repository.getAllStatuses()
        }
    }

    fun setUserStatus(userId: String, isStudying: Boolean) {
        viewModelScope.launch {
            repository.setStudyStatus(userId, isStudying)
        }
    }

    fun getUserStatus(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repository.getStudyStatus(userId))
        }
    }
}