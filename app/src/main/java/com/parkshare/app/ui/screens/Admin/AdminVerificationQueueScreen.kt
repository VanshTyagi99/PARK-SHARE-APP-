package com.parkshare.app.ui.screens.Admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.VerificationQueueItemDto
import com.parkshare.app.data.verification.VerificationRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import kotlinx.coroutines.launch

@Composable
fun AdminVerificationQueueScreen(
    onBackClick: () -> Unit = {},
    onReviewClick: (String) -> Unit = {}
) {
    val repository = remember { VerificationRepository() }
    val scope = rememberCoroutineScope()

    var queueItems by remember { mutableStateOf<List<VerificationQueueItemDto>>(emptyList()) }
    var selectedFilter by remember { mutableStateOf<String?>("PENDING") } // default to PENDING review
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadQueue() {
        isLoading = true
        errorMessage = null
        scope.launch {
            repository.getAdminVerificationQueue(selectedFilter)
                .onSuccess {
                    queueItems = it
                    isLoading = false
                }
                .onFailure {
                    errorMessage = it.message ?: "Failed to load verification queue"
                    isLoading = false
                }
        }
    }

    LaunchedEffect(selectedFilter) {
        loadQueue()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(20.dp)
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Verification Center",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Text(
                    text = "Admin Review Queue",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            IconButton(onClick = { loadQueue() }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = PrimaryBlue
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "PENDING" to "Pending",
                "APPROVED" to "Approved",
                "REJECTED" to "Rejected",
                null to "All"
            ).forEach { (filterVal, label) ->
                FilterChip(
                    selected = selectedFilter == filterVal,
                    onClick = { selectedFilter = filterVal },
                    label = { Text(text = label, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage!!, color = Color.Red, fontSize = 14.sp)
            }
        } else if (queueItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No applications found",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedFilter != null) "No $selectedFilter applications in queue" else "Queue is empty",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(queueItems) { item ->
                    AdminQueueItemCard(
                        item = item,
                        onClick = { onReviewClick(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminQueueItemCard(
    item: VerificationQueueItemDto,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.userName?.ifBlank { null } ?: item.userEmail.substringBefore("@"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                StatusBadge(status = item.status)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.userEmail,
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${formatDocName(item.identityDocumentType)}",
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Addr: ${formatDocName(item.addressDocumentType)}",
                    fontSize = 12.sp,
                    color = Color(0xFF334155),
                    fontWeight = FontWeight.Medium
                )
            }

            if (!item.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reason: ${item.rejectionReason}",
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.submittedAt?.take(16)?.replace("T", " ") ?: "",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Review",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "APPROVED" -> Color(0xFFDCFCE7) to SecondaryGreen
        "REJECTED" -> Color(0xFFFEE2E2) to Color(0xFFDC2626)
        "PENDING" -> Color(0xFFFEF3C7) to Color(0xFFD97706)
        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

private fun formatDocName(type: String): String {
    return when (type) {
        "DRIVING_LICENCE" -> "Driving Lic."
        "VOTER_ID" -> "Voter ID"
        "PASSPORT" -> "Passport"
        "AADHAAR" -> "Aadhaar"
        "GOVERNMENT_ID" -> "Govt ID"
        "UTILITY_BILL" -> "Utility Bill"
        "RENTAL_AGREEMENT" -> "Rental Agr."
        "BANK_STATEMENT" -> "Bank Stmt."
        else -> type
    }
}
