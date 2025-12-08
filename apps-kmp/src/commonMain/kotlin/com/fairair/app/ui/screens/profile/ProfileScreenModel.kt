package com.fairair.app.ui.screens.profile

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.fairair.app.api.ApiResult
import com.fairair.app.api.FairairApiClient
import com.fairair.app.persistence.LocalStorage
import com.fairair.contract.dto.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the Profile screen.
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val travelers: List<SavedTravelerDto> = emptyList(),
    val paymentMethods: List<SavedPaymentMethodDto> = emptyList(),
    val error: String? = null,
    val selectedTab: ProfileTab = ProfileTab.TRAVELERS,
    // Form states
    val showAddTravelerDialog: Boolean = false,
    val showAddPaymentDialog: Boolean = false,
    val showAddDocumentDialog: Boolean = false,
    val editingTraveler: SavedTravelerDto? = null,
    val selectedTravelerForDocument: SavedTravelerDto? = null,
    val isSaving: Boolean = false,
    val saveError: String? = null
)

enum class ProfileTab {
    TRAVELERS,
    PAYMENT_METHODS
}

/**
 * ScreenModel for managing saved travelers and payment methods.
 */
class ProfileScreenModel(
    private val apiClient: FairairApiClient,
    private val localStorage: LocalStorage
) : ScreenModel {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    fun selectTab(tab: ProfileTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun loadProfile() {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken()
            if (authToken == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Please log in to view your profile"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            // Load travelers
            when (val travelersResult = apiClient.getSavedTravelers(authToken)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(travelers = travelersResult.data)
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = travelersResult.message)
                }
            }

            // Load payment methods
            when (val paymentsResult = apiClient.getSavedPaymentMethods(authToken)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        paymentMethods = paymentsResult.data,
                        isLoading = false
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = paymentsResult.message,
                        isLoading = false
                    )
                }
            }
        }
    }

    // ==================== Traveler Actions ====================

    fun showAddTravelerDialog() {
        _uiState.value = _uiState.value.copy(
            showAddTravelerDialog = true,
            editingTraveler = null,
            saveError = null
        )
    }

    fun showEditTravelerDialog(traveler: SavedTravelerDto) {
        _uiState.value = _uiState.value.copy(
            showAddTravelerDialog = true,
            editingTraveler = traveler,
            saveError = null
        )
    }

    fun dismissTravelerDialog() {
        _uiState.value = _uiState.value.copy(
            showAddTravelerDialog = false,
            editingTraveler = null,
            saveError = null
        )
    }

    fun saveTraveler(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: Gender,
        nationality: String,
        email: String?,
        phone: String?,
        isMainTraveler: Boolean
    ) {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken() ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)

            val request = SaveTravelerRequest(
                firstName = firstName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                gender = gender,
                nationality = nationality,
                email = email,
                phone = phone,
                isMainTraveler = isMainTraveler
            )

            val result = if (_uiState.value.editingTraveler != null) {
                apiClient.updateSavedTraveler(authToken, _uiState.value.editingTraveler!!.id, request)
            } else {
                apiClient.createSavedTraveler(authToken, request)
            }

            when (result) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showAddTravelerDialog = false,
                        editingTraveler = null
                    )
                    loadProfile() // Refresh the list
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = result.message
                    )
                }
            }
        }
    }

    fun deleteTraveler(travelerId: String) {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken() ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true)

            when (val result = apiClient.deleteSavedTraveler(authToken, travelerId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadProfile()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.message
                    )
                }
            }
        }
    }

    // ==================== Document Actions ====================

    fun showAddDocumentDialog(traveler: SavedTravelerDto) {
        _uiState.value = _uiState.value.copy(
            showAddDocumentDialog = true,
            selectedTravelerForDocument = traveler,
            saveError = null
        )
    }

    fun dismissDocumentDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDocumentDialog = false,
            selectedTravelerForDocument = null,
            saveError = null
        )
    }

    fun saveDocument(
        documentType: DocumentType,
        documentNumber: String,
        issuingCountry: String,
        expiryDate: String,
        isDefault: Boolean
    ) {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken() ?: return@launch
            val traveler = _uiState.value.selectedTravelerForDocument ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)

            val request = AddDocumentRequest(
                type = documentType,
                number = documentNumber,
                issuingCountry = issuingCountry,
                expiryDate = expiryDate,
                isDefault = isDefault
            )

            when (val result = apiClient.addTravelDocument(authToken, traveler.id, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showAddDocumentDialog = false,
                        selectedTravelerForDocument = null
                    )
                    loadProfile()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = result.message
                    )
                }
            }
        }
    }

    fun deleteDocument(travelerId: String, documentId: String) {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken() ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true)

            when (val result = apiClient.deleteTravelDocument(authToken, travelerId, documentId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadProfile()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.message
                    )
                }
            }
        }
    }

    // ==================== Payment Method Actions ====================

    fun showAddPaymentDialog() {
        _uiState.value = _uiState.value.copy(
            showAddPaymentDialog = true,
            saveError = null
        )
    }

    fun dismissPaymentDialog() {
        _uiState.value = _uiState.value.copy(
            showAddPaymentDialog = false,
            saveError = null
        )
    }

    fun savePaymentMethod(
        cardToken: String,
        type: PaymentType,
        nickname: String?,
        isDefault: Boolean
    ) {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken() ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)

            val request = SavePaymentMethodRequest(
                cardToken = cardToken,
                type = type,
                nickname = nickname,
                isDefault = isDefault
            )

            when (val result = apiClient.createSavedPaymentMethod(authToken, request)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showAddPaymentDialog = false
                    )
                    loadProfile()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        saveError = result.message
                    )
                }
            }
        }
    }

    fun deletePaymentMethod(paymentMethodId: String) {
        screenModelScope.launch {
            val authToken = localStorage.getAuthToken() ?: return@launch
            _uiState.value = _uiState.value.copy(isSaving = true)

            when (val result = apiClient.deleteSavedPaymentMethod(authToken, paymentMethodId)) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    loadProfile()
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, saveError = null)
    }
}
