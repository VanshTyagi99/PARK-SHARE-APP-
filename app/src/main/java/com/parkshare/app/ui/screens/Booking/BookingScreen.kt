package com.parkshare.app.ui.screens.Booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen

@Composable
fun BookingScreen(
    onBackClick: () -> Unit = {},
    onConfirmBooking: () -> Unit = {}
) {

    var selectedDate by remember {
        mutableStateOf("14 August 2026")
    }

    var selectedTime by remember {
        mutableStateOf("10:00 AM")
    }

    var duration by remember {
        mutableIntStateOf(1)
    }

    val pricePerHour = 30
    val totalAmount = duration * pricePerHour

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
                text = "Book Parking",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Parking Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3F7FF)
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp)
            ) {

                Text(
                    text = "Parking Space 1",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "0.5 km away",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "₹30 / hour",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGreen
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Select Date
        Text(
            text = "Select Date",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = {
                // Date picker will be added later
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {

            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "Calendar"
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = selectedDate
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Select Time
        Text(
            text = "Select Start Time",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        OutlinedButton(
            onClick = {
                // Time picker will be added later
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {

            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Time"
            )

            Spacer(
                modifier = Modifier.width(10.dp)
            )

            Text(
                text = selectedTime
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Duration
        Text(
            text = "Parking Duration",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Duration",
                    fontSize = 16.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {
                            if (duration > 1) {
                                duration--
                            }
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease"
                        )
                    }

                    Text(
                        text = "$duration hour",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(
                        onClick = {
                            if (duration < 12) {
                                duration++
                            }
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase"
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Total Amount
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF0FFF8)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Total Amount",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "₹$totalAmount",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGreen
                )
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // Confirm Booking
        Button(
            onClick = onConfirmBooking,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue
            )
        ) {

            Text(
                text = "Confirm Booking",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}