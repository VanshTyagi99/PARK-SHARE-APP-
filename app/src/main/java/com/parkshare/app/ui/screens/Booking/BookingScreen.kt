package com.parkshare.app.ui.screens.Booking

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.CreateBookingRequest
import com.parkshare.app.data.api.ParkingListingDto
import com.parkshare.app.data.booking.BookingConflictException
import com.parkshare.app.data.booking.BookingRepository
import com.parkshare.app.data.parking.ParkingRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

private val IST_ZONE = ZoneId.of("Asia/Kolkata")
private val DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy")
private val TIME_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("hh:mm a")

@Composable
fun BookingScreen(
    listingId: String? = null,
    onBackClick: () -> Unit = {},
    onConfirmBooking: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parkingRepository = remember { ParkingRepository() }
    val bookingRepository = remember { BookingRepository() }

    // Listing data state
    var listing by remember { mutableStateOf<ParkingListingDto?>(null) }
    var isLoadingListing by remember { mutableStateOf(!listingId.isNullOrBlank()) }
    var listingError by remember { mutableStateOf<String?>(null) }

    // Booking form state initialized in IST
    val nowIst = remember { ZonedDateTime.now(IST_ZONE) }
    val initialDate = remember {
        val candidate = nowIst.plusHours(2)
        candidate.toLocalDate()
    }
    val initialTime = remember {
        val candidate = nowIst.plusHours(2)
        LocalTime.of(candidate.hour, 0)
    }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(initialTime) }
    var durationHours by remember { mutableIntStateOf(2) }
    var vehicleNumber by remember { mutableStateOf("") }
    var selectedVehicleType by remember { mutableStateOf("Car") }
    var driverNotes by remember { mutableStateOf("") }

    // Submission state
    var isSubmitting by remember { mutableStateOf(false) }
    var conflictError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    // Load real listing details
    LaunchedEffect(listingId) {
        if (!listingId.isNullOrBlank()) {
            isLoadingListing = true
            listingError = null
            val result = parkingRepository.getParkingDetails(listingId)
            result.onSuccess { data ->
                listing = data
                selectedVehicleType = if (data.vehicleCompatibility.equals("TWO_WHEELER", ignoreCase = true) || data.parkingType.equals("Bike", ignoreCase = true)) "Bike" else "Car"
                isLoadingListing = false
            }.onFailure { err ->
                listingError = err.message ?: "Failed to load listing"
                isLoadingListing = false
            }
        }
    }

    // Computed start and end times in IST
    val startDateTime = remember(selectedDate, selectedTime) {
        ZonedDateTime.of(selectedDate, selectedTime, IST_ZONE)
    }
    val endDateTime = remember(startDateTime, durationHours) {
        startDateTime.plusHours(durationHours.toLong())
    }

    // Validation: Start time must be at least 15 mins ahead of now in IST
    val isStartTimeValid = remember(startDateTime) {
        startDateTime.isAfter(ZonedDateTime.now(IST_ZONE).plusMinutes(15))
    }
    val hourlyRate = listing?.pricePerHour ?: 30.0
    val totalAmount = (durationHours * hourlyRate).toInt()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = PrimaryBlue
                )
            }

            Text(
                text = "Book Parking Space",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Listing Information Card
        if (isLoadingListing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBlue)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "Loading parking space...", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else if (listing != null) {
            val spot = listing!!
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7FF))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = spot.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "Location",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = spot.address,
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Text(
                            text = "₹${spot.pricePerHour.toInt()}/hr",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = spot.vehicleCompatibility.ifBlank { spot.parkingType },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = PrimaryBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } else if (listingError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = listingError ?: "Listing could not be loaded",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Date Picker Section
        Text(
            text = "Select Date (IST)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val today = remember { LocalDate.now(IST_ZONE) }
            val tomorrow = remember { today.plusDays(1) }

            FilterChip(
                selected = selectedDate == today,
                onClick = {
                    selectedDate = today
                    conflictError = null
                    generalError = null
                },
                label = { Text("Today") }
            )

            FilterChip(
                selected = selectedDate == tomorrow,
                onClick = {
                    selectedDate = tomorrow
                    conflictError = null
                    generalError = null
                },
                label = { Text("Tomorrow") }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val cal = Calendar.getInstance()
                cal.set(selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth)
                val dialog = DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                        conflictError = null
                        generalError = null
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                )
                dialog.datePicker.minDate = System.currentTimeMillis() - 1000
                dialog.show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Calendar",
                tint = PrimaryBlue
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = selectedDate.format(DATE_DISPLAY_FORMAT),
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time Picker Section
        Text(
            text = "Start Time (IST)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val dialog = TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        selectedTime = LocalTime.of(hourOfDay, minute)
                        conflictError = null
                        generalError = null
                    },
                    selectedTime.hour,
                    selectedTime.minute,
                    false
                )
                dialog.show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Time",
                tint = PrimaryBlue
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = selectedTime.format(TIME_DISPLAY_FORMAT),
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
        }

        if (!isStartTimeValid) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Start time must be at least 15 minutes in advance",
                    color = Color(0xFFD32F2F),
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Duration Stepper (1h to 24h)
        Text(
            text = "Parking Duration",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$durationHours ${if (durationHours == 1) "Hour" else "Hours"}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Ends at ${endDateTime.format(TIME_DISPLAY_FORMAT)} IST",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                if (durationHours > 1) {
                                    durationHours--
                                    conflictError = null
                                    generalError = null
                                }
                            },
                            enabled = durationHours > 1
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Text(
                            text = "$durationHours hr",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                if (durationHours < 24) {
                                    durationHours++
                                    conflictError = null
                                    generalError = null
                                }
                            },
                            enabled = durationHours < 24
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Vehicle Details
        Text(
            text = "Vehicle Information",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Car", "Bike", "SUV").forEach { type ->
                FilterChip(
                    selected = selectedVehicleType.equals(type, ignoreCase = true),
                    onClick = { selectedVehicleType = type },
                    label = { Text(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = vehicleNumber,
            onValueChange = { vehicleNumber = it.uppercase() },
            label = { Text("Vehicle Registration (Optional)") },
            placeholder = { Text("e.g. DL 01 AB 1234") },
            leadingIcon = {
                Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = "Vehicle")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = driverNotes,
            onValueChange = { driverNotes = it },
            label = { Text("Notes for Spot Owner (Optional)") },
            placeholder = { Text("e.g. Will arrive by 11:15 AM") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Conflict Error Alert Banner
        if (conflictError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Conflict",
                        tint = Color(0xFF856404),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = conflictError ?: "Slot unavailable",
                        color = Color(0xFF856404),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // General Error Banner
        if (generalError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
            ) {
                Text(
                    text = generalError ?: "Error processing booking",
                    color = Color.Red,
                    modifier = Modifier.padding(14.dp),
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Total Amount Summary Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FFF8))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Price",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "$durationHours hrs × ₹${hourlyRate.toInt()}/hr",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "₹$totalAmount",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm & Submit Booking Button
        Button(
            onClick = {
                if (listingId.isNullOrBlank()) {
                    generalError = "Missing listing information. Please select a valid parking space."
                    return@Button
                }
                val currentStart = ZonedDateTime.of(selectedDate, selectedTime, IST_ZONE)
                if (!currentStart.isAfter(ZonedDateTime.now(IST_ZONE).plusMinutes(15))) {
                    generalError = "Start time must be at least 15 minutes in advance to give the owner time to respond."
                    return@Button
                }

                scope.launch {
                    isSubmitting = true
                    conflictError = null
                    generalError = null

                    // Format start and end times to UTC ISO-8601 strings
                    val currentEnd = currentStart.plusHours(durationHours.toLong())
                    val isoFormatter = DateTimeFormatter.ISO_INSTANT
                    val startUtcIso = isoFormatter.format(currentStart.toInstant())
                    val endUtcIso = isoFormatter.format(currentEnd.toInstant())

                    val req = CreateBookingRequest(
                        listingId = listingId,
                        startTime = startUtcIso,
                        endTime = endUtcIso,
                        vehicleNumber = vehicleNumber.trim().ifBlank { null },
                        vehicleType = selectedVehicleType,
                        driverNotes = driverNotes.trim().ifBlank { null }
                    )

                    val result = bookingRepository.createBooking(req)
                    result.onSuccess { createdBooking ->
                        isSubmitting = false
                        onConfirmBooking(createdBooking.id)
                    }.onFailure { err ->
                        isSubmitting = false
                        if (err is BookingConflictException) {
                            conflictError = "This parking slot is already reserved for the selected time window. Please choose another time or spot."
                        } else {
                            generalError = err.message ?: "Failed to submit booking reservation."
                        }
                    }
                }
            },
            enabled = !isSubmitting && isStartTimeValid && !listingId.isNullOrBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Submitting...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Text(
                    text = "Submit Reservation Request",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}