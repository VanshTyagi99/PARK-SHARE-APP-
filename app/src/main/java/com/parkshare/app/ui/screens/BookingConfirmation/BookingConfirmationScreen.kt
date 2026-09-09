package com.parkshare.app.ui.screens.BookingConfirmation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen

@Composable
fun BookingConfirmationScreen(
    onViewBookingsClick: () -> Unit = {},
    onHomeClick: () -> Unit = {}
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(
            modifier = Modifier.height(40.dp)
        )

        // Success Icon
        Column(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFE8FFF3))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Booking Confirmed",
                tint = SecondaryGreen
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Title
        Text(
            text = "Booking Confirmed!",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Your parking space has been booked successfully.",
            fontSize = 15.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(28.dp)
        )

        // Booking Details
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3F7FF)
            )
        ) {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Booking Details",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                BookingDetailRow(
                    label = "Parking",
                    value = "Parking Space 1"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                BookingDetailRow(
                    label = "Date",
                    value = "14 August 2026"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                BookingDetailRow(
                    label = "Start Time",
                    value = "10:00 AM"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                BookingDetailRow(
                    label = "Duration",
                    value = "1 Hour"
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                BookingDetailRow(
                    label = "Booking ID",
                    value = "PS-2026-0001"
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

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
                        text = "₹30",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGreen
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )

        // View Bookings
        Button(
            onClick = onViewBookingsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue
            )
        ) {

            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = "Bookings"
            )

            Spacer(
                modifier = Modifier.height(0.dp)
            )

            Text(
                text = "View My Bookings",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        // Home
        Button(
            onClick = onHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondaryGreen
            )
        ) {

            Icon(
                imageVector = Icons.Default.Home,
                contentDescription = "Home"
            )

            Text(
                text = "Back to Home",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )
    }
}


@Composable
fun BookingDetailRow(
    label: String,
    value: String
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )

        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}