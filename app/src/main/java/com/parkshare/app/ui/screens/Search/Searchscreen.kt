package com.parkshare.app.ui.screens.Search

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.parkshare.app.data.api.ParkingListingDto
import com.parkshare.app.data.parking.ParkingRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun Searchscreen(
    onBackClick: () -> Unit = {},
    onParkingClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parkingRepository = remember { ParkingRepository() }

    val defaultNoidaLat = 28.6139
    val defaultNoidaLng = 77.3910

    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var isMapView by remember { mutableStateOf(false) }

    var userLat by remember { mutableStateOf(defaultNoidaLat) }
    var userLng by remember { mutableStateOf(defaultNoidaLng) }
    var locationLabel by remember { mutableStateOf("Noida, NCR (Default)") }
    var isGpsActive by remember { mutableStateOf(false) }

    var listings by remember { mutableStateOf<List<ParkingListingDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedListingForMapCard by remember { mutableStateOf<ParkingListingDto?>(null) }

    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun fetchListings(query: String = searchText, filter: String = selectedFilter) {
        searchJob?.cancel()
        searchJob = scope.launch {
            isLoading = true
            errorMessage = null
            val vehicleTypeParam = if (filter == "All") null else filter
            val queryParam = query.trim().ifBlank { null }

            val result = parkingRepository.searchParking(
                query = queryParam,
                vehicleType = vehicleTypeParam,
                latitude = userLat,
                longitude = userLng,
                radiusKm = 50.0,
                limit = 50
            )

            result.onSuccess { data ->
                listings = data
                isLoading = false
                if (selectedListingForMapCard == null && data.isNotEmpty()) {
                    selectedListingForMapCard = data.firstOrNull()
                }
            }.onFailure { err ->
                errorMessage = err.message ?: "Failed to find parking spots"
                isLoading = false
            }
        }
    }

    val requestLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                        locationLabel = "Current GPS Location"
                        isGpsActive = true
                        fetchListings()
                    }
                }
            } catch (_: SecurityException) {
                // Keep default Noida coordinates
            }
        }
    }

    fun requestGps() {
        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        userLat = location.latitude
                        userLng = location.longitude
                        locationLabel = "Current GPS Location"
                        isGpsActive = true
                        fetchListings()
                    }
                }
            } catch (_: SecurityException) {
                // Fallback gracefully
            }
        } else {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        fetchListings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                text = "Search Parking",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Input Box
        OutlinedTextField(
            value = searchText,
            onValueChange = { newValue ->
                searchText = newValue
                searchJob?.cancel()
                searchJob = scope.launch {
                    delay(350)
                    fetchListings(query = newValue, filter = selectedFilter)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = PrimaryBlue
                )
            },
            trailingIcon = {
                if (searchText.isNotBlank()) {
                    IconButton(onClick = {
                        searchText = ""
                        fetchListings(query = "", filter = selectedFilter)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            },
            placeholder = {
                Text(text = "Search sector, mall, metro, Noida, NCR...")
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Location Info & GPS Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF3F7FF)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = if (isGpsActive) "Active Search Center" else "Default Region",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = locationLabel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryBlue
                        )
                    }
                }

                IconButton(
                    onClick = { requestGps() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "Detect Location",
                        tint = if (isGpsActive) SecondaryGreen else PrimaryBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vehicle Type Filters
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Car", "Bike", "Car + Bike")
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedFilter = filter
                        fetchListings(filter = filter)
                    },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        when (filter) {
                            "Car" -> Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            "Bike" -> Icon(
                                imageVector = Icons.Default.TwoWheeler,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            else -> null
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Header with Result Count & Map/List Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Searching..." else "${listings.size} Spots Found",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            // Map / List Toggle Buttons
            Row(
                modifier = Modifier
                    .background(Color(0xFFF0F4FA), RoundedCornerShape(10.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { isMapView = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isMapView) PrimaryBlue else Color.Transparent,
                        contentColor = if (!isMapView) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "List View",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "List", fontSize = 12.sp)
                }

                Button(
                    onClick = { isMapView = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMapView) PrimaryBlue else Color.Transparent,
                        contentColor = if (isMapView) Color.White else Color.Gray
                    ),
                    shape = RoundedCornerShape(8.dp),
                    elevation = null,
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map View",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Map", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Content Area: Map View vs List View
        if (isLoading && listings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Searching verified parking spots...", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else if (errorMessage != null && listings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "Connection Error", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMessage ?: "Unable to fetch listings", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { fetchListings() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else if (isMapView) {
            // MAP VIEW with osmdroid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                var currentMapView by remember { mutableStateOf<MapView?>(null) }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(13.0)
                            controller.setCenter(GeoPoint(userLat, userLng))
                            currentMapView = this
                        }
                    },
                    update = { mapView ->
                        mapView.overlays.removeAll { it is Marker }
                        listings.forEach { item ->
                            val lat = item.latitude
                            val lng = item.longitude
                            if (lat != null && lng != null) {
                                val marker = Marker(mapView).apply {
                                    position = GeoPoint(lat, lng)
                                    title = item.title
                                    snippet = "₹${item.pricePerHour.toInt()}/hr • ${item.parkingType}"
                                    setOnMarkerClickListener { _, _ ->
                                        selectedListingForMapCard = item
                                        showInfoWindow()
                                        true
                                    }
                                }
                                mapView.overlays.add(marker)
                            }
                        }
                        mapView.invalidate()
                    }
                )

                DisposableEffect(Unit) {
                    onDispose {
                        currentMapView?.onPause()
                        currentMapView?.onDetach()
                    }
                }

                // Floating Selected Parking Card on Map
                val activeListing = selectedListingForMapCard
                if (activeListing != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(12.dp),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeListing.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "₹${activeListing.pricePerHour.toInt()} / hr",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryGreen
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = activeListing.address,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = activeListing.distanceKm?.let { "$it km away" } ?: "${activeListing.parkingType} Parking",
                                    fontSize = 12.sp,
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${activeListing.availableFrom} - ${activeListing.availableUntil}",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { onParkingClick(activeListing.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Text(text = "View Details", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // LIST VIEW
            if (listings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No parking spots found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try clearing search filters or searching a different area",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(listings, key = { it.id }) { item ->
                        ParkingItem(
                            listing = item,
                            onClick = { onParkingClick(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParkingItem(
    listing: ParkingListingDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = listing.distanceKm?.let { "$it km away" } ?: "${listing.parkingType} Parking",
                    fontSize = 13.sp,
                    color = PrimaryBlue,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "${listing.availableFrom} - ${listing.availableUntil}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(text = "View Details")
            }
        }
    }
}

@Composable
fun ParkingItem(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(text = "View Details")
            }
        }
    }
}