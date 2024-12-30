package com.iamashad.meraki.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
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

    // Paging Data for lazy loading
    val pagedJournals: Flow<PagingData<Journal>> = Pager(
        config = PagingConfig(pageSize = 10),
        pagingSourceFactory = { repository.getJournalPagingSource(userId) }
    ).flow.cachedIn(viewModelScope)

    // Search functionality
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> get() = _searchQuery

    private val _searchResults = MutableStateFlow<List<Journal>>(emptyList())
    val searchResults: StateFlow<List<Journal>> get() = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> get() = _isSearching

    // Add a new journal
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

    // Search journals based on query
    fun searchJournals(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repository.searchJournals(userId, query)
            _isSearching.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _isSearching.value = false
    }
}
