package com.parkshare.app.ui.screens.MyBookings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.BookingDto
import com.parkshare.app.data.booking.BookingRepository
import com.parkshare.app.data.booking.QrCodeHelper
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val IST_ZONE = ZoneId.of("Asia/Kolkata")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a")

private fun formatIstDate(isoUtc: String?): String {
    if (isoUtc.isNullOrBlank()) return "N/A"
    return try {
        Instant.parse(isoUtc).atZone(IST_ZONE).format(DATE_FORMAT)
    } catch (_: Exception) {
        isoUtc
    }
}

private fun formatIstTime(isoUtc: String?): String {
    if (isoUtc.isNullOrBlank()) return "N/A"
    return try {
        Instant.parse(isoUtc).atZone(IST_ZONE).format(TIME_FORMAT)
    } catch (_: Exception) {
        isoUtc
    }
}

@Composable
fun MyBookingsScreen(
    onBackClick: () -> Unit = {}
) {
    val bookingRepository = remember { BookingRepository() }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableStateOf("Upcoming") }
    var allBookings by remember { mutableStateOf<List<BookingDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var operationFeedback by remember { mutableStateOf<String?>(null) }

    // Dialog states
    var passDialogBooking by remember { mutableStateOf<BookingDto?>(null) }
    var cancelDialogBooking by remember { mutableStateOf<BookingDto?>(null) }
    var checkoutDialogBooking by remember { mutableStateOf<BookingDto?>(null) }
    var cancelReason by remember { mutableStateOf("") }
    var isOperating by remember { mutableStateOf(false) }

    fun refreshBookings() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = bookingRepository.getMyBookings()
            result.onSuccess { list ->
                allBookings = list
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to load bookings"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshBookings()
    }

    // Filter bookings by tab
    val filteredBookings = remember(allBookings, selectedTab) {
        allBookings.filter { b ->
            when (selectedTab) {
                "Upcoming" -> b.status in listOf("PENDING", "ACCEPTED", "AWAITING_DRIVER_CONFIRMATION", "CONFIRMED")
                "Active" -> b.status == "CHECKED_IN"
                "Completed" -> b.status in listOf("CHECKED_OUT", "COMPLETED", "CANCELLED", "REJECTED", "EXPIRED")
                else -> true
            }
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryBlue
                    )
                }

                Text(
                    text = "My Bookings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            IconButton(onClick = { refreshBookings() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == "Upcoming",
                onClick = { selectedTab = "Upcoming" },
                label = { Text("Upcoming") }
            )

            FilterChip(
                selected = selectedTab == "Active",
                onClick = { selectedTab = "Active" },
                label = {
                    val activeCount = allBookings.count { it.status == "CHECKED_IN" }
                    Text(if (activeCount > 0) "Active ($activeCount)" else "Active")
                }
            )

            FilterChip(
                selected = selectedTab == "Completed",
                onClick = { selectedTab = "Completed" },
                label = { Text("Past & Completed") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Operation Feedback Banner
        if (operationFeedback != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8FFF3))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = SecondaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = operationFeedback!!,
                        color = SecondaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { operationFeedback = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Loading bookings...", color = Color.Gray)
                }
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = errorMessage ?: "Failed to load", color = Color.Red, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { refreshBookings() }) {
                        Text("Retry")
                    }
                }
            }
        } else if (filteredBookings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No $selectedTab Bookings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bookings in this category will appear here.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(filteredBookings, key = { it.id }) { booking ->
                    DriverBookingCard(
                        booking = booking,
                        isOperating = isOperating,
                        onConfirmClick = {
                            scope.launch {
                                isOperating = true
                                val res = bookingRepository.confirmBooking(booking.id)
                                res.onSuccess {
                                    operationFeedback = "Spot confirmed! Your digital parking pass is ready."
                                    refreshBookings()
                                    isOperating = false
                                }.onFailure { err ->
                                    errorMessage = "Confirmation failed: ${err.message}"
                                    isOperating = false
                                }
                            }
                        },
                        onViewPassClick = {
                            passDialogBooking = booking
                        },
                        onCheckInClick = {
                            scope.launch {
                                isOperating = true
                                val token = booking.qrToken ?: ""
                                val res = bookingRepository.checkInBooking(booking.id, token)
                                res.onSuccess {
                                    operationFeedback = "Checked in successfully! Parking session is now active."
                                    refreshBookings()
                                    selectedTab = "Active"
                                    isOperating = false
                                }.onFailure { err ->
                                    errorMessage = "Check-in failed: ${err.message}"
                                    isOperating = false
                                }
                            }
                        },
                        onCheckOutClick = {
                            checkoutDialogBooking = booking
                        },
                        onCancelClick = {
                            cancelDialogBooking = booking
                            cancelReason = ""
                        }
                    )
                }
            }
        }
    }

    // QR Digital Pass Dialog
    if (passDialogBooking != null) {
        val b = passDialogBooking!!
        val qrContent = remember(b) {
            val token = b.qrToken ?: ""
            if (token.isNotBlank()) {
                QrCodeHelper.createQrPassPayload(b.id, token, b.bookingCode)
            } else ""
        }
        val qrBitmap: ImageBitmap? = remember(qrContent) {
            if (qrContent.isNotBlank()) {
                try {
                    QrCodeHelper.generateQrImageBitmap(qrContent, 400)
                } catch (_: Exception) {
                    null
                }
            } else null
        }

        AlertDialog(
            onDismissRequest = { if (!isOperating) passDialogBooking = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Digital Pass",
                    tint = SecondaryGreen,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Digital Parking Pass",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = b.listingTitle ?: "Parking Spot",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatIstDate(b.startTime)} • ${formatIstTime(b.startTime)} - ${formatIstTime(b.endTime)} IST",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // QR Code Image Rendered
                    if (qrBitmap != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                        ) {
                            Image(
                                bitmap = qrBitmap,
                                contentDescription = "Parking Pass QR Code",
                                modifier = Modifier
                                    .size(190.dp)
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("QR Code Unavailable", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "48-Character Verification Pass Token:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7FF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = b.qrToken ?: "No token available",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = PrimaryBlue,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    b.qrToken?.let { token ->
                                        clipboardManager.setText(AnnotatedString(token))
                                        operationFeedback = "Pass token copied to clipboard!"
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Token",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Booking Reference: ${b.bookingCode ?: b.id}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )

                    // Arrive & Check In button if CONFIRMED
                    if (b.status == "CONFIRMED") {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isOperating = true
                                    val token = b.qrToken ?: ""
                                    val res = bookingRepository.checkInBooking(b.id, token)
                                    res.onSuccess {
                                        passDialogBooking = null
                                        operationFeedback = "Checked in successfully! Parking session is now active."
                                        refreshBookings()
                                        selectedTab = "Active"
                                        isOperating = false
                                    }.onFailure { err ->
                                        operationFeedback = "Check-in failed: ${err.message}"
                                        isOperating = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                            enabled = !isOperating,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isOperating) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check In", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Arrive & Check In Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { passDialogBooking = null },
                    enabled = !isOperating
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Check-Out Confirmation Dialog
    if (checkoutDialogBooking != null) {
        val b = checkoutDialogBooking!!
        AlertDialog(
            onDismissRequest = { if (!isOperating) checkoutDialogBooking = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Check Out",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "End Parking Session?",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            },
            text = {
                Column {
                    Text(
                        text = "Are you ready to depart from \"${b.listingTitle ?: "this parking spot"}\"?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will complete your reservation and release the parking slot for other drivers.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isOperating = true
                            val token = b.qrToken ?: ""
                            val res = bookingRepository.checkOutBooking(b.id, token)
                            res.onSuccess {
                                checkoutDialogBooking = null
                                operationFeedback = "Check-out completed! Thank you for using ParkShare."
                                refreshBookings()
                                selectedTab = "Completed"
                                isOperating = false
                            }.onFailure { err ->
                                operationFeedback = "Check-out failed: ${err.message}"
                                isOperating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                    enabled = !isOperating,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Complete Check-Out", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { checkoutDialogBooking = null },
                    enabled = !isOperating
                ) {
                    Text("Stay Parked")
                }
            }
        )
    }

    // Cancel Booking Dialog
    if (cancelDialogBooking != null) {
        val b = cancelDialogBooking!!
        AlertDialog(
            onDismissRequest = {
                if (!isOperating) cancelDialogBooking = null
            },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = "Cancel", tint = Color.Red)
            },
            title = {
                Text("Cancel Reservation?")
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to cancel your reservation for \"${b.listingTitle ?: "this spot"}\"?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Reason (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isOperating = true
                            val res = bookingRepository.cancelBooking(b.id, cancelReason.ifBlank { null })
                            res.onSuccess {
                                cancelDialogBooking = null
                                operationFeedback = "Reservation cancelled."
                                refreshBookings()
                                isOperating = false
                            }.onFailure { err ->
                                operationFeedback = "Cancellation failed: ${err.message}"
                                isOperating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    enabled = !isOperating
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm Cancellation")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { cancelDialogBooking = null },
                    enabled = !isOperating
                ) {
                    Text("Keep Reservation")
                }
            }
        )
    }
}

@Composable
fun DriverBookingCard(
    booking: BookingDto,
    isOperating: Boolean = false,
    onConfirmClick: () -> Unit,
    onViewPassClick: () -> Unit,
    onCheckInClick: () -> Unit = {},
    onCheckOutClick: () -> Unit = {},
    onCancelClick: () -> Unit
) {
    val statusColor = when (booking.status) {
        "CHECKED_IN" -> Color(0xFF2E7D32)
        "CONFIRMED" -> SecondaryGreen
        "AWAITING_DRIVER_CONFIRMATION", "ACCEPTED" -> PrimaryBlue
        "PENDING" -> Color(0xFFF57C00)
        "CANCELLED", "REJECTED", "EXPIRED" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    val statusBg = when (booking.status) {
        "CHECKED_IN" -> Color(0xFFE8F5E9)
        "CONFIRMED" -> Color(0xFFE8FFF3)
        "AWAITING_DRIVER_CONFIRMATION", "ACCEPTED" -> Color(0xFFE3F2FD)
        "PENDING" -> Color(0xFFFFF3E0)
        "CANCELLED", "REJECTED", "EXPIRED" -> Color(0xFFFFEBEE)
        else -> Color(0xFFF5F5F5)
    }

    val cardBorder = if (booking.status == "CHECKED_IN") {
        Modifier.border(1.5.dp, SecondaryGreen, RoundedCornerShape(16.dp))
    } else Modifier

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(cardBorder),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Title & Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.listingTitle ?: "Parking Space",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = statusBg),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (booking.status == "CHECKED_IN") "PARKED ACTIVE" else (booking.status ?: "PENDING"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (!booking.bookingCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = booking.bookingCode,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date & Schedule in IST
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Date",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatIstDate(booking.startTime),
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${formatIstTime(booking.startTime)} - ${formatIstTime(booking.endTime)} IST",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duration & Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration: ${booking.totalHours} hr${if (booking.totalHours > 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Text(
                    text = "₹${booking.totalAmount.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGreen
                )
            }

            // Reason notice if cancelled, rejected, or expired
            if (!booking.rejectionReason.isNullOrBlank() || !booking.cancellationReason.isNullOrBlank() || !booking.expiryReason.isNullOrBlank()) {
                val reason = booking.rejectionReason ?: booking.cancellationReason ?: booking.expiryReason
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: $reason",
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F)
                )
            }

            // Contextual Action Buttons
            when (booking.status) {
                "AWAITING_DRIVER_CONFIRMATION", "ACCEPTED" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirmClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                        ) {
                            Text("Confirm Spot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onCancelClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(0.7f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = Color.Red, fontSize = 13.sp)
                        }
                    }
                }
                "CONFIRMED" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onViewPassClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(imageVector = Icons.Default.QrCode2, contentDescription = "Pass", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pass", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = onCheckInClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check-In", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check-In", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onCancelClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
                "CHECKED_IN" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    // Active Parking Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Session",
                                tint = SecondaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "PARKING SESSION ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "Checked in at: ${formatIstTime(booking.checkedInAt ?: booking.updatedAt)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onViewPassClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.QrCode2, contentDescription = "Pass", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("View Pass", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onCheckOutClick,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check Out", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check Out", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "PENDING" -> {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCancelClick,
                        enabled = !isOperating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel Request", color = Color.Red, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}