package com.parkshare.app.ui.screens.OfferParking



import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts


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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.CreateParkingRequest
import com.parkshare.app.data.api.UpdateParkingRequest
import com.parkshare.app.data.parking.ParkingRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferParkingScreen(
    onBackClick: () -> Unit = {},
    onSubmitClick: () -> Unit = {},
    onAddPhotoClick: () -> Unit = {},
    listingId: String? = null
) {
    val parkingRepository = remember { ParkingRepository() }
    val coroutineScope = rememberCoroutineScope()
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoadingListing by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }

    var parkingTitle by remember {
        mutableStateOf("")
    }

    var address by remember {
        mutableStateOf("")
    }

    var parkingType by remember {
        mutableStateOf("Car")
    }

    var pricePerHour by remember {
        mutableStateOf("")
    }

    var availableFrom by remember {
        mutableStateOf("")
    }

    var availableUntil by remember {
        mutableStateOf("")
    }

    var expanded by remember {
        mutableStateOf(false)
    }


    var showError by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }

    var selectedPhotos by remember {
        mutableStateOf<List<Uri>>(emptyList())
    }

    val parkingTypes = listOf(
        "Car",
        "Bike",
        "Car + Bike"
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->

        selectedPhotos = uris.take(3)
    }

    LaunchedEffect(listingId) {
        if (!listingId.isNullOrBlank()) {
            isLoadingListing = true
            val result = parkingRepository.getParkingDetails(listingId)
            result.onSuccess { listing ->
                parkingTitle = listing.title
                address = listing.address
                description = listing.description ?: ""
                parkingType = listing.parkingType
                pricePerHour = if (listing.pricePerHour % 1.0 == 0.0) {
                    listing.pricePerHour.toInt().toString()
                } else {
                    listing.pricePerHour.toString()
                }
                availableFrom = listing.availableFrom
                availableUntil = listing.availableUntil
            }.onFailure { error ->
                showError = true
                errorMessage = error.message ?: "Failed to load listing details"
            }
            isLoadingListing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
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
                text = if (listingId.isNullOrBlank()) "Offer Parking" else "Edit Listing",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Text(
            text = if (listingId.isNullOrBlank()) "List Your Parking Space" else "Edit Your Parking Space",
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = "Share your unused parking space and earn money.",
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        // Parking Title
        Text(
            text = "Parking Title",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = parkingTitle,
            onValueChange = {
                parkingTitle = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {
                Text("e.g. Safe parking near market")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocalParking,
                    contentDescription = "Parking"
                )
            }
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // Address
        Text(
            text = "Parking Address",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = address,
            onValueChange = {
                address = it
            },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            shape = RoundedCornerShape(12.dp),
            placeholder = {
                Text("Enter parking location")
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location"
                )
            }
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // Parking Type
        Text(
            text = "Parking Type",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            }
        ) {

            OutlinedTextField(
                value = parkingType,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                }
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {

                parkingTypes.forEach { type ->

                    DropdownMenuItem(
                        text = {
                            Text(type)
                        },
                        onClick = {
                            parkingType = type
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // Price
        Text(
            text = "Price Per Hour",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        OutlinedTextField(
            value = pricePerHour,
            onValueChange = {
                pricePerHour = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = {
                Text("e.g. 30")
            },
            leadingIcon = {
                Text(
                    text = "₹",
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Spacer(
            modifier = Modifier.height(18.dp)
        )

        // Availability
        Text(
            text = "Availability",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            OutlinedTextField(
                value = availableFrom,
                onValueChange = {
                    availableFrom = it
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = {
                    Text("From")
                },
                placeholder = {
                    Text("09:00 AM")
                }
            )

            OutlinedTextField(
                value = availableUntil,
                onValueChange = {
                    availableUntil = it
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                label = {
                    Text("Until")
                },
                placeholder = {
                    Text("09:00 PM")
                }
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        // Add Photos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3F7FF)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Add Photos",
                    tint = PrimaryBlue
                )

                Spacer(
                    modifier = Modifier.width(12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Parking Photos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (selectedPhotos.isEmpty()) {
                            "Add up to 3 photos"
                        } else {
                            "${selectedPhotos.size} photo(s) selected"
                        },
                        fontSize = 13.sp,
                        color = if (selectedPhotos.isEmpty()) {
                            Color.Gray
                        } else {
                            SecondaryGreen
                        },
                        fontWeight = if (selectedPhotos.isEmpty()) {
                            FontWeight.Normal
                        } else {
                            FontWeight.SemiBold
                        }
                    )
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {
                    Text(
                        text = if (selectedPhotos.isEmpty()) {
                            "Add"
                        } else {
                            "Change"
                        }
                    )
                }
            }
        }

        // Prototype note below photos card
        Text(
            text = "Note: Photo upload is in prototype preview mode. Images will be permanently persisted in an upcoming cloud storage update.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 6.dp, start = 4.dp)
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        if (showError) {

            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )
        }

        // Submit
        Button(
            enabled = !isSubmitting && !isLoadingListing,
            onClick = {
                val priceVal = pricePerHour.trim().toDoubleOrNull()
                when {
                    parkingTitle.trim().length < 3 -> {
                        errorMessage = "Please enter parking title (at least 3 characters)"
                        showError = true
                    }

                    address.trim().length < 3 -> {
                        errorMessage = "Please enter parking address (at least 3 characters)"
                        showError = true
                    }

                    priceVal == null || priceVal <= 0 -> {
                        errorMessage = "Please enter a valid price per hour greater than zero"
                        showError = true
                    }

                    availableFrom.isBlank() -> {
                        errorMessage = "Please enter available from time"
                        showError = true
                    }

                    availableUntil.isBlank() -> {
                        errorMessage = "Please enter available until time"
                        showError = true
                    }

                    else -> {
                        showError = false
                        isSubmitting = true
                        coroutineScope.launch {
                            if (listingId.isNullOrBlank()) {
                                val req = CreateParkingRequest(
                                    title = parkingTitle.trim(),
                                    address = address.trim(),
                                    description = description.trim(),
                                    parkingType = parkingType,
                                    pricePerHour = priceVal,
                                    availableFrom = availableFrom.trim(),
                                    availableUntil = availableUntil.trim()
                                )
                                val result = parkingRepository.createParking(req)
                                result.onSuccess {
                                    isSubmitting = false
                                    onSubmitClick()
                                }.onFailure { err ->
                                    isSubmitting = false
                                    showError = true
                                    errorMessage = err.message ?: "Failed to list parking"
                                }
                            } else {
                                val req = UpdateParkingRequest(
                                    title = parkingTitle.trim(),
                                    address = address.trim(),
                                    description = description.trim(),
                                    parkingType = parkingType,
                                    pricePerHour = priceVal,
                                    availableFrom = availableFrom.trim(),
                                    availableUntil = availableUntil.trim()
                                )
                                val result = parkingRepository.updateParking(listingId, req)
                                result.onSuccess {
                                    isSubmitting = false
                                    onSubmitClick()
                                }.onFailure { err ->
                                    isSubmitting = false
                                    showError = true
                                    errorMessage = err.message ?: "Failed to update listing"
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondaryGreen
            )
        ) {

            Text(
                text = if (isSubmitting) {
                    if (listingId.isNullOrBlank()) "Listing Space..." else "Updating Space..."
                } else {
                    if (listingId.isNullOrBlank()) "List Parking Space" else "Save Changes"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )
    }
}