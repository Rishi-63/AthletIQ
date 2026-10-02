package com.example.athletiq

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults


private val Navy = Color(0xFF0B1120)
private val CardNavy = Color(0xFF151D2D)
private val Lime = Color(0xFFC5F467)
private val Muted = Color(0xFFA0AABD)
private val White = Color(0xFFF5F7FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AthletIQHomeScreen()
            }
        }
    }
}
data class AssessmentRecord(
    val testName: String,
    val reps: Int?,
    val resultQuality: String
)
data class EquipmentItem(
    val name: String,
    val total: Int,
    val available: Int
)

data class EquipmentTransaction(
    val id: Int,
    val studentName: String,
    val equipmentName: String,
    val quantity: Int,
    val issueDate: String,
    val expectedReturnDate: String,
    val returned: Boolean = false
)
data class EquipmentRequest(
    val id: Int,
    val studentName: String,
    val equipmentName: String,
    val quantity: Int,
    val issueDate: String,
    val expectedReturnDate: String,
    val status: String = "Pending"
)
data class GroundBooking(
    val id: Int,
    val groundName: String,
    val sport: String,
    val date: String,
    val timeSlot: String,
    val bookedBy: String = "Student",
    val status: String = "Pending"
)
@Composable
fun AthletIQHomeScreen() {
    var selectedScreen by remember { mutableStateOf("login") }
    var selectedTest by remember { mutableStateOf("Sit-Ups") }
    var assessmentHistory by remember {
        mutableStateOf(listOf<AssessmentRecord>())
    }
    var detectedReps by remember { mutableStateOf<Int?>(null) }
    var resultQuality by remember { mutableStateOf("Pending") }
    var resultSummary by remember {
        mutableStateOf("Assessment results will appear here.")
    }
    var equipmentInventory by remember {
        mutableStateOf(
            listOf(
                EquipmentItem("Football", 10, 10),
                EquipmentItem("Basketball", 8, 8),
                EquipmentItem("Volleyball", 8, 8),
                EquipmentItem("Badminton Racket", 12, 12),
                EquipmentItem("Cricket Bat", 6, 6),
                EquipmentItem("Skipping Rope", 10, 10)
            )
        )
    }

    var equipmentTransactions by remember {
        mutableStateOf(listOf<EquipmentTransaction>())
    }

    var nextEquipmentTransactionId by remember {
        mutableIntStateOf(1)
    }
    var equipmentRequests by remember { mutableStateOf(listOf<EquipmentRequest>()) }
    var nextEquipmentRequestId by remember { mutableIntStateOf(1) }
    var groundBookings by remember {
        mutableStateOf(listOf<GroundBooking>())
    }

    var nextGroundBookingId by remember {
        mutableIntStateOf(1)
    }

    // Handle the Android device/system Back button for every app screen.
    BackHandler(enabled = selectedScreen != "login") {
        selectedScreen = when (selectedScreen) {
            "dashboard" -> "login"
            "tests" -> "dashboard"
            "setup" -> "tests"
            "results" -> "setup"
            "equipment", "groundBooking", "records", "history", "profile", "myRequests", "adminRequests" -> "dashboard"
            else -> "dashboard"
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Navy
    ) {
        when (selectedScreen) {
            "login" -> LoginScreen(
                onLogin = { selectedScreen = "dashboard" }
            )

            "dashboard" -> StudentDashboard(
                onStart = { selectedScreen = "tests" },
                onHistory = { selectedScreen = "history" },
                onProfile = { selectedScreen = "profile" },
                onEquipment = { selectedScreen = "equipment" },
                onGroundBooking = { selectedScreen = "groundBooking" },
                onRecords = { selectedScreen = "records" },
                onMyRequests = { selectedScreen = "myRequests" },
                onAdminRequests = { selectedScreen = "adminRequests" }
            )

            "equipment" -> EquipmentScreen(
                inventory = equipmentInventory,
                requests = equipmentRequests,
                onBack = { selectedScreen = "dashboard" },
                onRequest = { student, equipmentName, quantity, issueDate, returnDate ->
                    val item = equipmentInventory.find { it.name == equipmentName }
                    val pendingQty = equipmentRequests.filter { it.equipmentName == equipmentName && it.status == "Pending" }.sumOf { it.quantity }
                    if (item != null && quantity > 0 && quantity + pendingQty <= item.available) {
                        equipmentRequests = equipmentRequests + EquipmentRequest(
                            id = nextEquipmentRequestId++, studentName = student,
                            equipmentName = equipmentName, quantity = quantity,
                            issueDate = issueDate, expectedReturnDate = returnDate
                        )
                    }
                }
            )

            "groundBooking" -> GroundBookingScreen(
                bookings = groundBookings,
                onBack = {
                    selectedScreen = "dashboard"
                },
                onBook = { groundName, sport, date, timeSlot ->

                    val newBooking = GroundBooking(
                        id = nextGroundBookingId,
                        groundName = groundName,
                        sport = sport,
                        date = date,
                        timeSlot = timeSlot
                    )

                    groundBookings = groundBookings + newBooking
                    nextGroundBookingId++

                },
                onCancel = { booking ->

                    groundBookings = groundBookings.map {
                        if (it.id == booking.id) {
                            it.copy(status = "Cancelled")
                        } else {
                            it
                        }
                    }

                }
            )

            "myRequests" -> MyRequestsScreen(
                equipmentRequests = equipmentRequests,
                groundBookings = groundBookings,
                onBack = { selectedScreen = "dashboard" }
            )

            "adminRequests" -> AdminRequestsScreen(
                equipmentRequests = equipmentRequests,
                groundBookings = groundBookings,
                onBack = { selectedScreen = "dashboard" },
                onEquipmentDecision = { request, approved ->
                    equipmentRequests = equipmentRequests.map {
                        if (it.id == request.id && it.status == "Pending") it.copy(status = if (approved) "Approved" else "Rejected") else it
                    }
                    if (approved) {
                        equipmentInventory = equipmentInventory.map {
                            if (it.name == request.equipmentName) it.copy(available = (it.available - request.quantity).coerceAtLeast(0)) else it
                        }
                        equipmentTransactions = equipmentTransactions + EquipmentTransaction(
                            id = nextEquipmentTransactionId++, studentName = request.studentName,
                            equipmentName = request.equipmentName, quantity = request.quantity,
                            issueDate = request.issueDate, expectedReturnDate = request.expectedReturnDate
                        )
                    }
                },
                onGroundDecision = { booking, approved ->
                    groundBookings = groundBookings.map {
                        if (it.id == booking.id && it.status == "Pending") it.copy(status = if (approved) "Approved" else "Rejected") else it
                    }
                }
            )

            "records" -> SportsRecordsScreen(
                assessmentHistory = assessmentHistory,
                equipmentTransactions = equipmentTransactions,
                groundBookings = groundBookings,
                onBack = {
                    selectedScreen = "dashboard"
                }
            )

            "tests" -> SelectTestScreen(
                onBack = { selectedScreen = "dashboard" },
                onTestSelected = { test ->
                    selectedTest = test
                    selectedScreen = "setup"
                }
            )

            "setup" -> CapturePlaceholder(
                testName = selectedTest,
                onBack = { selectedScreen = "tests" },
                onContinue = {
                    assessmentHistory = assessmentHistory + AssessmentRecord(
                        testName = selectedTest,
                        reps = detectedReps,
                        resultQuality = resultQuality
                    )
                    selectedScreen = "results"
                }
            )

            "results" -> ResultsScreen(
                testName = selectedTest,
                reps = detectedReps,
                resultQuality = resultQuality,
                summary = resultSummary,
                onRetake = {
                    detectedReps = null
                    resultQuality = "Pending"
                    resultSummary = "Assessment results will appear here."
                    selectedScreen = "setup"
                },
                onDashboard = {
                    selectedScreen = "dashboard"
                }
            )

            "history" -> AssessmentHistoryScreen(
                history = assessmentHistory,
                onBack = { selectedScreen = "dashboard" }
            )

            "profile" -> StudentProfileScreen(
                onBack = { selectedScreen = "dashboard" },
                onLogout = { selectedScreen = "login" }
            )

            else -> LoginScreen(
                onLogin = { selectedScreen = "dashboard" }
            )
        }
    }
}

@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val canContinue = email.isNotBlank() && password.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AthletIQ",
            color = Lime,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "STUDENT FITNESS ASSESSMENT",
            color = Muted,
            fontSize = 12.sp,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        Text(
            text = "Welcome Back",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sign in to continue to your dashboard.",
            color = Muted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Student Email") },
            placeholder = { Text("Enter your email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                unfocusedBorderColor = Muted,
                focusedLabelColor = Lime,
                cursorColor = Lime,
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedPlaceholderColor = Muted,
                unfocusedPlaceholderColor = Muted
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            placeholder = { Text("Enter your password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Lime,
                unfocusedBorderColor = Muted,
                focusedLabelColor = Lime,
                cursorColor = Lime,
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedPlaceholderColor = Muted,
                unfocusedPlaceholderColor = Muted
            )
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onLogin,
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Navy,
                disabledContainerColor = CardNavy,
                disabledContentColor = Muted
            )
        ) {
            Text(
                text = "Sign In  →",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Your fitness journey starts here.",
            color = Muted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StudentDashboard(
    onStart: () -> Unit,
    onHistory: () -> Unit,
    onProfile: () -> Unit,
    onEquipment: () -> Unit,
    onGroundBooking: () -> Unit,
    onRecords: () -> Unit,
    onMyRequests: () -> Unit,
    onAdminRequests: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "AthletIQ",
            color = Lime,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hey, Student! 👋",
            color = White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your sports and fitness hub",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Sports & Fitness",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(14.dp))

        DashboardCard(
            title = "Sports Assessment",
            description = "Start a fitness test and track your performance",
            icon = "🏃",
            onClick = onStart
        )

        DashboardCard(
            title = "Equipment",
            description = "Check availability, issue and return equipment",
            icon = "🏀",
            onClick = onEquipment
        )

        DashboardCard(
            title = "Ground Booking",
            description = "Check ground availability and book a time slot",
            icon = "🏟️",
            onClick = onGroundBooking
        )

        DashboardCard(
            title = "Sports Records",
            description = "View your assessments, bookings and equipment records",
            icon = "📋",
            onClick = onRecords
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onMyRequests,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("My Requests", color = White) }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onAdminRequests,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Admin Requests (Demo)", color = Lime) }

        OutlinedButton(
            onClick = onHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("My Assessment History", color = White)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("My Profile", color = White)
        }
    }
}
@Composable
fun DashboardCard(
    title: String,
    description: String,
    icon: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardNavy
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = description,
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Text(
                text = "→",
                color = Lime,
                fontSize = 22.sp
            )
        }
    }
}
@Composable
fun EquipmentScreen(
    inventory: List<EquipmentItem>,
    requests: List<EquipmentRequest>,
    onBack: () -> Unit,
    onRequest: (String, String, Int, String, String) -> Unit
) {
    var studentName by remember { mutableStateOf("") }
    var selectedEquipment by remember {
        mutableStateOf(inventory.firstOrNull()?.name ?: "")
    }
    var quantityText by remember { mutableStateOf("1") }
    var issueDate by remember { mutableStateOf("") }
    var returnDate by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    val item = inventory.find { it.name == selectedEquipment }
    val quantity = quantityText.toIntOrNull() ?: 0

    val pendingQuantity = requests
        .filter {
            it.equipmentName == selectedEquipment &&
                    it.status == "Pending"
        }
        .sumOf { it.quantity }

    val canRequest =
        studentName.isNotBlank() &&
                item != null &&
                quantity > 0 &&
                quantity <= ((item?.available ?: 0) - pendingQuantity) &&
                issueDate.isNotBlank() &&
                returnDate.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Sports Equipment",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Submit a request. Staff approval is required before equipment is issued.",
            color = Muted
        )

        inventory.forEach { itemRow ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = CardNavy
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        itemRow.name,
                        color = White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${itemRow.available} available",
                        color = Lime
                    )
                }
            }
        }

        OutlinedTextField(
            value = studentName,
            onValueChange = { studentName = it },
            label = { Text("Student Name / ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedLabelColor = Lime,
                unfocusedLabelColor = Muted,
                focusedBorderColor = Lime,
                unfocusedBorderColor = Muted,
                cursorColor = Lime,
                focusedContainerColor = CardNavy,
                unfocusedContainerColor = CardNavy
            )
        )

        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedEquipment, color = White)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                inventory.forEach { row ->
                    DropdownMenuItem(
                        text = {
                            Text("${row.name} (${row.available} available)")
                        },
                        onClick = {
                            selectedEquipment = row.name
                            expanded = false
                        },
                        enabled = row.available > 0
                    )
                }
            }
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = {
                quantityText = it.filter(Char::isDigit)
            },
            label = { Text("Quantity") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedLabelColor = Lime,
                unfocusedLabelColor = Muted,
                focusedBorderColor = Lime,
                unfocusedBorderColor = Muted,
                cursorColor = Lime,
                focusedContainerColor = CardNavy,
                unfocusedContainerColor = CardNavy
            )
        )

        OutlinedTextField(
            value = issueDate,
            onValueChange = { issueDate = it },
            label = { Text("Required From (DD/MM/YYYY)") },
            placeholder = { Text("DD/MM/YYYY", color = Muted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedLabelColor = Lime,
                unfocusedLabelColor = Muted,
                focusedBorderColor = Lime,
                unfocusedBorderColor = Muted,
                focusedPlaceholderColor = Muted,
                unfocusedPlaceholderColor = Muted,
                cursorColor = Lime,
                focusedContainerColor = CardNavy,
                unfocusedContainerColor = CardNavy
            )
        )

        OutlinedTextField(
            value = returnDate,
            onValueChange = { returnDate = it },
            label = { Text("Expected Return Date") },
            placeholder = { Text("DD/MM/YYYY", color = Muted) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = White,
                unfocusedTextColor = White,
                focusedLabelColor = Lime,
                unfocusedLabelColor = Muted,
                focusedBorderColor = Lime,
                unfocusedBorderColor = Muted,
                focusedPlaceholderColor = Muted,
                unfocusedPlaceholderColor = Muted,
                cursorColor = Lime,
                focusedContainerColor = CardNavy,
                unfocusedContainerColor = CardNavy
            )
        )

        Button(
            onClick = {
                onRequest(
                    studentName.trim(),
                    selectedEquipment,
                    quantity,
                    issueDate.trim(),
                    returnDate.trim()
                )

                message = "Request submitted. Waiting for staff approval."
                studentName = ""
                quantityText = "1"
                issueDate = ""
                returnDate = ""
            },
            enabled = canRequest,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Navy
            )
        ) {
            Text("Submit Equipment Request")
        }

        if (message.isNotBlank()) {
            Text(message, color = Lime)
        }

        Text(
            "Recent Requests",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        if (requests.isEmpty()) {
            Text("No requests yet.", color = Muted)
        } else {
            requests.reversed().forEach { req ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CardNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "${req.equipmentName} × ${req.quantity}",
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Student: ${req.studentName}",
                            color = Muted
                        )
                        Text(
                            "Status: ${req.status}",
                            color = if (req.status == "Approved") Lime else Muted
                        )
                    }
                }
            }
        }

        // Back button at the bottom
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("← Back to Dashboard", color = Lime)
        }
    }
}
@Composable
fun EquipmentStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = CardNavy
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                color = Lime,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = title,
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}
@Composable
fun GroundBookingScreen(
    bookings: List<GroundBooking>,
    onBack: () -> Unit,
    onBook: (String, String, String, String) -> Unit,
    onCancel: (GroundBooking) -> Unit
) {
    var selectedGround by remember {
        mutableStateOf("Main Ground")
    }

    var selectedSport by remember {
        mutableStateOf("Football")
    }

    var bookingDate by remember {
        mutableStateOf("")
    }
    var showDatePicker by remember {
        mutableStateOf(false)
    }
    var selectedFilter by remember {
        mutableStateOf("All")
    }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(
                utcTimeMillis: Long
            ): Boolean {
                val selected = Calendar.getInstance(
                    TimeZone.getTimeZone("UTC")
                ).apply {
                    timeInMillis = utcTimeMillis
                }

                val today = Calendar.getInstance(
                    TimeZone.getTimeZone("UTC")
                )

                return selected.get(Calendar.YEAR) > today.get(Calendar.YEAR) ||
                        (selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                                selected.get(Calendar.DAY_OF_YEAR) >= today.get(Calendar.DAY_OF_YEAR))
            }

            override fun isSelectableYear(year: Int): Boolean {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                return year >= currentYear
            }
        }
    )

    var selectedTime by remember {
        mutableStateOf("9:00 AM - 10:00 AM")
    }

    var message by remember {
        mutableStateOf("")
    }

    val grounds = listOf(
        "Main Ground",
        "Practice Ground",
        "Indoor Court",
        "Basketball Court"
    )

    val sports = listOf(
        "Football",
        "Cricket",
        "Basketball",
        "Volleyball",
        "Badminton"
    )

    val timeSlots = listOf(
        "9:00 AM - 10:00 AM",
        "10:00 AM - 11:00 AM",
        "11:00 AM - 12:00 PM",
        "2:00 PM - 3:00 PM",
        "3:00 PM - 4:00 PM"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "← Back",
            color = Lime,
            modifier = Modifier.clickable {
                onBack()
            }
        )

        Text(
            text = "Ground Booking",
            color = White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Reserve a sports facility",
            color = Muted,
            fontSize = 14.sp
        )

        // Booking form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CardNavy,
                    RoundedCornerShape(18.dp)
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "New Booking",
                color = White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Text("Select Ground", color = Muted)

            DropdownField(
                value = selectedGround,
                options = grounds,
                onSelect = {
                    selectedGround = it
                }
            )

            Text("Select Sport", color = Muted)

            DropdownField(
                value = selectedSport,
                options = sports,
                onSelect = {
                    selectedSport = it
                }
            )

            Text("Booking Date", color = Muted)

            OutlinedTextField(
                value = bookingDate,
                onValueChange = {},
                readOnly = true,
                placeholder = {
                    Text("DD/MM/YYYY")
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            showDatePicker = true
                        }
                    ) {
                        Text("📅", color = Lime)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = Lime,
                    unfocusedBorderColor = Muted,
                    focusedPlaceholderColor = Muted,
                    unfocusedPlaceholderColor = Muted
                )
            )

            Text("Time Slot", color = Muted)

            DropdownField(
                value = selectedTime,
                options = timeSlots,
                onSelect = {
                    selectedTime = it
                }
            )

            Button(
                onClick = {
                    val dateFormat = SimpleDateFormat(
                        "dd/MM/yyyy",
                        Locale.getDefault()
                    ).apply {
                        isLenient = false
                    }

                    val enteredDate = bookingDate.trim()

                    val parsedDate = runCatching {
                        dateFormat.parse(enteredDate)
                    }.getOrNull()

                    val isValidDate =
                        parsedDate != null &&
                                dateFormat.format(parsedDate) == enteredDate

                    val today = dateFormat.parse(
                        dateFormat.format(Date())
                    ) ?: Date()

                    when {
                        enteredDate.isBlank() -> {
                            message = "Please enter a booking date."
                        }

                        !isValidDate -> {
                            message = "Enter a valid date in DD/MM/YYYY format."
                        }

                        parsedDate!!.before(today) -> {
                            message = "You cannot book a date in the past."
                        }

                        else -> {
                            val alreadyBooked = bookings.any {
                                it.groundName == selectedGround &&
                                        it.date == enteredDate &&
                                        it.timeSlot == selectedTime &&
                                        (it.status == "Approved" ||
                                                it.status == "Pending")
                            }

                            if (alreadyBooked) {
                                message =
                                    "This ground is already booked for that slot."
                            } else {
                                onBook(
                                    selectedGround,
                                    selectedSport,
                                    enteredDate,
                                    selectedTime
                                )

                                message =
                                    "Request submitted! Waiting for staff approval. 🏟️"
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Lime,
                    contentColor = Navy
                )
            ) {
                Text(
                    "Book Ground",
                    fontWeight = FontWeight.Bold
                )
            }

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    color = if (
                        message.startsWith("Request submitted")
                    ) Lime else Muted,
                    fontSize = 13.sp
                )
            }
        }

        // My Bookings
        Text(
            text = "My Bookings",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        val filters = listOf(
            "All",
            "Pending",
            "Approved",
            "Rejected"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = {
                        selectedFilter = filter
                    },
                    label = {
                        Text(filter)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Lime,
                        selectedLabelColor = Navy,
                        labelColor = White
                    )
                )
            }
        }

        val filteredBookings = bookings
            .filter {
                selectedFilter == "All" ||
                        it.status == selectedFilter
            }
            .reversed()

        if (filteredBookings.isEmpty()) {
            Text(
                text = when (selectedFilter) {
                    "All" -> "No ground bookings yet."
                    else -> "No $selectedFilter bookings."
                },
                color = Muted
            )
        } else {
            filteredBookings.forEach { booking ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CardNavy,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = booking.groundName,
                        color = White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Sport: ${booking.sport}",
                        color = Muted
                    )

                    Text(
                        text = "Date: ${booking.date}",
                        color = Muted
                    )

                    Text(
                        text = "Time: ${booking.timeSlot}",
                        color = Muted
                    )

                    Text(
                        text = "Status: ${booking.status}",
                        color = when (booking.status) {
                            "Approved" -> Lime
                            "Rejected" -> Color(0xFFFF6B6B)
                            else -> Color(0xFFFFC857)
                        },
                        fontWeight = FontWeight.SemiBold
                    )

                    if (booking.status == "Approved") {
                        OutlinedButton(
                            onClick = {
                                onCancel(booking)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Cancel Booking",
                                color = White
                            )
                        }
                    }
                }
            }
        }
        if (showDatePicker) {
            DatePickerDialog(
                onDismissRequest = {
                    showDatePicker = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val formatter = SimpleDateFormat(
                                    "dd/MM/yyyy",
                                    Locale.getDefault()
                                ).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }

                                bookingDate = formatter.format(Date(it))
                            }

                            showDatePicker = false
                        }
                    ) {
                        Text("Confirm", color = Lime)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                        }
                    ) {
                        Text("Cancel", color = White)
                    }
                }
            ) {
                DatePicker(
                    state = datePickerState
                )
            }
        }
    }
}
@Composable
fun MyRequestsScreen(equipmentRequests: List<EquipmentRequest>, groundBookings: List<GroundBooking>, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Back to Dashboard", color = Lime) }
        Text("My Requests", color = White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("Equipment", color = Lime, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (equipmentRequests.isEmpty()) Text("No equipment requests.", color = Muted)
        equipmentRequests.reversed().forEach { r -> RequestCard("${r.equipmentName} × ${r.quantity}", "${r.studentName} • ${r.issueDate} to ${r.expectedReturnDate}", r.status) }
        Text("Ground Slots", color = Lime, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        if (groundBookings.isEmpty()) Text("No ground requests.", color = Muted)
        groundBookings.reversed().forEach { b -> RequestCard("${b.groundName} — ${b.sport}", "${b.date} • ${b.timeSlot}", b.status) }
    }
}

@Composable
fun AdminRequestsScreen(
    equipmentRequests: List<EquipmentRequest>,
    groundBookings: List<GroundBooking>,
    onBack: () -> Unit,
    onEquipmentDecision: (EquipmentRequest, Boolean) -> Unit,
    onGroundDecision: (GroundBooking, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to Dashboard", color = Lime)
        }
        Text(
            text = "Admin Requests",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Text("Demo staff view — approve or reject pending requests.", color = Muted)

        Text("Equipment Requests", color = Lime, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        val pendingEquipment = equipmentRequests.filter { it.status == "Pending" }
        if (pendingEquipment.isEmpty()) {
            Text("No pending equipment requests.", color = Muted)
        }
        pendingEquipment.forEach { request ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${request.equipmentName} × ${request.quantity}", color = White, fontWeight = FontWeight.Bold)
                    Text("Student: ${request.studentName}", color = Muted)
                    Text("${request.issueDate} → ${request.expectedReturnDate}", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onEquipmentDecision(request, true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Navy)
                        ) { Text("Approve") }
                        OutlinedButton(
                            onClick = { onEquipmentDecision(request, false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject", color = White) }
                    }
                }
            }
        }

        Text("Ground Requests", color = Lime, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        val pendingGround = groundBookings.filter { it.status == "Pending" }
        if (pendingGround.isEmpty()) {
            Text("No pending ground requests.", color = Muted)
        }
        pendingGround.forEach { booking ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CardNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("${booking.groundName} — ${booking.sport}", color = White, fontWeight = FontWeight.Bold)
                    Text("${booking.date} • ${booking.timeSlot}", color = Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onGroundDecision(booking, true) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Navy)
                        ) { Text("Approve") }
                        OutlinedButton(
                            onClick = { onGroundDecision(booking, false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject", color = White) }
                    }
                }
            }
        }
    }
}

@Composable
fun RequestCard(title: String, detail: String, status: String) {
    Card(colors = CardDefaults.cardColors(containerColor = CardNavy), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, color = White, fontWeight = FontWeight.Bold); Text(detail, color = Muted); Text("Status: $status", color = if (status == "Approved") Lime else Muted, fontWeight = FontWeight.SemiBold) } }
}

@Composable
fun DropdownField(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = {
                expanded = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = White
            )
        ) {
            Text(
                text = value,
                modifier = Modifier.weight(1f)
            )

            Text("▼")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            },
            modifier = Modifier.background(CardNavy)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = White
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
@Composable
fun SportsRecordsScreen(
    assessmentHistory: List<AssessmentRecord>,
    equipmentTransactions: List<EquipmentTransaction>,
    groundBookings: List<GroundBooking>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf("All") }

    val tabs = listOf(
        "All",
        "Assessments",
        "Equipment",
        "Grounds"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to Dashboard", color = Lime)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sports Records",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your sports activity at a glance",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EquipmentStatCard(
                title = "Assessments",
                value = assessmentHistory.size.toString(),
                modifier = Modifier.weight(1f)
            )

            EquipmentStatCard(
                title = "Equipment",
                value = equipmentTransactions.size.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Record category selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = tab },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Lime else CardNavy
                ) {
                    Text(
                        text = tab,
                        modifier = Modifier.padding(
                            horizontal = 4.dp,
                            vertical = 12.dp
                        ),
                        color = if (isSelected) Navy else White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Assessment records
        if (selectedTab == "All" ||
            selectedTab == "Assessments"
        ) {
            Text(
                text = "Assessment History",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (assessmentHistory.isEmpty()) {
                Text(
                    text = "No assessment records yet.",
                    color = Muted
                )
            } else {
                assessmentHistory.reversed().forEach { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CardNavy
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = record.testName,
                                color = White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = when (record.testName) {
                                    "Sit-Ups" ->
                                        "Reps: ${record.reps ?: "--"}"

                                    "Vertical Jump" ->
                                        "Jump Height: -- cm"

                                    "Broad Jump" ->
                                        "Jump Distance: -- cm"

                                    "30m Sprint" ->
                                        "Sprint Time: -- sec"

                                    else ->
                                        "Result: ${record.reps ?: "--"}"
                                },
                                color = Lime
                            )

                            Text(
                                text = "Quality: ${record.resultQuality}",
                                color = Muted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Equipment records
        if (selectedTab == "All" ||
            selectedTab == "Equipment"
        ) {
            Text(
                text = "Equipment Records",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (equipmentTransactions.isEmpty()) {
                Text(
                    text = "No equipment transactions yet.",
                    color = Muted
                )
            } else {
                equipmentTransactions.reversed().forEach { transaction ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CardNavy
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = transaction.equipmentName,
                                color = White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = "Student: ${transaction.studentName}",
                                color = Muted
                            )

                            Text(
                                text = "Quantity: ${transaction.quantity}",
                                color = Muted
                            )

                            Text(
                                text = "Issue Date: ${transaction.issueDate}",
                                color = Muted
                            )

                            Text(
                                text = "Expected Return: ${transaction.expectedReturnDate}",
                                color = Muted
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text(
                                text = if (transaction.returned) {
                                    "Status: Returned"
                                } else {
                                    "Status: Issued"
                                },
                                color = if (transaction.returned) {
                                    Lime
                                } else {
                                    White
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Ground booking records — connected in the next step
        if (groundBookings.isEmpty()) {
            Text(
                text = "No ground booking records yet.",
                color = Muted
            )
        } else {
            groundBookings.reversed().forEach { booking ->

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            CardNavy,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = booking.groundName,
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = "${booking.sport} • ${booking.date}",
                        color = Muted
                    )

                    Text(
                        text = booking.timeSlot,
                        color = Muted
                    )

                    Text(
                        text = "Status: ${booking.status}",
                        color = if (booking.status == "Approved") {
                            Lime
                        } else {
                            Muted
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SelectTestScreen(
    onBack: () -> Unit,
    onTestSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Select a Test",
            color = White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Choose a fitness assessment to get started.",
            color = Muted,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        TestCard(
            title = "Flexibility (Sit & Reach)",
            description = "Flexibility • Forward reach distance",
            onClick = { onTestSelected("Flexibility (Sit & Reach)") }
        )

        TestCard(
            title = "Vertical Jump",
            description = "Explosive power • Jump height",
            onClick = { onTestSelected("Vertical Jump") }
        )

        TestCard(
            title = "Broad Jump",
            description = "Explosive strength • Jump distance",
            onClick = { onTestSelected("Broad Jump") }
        )

        TestCard(
            title = "Medicine Ball Throw",
            description = "Upper-body strength • Throw distance",
            onClick = { onTestSelected("Medicine Ball Throw") }
        )

        TestCard(
            title = "30m Sprint",
            description = "Speed • Sprint timing",
            onClick = { onTestSelected("30m Sprint") }
        )

        TestCard(
            title = "4×10m Shuttle Run",
            description = "Agility • Shuttle-run timing",
            onClick = { onTestSelected("4×10m Shuttle Run") }
        )

        TestCard(
            title = "Sit-Ups",
            description = "Abdominal strength • Repetition count",
            onClick = { onTestSelected("Sit-Ups") }
        )

        TestCard(
            title = "Endurance Run",
            description = "Endurance • 800m (under 12) / 1.6km (12+)",
            onClick = { onTestSelected("Endurance Run") }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
        ) {
            Text("← Back", color = Lime, fontSize = 16.sp)
        }
    }
}

@Composable
fun TestCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardNavy
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    color = Muted,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "→",
                color = Lime,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun CapturePlaceholder(
    testName: String,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val instructions = when (testName) {
        "Sit-Ups" ->
            "Lie on your back with your knees bent. " +
                    "Keep your upper body visible to the camera."

        "Vertical Jump" ->
            "Stand upright with your full body visible. " +
                    "Leave enough space above your head."

        "Broad Jump" ->
            "Stand behind the starting line. " +
                    "Make sure your full body and landing area are visible."

        "30m Sprint" ->
            "Position the camera to capture your running movement. " +
                    "Ensure the running area is clear."

        else -> "Position yourself correctly before starting."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Lime)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Assessment Setup",
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = testName,
            color = Lime,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Camera placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    CardNavy,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📷",
                    fontSize = 42.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Camera Preview",
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Camera integration coming soon",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Before You Start",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = instructions,
            color = Muted,
            fontSize = 15.sp,
            lineHeight = 23.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "✓  Ensure good lighting",
            color = White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "✓  Keep the required body parts visible",
            color = White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "✓  Make sure the surrounding area is clear",
            color = White
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Navy
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "View Results",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ResultsScreen(
    testName: String,
    reps: Int?,
    resultQuality: String,
    summary: String,
    onRetake: () -> Unit,
    onDashboard: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ASSESSMENT COMPLETE",
            color = Lime,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Your Results",
            color = White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = testName,
            color = Muted,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    CardNavy,
                    RoundedCornerShape(20.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = when (testName) {
                    "Sit-Ups" -> "TOTAL REPS"
                    "Vertical Jump" -> "JUMP HEIGHT"
                    "Broad Jump" -> "JUMP DISTANCE"
                    "30m Sprint" -> "SPRINT TIME"
                    else -> "YOUR SCORE"
                },
                color = Muted,
                fontSize = 12.sp,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = when (testName) {
                    "Sit-Ups" -> reps?.toString() ?: "--"
                    "Vertical Jump" -> "-- cm"
                    "Broad Jump" -> "-- cm"
                    "30m Sprint" -> "-- sec"
                    else -> "--"
                },
                color = Lime,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Result Quality: $resultQuality",
                color = White
            )

            Text(
                text = summary,
                color = Muted
            )

            Spacer(modifier = Modifier.height(20.dp))

            HorizontalDivider(
                color = Muted.copy(alpha = 0.3f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Performance Summary",
                color = White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (testName) {
                    "Sit-Ups" -> "Your sit-up count and form analysis will appear here."
                    "Vertical Jump" -> "Your jump height will appear here."
                    "Broad Jump" -> "Your jump distance will appear here."
                    "30m Sprint" -> "Your sprint time will appear here."
                    else -> "Your assessment results will appear here."
                },
                color = Muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onRetake,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Navy
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Retake Test",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDashboard,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Back to Dashboard",
                color = White
            )
        }
    }
}

@Composable
fun SimplePlaceholder(
    title: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "This section will be implemented next.",
            color = Muted
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Navy
            )
        ) {
            Text("← Back to Dashboard")
        }
    }
}
@Composable
fun AssessmentHistoryScreen(
    history: List<AssessmentRecord>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Lime)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "My History",
            color = White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (history.isEmpty()) {
            Text(
                "No assessments yet.",
                color = Muted
            )
        } else {
            history.reversed().forEach { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardNavy
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            record.testName,
                            color = White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Reps: ${record.reps?.toString() ?: "--"}",
                            color = Lime
                        )

                        Text(
                            "Quality: ${record.resultQuality}",
                            color = Muted
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun StudentProfileScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf("Student") }
    var studentId by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back", color = Lime)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "My Profile",
            color = White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Manage your student details",
            color = Muted
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = studentId,
            onValueChange = { studentId = it },
            label = { Text("Student ID") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Navy
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Profile", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Log Out", color = White)
        }
    }
}
