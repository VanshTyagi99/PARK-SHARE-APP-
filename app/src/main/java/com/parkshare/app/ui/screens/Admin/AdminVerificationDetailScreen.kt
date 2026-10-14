package com.parkshare.app.ui.screens.Admin

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PermIdentity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.api.VerificationDocumentsDto
import com.parkshare.app.data.verification.VerificationRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import com.parkshare.app.utils.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun AdminVerificationDetailScreen(
    verificationId: String?,
    onBackClick: () -> Unit = {},
    onReviewComplete: () -> Unit = {}
) {
    val repository = remember { VerificationRepository() }
    val scope = rememberCoroutineScope()

    var record by remember { mutableStateOf<VerificationDocumentsDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectionReasonText by remember { mutableStateOf("") }
    var rejectionReasonError by remember { mutableStateOf<String?>(null) }

    fun loadDetails() {
        if (verificationId.isNullOrBlank()) {
            errorMessage = "Invalid verification record ID"
            isLoading = false
            return
        }

        isLoading = true
        errorMessage = null
        scope.launch {
            repository.getAdminVerificationDocuments(verificationId)
                .onSuccess {
                    record = it
                    isLoading = false
                }
                .onFailure {
                    errorMessage = it.message ?: "Failed to load verification documents"
                    isLoading = false
                }
        }
    }

    LaunchedEffect(verificationId) {
        loadDetails()
    }

    fun submitReview(status: String, reason: String? = null) {
        if (verificationId.isNullOrBlank()) return

        isProcessing = true
        errorMessage = null
        scope.launch {
            repository.reviewVerification(
                id = verificationId,
                status = status,
                rejectionReason = reason
            ).onSuccess {
                isProcessing = false
                showRejectDialog = false
                onReviewComplete()
            }.onFailure {
                isProcessing = false
                errorMessage = it.message ?: "Failed to submit review"
            }
        }
    }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { if (!isProcessing) showRejectDialog = false },
            title = { Text(text = "Reject Verification", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Please provide a specific reason explaining why this application was rejected. The applicant will see this message.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rejectionReasonText,
                        onValueChange = {
                            rejectionReasonText = it
                            rejectionReasonError = null
                        },
                        label = { Text("Rejection Reason *") },
                        placeholder = { Text("e.g. Identity document photo is blurry") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = rejectionReasonError != null,
                        supportingText = rejectionReasonError?.let { { Text(it, color = Color.Red) } }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectionReasonText.isBlank()) {
                            rejectionReasonError = "Rejection reason is required"
                        } else {
                            submitReview("REJECTED", rejectionReasonText.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm Rejection")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRejectDialog = false },
                    enabled = !isProcessing
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
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
                text = "Applicant Review",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (record == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMessage ?: "Record not found", color = Color.Red)
            }
        } else {
            val doc = record!!

            // Applicant Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = doc.userName?.ifBlank { null } ?: doc.userEmail.substringBefore("@"),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    when (doc.status) {
                                        "APPROVED" -> Color(0xFFDCFCE7)
                                        "REJECTED" -> Color(0xFFFEE2E2)
                                        else -> Color(0xFFFEF3C7)
                                    },
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = doc.status,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (doc.status) {
                                    "APPROVED" -> SecondaryGreen
                                    "REJECTED" -> Color(0xFFDC2626)
                                    else -> Color(0xFFD97706)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Email: ${doc.userEmail}", fontSize = 13.sp, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "User ID: ${doc.userId}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Submitted: ${doc.submittedAt?.take(16)?.replace("T", " ") ?: "Unknown"}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    if (!doc.rejectionReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Current Rejection Reason: ${doc.rejectionReason}",
                            fontSize = 13.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Submitted Documents",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Identity Document Preview
            DocumentPreviewCard(
                title = "1. Identity Proof (${formatDocType(doc.identityDocumentType)})",
                icon = Icons.Default.PermIdentity,
                base64Data = doc.identityDocumentData
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Address Document Preview
            DocumentPreviewCard(
                title = "2. Address Proof (${formatDocType(doc.addressDocumentType)})",
                icon = Icons.Default.Home,
                base64Data = doc.addressDocumentData
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Selfie Photo Preview
            DocumentPreviewCard(
                title = "3. Live Selfie (Visual Match)",
                icon = Icons.Default.Face,
                base64Data = doc.selfieData
            )

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
                ) {
                    Text(text = err, color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showRejectDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    enabled = !isProcessing
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Reject", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { submitReview("APPROVED") },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen),
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Approve", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DocumentPreviewCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    base64Data: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val imageBitmap = remember(base64Data) {
                ImageUtils.base64ToImageBitmap(base64Data)
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Document preview unavailable", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatDocType(type: String): String {
    return when (type) {
        "DRIVING_LICENCE" -> "Driving Licence"
        "VOTER_ID" -> "Voter ID"
        "PASSPORT" -> "Passport"
        "AADHAAR" -> "Aadhaar Card"
        "GOVERNMENT_ID" -> "Government ID"
        "UTILITY_BILL" -> "Utility Bill"
        "RENTAL_AGREEMENT" -> "Rental Agreement"
        "BANK_STATEMENT" -> "Bank Statement"
        else -> type
    }
}
