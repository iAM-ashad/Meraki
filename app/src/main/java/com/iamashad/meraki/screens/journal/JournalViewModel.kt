package com.iamashad.meraki.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: FirestoreRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val userId: String = firebaseAuth.currentUser?.uid.orEmpty()

    private val _journals = MutableStateFlow<List<Journal>>(emptyList())
    val journals: StateFlow<List<Journal>> = _journals.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Journal>>(emptyList())
    val searchResults: StateFlow<List<Journal>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    init {
        listenToJournals()
        observeSearchQuery()
    }

    private fun listenToJournals() {
        viewModelScope.launch {
            try {
                repository.listenToJournals(userId).collect { updatedJournals ->
                    if (!_isSearching.value) {
                        _journals.value = updatedJournals
                    }
                }
            } catch (e: Exception) {
                _errorState.value = e.localizedMessage
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .collect { query ->
                    if (query.isEmpty()) {
                        clearSearchResults()
                    } else {
                        searchJournals(query)
                    }
                }
        }
    }

    private fun searchJournals(query: String) {
        viewModelScope.launch {
            try {
                _searchResults.value = repository.searchJournals(userId, query)
            } catch (e: Exception) {
                _errorState.value = e.localizedMessage
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _isSearching.value = query.isNotEmpty()
    }

    fun clearSearchResults() {
        _isSearching.value = false
        _searchResults.value = emptyList()
    }

    fun addJournal(journal: Journal) {
        viewModelScope.launch {
            try {
                repository.addJournal(journal)
            } catch (e: Exception) {
                _errorState.value = e.localizedMessage
            }
        }
    }

    fun deleteJournal(journalId: String) {
        viewModelScope.launch {
            try {
                repository.deleteJournal(journalId)
            } catch (e: Exception) {
                _errorState.value = e.localizedMessage
            }
        }
    }
}


