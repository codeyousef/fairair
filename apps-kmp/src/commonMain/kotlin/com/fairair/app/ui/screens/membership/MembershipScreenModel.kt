package com.fairair.app.ui.screens.membership

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.*
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.util.toDisplayMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ScreenModel for the Membership screen.
 * Handles membership plans display, subscription, and management.
 */
class MembershipScreenModel(
    private val apiClient: FairairApiClient,
    private val localStorage: LocalStorage
) : ScreenModel {

    private val _uiState = MutableStateFlow(MembershipUiState())
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    init {
        loadMembershipData()
    }

    /**
     * Loads membership plans and current subscription if logged in.
     */
    private fun loadMembershipData() {
        screenModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load plans
            when (val plansResult = apiClient.getMembershipPlans()) {
                is ApiResult.Success -> {
                    _uiState.update { 
                        it.copy(
                            plans = plansResult.data,
                            isLoading = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = plansResult.toDisplayMessage()
                        )
                    }
                    return@launch
                }
            }

            // Check for existing subscription if logged in
            val authToken = localStorage.getAuthToken()
            if (authToken != null) {
                when (val subResult = apiClient.getSubscription(authToken)) {
                    is ApiResult.Success -> {
                        _uiState.update {
                            it.copy(
                                currentSubscription = subResult.data,
                                isLoggedIn = true
                            )
                        }
                        // Also load usage stats
                        loadUsageStats(authToken)
                    }
                    is ApiResult.Error -> {
                        // User is logged in but no subscription - that's okay
                        _uiState.update { it.copy(isLoggedIn = true) }
                    }
                }
            }
        }
    }

    /**
     * Loads usage statistics for current subscription.
     */
    private fun loadUsageStats(authToken: String) {
        screenModelScope.launch {
            when (val result = apiClient.getUsageStats(authToken)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(usageStats = result.data) }
                }
                is ApiResult.Error -> {
                    // Non-critical, don't show error
                }
            }
        }
    }

    /**
     * Selects a plan for viewing details.
     */
    fun selectPlan(plan: MembershipPlanDto) {
        _uiState.update { 
            it.copy(
                selectedPlan = plan,
                step = MembershipStep.PLAN_DETAILS
            )
        }
    }

    /**
     * Starts the subscription flow for selected plan.
     */
    fun startSubscription(billingCycle: BillingCycle) {
        val state = _uiState.value
        
        if (!state.isLoggedIn) {
            _uiState.update { 
                it.copy(
                    step = MembershipStep.LOGIN_REQUIRED,
                    pendingBillingCycle = billingCycle
                )
            }
            return
        }

        _uiState.update { 
            it.copy(
                selectedBillingCycle = billingCycle,
                step = MembershipStep.PAYMENT
            )
        }
    }

    /**
     * Processes subscription with payment.
     */
    fun confirmSubscription(paymentMethodId: String?) {
        val state = _uiState.value
        val plan = state.selectedPlan ?: return
        val authToken = localStorage.getAuthToken() ?: return

        screenModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            val request = SubscribeRequestDto(
                planId = plan.id,
                billingCycle = state.selectedBillingCycle.name.lowercase(),
                paymentMethodId = paymentMethodId
            )

            when (val result = apiClient.subscribe(request, authToken)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            currentSubscription = result.data,
                            step = MembershipStep.SUBSCRIPTION_COMPLETE
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = result.toDisplayMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Cancels the current subscription.
     */
    fun cancelSubscription() {
        val authToken = localStorage.getAuthToken() ?: return

        screenModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }

            when (val result = apiClient.cancelSubscription(authToken)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            currentSubscription = null,
                            step = MembershipStep.PLANS,
                            showCancelConfirmation = false
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            error = result.toDisplayMessage()
                        )
                    }
                }
            }
        }
    }

    /**
     * Shows cancel confirmation dialog.
     */
    fun showCancelConfirmation() {
        _uiState.update { it.copy(showCancelConfirmation = true) }
    }

    /**
     * Dismisses cancel confirmation dialog.
     */
    fun dismissCancelConfirmation() {
        _uiState.update { it.copy(showCancelConfirmation = false) }
    }

    /**
     * Toggles auto-renewal setting.
     */
    fun toggleAutoRenewal(enabled: Boolean) {
        // This would update subscription settings via API
        _uiState.update { state ->
            state.currentSubscription?.let { sub ->
                state.copy(
                    currentSubscription = sub.copy(autoRenew = enabled)
                )
            } ?: state
        }
    }

    /**
     * Navigates to the subscription management view.
     */
    fun viewSubscription() {
        _uiState.update { it.copy(step = MembershipStep.MANAGE_SUBSCRIPTION) }
    }

    /**
     * Goes back to the previous step.
     */
    fun goBack() {
        _uiState.update { state ->
            when (state.step) {
                MembershipStep.PLAN_DETAILS -> state.copy(step = MembershipStep.PLANS, selectedPlan = null)
                MembershipStep.PAYMENT -> state.copy(step = MembershipStep.PLAN_DETAILS)
                MembershipStep.LOGIN_REQUIRED -> state.copy(step = MembershipStep.PLAN_DETAILS)
                MembershipStep.MANAGE_SUBSCRIPTION -> state.copy(step = MembershipStep.PLANS)
                MembershipStep.SUBSCRIPTION_COMPLETE -> state.copy(step = MembershipStep.PLANS)
                else -> state
            }
        }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Retries loading data.
     */
    fun retry() {
        loadMembershipData()
    }

    /**
     * Resets to plans view.
     */
    fun reset() {
        _uiState.update { 
            MembershipUiState(
                plans = it.plans,
                isLoggedIn = it.isLoggedIn,
                currentSubscription = it.currentSubscription,
                usageStats = it.usageStats
            )
        }
    }
}

/**
 * Steps in the membership flow.
 */
enum class MembershipStep {
    PLANS,
    PLAN_DETAILS,
    LOGIN_REQUIRED,
    PAYMENT,
    SUBSCRIPTION_COMPLETE,
    MANAGE_SUBSCRIPTION
}

/**
 * Billing cycle options.
 */
enum class BillingCycle {
    MONTHLY,
    ANNUAL
}

/**
 * UI state for the Membership screen.
 */
data class MembershipUiState(
    // Loading states
    val isLoading: Boolean = true,
    val isProcessing: Boolean = false,
    
    // Flow step
    val step: MembershipStep = MembershipStep.PLANS,
    
    // Auth state
    val isLoggedIn: Boolean = false,
    
    // Plans
    val plans: List<MembershipPlanDto> = emptyList(),
    val selectedPlan: MembershipPlanDto? = null,
    val selectedBillingCycle: BillingCycle = BillingCycle.MONTHLY,
    val pendingBillingCycle: BillingCycle? = null,
    
    // Current subscription
    val currentSubscription: SubscriptionDto? = null,
    val usageStats: UsageStatsDto? = null,
    
    // Dialogs
    val showCancelConfirmation: Boolean = false,
    
    // Error
    val error: String? = null
) {
    val hasActiveSubscription: Boolean
        get() = currentSubscription?.status?.uppercase() == "ACTIVE"
    
    val tierPlans: Map<String, List<MembershipPlanDto>>
        get() = plans.groupBy { it.tier }
}
