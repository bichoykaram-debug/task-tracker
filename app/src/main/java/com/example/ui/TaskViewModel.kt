package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class TaskStats(
    val total: Int = 0,
    val inProgress: Int = 0,
    val onHold: Int = 0,
    val completed: Int = 0,
    val completionPercentage: Int = 0
)

data class TaskFormState(
    val title: String = "",
    val details: String = "",
    val requester: String = "",
    val date: String = "",
    val dueDate: String = "",
    val priority: String = "medium", // "low", "medium", "high"
    val notes: String = ""
)

class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _filter = MutableStateFlow("all") // "all", "inProgress", "onHold", "completed"
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val _sortBy = MutableStateFlow("newest") // "newest", "oldest", "due", "priority"
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()

    private val _expandedId = MutableStateFlow<String?>(null)
    val expandedId: StateFlow<String?> = _expandedId.asStateFlow()

    private val _deletingId = MutableStateFlow<String?>(null)
    val deletingId: StateFlow<String?> = _deletingId.asStateFlow()

    private val _isSheetOpen = MutableStateFlow(false)
    val isSheetOpen: StateFlow<Boolean> = _isSheetOpen.asStateFlow()

    private val _editingId = MutableStateFlow<String?>(null)
    val editingId: StateFlow<String?> = _editingId.asStateFlow()

    private val _formState = MutableStateFlow(TaskFormState())
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    // Combined task stream for filtered lists
    val filteredTasks: StateFlow<List<Task>> = combine(
        repository.allTasks,
        _query,
        _filter,
        _sortBy
    ) { tasks, query, filter, sortBy ->
        var result = tasks

        // Filter status
        if (filter != "all") {
            result = result.filter { it.status == filter }
        }

        // Filter text query (Arabic-friendly, case-insensitive)
        val trimmedQuery = query.trim().lowercase(Locale.getDefault())
        if (trimmedQuery.isNotEmpty()) {
            result = result.filter {
                it.title.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                it.details.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                it.requester.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                it.notes.lowercase(Locale.getDefault()).contains(trimmedQuery)
            }
        }

        // Sort items
        val priorityWeight = mapOf("high" to 3, "medium" to 2, "low" to 1)
        result = when (sortBy) {
            "newest" -> result.sortedByDescending { it.createdAt }
            "oldest" -> result.sortedBy { it.createdAt }
            "priority" -> result.sortedWith(
                compareByDescending<Task> { priorityWeight[it.priority] ?: 2 }
                    .thenByDescending { it.createdAt }
            )
            "due" -> result.sortedWith(
                compareBy<Task> { it.dueDate.isEmpty() }
                    .thenBy { it.dueDate }
                    .thenByDescending { it.createdAt }
            )
            else -> result
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined statistics
    val stats: StateFlow<TaskStats> = repository.allTasks.map { tasks ->
        val total = tasks.size
        val completed = tasks.count { it.status == "completed" }
        val inProgress = tasks.count { it.status == "inProgress" }
        val onHold = tasks.count { it.status == "onHold" }
        val percent = if (total > 0) (completed * 100) / total else 0
        TaskStats(total, inProgress, onHold, completed, percent)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskStats()
    )

    // Suggestion options for requester list
    val uniqueRequesters: StateFlow<List<String>> = repository.allTasks.map { tasks ->
        tasks.map { it.requester }.filter { it.isNotEmpty() }.distinct()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun getTodayISO(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    fun isLate(task: Task): Boolean {
        if (task.dueDate.isEmpty() || task.status == "completed") return false
        val today = getTodayISO()
        return task.dueDate < today
    }

    fun isDueToday(task: Task): Boolean {
        if (task.dueDate.isEmpty() || task.status == "completed") return false
        val today = getTodayISO()
        return task.dueDate == today
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun setFilter(f: String) {
        _filter.value = f
    }

    fun setSortBy(s: String) {
        _sortBy.value = s
    }

    fun toggleCardExpansion(id: String) {
        _expandedId.value = if (_expandedId.value == id) null else id
    }

    fun setDeletingId(id: String?) {
        _deletingId.value = id
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val updatedTask = if (task.status == "completed") {
                task.copy(status = "inProgress", completedDate = null)
            } else {
                task.copy(status = "completed", completedDate = getTodayISO())
            }
            repository.update(updatedTask)
        }
    }

    fun setStatus(task: Task, newStatus: String) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                status = newStatus,
                completedDate = if (newStatus == "completed") getTodayISO() else null
            )
            repository.update(updatedTask)
        }
    }

    fun updateTaskNotes(task: Task, notes: String) {
        viewModelScope.launch {
            val updatedTask = task.copy(notes = notes)
            repository.update(updatedTask)
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_expandedId.value == id) {
                _expandedId.value = null
            }
            _deletingId.value = null
        }
    }

    fun openSheet(task: Task? = null) {
        if (task == null) {
            _editingId.value = null
            _formState.value = TaskFormState(
                date = getTodayISO()
            )
        } else {
            _editingId.value = task.id
            _formState.value = TaskFormState(
                title = task.title,
                details = task.details,
                requester = task.requester,
                date = task.date,
                dueDate = task.dueDate,
                priority = task.priority,
                notes = task.notes
            )
        }
        _isSheetOpen.value = true
    }

    fun closeSheet() {
        _isSheetOpen.value = false
        _editingId.value = null
        _formState.value = TaskFormState()
    }

    fun updateFormField(updater: (TaskFormState) -> TaskFormState) {
        _formState.value = updater(_formState.value)
    }

    fun saveForm() {
        val form = _formState.value
        val title = form.title.trim()
        if (title.isEmpty()) return

        viewModelScope.launch {
            val id = _editingId.value
            if (id != null) {
                // Update existing
                val existing = Task(
                    id = id,
                    title = title,
                    details = form.details,
                    requester = form.requester,
                    date = form.date,
                    dueDate = form.dueDate,
                    priority = form.priority,
                    notes = form.notes,
                    status = "inProgress", // Preserve existing status or let edit change it in sheet is optional
                    createdAt = System.currentTimeMillis(), // Or keep original, simplified here
                    completedDate = null
                )
                repository.update(existing)
            } else {
                // Insert new
                val newTask = Task(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    details = form.details,
                    requester = form.requester,
                    date = form.date,
                    dueDate = form.dueDate,
                    priority = form.priority,
                    notes = form.notes,
                    status = "inProgress",
                    createdAt = System.currentTimeMillis(),
                    completedDate = null
                )
                repository.insert(newTask)
            }
            closeSheet()
        }
    }

    fun generateCSVContent(tasks: List<Task>): String {
        val header = listOf("العنوان", "التفاصيل", "طالب المهمة", "تاريخ الإدخال", "تاريخ التسليم", "الأولوية", "الحالة", "الملاحظات")
        val priorityLabels = mapOf("high" to "عالية", "medium" to "متوسطة", "low" to "منخفضة")
        val statusLabels = mapOf("completed" to "مكتملة", "inProgress" to "قيد التنفيذ", "onHold" to "متوقفة")

        fun escapeCsv(value: Any?): String {
            val str = value?.toString() ?: ""
            return "\"" + str.replace("\"", "\"\"") + "\""
        }

        val rows = tasks.map { t ->
            listOf(
                t.title,
                t.details,
                t.requester,
                t.date,
                t.dueDate,
                priorityLabels[t.priority] ?: t.priority,
                statusLabels[t.status] ?: t.status,
                t.notes
            ).map { escapeCsv(it) }.joinToString(",")
        }

        // Add UTF-8 BOM (\uFEFF) to support Excel Arabic characters correctly
        return "\uFEFF" + (listOf(header.map { escapeCsv(it) }.joinToString(",")) + rows).joinToString("\n")
    }
}

class TaskViewModelFactory(private val repository: TaskRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
