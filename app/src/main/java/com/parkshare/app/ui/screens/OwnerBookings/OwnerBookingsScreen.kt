package com.parkshare.app.ui.screens.OwnerBookings

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
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
fun OwnerBookingsScreen(
    onBackClick: () -> Unit = {}
) {
    val bookingRepository = remember { BookingRepository() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf("Pending") }
    var allRequests by remember { mutableStateOf<List<BookingDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var operationFeedback by remember { mutableStateOf<String?>(null) }

    // Dialogs
    var rejectDialogBooking by remember { mutableStateOf<BookingDto?>(null) }
    var rejectReason by remember { mutableStateOf("") }
    var checkinDialogBooking by remember { mutableStateOf<BookingDto?>(null) }
    var checkinInputToken by remember { mutableStateOf("") }
    var checkoutDialogBooking by remember { mutableStateOf<BookingDto?>(null) }
    var showGlobalPassDialog by remember { mutableStateOf(false) }
    var globalPassInput by remember { mutableStateOf("") }
    var globalBookingIdInput by remember { mutableStateOf("") }
    var isOperating by remember { mutableStateOf(false) }

    fun refreshRequests() {
        scope.launch {
            isLoading = true
            errorMessage = null
            val result = bookingRepository.getOwnerRequests()
            result.onSuccess { list ->
                allRequests = list
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to fetch owner requests"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshRequests()
    }

    val filteredRequests = remember(allRequests, selectedTab) {
        allRequests.filter { b ->
            when (selectedTab) {
                "Pending" -> b.status == "PENDING"
                "Active" -> b.status in listOf("ACCEPTED", "AWAITING_DRIVER_CONFIRMATION", "CONFIRMED", "CHECKED_IN")
                "History" -> b.status in listOf("CHECKED_OUT", "COMPLETED", "REJECTED", "CANCELLED", "EXPIRED")
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
                    text = "Booking Requests",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
            }

            Row {
                IconButton(onClick = {
                    showGlobalPassDialog = true
                    globalPassInput = ""
                    globalBookingIdInput = ""
                }) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Verify Pass",
                        tint = PrimaryBlue
                    )
                }

                IconButton(onClick = { refreshRequests() }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = PrimaryBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == "Pending",
                onClick = { selectedTab = "Pending" },
                label = {
                    val pendingCount = allRequests.count { it.status == "PENDING" }
                    Text(if (pendingCount > 0) "Pending ($pendingCount)" else "Pending")
                }
            )

            FilterChip(
                selected = selectedTab == "Active",
                onClick = { selectedTab = "Active" },
                label = {
                    val activeCount = allRequests.count { it.status == "CHECKED_IN" }
                    Text(if (activeCount > 0) "Active ($activeCount)" else "Active / Confirmed")
                }
            )

            FilterChip(
                selected = selectedTab == "History",
                onClick = { selectedTab = "History" },
                label = { Text("History") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

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
                    Text(text = "Loading requests...", color = Color.Gray)
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
                    Button(onClick = { refreshRequests() }) {
                        Text("Retry")
                    }
                }
            }
        } else if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No $selectedTab Requests",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Requests from drivers for your parking spaces will appear here.",
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
                items(filteredRequests, key = { it.id }) { booking ->
                    OwnerRequestCard(
                        booking = booking,
                        isOperating = isOperating,
                        onAccept = {
                            scope.launch {
                                isOperating = true
                                operationFeedback = null
                                val res = bookingRepository.acceptBooking(booking.id)
                                res.onSuccess {
                                    operationFeedback = "Booking request accepted! Driver notified to confirm."
                                    refreshRequests()
                                    isOperating = false
                                }.onFailure { err ->
                                    operationFeedback = "Accept failed: ${err.message}"
                                    isOperating = false
                                }
                            }
                        },
                        onReject = {
                            rejectDialogBooking = booking
                            rejectReason = ""
                        },
                        onVerifyCheckIn = {
                            checkinDialogBooking = booking
                            checkinInputToken = booking.qrToken ?: ""
                        },
                        onCheckOut = {
                            checkoutDialogBooking = booking
                        }
                    )
                }
            }
        }
    }

    // Verify Check-In Dialog for specific booking
    if (checkinDialogBooking != null) {
        val b = checkinDialogBooking!!
        AlertDialog(
            onDismissRequest = { if (!isOperating) checkinDialogBooking = null },
            icon = {
                Icon(imageVector = Icons.Default.QrCode2, contentDescription = "Check-In", tint = SecondaryGreen, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Verify Driver Check-In", fontWeight = FontWeight.Bold, color = PrimaryBlue)
            },
            text = {
                Column {
                    Text(
                        text = "Spot: ${b.listingTitle ?: "Parking Space"}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Driver Vehicle: ${b.vehicleType ?: "Car"} ${b.vehicleNumber?.let { "• $it" } ?: ""}",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    if (!b.bookingCode.isNullOrBlank()) {
                        Text(
                            text = "Reference Code: ${b.bookingCode}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "48-Character Pass Token:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = checkinInputToken,
                        onValueChange = { checkinInputToken = it.trim() },
                        label = { Text("Driver Pass Token") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isOperating = true
                            val token = checkinInputToken.ifBlank { b.qrToken ?: "" }
                            val res = bookingRepository.checkInBooking(b.id, token)
                            res.onSuccess {
                                checkinDialogBooking = null
                                operationFeedback = "Driver verified and checked in! Spot is now actively occupied."
                                refreshRequests()
                                selectedTab = "Active"
                                isOperating = false
                            }.onFailure { err ->
                                operationFeedback = "Verification failed: ${err.message}"
                                isOperating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                    enabled = !isOperating && (checkinInputToken.isNotBlank() || !b.qrToken.isNullOrBlank())
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm Check-In", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { checkinDialogBooking = null },
                    enabled = !isOperating
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Owner Check-Out Confirmation Dialog
    if (checkoutDialogBooking != null) {
        val b = checkoutDialogBooking!!
        AlertDialog(
            onDismissRequest = { if (!isOperating) checkoutDialogBooking = null },
            icon = {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check Out", tint = PrimaryBlue, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Release Parking Spot?", fontWeight = FontWeight.Bold, color = PrimaryBlue)
            },
            text = {
                Column {
                    Text(
                        text = "Check out the vehicle from \"${b.listingTitle ?: "your spot"}\"?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This will mark the reservation as COMPLETED and make the spot available for new drivers.",
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
                                operationFeedback = "Driver checked out! Spot released successfully."
                                refreshRequests()
                                selectedTab = "History"
                                isOperating = false
                            }.onFailure { err ->
                                operationFeedback = "Check-out failed: ${err.message}"
                                isOperating = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                    enabled = !isOperating
                ) {
                    if (isOperating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm Check-Out", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { checkoutDialogBooking = null },
                    enabled = !isOperating
                ) {
                    Text("Keep Active")
                }
            }
        )
    }

    // Global Pass Verifier Dialog
    if (showGlobalPassDialog) {
        AlertDialog(
            onDismissRequest = { if (!isOperating) showGlobalPassDialog = false },
            icon = {
                Icon(imageVector = Icons.Default.QrCode2, contentDescription = "Verify Pass", tint = PrimaryBlue, modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Scan / Verify Driver Pass", fontWeight = FontWeight.Bold, color = PrimaryBlue)
            },
            text = {
                Column {
                    Text(
                        text = "Paste the driver's QR payload, reference, or 48-char pass token below:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = globalPassInput,
                        onValueChange = { input ->
                            globalPassInput = input
                            val parsed = QrCodeHelper.parseQrPass(input)
                            if (!parsed.first.isNullOrBlank()) {
                                globalBookingIdInput = parsed.first!!
                            }
                        },
                        label = { Text("Pass Content / QR String") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = globalBookingIdInput,
                        onValueChange = { globalBookingIdInput = it.trim() },
                        label = { Text("Booking ID (auto-parsed or enter)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                isOperating = true
                                val parsed = QrCodeHelper.parseQrPass(globalPassInput)
                                val bookingId = globalBookingIdInput.ifBlank { parsed.first ?: "" }
                                val token = parsed.second.ifBlank { globalPassInput.trim() }

                                if (bookingId.isBlank() || token.isBlank()) {
                                    operationFeedback = "Please provide both Booking ID and Pass Token."
                                    isOperating = false
                                    return@launch
                                }

                                val res = bookingRepository.checkInBooking(bookingId, token)
                                res.onSuccess {
                                    showGlobalPassDialog = false
                                    operationFeedback = "Driver checked in successfully via Pass Scanner!"
                                    refreshRequests()
                                    selectedTab = "Active"
                                    isOperating = false
                                }.onFailure { err ->
                                    operationFeedback = "Pass verification failed: ${err.message}"
                                    isOperating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                        enabled = !isOperating && globalPassInput.isNotBlank()
                    ) {
                        Text("Check-In", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isOperating = true
                                val parsed = QrCodeHelper.parseQrPass(globalPassInput)
                                val bookingId = globalBookingIdInput.ifBlank { parsed.first ?: "" }
                                val token = parsed.second.ifBlank { globalPassInput.trim() }

                                if (bookingId.isBlank() || token.isBlank()) {
                                    operationFeedback = "Please provide both Booking ID and Pass Token."
                                    isOperating = false
                                    return@launch
                                }

                                val res = bookingRepository.checkOutBooking(bookingId, token)
                                res.onSuccess {
                                    showGlobalPassDialog = false
                                    operationFeedback = "Driver checked out successfully via Pass Scanner!"
                                    refreshRequests()
                                    selectedTab = "History"
                                    isOperating = false
                                }.onFailure { err ->
                                    operationFeedback = "Check-out failed: ${err.message}"
                                    isOperating = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        enabled = !isOperating && globalPassInput.isNotBlank()
                    ) {
                        Text("Check-Out", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showGlobalPassDialog = false },
                    enabled = !isOperating
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Reject Dialog
    if (rejectDialogBooking != null) {
        val b = rejectDialogBooking!!
        AlertDialog(
            onDismissRequest = {
                if (!isOperating) rejectDialogBooking = null
            },
            icon = {
                Icon(imageVector = Icons.Default.Warning, contentDescription = "Reject", tint = Color.Red)
            },
            title = {
                Text("Decline Booking Request?")
            },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to decline this reservation request for \"${b.listingTitle ?: "your spot"}\"?",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason for Rejection (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            isOperating = true
                            val res = bookingRepository.rejectBooking(b.id, rejectReason.ifBlank { null })
                            res.onSuccess {
                                rejectDialogBooking = null
                                operationFeedback = "Booking request declined."
                                refreshRequests()
                                isOperating = false
                            }.onFailure { err ->
                                operationFeedback = "Decline failed: ${err.message}"
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
                        Text("Decline Request")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { rejectDialogBooking = null },
                    enabled = !isOperating
                ) {
                    Text("Keep Reviewing")
                }
            }
        )
    }
}

@Composable
fun OwnerRequestCard(
    booking: BookingDto,
    isOperating: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onVerifyCheckIn: () -> Unit = {},
    onCheckOut: () -> Unit = {}
) {
    val statusColor = when (booking.status) {
        "CHECKED_IN" -> Color(0xFF2E7D32)
        "CONFIRMED" -> SecondaryGreen
        "ACCEPTED", "AWAITING_DRIVER_CONFIRMATION" -> PrimaryBlue
        "PENDING" -> Color(0xFFF57C00)
        "CANCELLED", "REJECTED", "EXPIRED" -> Color(0xFFD32F2F)
        else -> Color.Gray
    }

    val statusBg = when (booking.status) {
        "CHECKED_IN" -> Color(0xFFE8F5E9)
        "CONFIRMED" -> Color(0xFFE8FFF3)
        "ACCEPTED", "AWAITING_DRIVER_CONFIRMATION" -> Color(0xFFE3F2FD)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = booking.listingTitle ?: "My Parking Spot",
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
                    text = "Code: ${booking.bookingCode}",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Schedule & Time
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

            // Vehicle and Notes
            if (!booking.vehicleNumber.isNullOrBlank() || !booking.vehicleType.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Vehicle",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${booking.vehicleType ?: "Car"} ${booking.vehicleNumber?.let { "• $it" } ?: ""}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            if (!booking.driverNotes.isNullOrBlank()) {
                Text(
                    text = "Driver Note: \"${booking.driverNotes}\"",
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Duration & Earnings
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
                    text = "Earnings: ₹${booking.totalAmount.toInt()}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGreen
                )
            }

            // Reason if cancelled, rejected, or expired
            if (!booking.rejectionReason.isNullOrBlank() || !booking.cancellationReason.isNullOrBlank() || !booking.expiryReason.isNullOrBlank()) {
                val reason = booking.rejectionReason ?: booking.cancellationReason ?: booking.expiryReason
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: $reason",
                    fontSize = 12.sp,
                    color = Color(0xFFD32F2F)
                )
            }

            // Actions based on booking status
            when (booking.status) {
                "PENDING" -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onAccept,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Accept", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onReject,
                            enabled = !isOperating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Decline", tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Decline", color = Color.Red, fontSize = 13.sp)
                        }
                    }
                }
                "CONFIRMED" -> {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onVerifyCheckIn,
                        enabled = !isOperating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                    ) {
                        Icon(imageVector = Icons.Default.QrCode2, contentDescription = "Verify Pass", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Check-In", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                "CHECKED_IN" -> {
                    Spacer(modifier = Modifier.height(12.dp))
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
                                    text = "SLOT OCCUPIED • ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "Driver checked in: ${formatIstTime(booking.checkedInAt ?: booking.updatedAt)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onCheckOut,
                        enabled = !isOperating,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Check Out Driver", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Out Driver / Release Slot", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
