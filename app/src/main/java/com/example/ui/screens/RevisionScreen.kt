package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RevisionStatus
import com.example.data.model.StudyConstants
import com.example.data.model.TopicRevision
import com.example.ui.components.CategoryBadge
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun RevisionScreen(
    viewModel: StudyViewModel
) {
    val topics by viewModel.allRevisions.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var selectedStatusFilter by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var topicToEdit by remember { mutableStateOf<TopicRevision?>(null) }
    var topicToDelete by remember { mutableStateOf<TopicRevision?>(null) }

    val filteredTopics = topics.filter { topic ->
        val matchesCategory = selectedCategoryFilter == null || topic.category == selectedCategoryFilter
        val matchesStatus = selectedStatusFilter == null || topic.status == selectedStatusFilter
        val matchesSearch = searchQuery.isBlank() ||
                topic.topicTitle.contains(searchQuery, ignoreCase = true) ||
                topic.subject.contains(searchQuery, ignoreCase = true) ||
                topic.notes.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesStatus && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    topicToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_topic_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Topic")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("revision_screen")
        ) {
            // Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notes & Revision Tracker",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Organize high-yield topics, take notes, and schedule spaced revisions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search topics, subjects, key formulas...", fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All") }
                    )
                }
                items(StudyConstants.CATEGORIES) { cat ->
                    val shortName = when {
                        cat.contains("BE") -> "BE Comp"
                        cat.contains("GPSC") -> "GPSC"
                        cat.contains("SSC") -> "SSC CGL"
                        else -> cat
                    }
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == cat) null else cat
                        },
                        label = { Text(shortName) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedStatusFilter == RevisionStatus.COMPLETED.name,
                        onClick = {
                            selectedStatusFilter = if (selectedStatusFilter == RevisionStatus.COMPLETED.name) null else RevisionStatus.COMPLETED.name
                        },
                        label = { Text("Completed") }
                    )
                }
            }

            // Topics List
            if (filteredTopics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No revision topics found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the + button to add a key subject topic and note for revision.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTopics, key = { it.id }) { topic ->
                        TopicRevisionCard(
                            topic = topic,
                            onMarkRevised = { viewModel.markTopicRevised(topic) },
                            onEdit = {
                                topicToEdit = topic
                                showAddEditDialog = true
                            },
                            onDelete = {
                                topicToDelete = topic
                            }
                        )
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        AddEditTopicDialog(
            initialTopic = topicToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { subject, category, title, status, notes, nextDate ->
                if (topicToEdit == null) {
                    viewModel.addRevisionTopic(subject, category, title, status, notes, nextDate)
                } else {
                    val updated = topicToEdit!!.copy(
                        subject = subject,
                        category = category,
                        topicTitle = title,
                        status = status,
                        notes = notes,
                        nextRevisionDate = nextDate
                    )
                    viewModel.updateRevisionTopic(updated)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    topicToDelete?.let { topic ->
        ConfirmDeleteDialog(
            title = "Delete Topic?",
            message = "Are you sure you want to delete '${topic.topicTitle}'?",
            onConfirm = {
                viewModel.deleteRevisionTopic(topic)
                topicToDelete = null
            },
            onDismiss = { topicToDelete = null }
        )
    }
}

@Composable
fun TopicRevisionCard(
    topic: TopicRevision,
    onMarkRevised: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val (statusLabel, statusColor) = when (topic.status) {
        RevisionStatus.COMPLETED.name -> "Mastered" to Color(0xFF10B981)
        RevisionStatus.LEARNING.name -> "Learning" to Color(0xFF3B82F6)
        else -> "Not Started" to Color(0xFFF59E0B)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryBadge(category = topic.category)
                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = topic.topicTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = topic.subject,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )

            if (topic.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = topic.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer info and Mark Revised button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (topic.revisionCount > 0)
                            "Revised ${topic.revisionCount}x • Next: ${topic.nextRevisionDate.ifBlank { "Anytime" }}"
                        else
                            "Next: ${topic.nextRevisionDate.ifBlank { "Unscheduled" }}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onMarkRevised,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Revise", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun AddEditTopicDialog(
    initialTopic: TopicRevision?,
    onDismiss: () -> Unit,
    onSave: (
        subject: String,
        category: String,
        topicTitle: String,
        status: String,
        notes: String,
        nextRevisionDate: String
    ) -> Unit
) {
    var title by remember { mutableStateOf(initialTopic?.topicTitle ?: "") }
    var selectedCategory by remember { mutableStateOf(initialTopic?.category ?: StudyConstants.CAT_BE_COMP) }
    var selectedSubject by remember {
        mutableStateOf(
            initialTopic?.subject ?: (StudyConstants.SUBJECTS_BY_CATEGORY[StudyConstants.CAT_BE_COMP]?.first() ?: "Computer Networks")
        )
    }
    var selectedStatus by remember { mutableStateOf(initialTopic?.status ?: RevisionStatus.LEARNING.name) }
    var notes by remember { mutableStateOf(initialTopic?.notes ?: "") }
    var nextRevisionDate by remember {
        mutableStateOf(
            initialTopic?.nextRevisionDate ?: java.time.LocalDate.now().plusDays(2).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
    var titleError by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    val subjects = StudyConstants.SUBJECTS_BY_CATEGORY[selectedCategory] ?: emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialTopic == null) "Add Important Topic" else "Edit Topic Notes",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text("Topic Title *") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        StudyConstants.CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                    val newSubs = StudyConstants.SUBJECTS_BY_CATEGORY[cat] ?: emptyList()
                                    if (newSubs.isNotEmpty()) selectedSubject = newSubs.first()
                                }
                            )
                        }
                    }
                }

                // Subject Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedSubject,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { subjectDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = subjectDropdownExpanded,
                        onDismissRequest = { subjectDropdownExpanded = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    selectedSubject = sub
                                    subjectDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Status selection
                Column {
                    Text("Revision Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RevisionStatus.values().forEach { st ->
                            FilterChip(
                                selected = selectedStatus == st.name,
                                onClick = { selectedStatus = st.name },
                                label = { Text(st.label, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Next revision date
                OutlinedTextField(
                    value = nextRevisionDate,
                    onValueChange = { nextRevisionDate = it },
                    label = { Text("Next Revision Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Key Formulas, Concepts & Notes") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                        return@Button
                    }
                    onSave(
                        selectedSubject,
                        selectedCategory,
                        title.trim(),
                        selectedStatus,
                        notes.trim(),
                        nextRevisionDate.trim()
                    )
                }
            ) {
                Text("Save Topic")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
