package com.fairair.app.admin.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.admin.api.AdminApiClient
import com.fairair.app.admin.api.AdminApiResult
import com.fairair.app.admin.state.AdminSection
import com.fairair.app.admin.state.AdminState
import com.fairair.app.ui.theme.FairairColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Main admin dashboard with navigation sidebar.
 */
class AdminDashboardScreen : Screen {

    @Composable
    override fun Content() {
        val adminState = koinInject<AdminState>()
        val adminApiClient = koinInject<AdminApiClient>()
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val currentAdmin by adminState.currentAdmin.collectAsState()
        var currentSection by remember { mutableStateOf(AdminSection.DASHBOARD) }

        // Check if logged in
        LaunchedEffect(Unit) {
            if (!adminState.isLoggedIn.value) {
                navigator.replace(AdminLoginScreen())
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar Navigation
            AdminSidebar(
                currentSection = currentSection,
                currentAdmin = currentAdmin,
                onSectionChange = { currentSection = it },
                onLogout = {
                    adminState.logout()
                    adminApiClient.setAuthToken(null)
                    navigator.replace(AdminLoginScreen())
                }
            )

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(FairairColors.Gray100)
            ) {
                when (currentSection) {
                    AdminSection.DASHBOARD -> DashboardContent(adminState, adminApiClient)
                    AdminSection.STATIC_PAGES -> StaticPagesContent(adminState, adminApiClient)
                    AdminSection.LEGAL_DOCUMENTS -> LegalDocumentsContent(adminState, adminApiClient)
                    AdminSection.PROMOTIONS -> PromotionsContent(adminState, adminApiClient)
                    AdminSection.DESTINATIONS -> DestinationsContent(adminState, adminApiClient)
                    AdminSection.ADMIN_USERS -> AdminUsersContent(adminState, adminApiClient)
                    AdminSection.AGENCIES -> AgenciesContent(adminState, adminApiClient)
                    AdminSection.GROUP_BOOKINGS -> GroupBookingsContent(adminState, adminApiClient)
                    AdminSection.CHARTER_REQUESTS -> CharterRequestsContent(adminState, adminApiClient)
                    AdminSection.SETTINGS -> SettingsContent(adminState)
                }
            }
        }
    }
}

@Composable
private fun AdminSidebar(
    currentSection: AdminSection,
    currentAdmin: com.fairair.app.admin.api.AdminDto?,
    onSectionChange: (AdminSection) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(FairairColors.Purple)
            .padding(vertical = 16.dp)
    ) {
        // Logo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = FairairColors.Yellow
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "fairair",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.White
                )
                Text(
                    text = "Admin Portal",
                    fontSize = 12.sp,
                    color = FairairColors.White.copy(alpha = 0.7f)
                )
            }
        }

        HorizontalDivider(color = FairairColors.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // Navigation Items
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                SidebarSection("Dashboard")
                SidebarItem(
                    icon = Icons.Default.Home,
                    label = "Overview",
                    isSelected = currentSection == AdminSection.DASHBOARD,
                    onClick = { onSectionChange(AdminSection.DASHBOARD) }
                )
            }

            item {
                SidebarSection("Content Management")
                SidebarItem(
                    icon = Icons.Default.Menu,
                    label = "Static Pages",
                    isSelected = currentSection == AdminSection.STATIC_PAGES,
                    onClick = { onSectionChange(AdminSection.STATIC_PAGES) }
                )
                SidebarItem(
                    icon = Icons.Default.Check,
                    label = "Legal Documents",
                    isSelected = currentSection == AdminSection.LEGAL_DOCUMENTS,
                    onClick = { onSectionChange(AdminSection.LEGAL_DOCUMENTS) }
                )
                SidebarItem(
                    icon = Icons.Default.LocationOn,
                    label = "Destinations",
                    isSelected = currentSection == AdminSection.DESTINATIONS,
                    onClick = { onSectionChange(AdminSection.DESTINATIONS) }
                )
            }

            item {
                SidebarSection("Marketing")
                SidebarItem(
                    icon = Icons.Default.Star,
                    label = "Promotions",
                    isSelected = currentSection == AdminSection.PROMOTIONS,
                    onClick = { onSectionChange(AdminSection.PROMOTIONS) }
                )
            }

            item {
                SidebarSection("B2B Management")
                SidebarItem(
                    icon = Icons.Default.Person,
                    label = "Agencies",
                    isSelected = currentSection == AdminSection.AGENCIES,
                    onClick = { onSectionChange(AdminSection.AGENCIES) }
                )
                SidebarItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "Group Bookings",
                    isSelected = currentSection == AdminSection.GROUP_BOOKINGS,
                    onClick = { onSectionChange(AdminSection.GROUP_BOOKINGS) }
                )
                SidebarItem(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = "Charter Requests",
                    isSelected = currentSection == AdminSection.CHARTER_REQUESTS,
                    onClick = { onSectionChange(AdminSection.CHARTER_REQUESTS) }
                )
            }

            item {
                SidebarSection("Administration")
                SidebarItem(
                    icon = Icons.Default.Person,
                    label = "Admin Users",
                    isSelected = currentSection == AdminSection.ADMIN_USERS,
                    onClick = { onSectionChange(AdminSection.ADMIN_USERS) }
                )
                SidebarItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isSelected = currentSection == AdminSection.SETTINGS,
                    onClick = { onSectionChange(AdminSection.SETTINGS) }
                )
            }
        }

        HorizontalDivider(color = FairairColors.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

        // User Info & Logout
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(FairairColors.Yellow),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentAdmin?.let { "${it.firstName.first()}${it.lastName.first()}" } ?: "AD",
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Purple
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentAdmin?.fullName ?: "Admin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = FairairColors.White
                )
                Text(
                    text = currentAdmin?.roleDisplayName ?: "Admin",
                    fontSize = 12.sp,
                    color = FairairColors.White.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = FairairColors.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun SidebarSection(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = FairairColors.White.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
    )
}

@Composable
private fun SidebarItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) FairairColors.White.copy(alpha = 0.15f) else Color.Transparent
    val textColor = if (isSelected) FairairColors.White else FairairColors.White.copy(alpha = 0.8f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = textColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

// ============================================================================
// DASHBOARD CONTENT
// ============================================================================

@Composable
private fun DashboardContent(
    adminState: AdminState,
    adminApiClient: AdminApiClient
) {
    val scope = rememberCoroutineScope()
    val currentAdmin by adminState.currentAdmin.collectAsState()

    // Stats
    var pageCount by remember { mutableStateOf(0) }
    var promoCount by remember { mutableStateOf(0) }
    var pendingAgencies by remember { mutableStateOf(0) }
    var pendingGroupBookings by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true
        
        // Load stats
        when (val result = adminApiClient.getAllPages()) {
            is AdminApiResult.Success -> pageCount = result.data.size
            else -> {}
        }
        when (val result = adminApiClient.getActivePromotions()) {
            is AdminApiResult.Success -> promoCount = result.data.size
            else -> {}
        }
        when (val result = adminApiClient.getPendingAgencies()) {
            is AdminApiResult.Success -> pendingAgencies = result.data.size
            else -> {}
        }
        when (val result = adminApiClient.getPendingGroupBookings()) {
            is AdminApiResult.Success -> pendingGroupBookings = result.data.size
            else -> {}
        }
        
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "Welcome back, ${currentAdmin?.firstName ?: "Admin"}!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FairairColors.Gray900
        )
        Text(
            text = "Here's what's happening with your content today.",
            fontSize = 16.sp,
            color = FairairColors.Gray600,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FairairColors.Purple)
            }
        } else {
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Static Pages",
                    value = pageCount.toString(),
                    icon = Icons.Default.Menu,
                    color = FairairColors.Purple
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Promotions",
                    value = promoCount.toString(),
                    icon = Icons.Default.Star,
                    color = FairairColors.Success
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending Agencies",
                    value = pendingAgencies.toString(),
                    icon = Icons.Default.Person,
                    color = FairairColors.Warning
                )
                DashboardStatCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending Group Bookings",
                    value = pendingGroupBookings.toString(),
                    icon = Icons.AutoMirrored.Filled.List,
                    color = FairairColors.Info
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Quick Actions
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = FairairColors.Gray900
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Create New Page",
                    description = "Add a new static content page",
                    icon = Icons.Default.Add
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "New Promotion",
                    description = "Create a promotional campaign",
                    icon = Icons.Default.Notifications
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    title = "Review Agencies",
                    description = "Approve pending agency applications",
                    icon = Icons.Default.ThumbUp
                )
            }
        }
    }
}

@Composable
private fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FairairColors.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = FairairColors.Gray900
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = FairairColors.Gray600
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier.clickable { /* TODO */ },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = FairairColors.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = FairairColors.Purple
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = FairairColors.Gray900
            )
            Text(
                text = description,
                fontSize = 13.sp,
                color = FairairColors.Gray600,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ============================================================================
// PLACEHOLDER CONTENT FOR OTHER SECTIONS
// ============================================================================

@Composable
private fun StaticPagesContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.content.StaticPagesManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun LegalDocumentsContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.content.LegalDocumentsManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun PromotionsContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.content.PromotionsManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun DestinationsContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.content.DestinationsManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun AdminUsersContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    ContentSection(
        title = "Admin Users",
        description = "Manage admin user accounts and permissions."
    )
}

@Composable
private fun AgenciesContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.b2b.AgenciesManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun GroupBookingsContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.b2b.GroupBookingsManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun CharterRequestsContent(adminState: AdminState, adminApiClient: AdminApiClient) {
    com.fairair.app.admin.screens.b2b.CharterRequestsManager(
        adminState = adminState,
        adminApiClient = adminApiClient
    )
}

@Composable
private fun SettingsContent(adminState: AdminState) {
    ContentSection(
        title = "Settings",
        description = "Configure system settings and preferences."
    )
}

@Composable
private fun ContentSection(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = FairairColors.Gray900
        )
        Text(
            text = description,
            fontSize = 16.sp,
            color = FairairColors.Gray600,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = FairairColors.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Build,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = FairairColors.Gray400
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Content management coming soon",
                        fontSize = 16.sp,
                        color = FairairColors.Gray500
                    )
                }
            }
        }
    }
}
