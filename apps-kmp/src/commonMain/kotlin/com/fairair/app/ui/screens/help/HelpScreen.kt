package com.fairair.app.ui.screens.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.ui.theme.VelocityColors
import com.fairair.app.ui.theme.VelocityTheme
import com.fairair.app.ui.theme.VelocityThemeWithBackground

/**
 * Help & FAQ screen with categorized help topics and contact options.
 * 
 * Based on flyadeal help center structure:
 * - Booking queries
 * - Baggage
 * - Flight-related queries
 * - Special Assistance
 * - Website/App queries
 * - International flights
 * - Membership services
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onBack: () -> Unit,
    onContactUs: () -> Unit,
    isRtl: Boolean = false,
    modifier: Modifier = Modifier
) {
    VelocityThemeWithBackground(isRtl = isRtl) {
        var selectedCategory by remember { mutableStateOf<HelpCategory?>(null) }
        var expandedFaqId by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Help Center",
                            style = VelocityTheme.typography.timeBig,
                            color = VelocityColors.TextMain
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = VelocityColors.TextMain
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search bar placeholder
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = VelocityColors.GlassBg
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = VelocityColors.TextMuted
                            )
                            Text(
                                text = "Search for help...",
                                style = VelocityTheme.typography.body,
                                color = VelocityColors.TextMuted
                            )
                        }
                    }
                }

                // Quick actions
                item {
                    Text(
                        text = "Quick Actions",
                        style = VelocityTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = VelocityColors.TextMain,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionCard(
                            icon = Icons.Default.Phone,
                            label = "Call Us",
                            onClick = { /* Open dialer */ },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Default.Email,
                            label = "Email",
                            onClick = onContactUs,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            icon = Icons.Default.Info,
                            label = "Live Chat",
                            onClick = { /* Open chat */ },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Help categories
                item {
                    Text(
                        text = "Browse by Category",
                        style = VelocityTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                        color = VelocityColors.TextMain,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                items(HelpCategory.entries) { category ->
                    HelpCategoryCard(
                        category = category,
                        isExpanded = selectedCategory == category,
                        onToggle = {
                            selectedCategory = if (selectedCategory == category) null else category
                        }
                    )

                    // Show FAQs when expanded
                    AnimatedVisibility(
                        visible = selectedCategory == category,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            category.faqs.forEach { faq ->
                                FaqItem(
                                    faq = faq,
                                    isExpanded = expandedFaqId == faq.id,
                                    onToggle = {
                                        expandedFaqId = if (expandedFaqId == faq.id) null else faq.id
                                    }
                                )
                            }
                        }
                    }
                }

                // Contact section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    ContactSection(onContactUs = onContactUs)
                }

                // Footer
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Can't find what you're looking for?\nContact our 24/7 support team.",
                        style = VelocityTheme.typography.body,
                        color = VelocityColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = VelocityColors.GlassBg
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(VelocityColors.Accent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = VelocityColors.Accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = label,
                style = VelocityTheme.typography.labelSmall,
                color = VelocityColors.TextMain
            )
        }
    }
}

@Composable
private fun HelpCategoryCard(
    category: HelpCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(12.dp),
        color = if (isExpanded) VelocityColors.GlassHover else VelocityColors.GlassBg
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(category.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = VelocityTheme.typography.body.copy(fontWeight = FontWeight.SemiBold),
                    color = VelocityColors.TextMain
                )
                Text(
                    text = "${category.faqs.size} articles",
                    style = VelocityTheme.typography.labelSmall,
                    color = VelocityColors.TextMuted
                )
            }

            Icon(
                imageVector = if (isExpanded) 
                    Icons.Default.Close 
                else 
                    Icons.Default.Add,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = VelocityColors.TextMuted
            )
        }
    }
}

@Composable
private fun FaqItem(
    faq: FaqQuestion,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        color = VelocityColors.BackgroundMid
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = faq.question,
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMain,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) 
                        Icons.Default.Close 
                    else 
                        Icons.Default.Add,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = VelocityColors.Accent,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Text(
                    text = faq.answer,
                    style = VelocityTheme.typography.body.copy(fontSize = 14.sp),
                    color = VelocityColors.TextMuted,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactSection(
    onContactUs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = VelocityColors.Primary.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = VelocityColors.Accent,
                modifier = Modifier.size(48.dp)
            )
            
            Text(
                text = "Need more help?",
                style = VelocityTheme.typography.timeBig,
                color = VelocityColors.TextMain
            )
            
            Text(
                text = "Our support team is available 24/7",
                style = VelocityTheme.typography.body,
                color = VelocityColors.TextMuted,
                textAlign = TextAlign.Center
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Call support */ },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VelocityColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call Us", color = VelocityColors.BackgroundDeep)
                }

                OutlinedButton(
                    onClick = onContactUs,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VelocityColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Email Us")
                }
            }

            // Contact info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "📞 +966 920 000 123",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted
                )
                Text(
                    text = "✉️ support@fairair.com",
                    style = VelocityTheme.typography.body,
                    color = VelocityColors.TextMuted
                )
            }
        }
    }
}

/**
 * Help categories based on flyadeal help center structure.
 */
enum class HelpCategory(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val faqs: List<FaqQuestion>
) {
    BOOKING(
        title = "Booking Queries",
        icon = Icons.Default.DateRange,
        color = Color(0xFF8B5CF6),
        faqs = listOf(
            FaqQuestion(
                id = "booking_1",
                question = "How do I change my booking?",
                answer = "You can change your booking by going to 'Manage Booking' and entering your PNR and last name. Changes are subject to fare rules and may incur fees depending on your fare type."
            ),
            FaqQuestion(
                id = "booking_2",
                question = "How do I add an infant or child to my booking?",
                answer = "Infants and children can be added during the booking process. If you need to add them after booking, please contact our customer service team or use the Manage Booking feature."
            ),
            FaqQuestion(
                id = "booking_3",
                question = "What is the cancellation policy?",
                answer = "Cancellation policies vary by fare type. Fly fares are non-refundable but may allow changes. Fly+ and FlyMax fares offer more flexibility including free changes and partial refunds."
            ),
            FaqQuestion(
                id = "booking_4",
                question = "How do I change my contact information?",
                answer = "Contact information can be updated through Manage Booking. Go to your booking, select 'Edit Details', and update your email or phone number."
            ),
            FaqQuestion(
                id = "booking_5",
                question = "Can I change the passenger name on my booking?",
                answer = "Minor name corrections (up to 3 characters) can be made through Manage Booking. For larger changes, please contact customer service. Note that name changes may not be possible for all fare types."
            )
        )
    ),
    BAGGAGE(
        title = "Baggage",
        icon = Icons.Default.Star,
        color = Color(0xFF06B6D4),
        faqs = listOf(
            FaqQuestion(
                id = "baggage_1",
                question = "What is the carry-on baggage allowance?",
                answer = "All passengers are allowed one carry-on bag up to 7kg with maximum dimensions of 55x40x20cm. Personal items like laptops and handbags are also permitted."
            ),
            FaqQuestion(
                id = "baggage_2",
                question = "How much checked baggage can I bring?",
                answer = "Checked baggage allowance depends on your fare type. Fly: No included checked bag (purchase available). Fly+: One 23kg bag. FlyMax: Two 23kg bags."
            ),
            FaqQuestion(
                id = "baggage_3",
                question = "How do I add extra baggage?",
                answer = "Extra baggage can be added during booking or through Manage Booking up to 4 hours before departure. Prices are lower when booked in advance online."
            ),
            FaqQuestion(
                id = "baggage_4",
                question = "What if my baggage is lost or damaged?",
                answer = "Report lost or damaged baggage immediately at the airport baggage service desk. You can also file a claim online within 21 days for delayed baggage or 7 days for damaged baggage."
            )
        )
    ),
    FLIGHT(
        title = "Flight-Related Queries",
        icon = Icons.Default.Send,
        color = Color(0xFF10B981),
        faqs = listOf(
            FaqQuestion(
                id = "flight_1",
                question = "What happens if my flight is delayed?",
                answer = "In case of delays, we'll notify you via SMS and email. Depending on the delay duration, you may be entitled to refreshments, meals, or accommodation. Compensation may apply per GACA regulations."
            ),
            FaqQuestion(
                id = "flight_2",
                question = "My flight was cancelled. What are my options?",
                answer = "For cancelled flights, you can choose a full refund or rebooking to an alternative flight. If the cancellation is within our control, we'll provide accommodation and meals as needed."
            ),
            FaqQuestion(
                id = "flight_3",
                question = "How do I select my seat?",
                answer = "Seat selection is available during booking or through Manage Booking. Free seat selection is included with Fly+ and FlyMax fares. Fly fare passengers can purchase seat selection."
            ),
            FaqQuestion(
                id = "flight_4",
                question = "What are the check-in times?",
                answer = "Online check-in opens 24 hours before departure and closes 4 hours prior. Airport check-in counters open 3 hours before and close 60 minutes before domestic flights."
            )
        )
    ),
    SPECIAL_ASSISTANCE(
        title = "Special Assistance",
        icon = Icons.Default.Person,
        color = Color(0xFFF59E0B),
        faqs = listOf(
            FaqQuestion(
                id = "special_1",
                question = "How do I request wheelchair assistance?",
                answer = "Wheelchair assistance can be requested during booking or by contacting us at least 48 hours before departure. Services include airport assistance and boarding support."
            ),
            FaqQuestion(
                id = "special_2",
                question = "Can I travel while pregnant?",
                answer = "Pregnant passengers can travel up to 28 weeks without a medical certificate. Between 28-35 weeks, a fit-to-fly certificate is required. Travel is not permitted after 35 weeks."
            ),
            FaqQuestion(
                id = "special_3",
                question = "What medical equipment can I bring on board?",
                answer = "Medical equipment like CPAP machines and portable oxygen concentrators (POC) are permitted. Please notify us at least 48 hours before travel. Some equipment may need airline approval."
            ),
            FaqQuestion(
                id = "special_4",
                question = "Do you offer services for visually impaired passengers?",
                answer = "Yes, we provide assistance for visually impaired passengers including priority boarding, cabin crew assistance, and guide dogs are welcome on board at no extra charge."
            )
        )
    ),
    APP_WEBSITE(
        title = "Website & App",
        icon = Icons.Default.Settings,
        color = Color(0xFFF43F5E),
        faqs = listOf(
            FaqQuestion(
                id = "app_1",
                question = "Why is my payment not going through?",
                answer = "Payment issues can occur due to card limits, 3D Secure verification, or temporary bank blocks. Try a different card or contact your bank. Ensure pop-ups are enabled for 3DS verification."
            ),
            FaqQuestion(
                id = "app_2",
                question = "I can't access my boarding pass. What should I do?",
                answer = "Boarding passes are available in the app and sent via email after check-in. If you can't access it, try logging out and back in, or retrieve it through Manage Booking using your PNR."
            ),
            FaqQuestion(
                id = "app_3",
                question = "How do I reset my password?",
                answer = "Click 'Forgot Password' on the login screen and enter your email. A reset link will be sent. If you don't receive it, check your spam folder or contact support."
            ),
            FaqQuestion(
                id = "app_4",
                question = "The app is not working properly",
                answer = "Try updating to the latest version, clearing the app cache, or reinstalling. If issues persist, please contact support with your device model and OS version."
            )
        )
    ),
    MEMBERSHIP(
        title = "Membership & Services",
        icon = Icons.Default.Star,
        color = Color(0xFF7C3AED),
        faqs = listOf(
            FaqQuestion(
                id = "member_1",
                question = "What is Adeal Membership?",
                answer = "Adeal Membership is our flight subscription program offering 12, 24, or 36 round trips per year for a fixed monthly price. Perfect for frequent domestic travelers."
            ),
            FaqQuestion(
                id = "member_2",
                question = "How do I book flights with my membership?",
                answer = "Log in to your membership account and select available flights. Flights must be booked at least 3 days before departure. Only domestic destinations are included."
            ),
            FaqQuestion(
                id = "member_3",
                question = "Can I cancel my membership?",
                answer = "Memberships are 12-month commitments. Early cancellation may incur fees based on remaining months. Contact member services for cancellation requests."
            ),
            FaqQuestion(
                id = "member_4",
                question = "What's included in my membership flights?",
                answer = "Membership flights include one small under-seat bag. Checked baggage, seat selection, and meals can be added for an additional fee during booking."
            )
        )
    )
}

/**
 * FAQ question and answer data.
 */
data class FaqQuestion(
    val id: String,
    val question: String,
    val answer: String
)
