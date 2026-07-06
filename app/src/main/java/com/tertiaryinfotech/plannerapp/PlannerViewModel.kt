package com.tertiaryinfotech.plannerapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tertiaryinfotech.plannerapp.data.PlannerDatabase
import com.tertiaryinfotech.plannerapp.data.PlannerItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Single app-wide view model exposing the planner store, mirroring the iOS SwiftData context. */
class PlannerViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = PlannerDatabase.get(app).plannerDao()

    val activeItems = dao.activeItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val archivedItems = dao.archivedItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(item: PlannerItem) = viewModelScope.launch { dao.insert(item) }

    fun update(item: PlannerItem) = viewModelScope.launch { dao.update(item) }

    /** Checking auto-archives; unchecking restores to the active list. */
    fun toggleDone(item: PlannerItem) = viewModelScope.launch { dao.update(item.toggledDone()) }

    fun delete(item: PlannerItem) = viewModelScope.launch { dao.delete(item) }

    fun deleteById(id: String) = viewModelScope.launch { dao.deleteById(id) }

    fun clearArchive() = viewModelScope.launch { dao.clearArchive() }
}
