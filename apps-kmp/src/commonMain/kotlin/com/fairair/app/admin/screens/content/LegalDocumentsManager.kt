package com.fairair.app.admin.screens.content

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.admin.api.*
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch

/**
 * Legal Documents management screen for admin portal.
 * Manages Privacy Policy, Terms of Use, Cookie Policy, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalDocumentsManager(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val legalDocuments by adminState.legalDocuments.collectAsState()
    val isLoading by adminState.legalDocumentsLoading.collectAsState()
    
    var editingDocument by remember { mutableStateOf<LegalDocumentDto?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadLegalDocuments(adminState, adminApiClient)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Legal Documents",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Gray900
                )
                Text(
                    text = "Manage Privacy Policy, Terms of Use, Cookie Policy, and other legal documents.",
                    fontSize = 16.sp,
                    color = FairairColors.Gray600,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Button(
                onClick = { showCreateDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Document")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Legal Documents List
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FairairColors.White)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FairairColors.Purple)
                }
            } else if (legalDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = FairairColors.Gray400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No legal documents yet",
                            fontSize = 16.sp,
                            color = FairairColors.Gray500
                        )
                        Text(
                            text = "Create your first legal document",
                            fontSize = 14.sp,
                            color = FairairColors.Gray400
                        )
                    }
                }
            } else {
                LazyColumn {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FairairColors.Gray100)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Type", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Version", Modifier.weight(0.5f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Effective Date", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Status", Modifier.weight(0.7f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Last Updated", Modifier.weight(1f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                            Text("Actions", Modifier.width(120.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FairairColors.Gray700)
                        }
                    }

                    items(legalDocuments) { document ->
                        LegalDocumentRow(
                            document = document,
                            onEdit = { editingDocument = document },
                            onPublish = {
                                scope.launch {
                                    adminApiClient.publishLegalDocument(document.id)
                                    loadLegalDocuments(adminState, adminApiClient)
                                }
                            }
                        )
                        HorizontalDivider(color = FairairColors.Gray200)
                    }
                }
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        CreateLegalDocumentDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { request ->
                scope.launch {
                    adminApiClient.createLegalDocument(request)
                    loadLegalDocuments(adminState, adminApiClient)
                    showCreateDialog = false
                }
            }
        )
    }

    // Edit Dialog
    editingDocument?.let { document ->
        EditLegalDocumentDialog(
            document = document,
            onDismiss = { editingDocument = null },
            onSave = { request ->
                scope.launch {
                    adminApiClient.updateLegalDocument(document.id, request)
                    loadLegalDocuments(adminState, adminApiClient)
                    editingDocument = null
                }
            }
        )
    }
}

@Composable
private fun LegalDocumentRow(
    document: LegalDocumentDto,
    onEdit: () -> Unit,
    onPublish: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = document.typeDisplayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = FairairColors.Gray900
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = FairairColors.Gray500
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = document.language.uppercase(),
                    fontSize = 12.sp,
                    color = FairairColors.Gray600
                )
            }
        }

        Text(
            text = "v${document.version}",
            modifier = Modifier.weight(0.5f),
            fontSize = 14.sp,
            color = FairairColors.Gray700
        )

        Text(
            text = document.effectiveDate?.take(10) ?: "Not set",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = FairairColors.Gray700
        )

        Box(modifier = Modifier.weight(0.7f)) {
            val (statusColor, statusBg, statusText) = when {
                document.isPublished -> Triple(FairairColors.Success, FairairColors.Success.copy(alpha = 0.1f), "Published")
                else -> Triple(FairairColors.Warning, FairairColors.Warning.copy(alpha = 0.1f), "Draft")
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Text(
            text = document.lastUpdated?.take(10) ?: "-",
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            color = FairairColors.Gray600
        )

        Row(modifier = Modifier.width(120.dp)) {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, "Edit", tint = FairairColors.Gray600)
            }
            if (!document.isPublished) {
                IconButton(onClick = onPublish) {
                    Icon(Icons.Default.Check, "Publish", tint = FairairColors.Success)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateLegalDocumentDialog(
    onDismiss: () -> Unit,
    onCreate: (CreateLegalDocumentRequest) -> Unit
) {
    var type by remember { mutableStateOf("PRIVACY_POLICY") }
    var language by remember { mutableStateOf("en") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("1.0") }
    var effectiveDate by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var langExpanded by remember { mutableStateOf(false) }

    val documentTypes = listOf(
        "PRIVACY_POLICY" to "Privacy Policy",
        "TERMS_OF_USE" to "Terms of Use",
        "TERMS_OF_SERVICE" to "Terms of Service",
        "COOKIE_POLICY" to "Cookie Policy",
        "CONDITIONS_OF_CARRIAGE" to "Conditions of Carriage",
        "BAGGAGE_POLICY" to "Baggage Policy",
        "REFUND_POLICY" to "Refund Policy"
    )

    val languages = listOf("en" to "English", "ar" to "Arabic")

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 600.dp),
        title = {
            Text("Create Legal Document", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Type dropdown
                Text("Document Type", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = documentTypes.find { it.first == type }?.second ?: type,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        documentTypes.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    type = value
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Language dropdown
                Text("Language", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                ExposedDropdownMenuBox(
                    expanded = langExpanded,
                    onExpandedChange = { langExpanded = it }
                ) {
                    OutlinedTextField(
                        value = languages.find { it.first == language }?.second ?: language,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) }
                    )
                    ExposedDropdownMenu(
                        expanded = langExpanded,
                        onDismissRequest = { langExpanded = false }
                    ) {
                        languages.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    language = value
                                    langExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Version
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Version") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Effective Date
                OutlinedTextField(
                    value = effectiveDate,
                    onValueChange = { effectiveDate = it },
                    label = { Text("Effective Date (YYYY-MM-DD)") },
                    placeholder = { Text("2025-01-01") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Markdown supported)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(CreateLegalDocumentRequest(
                        type = type,
                        language = language,
                        title = title,
                        content = content,
                        version = version,
                        effectiveDate = effectiveDate.ifBlank { null }
                    ))
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditLegalDocumentDialog(
    document: LegalDocumentDto,
    onDismiss: () -> Unit,
    onSave: (UpdateLegalDocumentRequest) -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    var content by remember { mutableStateOf(document.content) }
    var version by remember { mutableStateOf(document.version) }
    var effectiveDate by remember { mutableStateOf(document.effectiveDate ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 600.dp),
        title = {
            Text("Edit ${document.typeDisplayName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(FairairColors.Gray100)
                        .padding(12.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = FairairColors.Gray600, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Type: ${document.typeDisplayName} | Language: ${document.language.uppercase()}",
                        fontSize = 14.sp,
                        color = FairairColors.Gray700
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Version
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Version") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Effective Date
                OutlinedTextField(
                    value = effectiveDate,
                    onValueChange = { effectiveDate = it },
                    label = { Text("Effective Date (YYYY-MM-DD)") },
                    placeholder = { Text("2025-01-01") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content (Markdown supported)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    maxLines = 10
                )

                if (document.isPublished) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FairairColors.Warning.copy(alpha = 0.1f))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, tint = FairairColors.Warning)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "This document is published. Changes will be visible immediately.",
                            fontSize = 14.sp,
                            color = FairairColors.Warning
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(UpdateLegalDocumentRequest(
                        title = title,
                        content = content,
                        version = version,
                        effectiveDate = effectiveDate.ifBlank { null }
                    ))
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FairairColors.Purple)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private suspend fun loadLegalDocuments(adminState: AdminState, adminApiClient: AdminApiClient) {
    adminState.setLegalDocumentsLoading(true)
    
    when (val result = adminApiClient.getAllLegalDocuments()) {
        is AdminApiResult.Success -> adminState.setLegalDocuments(result.data)
        is AdminApiResult.Error -> adminState.setError(result.message)
    }
    
    adminState.setLegalDocumentsLoading(false)
}
