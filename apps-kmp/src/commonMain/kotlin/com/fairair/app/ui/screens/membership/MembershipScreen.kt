package com.fairair.app.ui.screens.membership

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.fairair.app.api.*
import com.fairair.app.ui.components.velocity.GlassCard
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme

/**
 * Screen for membership plans and subscription management.
 * Shows available plans, allows subscription, and manages active memberships.
 */
class MembershipScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = getScreenModel<MembershipScreenModel>()
        val uiState by screenModel.uiState.collectAsState()

        VelocityTheme {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                VelocityColors.BackgroundDeep,
                                VelocityColors.BackgroundMid
                            )
                        )
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    MembershipHeader(
                        title = when (uiState.step) {
                            MembershipStep.PLANS -> "Membership"
                            MembershipStep.PLAN_DETAILS -> uiState.selectedPlan?.name ?: "Plan Details"
                            MembershipStep.PAYMENT -> "Complete Subscription"
                            MembershipStep.LOGIN_REQUIRED -> "Sign In Required"
                            MembershipStep.SUBSCRIPTION_COMPLETE -> "Welcome!"
                            MembershipStep.MANAGE_SUBSCRIPTION -> "Your Subscription"
                        },
                        showBack = uiState.step != MembershipStep.PLANS,
                        onBack = {
                            if (uiState.step == MembershipStep.PLANS) {
                                navigator.pop()
                            } else {
                                screenModel.goBack()
                            }
                        }
                    )

                    // Content
                    AnimatedContent(
                        targetState = uiState.step,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                        }
                    ) { step ->
                        when {
                            uiState.isLoading && step == MembershipStep.PLANS -> {
                                LoadingView()
                            }
                            uiState.error != null && uiState.plans.isEmpty() -> {
                                ErrorView(
                                    error = uiState.error!!,
                                    onRetry = screenModel::retry
                                )
                            }
                            step == MembershipStep.PLANS -> {
                                PlansView(
                                    plans = uiState.plans,
                                    currentSubscription = uiState.currentSubscription,
                                    usageStats = uiState.usageStats,
                                    onSelectPlan = screenModel::selectPlan,
                                    onViewSubscription = screenModel::viewSubscription
                                )
                            }
                            step == MembershipStep.PLAN_DETAILS -> {
                                PlanDetailsView(
                                    plan = uiState.selectedPlan!!,
                                    hasActiveSubscription = uiState.hasActiveSubscription,
                                    onSubscribe = screenModel::startSubscription
                                )
                            }
                            step == MembershipStep.LOGIN_REQUIRED -> {
                                LoginRequiredView(
                                    onLogin = { /* Navigate to login */ },
                                    onBack = screenModel::goBack
                                )
                            }
                            step == MembershipStep.PAYMENT -> {
                                PaymentView(
                                    plan = uiState.selectedPlan!!,
                                    billingCycle = uiState.selectedBillingCycle,
                                    isProcessing = uiState.isProcessing,
                                    error = uiState.error,
                                    onConfirm = { screenModel.confirmSubscription(null) }
                                )
                            }
                            step == MembershipStep.SUBSCRIPTION_COMPLETE -> {
                                SubscriptionCompleteView(
                                    subscription = uiState.currentSubscription!!,
                                    onDone = screenModel::reset
                                )
                            }
                            step == MembershipStep.MANAGE_SUBSCRIPTION -> {
                                ManageSubscriptionView(
                                    subscription = uiState.currentSubscription!!,
                                    usageStats = uiState.usageStats,
                                    plan = uiState.plans.find { it.id == uiState.currentSubscription?.planId },
                                    isProcessing = uiState.isProcessing,
                                    onToggleAutoRenewal = screenModel::toggleAutoRenewal,
                                    onCancel = screenModel::showCancelConfirmation
                                )
                            }
                        }
                    }
                }

                // Cancel confirmation dialog
                if (uiState.showCancelConfirmation) {
                    CancelSubscriptionDialog(
                        isProcessing = uiState.isProcessing,
                        onConfirm = screenModel::cancelSubscription,
                        onDismiss = screenModel::dismissCancelConfirmation
                    )
                }
            }
        }
    }
}

@Composable
private fun MembershipHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = VelocityColors.TextMain
            )
        }
        
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = VelocityColors.Accent)
    }
}

@Composable
private fun ErrorView(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp)
        )
        
        Button(
            onClick = onRetry,
            modifier = Modifier.padding(top = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun PlansView(
    plans: List<MembershipPlanDto>,
    currentSubscription: SubscriptionDto?,
    usageStats: UsageStatsDto?,
    onSelectPlan: (MembershipPlanDto) -> Unit,
    onViewSubscription: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = VelocityColors.Accent,
                    modifier = Modifier.size(64.dp)
                )
                
                Text(
                    text = "FairAir Membership",
                    style = MaterialTheme.typography.headlineMedium,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                
                Text(
                    text = "Unlimited flights, premium benefits",
                    style = MaterialTheme.typography.bodyLarge,
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Current subscription banner
        if (currentSubscription != null) {
            item {
                CurrentSubscriptionBanner(
                    subscription = currentSubscription,
                    usageStats = usageStats,
                    onClick = onViewSubscription
                )
            }
        }

        // Plans
        items(plans) { plan ->
            PlanCard(
                plan = plan,
                isCurrentPlan = currentSubscription?.planId == plan.id,
                onClick = { onSelectPlan(plan) }
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun CurrentSubscriptionBanner(
    subscription: SubscriptionDto,
    usageStats: UsageStatsDto?,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Subscription",
                        style = MaterialTheme.typography.labelMedium,
                        color = VelocityColors.Accent
                    )
                    Text(
                        text = subscription.planName,
                        style = MaterialTheme.typography.titleLarge,
                        color = VelocityColors.TextMain,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = VelocityColors.TextMuted
                )
            }
            
            if (usageStats != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    UsageItem(
                        label = "Flights",
                        used = usageStats.flightsUsed,
                        total = usageStats.flightsLimit
                    )
                    UsageItem(
                        label = "Guest Passes",
                        used = usageStats.guestPassesUsed,
                        total = usageStats.guestPassesLimit
                    )
                }
            }
        }
    }
}

@Composable
private fun UsageItem(
    label: String,
    used: Int,
    total: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$used / $total",
            style = MaterialTheme.typography.titleMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = VelocityColors.TextMuted
        )
    }
}

@Composable
private fun PlanCard(
    plan: MembershipPlanDto,
    isCurrentPlan: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isCurrentPlan) VelocityColors.Accent else VelocityColors.GlassBorder
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentPlan) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    if (isCurrentPlan) {
                        Surface(
                            color = VelocityColors.Accent,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "CURRENT PLAN",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = VelocityColors.TextMain,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${plan.flightsPerMonth} flights/month",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelocityColors.TextMuted
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = plan.monthlyPriceFormatted,
                        style = MaterialTheme.typography.headlineSmall,
                        color = VelocityColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "/month",
                        style = MaterialTheme.typography.bodySmall,
                        color = VelocityColors.TextMuted
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Key benefits
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (plan.priorityBoarding) {
                    item { BenefitChip(Icons.Default.Send, "Priority") }
                }
                if (plan.loungeAccess) {
                    item { BenefitChip(Icons.Default.Home, "Lounge") }
                }
                if (plan.flexibleChanges) {
                    item { BenefitChip(Icons.Default.Refresh, "Flexible") }
                }
                if (plan.guestPasses > 0) {
                    item { BenefitChip(Icons.Default.Person, "${plan.guestPasses} Guests") }
                }
            }
        }
    }
}

@Composable
private fun BenefitChip(
    icon: ImageVector,
    text: String
) {
    Surface(
        color = VelocityColors.Accent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VelocityColors.Accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = VelocityColors.Accent
            )
        }
    }
}

@Composable
private fun PlanDetailsView(
    plan: MembershipPlanDto,
    hasActiveSubscription: Boolean,
    onSubscribe: (BillingCycle) -> Unit
) {
    var selectedCycle by remember { mutableStateOf(BillingCycle.MONTHLY) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Plan summary
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = VelocityColors.TextMain,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${plan.flightsPerMonth} flights per month",
                        style = MaterialTheme.typography.bodyLarge,
                        color = VelocityColors.TextMuted
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Billing cycle toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(VelocityColors.BackgroundDeep)
                            .padding(4.dp)
                    ) {
                        BillingCycleTab(
                            label = "Monthly",
                            price = plan.monthlyPriceFormatted,
                            selected = selectedCycle == BillingCycle.MONTHLY,
                            onClick = { selectedCycle = BillingCycle.MONTHLY },
                            modifier = Modifier.weight(1f)
                        )
                        BillingCycleTab(
                            label = "Annual",
                            price = plan.annualPriceFormatted,
                            savings = "Save 20%",
                            selected = selectedCycle == BillingCycle.ANNUAL,
                            onClick = { selectedCycle = BillingCycle.ANNUAL },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Benefits
        item {
            Text(
                text = "What's included",
                style = MaterialTheme.typography.titleLarge,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(plan.benefits) { benefit ->
            BenefitRow(benefit = benefit)
        }
        
        // Additional features
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    DetailRow(Icons.Default.List, "Baggage", plan.baggageAllowance)
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = VelocityColors.GlassBorder
                    )
                    DetailRow(
                        Icons.Default.Person,
                        "Guest Passes",
                        "${plan.guestPasses} per month"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = VelocityColors.GlassBorder
                    )
                    DetailRow(
                        Icons.Default.Send,
                        "Priority Boarding",
                        if (plan.priorityBoarding) "Included" else "Not included"
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = VelocityColors.GlassBorder
                    )
                    DetailRow(
                        Icons.Default.Home,
                        "Lounge Access",
                        if (plan.loungeAccess) "Included" else "Not included"
                    )
                }
            }
        }
        
        // Subscribe button
        item {
            Button(
                onClick = { onSubscribe(selectedCycle) },
                enabled = !hasActiveSubscription,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
            ) {
                Text(
                    text = if (hasActiveSubscription) "Already Subscribed" else "Subscribe Now",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun BillingCycleTab(
    label: String,
    price: String,
    savings: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) VelocityColors.Accent else Color.Transparent
    val textColor = if (selected) Color.White else VelocityColors.TextMuted
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = price,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
        if (savings != null) {
            Text(
                text = savings,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White.copy(alpha = 0.8f) else Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
private fun BenefitRow(benefit: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = benefit,
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMain
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VelocityColors.TextMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = VelocityColors.TextMuted
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LoginRequiredView(
    onLogin: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = VelocityColors.Accent,
            modifier = Modifier.size(80.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Sign In Required",
            style = MaterialTheme.typography.headlineMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Please sign in to subscribe to a membership plan",
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onLogin,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
        ) {
            Text("Sign In", fontWeight = FontWeight.SemiBold)
        }
        
        TextButton(
            onClick = onBack,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Text("Go Back", color = VelocityColors.TextMuted)
        }
    }
}

@Composable
private fun PaymentView(
    plan: MembershipPlanDto,
    billingCycle: BillingCycle,
    isProcessing: Boolean,
    error: String?,
    onConfirm: () -> Unit
) {
    val price = if (billingCycle == BillingCycle.MONTHLY) {
        plan.monthlyPriceFormatted
    } else {
        plan.annualPriceFormatted
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Order Summary",
                    style = MaterialTheme.typography.titleLarge,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = VelocityColors.TextMain
                    )
                    Text(
                        text = price,
                        style = MaterialTheme.typography.bodyLarge,
                        color = VelocityColors.TextMain
                    )
                }
                
                Text(
                    text = billingCycle.name.lowercase().replaceFirstChar { it.uppercase() } + " billing",
                    style = MaterialTheme.typography.bodySmall,
                    color = VelocityColors.TextMuted
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = VelocityColors.GlassBorder
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        color = VelocityColors.TextMain,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleMedium,
                        color = VelocityColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Placeholder for payment method
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VelocityColors.BackgroundDeep)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = VelocityColors.TextMuted
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Add payment method",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }
        
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onConfirm,
            enabled = !isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Confirm Subscription", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SubscriptionCompleteView(
    subscription: SubscriptionDto,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(100.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome to ${subscription.planName}!",
            style = MaterialTheme.typography.headlineMedium,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Your subscription is now active. Enjoy unlimited flights and premium benefits!",
            style = MaterialTheme.typography.bodyLarge,
            color = VelocityColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VelocityColors.Accent)
        ) {
            Text("Start Booking", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ManageSubscriptionView(
    subscription: SubscriptionDto,
    usageStats: UsageStatsDto?,
    plan: MembershipPlanDto?,
    isProcessing: Boolean,
    onToggleAutoRenewal: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = subscription.planName,
                                style = MaterialTheme.typography.headlineSmall,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = subscription.billingCycle.replaceFirstChar { it.uppercase() } + " billing",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VelocityColors.TextMuted
                            )
                        }
                        
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "Active",
                                color = Color(0xFF4CAF50),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 16.dp),
                        color = VelocityColors.GlassBorder
                    )
                    
                    Text(
                        text = "Next billing: ${subscription.currentPeriodEnd}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = VelocityColors.TextMuted
                    )
                }
            }
        }
        
        // Usage stats
        if (usageStats != null) {
            item {
                Text(
                    text = "This Month's Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    UsageCard(
                        label = "Flights",
                        used = usageStats.flightsUsed,
                        total = usageStats.flightsLimit,
                        modifier = Modifier.weight(1f)
                    )
                    UsageCard(
                        label = "Guest Passes",
                        used = usageStats.guestPassesUsed,
                        total = usageStats.guestPassesLimit,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Savings this period",
                                style = MaterialTheme.typography.bodyMedium,
                                color = VelocityColors.TextMuted
                            )
                            Text(
                                text = usageStats.savingsThisPeriodFormatted,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        // Settings
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-renewal",
                            style = MaterialTheme.typography.bodyLarge,
                            color = VelocityColors.TextMain
                        )
                        Text(
                            text = "Automatically renew at period end",
                            style = MaterialTheme.typography.bodySmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                    Switch(
                        checked = subscription.autoRenew,
                        onCheckedChange = onToggleAutoRenewal,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VelocityColors.Accent,
                            checkedTrackColor = VelocityColors.Accent.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
        
        // Cancel button
        item {
            TextButton(
                onClick = onCancel,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel Subscription",
                    color = MaterialTheme.colorScheme.error,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun UsageCard(
    label: String,
    used: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (total > 0) used.toFloat() / total else 0f
    
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = VelocityColors.TextMuted
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(60.dp),
                    color = VelocityColors.Accent,
                    trackColor = VelocityColors.GlassBorder,
                    strokeWidth = 6.dp
                )
                Text(
                    text = "$used",
                    style = MaterialTheme.typography.titleLarge,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "of $total",
                style = MaterialTheme.typography.bodySmall,
                color = VelocityColors.TextMuted
            )
        }
    }
}

@Composable
private fun CancelSubscriptionDialog(
    isProcessing: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VelocityColors.BackgroundMid,
        title = {
            Text(
                text = "Cancel Subscription?",
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Your subscription will remain active until the end of your current billing period. You won't be charged again.",
                color = VelocityColors.TextMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cancel Subscription")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text("Keep Subscription", color = VelocityColors.TextMuted)
            }
        }
    )
}
