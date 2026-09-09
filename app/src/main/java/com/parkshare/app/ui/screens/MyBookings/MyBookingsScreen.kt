package com.parkshare.app.ui.screens.MyBookings

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen

data class BookingItem(
    val parkingName: String,
    val date: String,
    val time: String,
    val duration: String,
    val amount: String,
    val status: String
)

@Composable
fun MyBookingsScreen(
    onBackClick: () -> Unit = {}
) {

    var selectedTab by remember {
        mutableStateOf("Upcoming")
    }

    val bookings = listOf(
        BookingItem(
            parkingName = "Parking Space 1",
            date = "14 August 2026",
            time = "10:00 AM - 11:00 AM",
            duration = "1 Hour",
            amount = "₹30",
            status = "Upcoming"
        ),
        BookingItem(
            parkingName = "Parking Space 2",
            date = "16 August 2026",
            time = "02:00 PM - 04:00 PM",
            duration = "2 Hours",
            amount = "₹60",
            status = "Upcoming"
        ),
        BookingItem(
            parkingName = "Parking Space 3",
            date = "10 August 2026",
            time = "11:00 AM - 12:00 PM",
            duration = "1 Hour",
            amount = "₹30",
            status = "Completed"
        )
    )

    val filteredBookings = bookings.filter {
        it.status == selectedTab
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
                text = "My Bookings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            FilterChip(
                selected = selectedTab == "Upcoming",
                onClick = {
                    selectedTab = "Upcoming"
                },
                label = {
                    Text("Upcoming")
                }
            )

            FilterChip(
                selected = selectedTab == "Active",
                onClick = {
                    selectedTab = "Active"
                },
                label = {
                    Text("Active")
                }
            )

            FilterChip(
                selected = selectedTab == "Completed",
                onClick = {
                    selectedTab = "Completed"
                },
                label = {
                    Text("Completed")
                }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        if (filteredBookings.isEmpty()) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "No $selectedTab bookings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Your bookings will appear here.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                items(filteredBookings) { booking ->

                    BookingCard(
                        booking = booking
                    )
                }
            }
        }
    }
}


@Composable
fun BookingCard(
    booking: BookingItem
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = booking.parkingName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = booking.status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (booking.status == "Completed") {
                        Color.Gray
                    } else {
                        SecondaryGreen
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            // Date
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Date",
                    tint = PrimaryBlue
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = booking.date,
                    fontSize = 14.sp
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // Time
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time",
                    tint = PrimaryBlue
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                Text(
                    text = booking.time,
                    fontSize = 14.sp
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // Duration and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "Duration: ${booking.duration}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Text(
                    text = booking.amount,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryGreen
                )
            }
        }
    }
}