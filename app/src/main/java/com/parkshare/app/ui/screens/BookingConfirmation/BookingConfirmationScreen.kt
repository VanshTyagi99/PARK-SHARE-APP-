package com.parkshare.app.ui.screens.BookingConfirmation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.BookingDto
import com.parkshare.app.data.booking.BookingRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val IST_ZONE = ZoneId.of("Asia/Kolkata")
private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a")

private fun formatIstDateTime(isoUtcString: String?): String {
    if (isoUtcString.isNullOrBlank()) return "N/A"
    return try {
        val instant = Instant.parse(isoUtcString)
        val zdt = instant.atZone(IST_ZONE)
        "${zdt.format(DATE_FORMAT)} at ${zdt.format(TIME_FORMAT)} IST"
    } catch (_: Exception) {
        isoUtcString
    }
}

private fun formatIstTime(isoUtcString: String?): String {
    if (isoUtcString.isNullOrBlank()) return "N/A"
    return try {
        val instant = Instant.parse(isoUtcString)
        instant.atZone(IST_ZONE).format(TIME_FORMAT)
    } catch (_: Exception) {
        isoUtcString
    }
}

private fun formatIstDate(isoUtcString: String?): String {
    if (isoUtcString.isNullOrBlank()) return "N/A"
    return try {
        val instant = Instant.parse(isoUtcString)
        instant.atZone(IST_ZONE).format(DATE_FORMAT)
    } catch (_: Exception) {
        isoUtcString
    }
}

@Composable
fun BookingConfirmationScreen(
    bookingId: String? = null,
    onViewBookingsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {
    val bookingRepository = remember { BookingRepository() }
    val scope = rememberCoroutineScope()

    var booking by remember { mutableStateOf<BookingDto?>(null) }
    var isLoading by remember { mutableStateOf(!bookingId.isNullOrBlank()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isConfirming by remember { mutableStateOf(false) }
    var actionFeedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bookingId) {
        if (!bookingId.isNullOrBlank()) {
            isLoading = true
            errorMessage = null
            val result = bookingRepository.getBookingDetails(bookingId)
            result.onSuccess { data ->
                booking = data
                isLoading = false
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to fetch booking details"
                isLoading = false
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "Loading booking confirmation...", color = Color.Gray, fontSize = 15.sp)
                }
            }
        } else if (booking != null) {
            val b = booking!!
            val isAwaitingConfirmation = b.status == "AWAITING_DRIVER_CONFIRMATION" || b.status == "ACCEPTED"
            val isConfirmed = b.status == "CONFIRMED" || b.status == "CHECKED_IN"

            // Status Icon & Header
            val headerColor = when {
                isConfirmed -> SecondaryGreen
                isAwaitingConfirmation -> PrimaryBlue
                b.status == "PENDING" -> Color(0xFFF57C00)
                else -> Color.Gray
            }
            val bgIconColor = when {
                isConfirmed -> Color(0xFFE8FFF3)
                isAwaitingConfirmation -> Color(0xFFE8F0FE)
                b.status == "PENDING" -> Color(0xFFFFF3E0)
                else -> Color(0xFFF5F5F5)
            }

            Column(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bgIconColor)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        isConfirmed -> Icons.Default.Check
                        isAwaitingConfirmation -> Icons.Default.Check
                        b.status == "PENDING" -> Icons.Default.AccessTime
                        else -> Icons.Default.Receipt
                    },
                    contentDescription = "Status",
                    tint = headerColor,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when {
                    isConfirmed -> "Reservation Confirmed!"
                    isAwaitingConfirmation -> "Owner Accepted!"
                    b.status == "PENDING" -> "Request Submitted!"
                    else -> "Booking Status: ${b.status}"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when {
                    isConfirmed -> "Your spot is secured. Show your digital pass upon arrival."
                    isAwaitingConfirmation -> "Please confirm your reservation before the deadline."
                    b.status == "PENDING" -> "Your booking request is sent to the parking spot owner."
                    else -> "Reference ID: ${b.bookingCode ?: b.id}"
                },
                fontSize = 14.sp,
                color = Color.Gray,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action Feedback if any
            if (actionFeedback != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8FFF3))
                ) {
                    Text(
                        text = actionFeedback!!,
                        color = SecondaryGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Awaiting driver confirmation callout
            if (isAwaitingConfirmation) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Deadline",
                                tint = Color(0xFFF57C00),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Action Required: Confirm Spot",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF57C00),
                                fontSize = 15.sp
                            )
                        }
                        if (!b.confirmationDeadline.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Confirmation deadline: ${formatIstDateTime(b.confirmationDeadline)}",
                                fontSize = 13.sp,
                                color = Color.DarkGray
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    isConfirming = true
                                    actionFeedback = null
                                    val confirmRes = bookingRepository.confirmBooking(b.id)
                                    confirmRes.onSuccess { updated ->
                                        booking = updated
                                        actionFeedback = "Booking confirmed successfully! Digital pass issued."
                                        isConfirming = false
                                    }.onFailure { err ->
                                        actionFeedback = "Failed to confirm: ${err.message}"
                                        isConfirming = false
                                    }
                                }
                            },
                            enabled = !isConfirming,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isConfirming) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("Confirm Reservation Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // QR Token / Passcard if Confirmed
            if (isConfirmed && !b.qrToken.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = "Pass",
                                tint = SecondaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Digital Check-In Pass",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SecondaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = b.qrToken,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray,
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Provide this 48-character code or QR pass to the owner at arrival.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Booking Details Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7FF))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Booking Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )

                        // Status Badge
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when (b.status) {
                                    "CONFIRMED", "CHECKED_IN" -> Color(0xFFE8FFF3)
                                    "AWAITING_DRIVER_CONFIRMATION", "ACCEPTED" -> Color(0xFFE3F2FD)
                                    "PENDING" -> Color(0xFFFFF3E0)
                                    else -> Color(0xFFF5F5F5)
                                }
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = b.status ?: "PENDING",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (b.status) {
                                    "CONFIRMED", "CHECKED_IN" -> SecondaryGreen
                                    "AWAITING_DRIVER_CONFIRMATION", "ACCEPTED" -> PrimaryBlue
                                    "PENDING" -> Color(0xFFF57C00)
                                    else -> Color.Gray
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    BookingDetailRow(label = "Booking ID", value = b.bookingCode ?: b.id)
                    Spacer(modifier = Modifier.height(10.dp))

                    BookingDetailRow(label = "Parking Space", value = b.listingTitle ?: "Parking Space")
                    Spacer(modifier = Modifier.height(10.dp))

                    if (!b.listingAddress.isNullOrBlank()) {
                        BookingDetailRow(label = "Address", value = b.listingAddress)
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    BookingDetailRow(label = "Date", value = formatIstDate(b.startTime))
                    Spacer(modifier = Modifier.height(10.dp))

                    BookingDetailRow(
                        label = "Schedule (IST)",
                        value = "${formatIstTime(b.startTime)} - ${formatIstTime(b.endTime)}"
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    BookingDetailRow(label = "Duration", value = "${b.totalHours} Hour${if (b.totalHours > 1) "s" else ""}")
                    Spacer(modifier = Modifier.height(10.dp))

                    BookingDetailRow(
                        label = "Rate",
                        value = "₹${b.hourlyRateSnapshot.toInt()} / hour"
                    )

                    if (!b.vehicleNumber.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        BookingDetailRow(
                            label = "Vehicle",
                            value = "${b.vehicleType ?: "Car"} (${b.vehicleNumber})"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Amount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "₹${b.totalAmount.toInt()}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGreen
                        )
                    }
                }
            }
        } else {
            // Error or Missing ID
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = errorMessage ?: "No booking information found",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Action Buttons
        Button(
            onClick = onViewBookingsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Icon(imageVector = Icons.Default.Receipt, contentDescription = "Bookings")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "View My Bookings",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = "Home", tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Home",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BookingDetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray,
            modifier = Modifier.weight(1.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}