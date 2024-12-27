package com.iamashad.meraki.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.repository.FirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val repository: FirestoreRepository
) : ViewModel() {

    private val _journals = MutableStateFlow<List<Journal>>(emptyList())
    val journals: StateFlow<List<Journal>> get() = _journals

    init {
        listenToJournals(FirebaseAuth.getInstance().currentUser?.uid ?: "")
    }

    private fun listenToJournals(userId: String) {
        repository.listenToJournals(userId) { updatedJournals ->
            _journals.value = updatedJournals
        }
    }

    fun addJournal(journal: Journal) {
        viewModelScope.launch {
            repository.addJournal(journal)
            // No need to call loadJournals because real-time updates handle it
        }
    }

    fun deleteJournal(journalId: String, userId: String) {
        viewModelScope.launch {
            repository.deleteJournal(journalId)
            // No need to call loadJournals because real-time updates handle it
        }
    }
}


