package com.parkshare.app.ui.screens.Home



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.ParkingListingDto
import com.parkshare.app.data.parking.ParkingRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen

@Composable
fun Homescreen(
    onProfileClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onOfferParkingClick: () -> Unit = {},
    onParkingClick: (String) -> Unit = {}
) {
    val parkingRepository = remember { ParkingRepository() }
    var availableListings by remember { mutableStateOf<List<ParkingListingDto>>(emptyList()) }
    var isLoadingListings by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val result = parkingRepository.searchParking(limit = 2)
        result.onSuccess { list ->
            availableListings = list
            isLoadingListings = false
        }.onFailure {
            isLoadingListings = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {

        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Text(
                    text = "ParkShare",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )

                Text(
                    text = "Find your perfect parking spot",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            TextButton(
                onClick = onProfileClick
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Location
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3F7FF)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = PrimaryBlue
                )

                Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                Column {
                    Text(
                        text = "Your Location",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "Current Location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Search Parking
        Button(
            onClick = onSearchClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue
            )
        ) {

            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )

            Spacer(modifier = Modifier.padding(horizontal = 4.dp))

            Text(
                text = "Search Parking",
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Available Parking Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Available Parking",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            TextButton(
                onClick = onSearchClick
            ) {
                Text(
                    text = "See All",
                    color = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoadingListings) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FC))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp),
                        color = PrimaryBlue,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Finding nearby spots...",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (availableListings.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSearchClick() },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Explore Parking in NCR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap to search spots in Noida, Ghaziabad & Delhi",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "From ₹25 / hour",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryGreen
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                availableListings.forEach { listing ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onParkingClick(listing.id) },
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = listing.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${listing.pricePerHour.toInt()} / hr",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = listing.address,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = listing.distanceKm?.let { "$it km away" } ?: "${listing.parkingType} Parking",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "View Details →",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Offer Parking
        Button(
            onClick = onOfferParkingClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondaryGreen
            )
        ) {
            Text(
                text = "Offer Your Parking Space",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}