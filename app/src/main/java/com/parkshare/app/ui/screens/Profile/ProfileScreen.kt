package com.parkshare.app.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.ApiClient
import com.parkshare.app.data.api.UserProfileResponse
import com.parkshare.app.data.auth.AuthRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onOwnerBookingsClick: () -> Unit = {},
    onOfferParkingClick: () -> Unit = {},
    onMyListingsClick: () -> Unit = {},
    onVerificationStatusClick: () -> Unit = {},
    onAdminCenterClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val authRepository = remember { AuthRepository() }
    val userEmail = authRepository.getCurrentUserEmail() ?: "Not signed in"
    var userProfile by remember { mutableStateOf<UserProfileResponse?>(null) }
    var verificationStatus by remember { mutableStateOf("NOT_SUBMITTED") }

    LaunchedEffect(Unit) {
        if (authRepository.isUserLoggedIn()) {
            try {
                val response = ApiClient.apiService.getCurrentUser()
                if (response.isSuccessful && response.body()?.data != null) {
                    userProfile = response.body()?.data
                }
            } catch (_: Exception) {}

            try {
                val verifResponse = ApiClient.apiService.getVerificationStatus()
                if (verifResponse.isSuccessful && verifResponse.body()?.data != null) {
                    verificationStatus = verifResponse.body()?.data?.status ?: "NOT_SUBMITTED"
                }
            } catch (_: Exception) {}
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = onBackClick
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlue
                )
            }

            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {

            item {

                // Profile Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF3F7FF)
                    )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        val displayName = userProfile?.fullName?.ifBlank { null }
                            ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                        val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                        val displayEmail = userProfile?.email ?: userEmail
                        val displayPhone = userProfile?.phone?.ifBlank { null } ?: "Member"

                        // Profile Circle
                        Column(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryBlue)
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {

                            Text(
                                text = initial,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(16.dp)
                        )

                        Column {

                            Text(
                                text = displayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = displayEmail,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )

                            Text(
                                text = displayPhone,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                // Verification Status Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onVerificationStatusClick() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (verificationStatus) {
                            "APPROVED" -> Color(0xFFF0FDF4)
                            "PENDING" -> Color(0xFFFFFBEB)
                            "REJECTED" -> Color(0xFFFEF2F2)
                            else -> Color(0xFFEFF6FF)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (verificationStatus) {
                                "APPROVED" -> Icons.Default.VerifiedUser
                                else -> Icons.Default.Shield
                            },
                            contentDescription = "Verification",
                            tint = when (verificationStatus) {
                                "APPROVED" -> SecondaryGreen
                                "PENDING" -> Color(0xFFD97706)
                                "REJECTED" -> Color(0xFFDC2626)
                                else -> PrimaryBlue
                            },
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Account Verification",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                Box(
                                    modifier = Modifier
                                        .background(
                                            when (verificationStatus) {
                                                "APPROVED" -> Color(0xFFDCFCE7)
                                                "PENDING" -> Color(0xFFFEF3C7)
                                                "REJECTED" -> Color(0xFFFEE2E2)
                                                else -> Color(0xFFDBEAFE)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = verificationStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (verificationStatus) {
                                            "APPROVED" -> SecondaryGreen
                                            "PENDING" -> Color(0xFFD97706)
                                            "REJECTED" -> Color(0xFFDC2626)
                                            else -> PrimaryBlue
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = when (verificationStatus) {
                                    "APPROVED" -> "Verified for finding & offering parking"
                                    "PENDING" -> "Documents under review. Tap to check"
                                    "REJECTED" -> "Verification rejected. Tap to resubmit"
                                    else -> "Required to find & offer parking. Tap to verify"
                                },
                                fontSize = 12.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = "My Activity",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )
            }

            item {

                val isAdmin = userProfile?.role == "admin" ||
                    userEmail.equals("vanshtyagi7032@gmail.com", ignoreCase = true) ||
                    userEmail.equals("admin@parkshare.com", ignoreCase = true)

                if (isAdmin) {
                    ProfileMenuItem(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "Admin Dashboard & Operations",
                        subtitle = "Platform metrics, KYC verification, users & bookings",
                        onClick = onAdminCenterClick,
                        iconTint = Color(0xFF7C3AED)
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }

                ProfileMenuItem(
                    icon = Icons.Default.Bookmark,
                    title = "My Bookings",
                    subtitle = "View your parking bookings",
                    onClick = onBookingsClick
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "Messages & Support",
                    subtitle = "Chat with drivers and parking owners",
                    onClick = onMessagesClick,
                    iconTint = PrimaryBlue
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.LocalParking,
                    title = "Offer Parking",
                    subtitle = "List your parking space",
                    onClick = onOfferParkingClick
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.DirectionsCar,
                    title = "My Listings",
                    subtitle = "Manage your parking spaces",
                    onClick = onMyListingsClick
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.ReceiptLong,
                    title = "Owner Booking Requests",
                    subtitle = "Review and accept driver reservations",
                    onClick = onOwnerBookingsClick
                )

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Manage your notifications",
                    onClick = {}
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "App preferences and account settings",
                    onClick = {}
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.DirectionsCar,
                    title = "My Vehicles",
                    subtitle = "Manage your vehicles",
                    onClick = {}
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                ProfileMenuItem(
                    icon = Icons.Default.Logout,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = {
                        authRepository.logout()
                        onLogoutClick()
                    },
                    iconTint = Color.Red
                )
            }
        }
    }
}


@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = PrimaryBlue
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FC)
        ),
        onClick = onClick
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint
            )

            Spacer(
                modifier = Modifier.width(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }
    }
}