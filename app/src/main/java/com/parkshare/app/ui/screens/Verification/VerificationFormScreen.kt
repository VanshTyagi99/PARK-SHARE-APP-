package com.parkshare.app.ui.screens.Verification

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PermIdentity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.parkshare.app.data.verification.VerificationRepository
import com.parkshare.app.ui.theme.PrimaryBlue
import com.parkshare.app.ui.theme.SecondaryGreen
import com.parkshare.app.utils.ImageUtils
import kotlinx.coroutines.launch

@Composable
fun VerificationFormScreen(
    onBackClick: () -> Unit = {},
    onSubmissionSuccess: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { VerificationRepository() }

    // Form fields
    var selectedIdentityType by remember { mutableStateOf("DRIVING_LICENCE") }
    var identityBase64 by remember { mutableStateOf<String?>(null) }
    var identityTypeExpanded by remember { mutableStateOf(false) }

    var selectedAddressType by remember { mutableStateOf("UTILITY_BILL") }
    var addressBase64 by remember { mutableStateOf<String?>(null) }
    var addressTypeExpanded by remember { mutableStateOf(false) }

    var selfieBase64 by remember { mutableStateOf<String?>(null) }

    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Pickers
    val identityPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val encoded = ImageUtils.uriToBase64(context, it)
            if (encoded != null) {
                identityBase64 = encoded
                errorMessage = null
            }
        }
    }

    val addressPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val encoded = ImageUtils.uriToBase64(context, it)
            if (encoded != null) {
                addressBase64 = encoded
                errorMessage = null
            }
        }
    }

    val selfiePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val encoded = ImageUtils.uriToBase64(context, it)
            if (encoded != null) {
                selfieBase64 = encoded
                errorMessage = null
            }
        }
    }

    val identityOptions = listOf(
        "DRIVING_LICENCE" to "Driving Licence",
        "VOTER_ID" to "Voter ID Card",
        "PASSPORT" to "Passport",
        "AADHAAR" to "Aadhaar Card"
    )

    val addressOptions = listOf(
        "GOVERNMENT_ID" to "Government ID with Address",
        "UTILITY_BILL" to "Electricity / Water Utility Bill",
        "RENTAL_AGREEMENT" to "Registered Rental Agreement",
        "BANK_STATEMENT" to "Bank Account Statement",
        "OTHER" to "Other Valid Address Proof"
    )

    val isFormComplete = !identityBase64.isNullOrBlank() &&
            !addressBase64.isNullOrBlank() &&
            !selfieBase64.isNullOrBlank()

    fun autofillDemoData() {
        identityBase64 = ImageUtils.createSampleDocumentBase64(
            title = "DEMO DRIVING LICENCE",
            subtitle = "DL No: DL-0420110098765",
            bgColor = 0xFF1E3A8A.toInt()
        )
        addressBase64 = ImageUtils.createSampleDocumentBase64(
            title = "ELECTRICITY UTILITY BILL",
            subtitle = "Consumer: DLF-SEC-62 NOIDA",
            bgColor = 0xFF0D9488.toInt()
        )
        selfieBase64 = ImageUtils.createSampleDocumentBase64(
            title = "USER VERIFICATION SELFIE",
            subtitle = "Identity Face Match: PASS",
            bgColor = 0xFF4338CA.toInt()
        )
        errorMessage = null
    }

    fun submitVerification() {
        if (!isFormComplete) {
            errorMessage = "Please attach all 3 required documents (Identity Proof, Address Proof, Selfie)"
            return
        }

        isSubmitting = true
        errorMessage = null

        scope.launch {
            repository.submitVerification(
                identityDocumentType = selectedIdentityType,
                identityDocumentData = identityBase64!!,
                addressDocumentType = selectedAddressType,
                addressDocumentData = addressBase64!!,
                selfieData = selfieBase64!!
            ).onSuccess {
                isSubmitting = false
                onSubmissionSuccess()
            }.onFailure {
                isSubmitting = false
                errorMessage = it.message ?: "Failed to submit verification"
            }
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
                text = "Identity Verification",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Mandatory 3-document verification to unlock parking discovery and hosting.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Demo Autofill Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Demonstration Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF6B21A8)
                    )
                    Text(
                        text = "Quickly attach all 3 demo documents for testing",
                        fontSize = 12.sp,
                        color = Color(0xFF7E22CE)
                    )
                }
                OutlinedButton(
                    onClick = { autofillDemoData() },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF6B21A8))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Autofill All", fontSize = 12.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Document 1: Identity Proof
        DocumentSection(
            stepNumber = "1",
            title = "Identity Proof",
            icon = Icons.Default.PermIdentity,
            typeLabel = identityOptions.find { it.first == selectedIdentityType }?.second ?: selectedIdentityType,
            onTypeClick = { identityTypeExpanded = true },
            dropdownContent = {
                DropdownMenu(
                    expanded = identityTypeExpanded,
                    onDismissRequest = { identityTypeExpanded = false }
                ) {
                    identityOptions.forEach { (typeKey, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedIdentityType = typeKey
                                identityTypeExpanded = false
                            }
                        )
                    }
                }
            },
            base64Data = identityBase64,
            onPickImage = { identityPicker.launch("image/*") },
            onUseDemo = {
                identityBase64 = ImageUtils.createSampleDocumentBase64(
                    title = "DEMO ID: ${identityOptions.find { it.first == selectedIdentityType }?.second}",
                    subtitle = "Applicant Verified Identity",
                    bgColor = 0xFF1E3A8A.toInt()
                )
            },
            onClear = { identityBase64 = null }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Document 2: Address Proof
        DocumentSection(
            stepNumber = "2",
            title = "Address Proof",
            icon = Icons.Default.Home,
            typeLabel = addressOptions.find { it.first == selectedAddressType }?.second ?: selectedAddressType,
            onTypeClick = { addressTypeExpanded = true },
            dropdownContent = {
                DropdownMenu(
                    expanded = addressTypeExpanded,
                    onDismissRequest = { addressTypeExpanded = false }
                ) {
                    addressOptions.forEach { (typeKey, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                selectedAddressType = typeKey
                                addressTypeExpanded = false
                            }
                        )
                    }
                }
            },
            base64Data = addressBase64,
            onPickImage = { addressPicker.launch("image/*") },
            onUseDemo = {
                addressBase64 = ImageUtils.createSampleDocumentBase64(
                    title = "DEMO ADDRESS: ${addressOptions.find { it.first == selectedAddressType }?.second}",
                    subtitle = "Address: Sector 62, Noida, UP",
                    bgColor = 0xFF0D9488.toInt()
                )
            },
            onClear = { addressBase64 = null }
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Document 3: Live Selfie
        DocumentSection(
            stepNumber = "3",
            title = "Selfie Photo",
            icon = Icons.Default.Face,
            typeLabel = "Live Visual Check",
            onTypeClick = null,
            dropdownContent = null,
            base64Data = selfieBase64,
            onPickImage = { selfiePicker.launch("image/*") },
            onUseDemo = {
                selfieBase64 = ImageUtils.createSampleDocumentBase64(
                    title = "DEMO SELFIE",
                    subtitle = "Visual Biometric Match: Approved",
                    bgColor = 0xFF4338CA.toInt()
                )
            },
            onClear = { selfieBase64 = null }
        )

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2))
            ) {
                Text(
                    text = error,
                    color = Color(0xFFB91C1C),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = { submitVerification() },
            enabled = isFormComplete && !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                disabledContainerColor = Color(0xFFCBD5E1)
            )
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Uploading Verification...", fontSize = 16.sp)
            } else {
                Text(
                    text = if (isFormComplete) "Submit Verification" else "Attach All 3 Documents to Submit",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun DocumentSection(
    stepNumber: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    typeLabel: String,
    onTypeClick: (() -> Unit)?,
    dropdownContent: (@Composable () -> Unit)?,
    base64Data: String?,
    onPickImage: () -> Unit,
    onUseDemo: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!base64Data.isNullOrBlank()) Color(0xFFF0FDF4) else Color(0xFFF8FAFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (!base64Data.isNullOrBlank()) SecondaryGreen else PrimaryBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$stepNumber. $title",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.weight(1f))
                if (!base64Data.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SecondaryGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Attached", color = SecondaryGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Dropdown selection (if applicable)
            if (onTypeClick != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .clickable { onTypeClick() }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = typeLabel, fontSize = 14.sp, color = Color(0xFF334155))
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                    }
                    dropdownContent?.invoke()
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview or upload buttons
            if (!base64Data.isNullOrBlank()) {
                val imageBitmap = remember(base64Data) {
                    ImageUtils.base64ToImageBitmap(base64Data)
                }

                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = "Document Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onPickImage,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "Change", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onClear,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(text = "Remove", fontSize = 12.sp, color = Color.Red)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPickImage,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Upload Image", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onUseDemo,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Use Demo Doc", fontSize = 12.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
