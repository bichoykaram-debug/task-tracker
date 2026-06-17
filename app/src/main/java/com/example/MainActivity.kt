package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import com.example.ui.*
import com.example.ui.theme.*
import java.io.File
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = ViewModelProvider(this, TaskViewModelFactory(repository))[TaskViewModel::class.java]

        // Load persisted dark mode override if saved
        val sharedPrefs = getSharedPreferences("laroche_prefs", Context.MODE_PRIVATE)

        setContent {
            var darkThemeOverride by remember { 
                mutableStateOf(sharedPrefs.getBoolean("dark_theme", true)) 
            }

            MyApplicationTheme(darkTheme = darkThemeOverride) {
                // Force RTL Arabic environment
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        TasksMainScreen(
                            viewModel = viewModel,
                            isDarkTheme = darkThemeOverride,
                            onThemeToggle = {
                                val nextTheme = !darkThemeOverride
                                darkThemeOverride = nextTheme
                                sharedPrefs.edit().putBoolean("dark_theme", nextTheme).apply()
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TasksMainScreen(
    viewModel: TaskViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.filteredTasks.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val query by viewModel.query.collectAsState()
    val sortBy by viewModel.sortBy.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isSheetOpen by viewModel.isSheetOpen.collectAsState()
    val editingId by viewModel.editingId.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val uniqueRequesters by viewModel.uniqueRequesters.collectAsState()
    val expandedId by viewModel.expandedId.collectAsState()
    val deletingId by viewModel.deletingId.collectAsState()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    // Theme values resolved at runtime dynamically
    val currentBgColors = if (isDarkTheme) {
        listOf(DarkBg0, DarkBg1, DarkBg2)
    } else {
        listOf(LightBg0, LightBg1, LightBg2)
    }

    val textColor = if (isDarkTheme) DarkText else LightText
    val mutedColor = if (isDarkTheme) DarkMuted else LightMuted
    val dimColor = if (isDarkTheme) DarkDim else LightDim
    val accentColor = if (isDarkTheme) DarkAccent else LightAccent
    val accent2Color = if (isDarkTheme) DarkAccent2 else LightAccent2
    val accentSoftColor = if (isDarkTheme) DarkAccentSoft else LightAccentSoft
    val doneColor = if (isDarkTheme) DarkDone else LightDone
    val doneSoftColor = if (isDarkTheme) DarkDoneSoft else LightDoneSoft
    val holdColor = if (isDarkTheme) DarkHold else LightHold
    val holdSoftColor = if (isDarkTheme) DarkHoldSoft else LightHoldSoft
    val dangerColor = if (isDarkTheme) DarkDanger else LightDanger
    val dangerSoftColor = if (isDarkTheme) DarkDangerSoft else LightDangerSoft
    val cardSurface = if (isDarkTheme) DarkSurface else LightSurface
    val cardSurface2 = if (isDarkTheme) DarkSurface2 else LightSurface2
    val borderColor = if (isDarkTheme) DarkBorder else LightBorder
    val border2Color = if (isDarkTheme) DarkBorder2 else LightBorder2

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(currentBgColors))
    ) {
        // Main single-view Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Icon Gradient
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(accentColor, accent2Color)
                                    )
                                )
                                .shadow(8.dp, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "لا روش",
                                tint = if (isDarkTheme) DarkBg0 else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Logo text
                        Column {
                            Text(
                                text = "LA ROCHE • إدارة المهام",
                                color = accentColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "متابعة المهام",
                                color = textColor,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    // Theme Selector Option
                    IconButton(
                        onClick = onThemeToggle,
                        modifier = Modifier
                            .size(42.dp)
                            .background(cardSurface)
                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "تبديل المظهر",
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Metrics Block
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress Indicator Top Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نسبة الإنجاز",
                            color = mutedColor,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${stats.completionPercentage}%",
                            color = accent2Color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Dynamic Gradient Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(cardSurface)
                            .border(1.dp, borderColor, RoundedCornerShape(99.dp))
                    ) {
                        val fraction = if (stats.completionPercentage > 0) stats.completionPercentage / 100f else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .clip(RoundedCornerShape(99.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accent2Color, accentColor)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Adaptive Columns Stats grid (2x2 on phones, 4 columns on tablets)
                    if (isTablet) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                title = "الإجمالي",
                                count = stats.total,
                                colorLabel = accent2Color,
                                modifier = Modifier.weight(1f),
                                cardSurface = cardSurface,
                                borderColor = borderColor,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                            StatCard(
                                title = "قيد التنفيذ",
                                count = stats.inProgress,
                                colorLabel = accentColor,
                                modifier = Modifier.weight(1f),
                                cardSurface = cardSurface,
                                borderColor = borderColor,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                            StatCard(
                                title = "متوقفة",
                                count = stats.onHold,
                                colorLabel = holdColor,
                                modifier = Modifier.weight(1f),
                                cardSurface = cardSurface,
                                borderColor = borderColor,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                            StatCard(
                                title = "مكتملة",
                                count = stats.completed,
                                colorLabel = doneColor,
                                modifier = Modifier.weight(1f),
                                cardSurface = cardSurface,
                                borderColor = borderColor,
                                textColor = textColor,
                                mutedColor = mutedColor
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(
                                    title = "الإجمالي",
                                    count = stats.total,
                                    colorLabel = accent2Color,
                                    modifier = Modifier.weight(1f),
                                    cardSurface = cardSurface,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    mutedColor = mutedColor
                                )
                                StatCard(
                                    title = "قيد التنفيذ",
                                    count = stats.inProgress,
                                    colorLabel = accentColor,
                                    modifier = Modifier.weight(1f),
                                    cardSurface = cardSurface,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    mutedColor = mutedColor
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatCard(
                                    title = "متوقفة",
                                    count = stats.onHold,
                                    colorLabel = holdColor,
                                    modifier = Modifier.weight(1f),
                                    cardSurface = cardSurface,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    mutedColor = mutedColor
                                )
                                StatCard(
                                    title = "مكتملة",
                                    count = stats.completed,
                                    colorLabel = doneColor,
                                    modifier = Modifier.weight(1f),
                                    cardSurface = cardSurface,
                                    borderColor = borderColor,
                                    textColor = textColor,
                                    mutedColor = mutedColor
                                )
                            }
                        }
                    }
                }
            }

            // Toolbar: Search + Sort + Export
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Search layout inside Box
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(cardSurface)
                            .border(1.dp, borderColor, RoundedCornerShape(13.dp))
                            .clip(RoundedCornerShape(13.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = mutedColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text("ابحث في المهام...", color = dimColor, fontSize = 14.sp)
                            }
                            // Custom integrated basic text input to preserve styled aesthetics
                            BasicTextFieldWithQuery(
                                value = query,
                                onValueChange = { viewModel.setQuery(it) },
                                textColor = textColor
                            )
                        }

                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.setQuery("") },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح",
                                    tint = mutedColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Sort menu trigger
                    var showSortMenu by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            modifier = Modifier
                                .height(44.dp)
                                .background(cardSurface)
                                .border(1.dp, borderColor, RoundedCornerShape(13.dp))
                                .clip(RoundedCornerShape(13.dp))
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val sortLabels = mapOf(
                                "newest" to "الأحدث",
                                "oldest" to "الأقدم",
                                "due" to "تسليم",
                                "priority" to "الأولوية"
                            )
                            Text(
                                text = sortLabels[sortBy] ?: sortBy,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "ترتيب",
                                tint = textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(cardSurface2)
                        ) {
                            listOf(
                                "newest" to "الأحدث",
                                "oldest" to "الأقدم",
                                "due" to "تاريخ التسليم",
                                "priority" to "الأولوية"
                            ).forEach { pair ->
                                DropdownMenuItem(
                                    text = { Text(pair.second, color = textColor) },
                                    onClick = {
                                        viewModel.setSortBy(pair.first)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Export CSV sharing trigger icon
                    IconButton(
                        onClick = {
                            val csvContent = viewModel.generateCSVContent(tasks)
                            exportTasksToCSV(context, csvContent)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(cardSurface)
                            .border(1.dp, borderColor, RoundedCornerShape(13.dp))
                            .clip(RoundedCornerShape(13.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "تحميل CSV",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Filter pills row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterConfig = listOf(
                        Triple("all", "الكل", stats.total),
                        Triple("inProgress", "قيد التنفيذ", stats.inProgress),
                        Triple("onHold", "متوقفة", stats.onHold),
                        Triple("completed", "مكتملة", stats.completed)
                    )

                    filterConfig.forEach { (filterKey, filterLabel, count) ->
                        val isActive = filter == filterKey
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(if (isActive) accentSoftColor else cardSurface)
                                .border(
                                    1.dp,
                                    if (isActive) border2Color else borderColor,
                                    RoundedCornerShape(99.dp)
                                )
                                .clickable { viewModel.setFilter(filterKey) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = filterLabel,
                                    color = if (isActive) accentColor else mutedColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isActive) accentColor.copy(alpha = 0.2f) else borderColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$count",
                                        color = if (isActive) accentColor else mutedColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tasks items feed
            if (tasks.isEmpty()) {
                item {
                    EmptyFeedState(
                        isQueryEmpty = query.isEmpty(),
                        onAddNew = { viewModel.openSheet(null) },
                        cardSurface = cardSurface,
                        borderColor = borderColor,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        accentColor = accentColor,
                        accentSoftColor = accentSoftColor
                    )
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        isExpanded = expandedId == task.id,
                        isDeleting = deletingId == task.id,
                        isLate = viewModel.isLate(task),
                        isDueToday = viewModel.isDueToday(task),
                        onToggleDone = { viewModel.toggleDone(task) },
                        onCardToggle = { viewModel.toggleCardExpansion(task.id) },
                        onStatusChange = { newStatus -> viewModel.setStatus(task, newStatus) },
                        onUpdateNotes = { notes -> viewModel.updateTaskNotes(task, notes) },
                        onEdit = { viewModel.openSheet(task) },
                        onDeleteRequest = { viewModel.setDeletingId(task.id) },
                        onCancelDelete = { viewModel.setDeletingId(null) },
                        onConfirmDelete = { viewModel.deleteTask(task.id) },
                        // Theme parameters
                        textColor = textColor,
                        mutedColor = mutedColor,
                        dimColor = dimColor,
                        accentColor = accentColor,
                        accent2Color = accent2Color,
                        accentSoftColor = accentSoftColor,
                        doneColor = doneColor,
                        doneSoftColor = doneSoftColor,
                        holdColor = holdColor,
                        holdSoftColor = holdSoftColor,
                        dangerColor = dangerColor,
                        dangerSoftColor = dangerSoftColor,
                        cardSurface = cardSurface,
                        cardSurface2 = cardSurface2,
                        borderColor = borderColor,
                        border2Color = border2Color
                    )
                }
            }
        }

        // Floating Action Button (FAB) at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accentColor, accent2Color)
                        )
                    )
                    .clickable { viewModel.openSheet(null) }
                    .padding(horizontal = 22.dp, vertical = 14.dp)
                    .shadow(16.dp, RoundedCornerShape(99.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "مهمة جديدة",
                    tint = if (isDarkTheme) DarkBg0 else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "مهمة جديدة",
                    color = if (isDarkTheme) DarkBg0 else Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        // Slide-up Form sheet (drawn responsive on Dialog overlay)
        if (isSheetOpen) {
            Dialog(
                onDismissRequest = { viewModel.closeSheet() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { viewModel.closeSheet() },
                    contentAlignment = if (isTablet) Alignment.Center else Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(if (isTablet) 0.65f else 1f)
                            .clip(
                                if (isTablet) RoundedCornerShape(24.dp)
                                else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .background(cardSurface2)
                            .border(
                                1.dp,
                                border2Color,
                                if (isTablet) RoundedCornerShape(24.dp)
                                else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            )
                            .clickable(enabled = false) {}
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Drag header on mobile
                        if (!isTablet) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(dimColor.copy(alpha = 0.5f))
                            )
                        }

                        // Form Header row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingId != null) "تعديل المهمة" else "مهمة جديدة",
                                color = textColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            IconButton(
                                onClick = { viewModel.closeSheet() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(cardSurface)
                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = textColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Form body inputs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Title input
                            Column {
                                Row {
                                    Text("عنوان المهمة ", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Text("*", color = dangerColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = formState.title,
                                    onValueChange = { txt -> viewModel.updateFormField { it.copy(title = txt) } },
                                    placeholder = { Text("مثال: تجهيز عرض أسعار لعميل...", color = dimColor, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = borderColor,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        unfocusedContainerColor = cardSurface,
                                        focusedContainerColor = cardSurface
                                    )
                                )
                            }

                            // Details input
                            Column {
                                Text("تفاصيل المهمة", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = formState.details,
                                    onValueChange = { txt -> viewModel.updateFormField { it.copy(details = txt) } },
                                    placeholder = { Text("اكتب تفاصيل وخطوات المهمة...", color = dimColor, fontSize = 13.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(82.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = borderColor,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        unfocusedContainerColor = cardSurface,
                                        focusedContainerColor = cardSurface
                                    )
                                )
                            }

                            // Requester input
                            Column {
                                Text("اسم طالب المهمة", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = formState.requester,
                                    onValueChange = { txt -> viewModel.updateFormField { it.copy(requester = txt) } },
                                    placeholder = { Text("مين طلب المهمة؟", color = dimColor, fontSize = 13.sp) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = borderColor,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        unfocusedContainerColor = cardSurface,
                                        focusedContainerColor = cardSurface
                                    )
                                )
                            }

                            // Dates Input column triggers (Native picker)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(11.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تاريخ الإدخال", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(cardSurface)
                                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                showDatePicker(context, formState.date) { date ->
                                                    viewModel.updateFormField { it.copy(date = date) }
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = "تاريخ الإدخال", tint = dimColor, modifier = Modifier.size(16.dp))
                                        Text(text = fmtDate(formState.date), color = textColor, fontSize = 13.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("تاريخ التسليم", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                            .background(cardSurface)
                                            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                showDatePicker(context, formState.dueDate) { date ->
                                                    viewModel.updateFormField { it.copy(dueDate = date) }
                                                }
                                            }
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = "تاريخ التسليم", tint = dimColor, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = if (formState.dueDate.isNotEmpty()) fmtDate(formState.dueDate) else "بلا تسليم",
                                            color = if (formState.dueDate.isNotEmpty()) textColor else dimColor,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Priority Row buttons (Segment style)
                            Column {
                                Text("الأولوية", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val priorityLevels = listOf(
                                        Triple("low", "منخفضة", doneColor),
                                        Triple("medium", "متوسطة", accentColor),
                                        Triple("high", "عالية", dangerColor)
                                    )

                                    priorityLevels.forEach { (key, label, color) ->
                                        val isActive = formState.priority == key
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(11.dp))
                                                .background(if (isActive) color.copy(alpha = 0.15f) else cardSurface)
                                                .border(
                                                    1.dp,
                                                    if (isActive) color else borderColor,
                                                    RoundedCornerShape(11.dp)
                                                )
                                                .clickable { viewModel.updateFormField { it.copy(priority = key) } },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isActive) color else mutedColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Optional Notes input inside overlay
                            Column {
                                Text("ملاحظات (اختياري)", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = formState.notes,
                                    onValueChange = { txt -> viewModel.updateFormField { it.copy(notes = txt) } },
                                    placeholder = { Text("سبب تأخير أو توقف لو موجود...", color = dimColor, fontSize = 13.sp) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(68.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = borderColor,
                                        focusedTextColor = textColor,
                                        unfocusedTextColor = textColor,
                                        unfocusedContainerColor = cardSurface,
                                        focusedContainerColor = cardSurface
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Submit action Button
                        Button(
                            onClick = { viewModel.saveForm() },
                            enabled = formState.title.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(13.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                                contentColor = if (isDarkTheme) DarkBg0 else Color.White,
                                disabledContainerColor = accentColor.copy(alpha = 0.35f),
                                disabledContentColor = (if (isDarkTheme) DarkBg0 else Color.White).copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = if (editingId != null) "حفظ التعديلات" else "إضافة المهمة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    colorLabel: Color,
    modifier: Modifier = Modifier,
    cardSurface: Color,
    borderColor: Color,
    textColor: Color,
    mutedColor: Color
) {
    Box(
        modifier = modifier
            .background(cardSurface)
            .border(1.dp, borderColor, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Horizontal colored indicator bar at top of card
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(colorLabel)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$count",
                color = colorLabel,
                fontSize = 21.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 1.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                color = mutedColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    isExpanded: Boolean,
    isDeleting: Boolean,
    isLate: Boolean,
    isDueToday: Boolean,
    onToggleDone: () -> Unit,
    onCardToggle: () -> Unit,
    onStatusChange: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    // Theme options
    textColor: Color,
    mutedColor: Color,
    dimColor: Color,
    accentColor: Color,
    accent2Color: Color,
    accentSoftColor: Color,
    doneColor: Color,
    doneSoftColor: Color,
    holdColor: Color,
    holdSoftColor: Color,
    dangerColor: Color,
    dangerSoftColor: Color,
    cardSurface: Color,
    cardSurface2: Color,
    borderColor: Color,
    border2Color: Color
) {
    val isCompleted = task.status == "completed"

    val statusLabel = when (task.status) {
        "completed" -> "مكتملة"
        "onHold" -> "متوقفة"
        else -> "قيد التنفيذ"
    }

    val statusTextColor = when (task.status) {
        "completed" -> doneColor
        "onHold" -> holdColor
        else -> accentColor
    }

    val statusBgColor = when (task.status) {
        "completed" -> doneSoftColor
        "onHold" -> holdSoftColor
        else -> accentSoftColor
    }

    val priorityColor = when (task.priority) {
        "high" -> dangerColor
        "low" -> accent2Color
        else -> accentColor
    }

    // Dynamic scale opacity on completion
    val cardAlpha = if (isCompleted) 0.72f else 1.0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardSurface.copy(alpha = cardAlpha))
            .border(
                1.dp,
                if (isExpanded) border2Color else borderColor,
                RoundedCornerShape(17.dp)
            )
            .clip(RoundedCornerShape(17.dp))
    ) {
        // Card head triggers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            // Checkbox on left (RTL)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .border(
                        2.dp,
                        if (isCompleted) Color.Transparent else dimColor,
                        RoundedCornerShape(9.dp)
                    )
                    .background(
                        if (isCompleted) Brush.linearGradient(listOf(doneColor, accent2Color))
                        else SolidColor(Color.Transparent)
                    )
                    .clickable { onToggleDone() },
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "مكتملة",
                        tint = cardSurface2, // High contrast check mark color
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Main Details trigger toggle click
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCardToggle() }
            ) {
                Text(
                    text = task.title,
                    color = if (isCompleted) mutedColor else textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    lineHeight = 21.sp
                )

                // Meta section row
                Row(
                    modifier = Modifier.padding(top = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.requester.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "طالب المهمة", tint = dimColor, modifier = Modifier.size(12.dp))
                            Text(task.requester, color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = "تاريخ الإدخال", tint = dimColor, modifier = Modifier.size(12.dp))
                        Text(fmtDate(task.date), color = mutedColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Alerts
                    if (isLate) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(dangerSoftColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = "متأخرة", tint = dangerColor, modifier = Modifier.size(11.dp))
                                Text("متأخرة", color = dangerColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else if (isDueToday) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(99.dp))
                                .background(holdSoftColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = "تسليم اليوم", tint = holdColor, modifier = Modifier.size(11.dp))
                                Text("تسليم اليوم", color = holdColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Notes Indicator
                    if (task.notes.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "ملاحظة", tint = accent2Color, modifier = Modifier.size(11.dp))
                            Text("ملاحظة", color = accent2Color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Head and trigger arrow
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.clickable { onCardToggle() }
            ) {
                // Priority color dot
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )

                // Status Banner
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Chevron icon
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "عرض التفاصيل",
                    tint = mutedColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Expanded actions block
        if (isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(bottomStart = 17.dp, bottomEnd = 17.dp)
                    )
                    .background(cardSurface2)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                // Task details (if optional details is present)
                if (task.details.isNotEmpty()) {
                    Text(
                        text = task.details,
                        color = mutedColor,
                        fontSize = 13.5.sp,
                        lineHeight = 22.sp
                    )
                }

                // Simple info Grid fields
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        InfoCardField(
                            label = "طالب المهمة",
                            value = if (task.requester.isNotEmpty()) task.requester else "—",
                            modifier = Modifier.weight(1f),
                            cardSurface2 = cardSurface,
                            borderColor = borderColor,
                            textColor = textColor,
                            dimColor = dimColor
                        )
                        InfoCardField(
                            label = "تاريخ الإدخال",
                            value = fmtDate(task.date),
                            modifier = Modifier.weight(1f),
                            cardSurface2 = cardSurface,
                            borderColor = borderColor,
                            textColor = textColor,
                            dimColor = dimColor
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        InfoCardField(
                            label = "تاريخ التسليم",
                            value = if (task.dueDate.isNotEmpty()) fmtDate(task.dueDate) else "—",
                            valueColor = if (isLate) dangerColor else textColor,
                            modifier = Modifier.weight(1f),
                            cardSurface2 = cardSurface,
                            borderColor = borderColor,
                            textColor = textColor,
                            dimColor = dimColor
                        )

                        val priorityLabels = mapOf("high" to "عالية", "medium" to "متوسطة", "low" to "منخفضة")
                        InfoCardField(
                            label = "الأولوية",
                            value = priorityLabels[task.priority] ?: task.priority,
                            modifier = Modifier.weight(1f),
                            cardSurface2 = cardSurface,
                            borderColor = borderColor,
                            textColor = textColor,
                            dimColor = dimColor
                        )
                    }
                }

                // Interactive Notes fields (saves in VM instantly)
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ملاحظات / سبب التأخير أو التوقف",
                        color = accent2Color,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    var notesDraftState by remember(task.id) { mutableStateOf(task.notes) }
                    OutlinedTextField(
                        value = notesDraftState,
                        onValueChange = { changedText ->
                            notesDraftState = changedText
                            onUpdateNotes(changedText)
                        },
                        placeholder = { Text("اكتب أي ملاحظة أو سبب توقف المهمة...", color = dimColor, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(11.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = borderColor,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            unfocusedContainerColor = cardSurface,
                            focusedContainerColor = cardSurface
                        )
                    )
                }

                // Horizontal Status changes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    listOf(
                        "inProgress" to ("قيد التنفيذ" to accentColor),
                        "onHold" to ("متوقفة" to holdColor),
                        "completed" to ("مكتملة" to doneColor)
                    ).forEach { (statusKey, statusPair) ->
                        val (label, color) = statusPair
                        val isMatchedStatus = task.status == statusKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isMatchedStatus) color.copy(alpha = 0.15f)
                                    else cardSurface
                                )
                                .border(
                                    1.dp,
                                    if (isMatchedStatus) color else borderColor,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onStatusChange(statusKey) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isMatchedStatus) color else mutedColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Edit + Delete trigger Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit button
                    Row(
                        modifier = Modifier
                            .background(cardSurface)
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onEdit() }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل", tint = accentColor, modifier = Modifier.size(14.dp))
                        Text("تعديل", color = accentColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }

                    // Delete confirmation flow to fit HTML exactly
                    if (isDeleting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("متأكد؟", color = mutedColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            // Delete Confirm button
                            Row(
                                modifier = Modifier
                                    .background(dangerColor)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onConfirmDelete() }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "احذف", tint = Color.White, modifier = Modifier.size(14.dp))
                                Text("احذف", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            // Cancel button
                            Row(
                                modifier = Modifier
                                    .background(cardSurface)
                                    .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onCancelDelete() }
                                    .padding(horizontal = 13.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text("إلغاء", color = mutedColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Regular Delete trigger button
                        Row(
                            modifier = Modifier
                                .background(cardSurface)
                                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onDeleteRequest() }
                                .padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = dangerColor, modifier = Modifier.size(14.dp))
                            Text("حذف", color = dangerColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoCardField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    cardSurface2: Color,
    borderColor: Color,
    textColor: Color,
    dimColor: Color,
    valueColor: Color = textColor
) {
    Column(
        modifier = modifier
            .background(cardSurface2)
            .border(1.dp, borderColor, RoundedCornerShape(11.dp))
            .clip(RoundedCornerShape(11.dp))
            .padding(9.dp)
    ) {
        Text(label, color = dimColor, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyFeedState(
    isQueryEmpty: Boolean,
    onAddNew: () -> Unit,
    cardSurface: Color,
    borderColor: Color,
    textColor: Color,
    mutedColor: Color,
    accentColor: Color,
    accentSoftColor: Color
) {
    if (isQueryEmpty) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .background(accentSoftColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = "لا توجد مهام",
                    tint = accentColor,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("مفيش مهام لسه", color = textColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "ابدأ بإضافة أول مهمة من زر «مهمة جديدة»",
                color = mutedColor,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = onAddNew,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة مهمة", modifier = Modifier.size(16.dp))
                    Text("إضافة مهمة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .background(accentSoftColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "لا نتائج",
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("لا توجد نتائج", color = textColor, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "جرّب كلمة بحث أو فلتر مختلف",
                color = mutedColor,
                fontSize = 13.5.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Custom integrated basic text input to preserve styled aesthetics
@Composable
fun BasicTextFieldWithQuery(
    value: String,
    onValueChange: (String) -> Unit,
    textColor: Color
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = textColor,
            fontSize = 14.sp
        ),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

// Standard Date Pick dialog wrapper
fun showDatePicker(context: Context, currentDateStr: String, onDateSelected: (String) -> Unit) {
    val calendar = Calendar.getInstance()
    if (currentDateStr.isNotEmpty()) {
        try {
            val parts = currentDateStr.split("-")
            if (parts.size == 3) {
                calendar.set(Calendar.YEAR, parts[0].toInt())
                calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                calendar.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
        } catch (e: Exception) {}
    }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    android.app.DatePickerDialog(context, { _, y, m, d ->
        val formatted = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
        onDateSelected(formatted)
    }, year, month, day).show()
}

// Arabic Gregorian locale date parser
fun fmtDate(iso: String): String {
    if (iso.isEmpty()) return ""
    return try {
        val parts = iso.split("-")
        if (parts.size == 3) {
            val year = parts[0]
            val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: 0
            val day = parts[2].toIntOrNull() ?: 1
            val monthsExist = listOf(
                "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
                "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
            )
            val monthName = if (monthIdx in 0..11) monthsExist[monthIdx] else parts[1]
            "$day $monthName $year"
        } else {
            iso
        }
    } catch (e: Exception) {
        iso
    }
}

// Native Intent Cache CSV export triggers
fun exportTasksToCSV(context: Context, csvContent: String) {
    try {
        val file = File(context.cacheDir, "laroche-tasks.csv")
        file.writeBytes(csvContent.toByteArray(Charsets.UTF_8))

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(intent, "تصدير المهام"))
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "فشل تصدير الملف: ${e.message}", Toast.LENGTH_LONG).show()
    }
}
