package com.iamashad.meraki.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // Real-time journal flow
    private val _journals = MutableStateFlow<List<Journal>>(emptyList())
    val journals: StateFlow<List<Journal>> get() = _journals

    // Search functionality
    private val _searchResults = MutableStateFlow<List<Journal>>(emptyList())
    val searchResults: StateFlow<List<Journal>> get() = _searchResults

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> get() = _searchQuery

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> get() = _isSearching

    init {
        listenToJournals()
    }

    private fun listenToJournals() {
        viewModelScope.launch {
            repository.listenToJournals(userId).collect { updatedJournals ->
                if (!_isSearching.value) {
                    _journals.value = updatedJournals
                }
            }
        }
    }

    fun searchJournals(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchQuery.value = query
            _searchResults.value = repository.searchJournals(userId, query)
        }
    }

    fun clearSearchResults() {
        _isSearching.value = false
        _searchResults.value = emptyList()
        _searchQuery.value = ""
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isEmpty()) {
            clearSearchResults()
        } else {
            searchJournals(query)
        }
    }

    // Add a new journal with mood score
    fun addJournal(journal: Journal) {
        viewModelScope.launch {
            repository.addJournal(journal)
        }
    }

    // Delete a journal
    fun deleteJournal(journalId: String) {
        viewModelScope.launch {
            repository.deleteJournal(journalId)
        }
    }
}


