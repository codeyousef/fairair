package com.fairair.app.ui.screens.landing

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fairair.app.localization.LocalStrings
import com.fairair.app.ui.theme.VelocityColors

// Max width for content on desktop
private val MaxContentWidth = 1200.dp
private val MaxCardWidth = 800.dp

/**
 * Landing page for FairAir - the main entry point before booking.
 * Responsive design with proper max-width constraints for desktop.
 * 
 * @param onFlyNowClick Called when the main CTA is clicked
 * @param onLoginClick Called when sign in is clicked
 * @param onLogoutClick Called when sign out is clicked
 * @param onMyBookingsClick Called when my bookings is clicked
 * @param onSettingsClick Called when settings is clicked
 * @param onCheckInClick Called when check-in is clicked
 * @param onManageBookingClick Called when manage booking is clicked
 * @param onMembershipClick Called when membership is clicked
 * @param onHotelsClick Called when hotels external service is clicked
 * @param onCarRentalClick Called when car rental external service is clicked
 * @param onHelpClick Called when help center is clicked
 * @param onDealClick Called when a deal is clicked, with origin and destination codes
 * @param onDestinationClick Called when a destination is clicked, with destination code
 * @param userName The logged in user's name, or null if not logged in
 * @param userOriginCity The user's detected origin city name (e.g., "Jeddah")
 * @param popularDestinationCodes List of destination codes to show as popular (based on user's origin)
 * @param isRtl Whether to use RTL layout
 */
@Composable
fun LandingScreen(
    onFlyNowClick: () -> Unit,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit = {},
    onMyBookingsClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onCheckInClick: () -> Unit = {},
    onManageBookingClick: () -> Unit = {},
    onMembershipClick: () -> Unit = {},
    onHotelsClick: () -> Unit = {},
    onCarRentalClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onDealClick: ((origin: String, destination: String) -> Unit)? = null,
    onDestinationClick: ((destination: String) -> Unit)? = null,
    userName: String? = null,
    userOriginCity: String? = null,
    popularDestinationCodes: List<String>? = null,
    isRtl: Boolean = false
) {
    val scrollState = rememberScrollState()
    
    // Filter destinations based on provided codes, or use all if not specified
    val filteredDestinations = remember(popularDestinationCodes) {
        if (popularDestinationCodes != null) {
            destinations.filter { it.code in popularDestinationCodes }
        } else {
            destinations
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        VelocityColors.GradientStart,
                        VelocityColors.GradientEnd
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header - full width but content constrained
            LandingHeader(
                onLoginClick = onLoginClick,
                onLogoutClick = onLogoutClick,
                onMyBookingsClick = onMyBookingsClick,
                onSettingsClick = onSettingsClick,
                userName = userName
            )
            
            // Hero Section
            HeroSection(onFlyNowClick = onFlyNowClick)
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Services Section - Quick Links
            ServicesSection(
                onCheckInClick = onCheckInClick,
                onManageBookingClick = onManageBookingClick,
                onMembershipClick = onMembershipClick,
                onHotelsClick = onHotelsClick,
                onCarRentalClick = onCarRentalClick,
                onHelpClick = onHelpClick
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Promotional Deals
            DealsSection(onDealClick = onDealClick)
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Popular Destinations (filtered based on user's origin)
            DestinationsSection(
                onDestinationClick = onDestinationClick,
                destinations = filteredDestinations,
                userOriginCity = userOriginCity
            )
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Features
            FeaturesSection()
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Footer
            LandingFooter(
                onCheckInClick = onCheckInClick,
                onManageBookingClick = onManageBookingClick,
                onMembershipClick = onMembershipClick
            )
        }
    }
}

@Composable
private fun LandingHeader(
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onMyBookingsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    userName: String?
) {
    val strings = LocalStrings.current
    val isLoggedIn = userName != null
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "FairAir",
                    tint = VelocityColors.Primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = strings.appName,
                    style = MaterialTheme.typography.titleLarge,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Nav buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = strings.settings,
                        tint = VelocityColors.TextMuted
                    )
                }
                
                if (isLoggedIn) {
                        // Welcome message
                        Text(
                            text = "${strings.landingWelcome}, $userName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = VelocityColors.TextMuted
                        )
                        
                        // My Bookings button
                        Button(
                            onClick = onMyBookingsClick,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VelocityColors.Accent.copy(alpha = 0.1f),
                                contentColor = VelocityColors.Accent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.landingMyBookings)
                        }
                        
                        // Sign Out button
                        Button(
                            onClick = onLogoutClick,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = VelocityColors.TextMain
                            ),
                            border = BorderStroke(1.dp, VelocityColors.GlassBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(strings.landingSignOut)
                        }
                    } else {
                        // Sign In button
                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = VelocityColors.TextMain
                            ),
                            border = BorderStroke(1.dp, VelocityColors.GlassBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                        Text(strings.landingSignIn)
                    }
                }
            }
        }
    }
}@Composable
private fun HeroSection(onFlyNowClick: () -> Unit) {
    val strings = LocalStrings.current
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxCardWidth)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 64.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.landingHeroTitle1,
                style = MaterialTheme.typography.displaySmall,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = strings.landingHeroTitle2,
                style = MaterialTheme.typography.displaySmall,
                color = VelocityColors.Accent,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = strings.landingHeroSubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = VelocityColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 500.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Fly Now Button
            Button(
                onClick = onFlyNowClick,
                modifier = Modifier.height(56.dp).pointerHoverIcon(PointerIcon.Hand),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VelocityColors.Accent,
                    contentColor = VelocityColors.BackgroundDeep
                ),
                shape = RoundedCornerShape(28.dp),
                contentPadding = PaddingValues(horizontal = 48.dp)
            ) {
                Text(
                    text = strings.landingSearchFlights,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = strings.landingFaresFrom,
                style = MaterialTheme.typography.bodySmall,
                color = VelocityColors.TextMuted
            )
        }
    }
}

@Composable
private fun DealsSection(onDealClick: ((String, String) -> Unit)?) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            SectionHeader(
                title = strings.landingSpecialOffers,
                subtitle = strings.landingSpecialOffersSubtitle
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(deals) { deal ->
                DealCard(
                    deal = deal,
                    onClick = if (onDealClick != null) {
                        { onDealClick(deal.originCode, deal.destinationCode) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ServicesSection(
    onCheckInClick: () -> Unit,
    onManageBookingClick: () -> Unit,
    onMembershipClick: () -> Unit,
    onHotelsClick: () -> Unit,
    onCarRentalClick: () -> Unit,
    onHelpClick: () -> Unit
) {
    val strings = LocalStrings.current
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SectionHeader(
                title = "Quick Services",
                subtitle = "Everything you need, just a tap away"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ServiceCard(
                    icon = Icons.AutoMirrored.Filled.Send,
                    title = "Check-In",
                    description = "Check in online 48h before departure",
                    onClick = onCheckInClick,
                    modifier = Modifier.weight(1f)
                )
                ServiceCard(
                    icon = Icons.Default.Search,
                    title = "Manage Booking",
                    description = "View, modify, or cancel your trip",
                    onClick = onManageBookingClick,
                    modifier = Modifier.weight(1f)
                )
                ServiceCard(
                    icon = Icons.Default.Star,
                    title = "Membership",
                    description = "Unlimited flights, premium benefits",
                    onClick = onMembershipClick,
                    accentColor = Color(0xFFFFD700),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // External services row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExternalServiceCard(
                    icon = Icons.Default.Home,
                    title = "Hotels",
                    partner = "Booking.com",
                    onClick = onHotelsClick,
                    modifier = Modifier.weight(1f)
                )
                ExternalServiceCard(
                    icon = Icons.Default.LocationOn,
                    title = "Car Rental",
                    partner = "Rentalcars.com",
                    onClick = onCarRentalClick,
                    modifier = Modifier.weight(1f)
                )
                ExternalServiceCard(
                    icon = Icons.Default.Info,
                    title = "Help Center",
                    partner = "FAQs & Support",
                    onClick = onHelpClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    accentColor: Color = VelocityColors.Accent,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = VelocityColors.BackgroundMid,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = VelocityColors.TextMuted,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ExternalServiceCard(
    icon: ImageVector,
    title: String,
    partner: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = VelocityColors.BackgroundMid.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, VelocityColors.GlassBorder.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VelocityColors.TextMuted,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = VelocityColors.TextMain,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "via $partner",
                    style = MaterialTheme.typography.bodySmall,
                    color = VelocityColors.TextMuted
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = VelocityColors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun DealCard(
    deal: Deal,
    onClick: (() -> Unit)?
) {
    val strings = LocalStrings.current
    val badgeText = when (deal.badgeKey) {
        "flashSale" -> strings.landingFlashSale
        "percentOff" -> strings.landingPercentOff
        "newRoute" -> strings.landingNewRoute
        "popular" -> strings.landingPopular
        else -> deal.badgeKey
    }
    Surface(
        modifier = Modifier
            .width(300.dp)
            .then(if (onClick != null) Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = VelocityColors.BackgroundMid,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder)
    ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = deal.badgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = deal.badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Route
            Text(
                text = "${deal.originCity} to ${deal.destinationCity}",
                style = MaterialTheme.typography.titleMedium,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = deal.dates,
                style = MaterialTheme.typography.bodySmall,
                color = VelocityColors.TextMuted
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = strings.landingFrom,
                        style = MaterialTheme.typography.bodySmall,
                        color = VelocityColors.TextMuted
                    )
                    Text(
                        text = "SAR ${deal.price}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = VelocityColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationsSection(
    onDestinationClick: ((String) -> Unit)?,
    destinations: List<Destination>,
    userOriginCity: String? = null
) {
    val strings = LocalStrings.current
    
    // Customize subtitle based on user's origin
    val subtitle = if (userOriginCity != null) {
        "Fly from $userOriginCity to these amazing places"
    } else {
        strings.landingPopularDestinationsSubtitle
    }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            SectionHeader(
                title = strings.landingPopularDestinations,
                subtitle = subtitle
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(destinations) { destination ->
                DestinationCard(
                    destination = destination,
                    onClick = if (onDestinationClick != null) {
                        { onDestinationClick(destination.code) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun DestinationCard(
    destination: Destination,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .then(if (onClick != null) Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = VelocityColors.BackgroundMid,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // City code in accent circle
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = VelocityColors.Accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = destination.code,
                        style = MaterialTheme.typography.titleMedium,
                        color = VelocityColors.Accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = destination.city,
                style = MaterialTheme.typography.bodyMedium,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = destination.country,
                style = MaterialTheme.typography.bodySmall,
                color = VelocityColors.TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeaturesSection() {
    val strings = LocalStrings.current
    val localizedFeatures = listOf(
        Feature(
            strings.landingBestPrice,
            strings.landingBestPriceDesc,
            Icons.Default.CheckCircle
        ),
        Feature(
            strings.landingFlexibleBooking,
            strings.landingFlexibleBookingDesc,
            Icons.Default.Refresh
        ),
        Feature(
            strings.landingSupport,
            strings.landingSupportDesc,
            Icons.Default.Phone
        ),
        Feature(
            strings.landingRewards,
            strings.landingRewardsDesc,
            Icons.Default.Star
        )
    )
    
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxCardWidth)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            SectionHeader(
                title = strings.landingWhyUs,
                subtitle = strings.landingWhyUsSubtitle
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Features in a 2x2 grid on larger screens, stack on mobile
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        feature = localizedFeatures[0],
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        feature = localizedFeatures[1],
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureCard(
                        feature = localizedFeatures[2],
                        modifier = Modifier.weight(1f)
                    )
                    FeatureCard(
                        feature = localizedFeatures[3],
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    feature: Feature,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = VelocityColors.BackgroundMid,
        border = BorderStroke(1.dp, VelocityColors.GlassBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = VelocityColors.Accent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = VelocityColors.Accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleSmall,
                color = VelocityColors.TextMain,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
                color = VelocityColors.TextMuted,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = VelocityColors.TextMain,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = VelocityColors.TextMuted
        )
    }
}

@Composable
private fun LandingFooter(
    onCheckInClick: () -> Unit = {},
    onManageBookingClick: () -> Unit = {},
    onMembershipClick: () -> Unit = {}
) {
    val strings = LocalStrings.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = VelocityColors.BackgroundMid
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MaxContentWidth)
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                // Footer grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Company
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "FairAir",
                                tint = VelocityColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = strings.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = VelocityColors.TextMain,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your journey, perfected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VelocityColors.TextMuted
                        )
                    }
                    
                    // Quick Links
                    Column {
                        Text(
                            text = "Quick Links",
                            style = MaterialTheme.typography.labelLarge,
                            color = VelocityColors.TextMain,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FooterLink("Check-In", onCheckInClick)
                        FooterLink("Manage Booking", onManageBookingClick)
                        FooterLink("Membership", onMembershipClick)
                    }
                    
                    // Support
                    Column {
                        Text(
                            text = "Support",
                            style = MaterialTheme.typography.labelLarge,
                            color = VelocityColors.TextMain,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FooterLink(strings.landingFooterHelp)
                        FooterLink("Contact Us")
                        FooterLink("FAQs")
                    }
                    
                    // Legal
                    Column {
                        Text(
                            text = "Legal",
                            style = MaterialTheme.typography.labelLarge,
                            color = VelocityColors.TextMain,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FooterLink(strings.landingFooterPrivacy)
                        FooterLink(strings.landingFooterTerms)
                        FooterLink(strings.landingFooterAbout)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                HorizontalDivider(color = VelocityColors.GlassBorder)
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Bottom row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.landingFooterCopyright,
                        style = MaterialTheme.typography.bodySmall,
                        color = VelocityColors.TextMuted
                    )
                    
                    Text(
                        text = strings.landingFooterPortfolio,
                        style = MaterialTheme.typography.bodySmall,
                        color = VelocityColors.TextMuted.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FooterLink(text: String, onClick: (() -> Unit)? = null) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = VelocityColors.TextMuted,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .then(
                if (onClick != null) {
                    Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick)
                } else {
                    Modifier.pointerHoverIcon(PointerIcon.Hand).clickable { /* placeholder */ }
                }
            )
    )
}

// Data classes and sample data

private data class Deal(
    val originCity: String,
    val originCode: String,
    val destinationCity: String,
    val destinationCode: String,
    val price: Int,
    val badgeKey: String,
    val badgeColor: Color,
    val dates: String
)

private data class Destination(
    val city: String,
    val country: String,
    val code: String
)

private data class Feature(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val deals = listOf(
    Deal("Riyadh", "RUH", "Dubai", "DXB", 299, "flashSale", VelocityColors.Accent, "Dec 15 - Jan 15"),
    Deal("Jeddah", "JED", "Cairo", "CAI", 349, "percentOff", VelocityColors.Warning, "Flexible dates"),
    Deal("Dammam", "DMM", "Dubai", "DXB", 249, "newRoute", VelocityColors.Success, "Daily flights"),
    Deal("Riyadh", "RUH", "Jeddah", "JED", 179, "popular", Color(0xFF8B5CF6), "Multiple daily")
)

private val destinations = listOf(
    Destination("Dubai", "UAE", "DXB"),
    Destination("Cairo", "Egypt", "CAI"),
    Destination("Jeddah", "KSA", "JED"),
    Destination("Riyadh", "KSA", "RUH"),
    Destination("Dammam", "KSA", "DMM"),
    Destination("Abha", "KSA", "AHB")
)
