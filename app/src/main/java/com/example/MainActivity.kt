package com.example

import android.app.Application
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val database = AppDatabase.getDatabase(context.applicationContext)
                val repository = ExpenseRepository(database.expenseDao())
                val appViewModel: ExpenseViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ExpenseViewModel(context.applicationContext as Application, repository) as T
                        }
                    }
                )

                val setupProfile by appViewModel.setupProfile.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (setupProfile == null || !setupProfile!!.isSetup) {
                            SetupScreen(viewModel = appViewModel)
                        } else {
                            DashboardScreen(viewModel = appViewModel, profile = setupProfile!!)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SetupScreen(viewModel: ExpenseViewModel) {
    var user1 by remember { mutableStateOf("") }
    var user2 by remember { mutableStateOf("") }
    val localContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Welcome.",
            style = MaterialTheme.typography.headlineLarge,
            color = CharcoalText,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "A quiet place to track shared expenses. Set your names — you'll only do this once.",
            style = MaterialTheme.typography.bodyLarge,
            color = WarmGreyMuted,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "YOUR NAME",
            style = MaterialTheme.typography.labelSmall,
            color = WarmGreyMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = user1,
            onValueChange = { user1 = it },
            placeholder = { Text("e.g. Rahul") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                disabledContainerColor = SurfaceWhite,
                focusedIndicatorColor = CharcoalText,
                unfocusedIndicatorColor = BorderStrong,
                cursorColor = CharcoalText
            ),
            singleLine = true
        )

        Text(
            text = "YOUR WIFE'S NAME",
            style = MaterialTheme.typography.labelSmall,
            color = WarmGreyMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = user2,
            onValueChange = { user2 = it },
            placeholder = { Text("e.g. Priya") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = SurfaceWhite,
                disabledContainerColor = SurfaceWhite,
                focusedIndicatorColor = CharcoalText,
                unfocusedIndicatorColor = BorderStrong,
                cursorColor = CharcoalText
            ),
            singleLine = true
        )

        Button(
            onClick = {
                val u1 = user1.trim()
                val u2 = user2.trim()
                if (u1.isEmpty() || u2.isEmpty()) {
                    Toast.makeText(localContext, "Both names are required", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveSetup(u1, u2) { ok ->
                        if (!ok) {
                            Toast.makeText(localContext, "Couldn't save", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SageLife,
                contentColor = SurfaceWhite
            )
        ) {
            Text(
                text = "Get started",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DashboardScreen(viewModel: ExpenseViewModel, profile: SetupProfile) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val runningBalance by viewModel.runningBalance.collectAsStateWithLifecycle()
    val unsettledCount by viewModel.unsettledCount.collectAsStateWithLifecycle()

    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val txDisplayLimit by viewModel.txDisplayLimit.collectAsStateWithLifecycle()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<Expense?>(null) }
    val localContext = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CharcoalText
                )
                Text(
                    text = "${profile.user1Name.uppercase()} & ${profile.user2Name.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyFaint
                )
            }

            // Running Balance Card Styled in High-Contrast Natural Tonal Slate
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                colors = CardDefaults.cardColors(containerColor = BorderStrong),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 26.dp, horizontal = 24.dp)) {
                    val formattedAbs = fmtAmount(runningBalance)
                    
                    val directionText = when {
                        kotlin.math.abs(runningBalance) < 0.5 -> "All settled up"
                        runningBalance > 0 -> "${profile.user2Name} owes ${profile.user1Name}"
                        else -> "${profile.user1Name} owes ${profile.user2Name}"
                    }

                    val colorState = when {
                        kotlin.math.abs(runningBalance) < 0.5 -> CharcoalText
                        runningBalance > 0 -> ForestPositive
                        else -> ClayNegative
                    }

                    val metaText = when {
                        kotlin.math.abs(runningBalance) < 0.5 -> "No outstanding balance."
                        else -> "Across $unsettledCount unsettled transactions."
                    }

                    Text(
                        text = directionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGreyMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formattedAbs,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colorState,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarmGreyMuted,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Monthly Buckets Section Heading
            Text(
                text = viewModel.formatMonthLabel(selectedMonth),
                style = MaterialTheme.typography.labelMedium,
                color = WarmGreyMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            // Monthly Buckets (Grid layout equivalent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val totals = calculateBucketTotals(expenses, selectedMonth, viewModel)
                
                BucketItem(
                    name = "Life",
                    amount = totals["Life Expenses"] ?: 0.0,
                    trendColor = SageLife,
                    trendText = getTrendTextForBucket(expenses, selectedMonth, "Life Expenses", viewModel),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectedCategory.value = "bucket:Life Expenses" }
                )
                BucketItem(
                    name = "Travel",
                    amount = totals["Travel"] ?: 0.0,
                    trendColor = GoldTravel,
                    trendText = getTrendTextForBucket(expenses, selectedMonth, "Travel", viewModel),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectedCategory.value = "bucket:Travel" }
                )
                BucketItem(
                    name = "JUNA",
                    amount = totals["JUNA"] ?: 0.0,
                    trendColor = RoseJUNA,
                    trendText = getTrendTextForBucket(expenses, selectedMonth, "JUNA", viewModel),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectedCategory.value = "bucket:JUNA" }
                )
            }

            // Monthly Trends Bar Charts
            Text(
                text = "Monthly trends",
                style = MaterialTheme.typography.labelMedium,
                color = WarmGreyMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            TrendsCard(expenses = expenses, viewModel = viewModel)

            // Travel Snapshot Area
            val travelExpensesInMonth = expenses.filter {
                (selectedMonth == "all" || it.date.substring(0, 7) == selectedMonth) &&
                        viewModel.bucketFor(it.category) == "Travel"
            }
            if (travelExpensesInMonth.isNotEmpty()) {
                TravelSnapshotCard(travelExpenses = travelExpensesInMonth, selectedMonthLabel = viewModel.formatMonthLabel(selectedMonth))
            }

            // Transaction Section Heading
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleLarge,
                color = CharcoalText,
                modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp)
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search description or tag…") },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = WarmGreyMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(10.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite,
                    focusedIndicatorColor = CharcoalText,
                    unfocusedIndicatorColor = BorderStrong,
                    cursorColor = CharcoalText
                ),
                singleLine = true
            )

            // Dynamic filter options
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Month filter spinner
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGreyFaint,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    MonthFilterDropdown(viewModel = viewModel, currentMonth = selectedMonth)
                }

                // Category/Bucket filter spinner
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CATEGORY",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGreyFaint,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    CategoryFilterDropdown(viewModel = viewModel, currentCategory = selectedCategory)
                }
            }

            // Unsettled Transactions count and list card
            val filteredList = filterExpenses(expenses, selectedMonth, selectedCategory, searchQuery, viewModel)
            
            // Limit renderer
            val shownCount = kotlin.math.min(txDisplayLimit, filteredList.size)
            val hasMore = filteredList.size > shownCount

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderLight),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (filteredList.isNotEmpty()) {
                        val sumFormatted = fmtAmount(filteredList.sumOf { it.amount })
                        val labelSummary = if (hasMore) {
                            "Showing $shownCount of ${filteredList.size} transactions · $sumFormatted total"
                        } else {
                            "${filteredList.size} ${if (filteredList.size == 1) "transaction" else "transactions"} · $sumFormatted total"
                        }
                        Text(
                            text = labelSummary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = WarmGreyFaint,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        filteredList.take(shownCount).forEachIndexed { index, expense ->
                            TransactionRow(
                                expense = expense,
                                u1 = profile.user1Name,
                                u2 = profile.user2Name,
                                viewModel = viewModel,
                                onEditClick = {
                                    editingExpense = expense
                                    showAddEditDialog = true
                                }
                            )
                            if (index < shownCount - 1) {
                                HorizontalDivider(color = BorderLight, thickness = 1.dp)
                            }
                        }

                        if (hasMore) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.txDisplayLimit.value = filteredList.size },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderStrong),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmGreyMuted)
                            ) {
                                Text(
                                    text = "Show all ${filteredList.size} transactions",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matches for \"$searchQuery\"" else "No expenses match these filters.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarmGreyMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Settle up action footer
            Button(
                onClick = {
                    if (kotlin.math.abs(runningBalance) < 0.5) {
                        Toast.makeText(localContext, "Nothing to settle", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.settleUp { ok ->
                            Toast.makeText(
                                localContext,
                                if (ok) "Settled up!" else "Couldn't settle",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderStrong),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceWhite,
                    contentColor = CharcoalText
                )
            ) {
                Text(
                    text = "Settle up",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(72.dp)) //FAB buffer space
        }

        // Sticky FAB Styled in High-Contrast Botanic Highlight
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Button(
                onClick = {
                    editingExpense = null
                    showAddEditDialog = true
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoftGreenAccent,
                    contentColor = DeepGreenOnAccent
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add icon",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Add expense",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // Modal Add / Edit Expense Dialog
    if (showAddEditDialog) {
        AddEditExpenseDialog(
            viewModel = viewModel,
            expense = editingExpense,
            user1 = profile.user1Name,
            user2 = profile.user2Name,
            onClose = { showAddEditDialog = false }
        )
    }
}

@Composable
fun BucketItem(
    name: String,
    amount: Double,
    trendColor: Color,
    trendText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderLight),
        colors = CardDefaults.cardColors(containerColor = TonalCardBg)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Bucket accent color bar at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(trendColor)
            )

            Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
                Text(
                    text = name.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = fmtAmount(amount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = CharcoalText,
                    fontWeight = FontWeight.Bold
                )
                if (trendText.isNotEmpty()) {
                    Text(
                        text = trendText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (trendText.startsWith("↑")) ClayNegative else ForestPositive,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TrendsCard(expenses: List<Expense>, viewModel: ExpenseViewModel) {
    val buckets = listOf(
        Triple("Life Expenses", SageLife, "life"),
        Triple("Travel", GoldTravel, "travel"),
        Triple("JUNA", RoseJUNA, "juna")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderLight),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            buckets.forEachIndexed { index, (bucket, barColor, accentId) ->
                val trendMonths = viewModel.getTrendMonths(expenses, bucket)
                val maxVal = trendMonths.maxOfOrNull { it.amount } ?: 1.0
                val safeMax = if (maxVal == 0.0) 1.0 else maxVal
                val avgVal = if (trendMonths.isNotEmpty()) trendMonths.map { it.amount }.sum() / trendMonths.size else 0.0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < buckets.size - 1) 18.dp else 0.dp)
                ) {
                    // Header for bucket trends
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (bucket == "Life Expenses") "Life" else bucket,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGreyMuted
                        )
                        Text(
                            text = "${fmtAmount(avgVal)} avg",
                            style = MaterialTheme.typography.bodySmall,
                            color = CharcoalText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chart Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        trendMonths.forEach { trendMonth ->
                            val heightPercent = (trendMonth.amount / safeMax).toFloat()
                            val adjustedHeightPercent = if (trendMonth.amount > 0 && heightPercent < 0.05f) 0.05f else heightPercent

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // compact amount display on top
                                if (trendMonth.amount > 0) {
                                    Text(
                                        text = fmtCompact(trendMonth.amount),
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = if (trendMonth.isCurrent) CharcoalText else WarmGreyMuted,
                                        fontWeight = if (trendMonth.isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(11.dp))
                                }

                                // Bar
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(adjustedHeightPercent)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(barColor)
                                        .then(
                                            if (trendMonth.isCurrent) {
                                                Modifier.border(
                                                    1.5.dp,
                                                    CharcoalText,
                                                    RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                                )
                                            } else Modifier
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    // Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        trendMonths.forEach { trendMonth ->
                            Text(
                                text = trendMonth.label,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = WarmGreyFaint,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TravelSnapshotCard(travelExpenses: List<Expense>, selectedMonthLabel: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderLight),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(GoldTravel)
            )

            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Travel · $selectedMonthLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = GoldTravel,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val subGroup = travelExpenses.groupBy { it.category }
                val keysSorted = subGroup.keys.sortedByDescending { cat ->
                    subGroup[cat]?.sumOf { it.amount } ?: 0.0
                }

                keysSorted.forEach { cat ->
                    val totalSub = subGroup[cat]?.sumOf { it.amount } ?: 0.0
                    val subClean = cat.replace("Travel — ", "")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = subClean,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CharcoalText
                        )
                        Text(
                            text = fmtAmount(totalSub),
                            style = MaterialTheme.typography.bodyLarge,
                            color = CharcoalText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val totalSum = travelExpenses.sumOf { it.amount }
                Text(
                    text = "Total · ${fmtAmount(totalSum)} across ${travelExpenses.size} ${if (travelExpenses.size == 1) "transaction" else "transactions"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGreyMuted,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionRow(
    expense: Expense,
    u1: String,
    u2: String,
    viewModel: ExpenseViewModel,
    onEditClick: () -> Unit
) {
    val isPersonal = expense.split == "Personal"
    val bucket = viewModel.bucketFor(expense.category)
    val barColor = when {
        isPersonal -> WarmGreyFaint
        bucket == "Travel" -> GoldTravel
        bucket == "JUNA" -> RoseJUNA
        else -> SageLife
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vertical Category Bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

        // Main info block
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = expense.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = CharcoalText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isPersonal) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .background(BorderLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "PERSONAL",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = WarmGreyMuted
                        )
                    }
                }
            }

            val cleanCat = expense.category
                .replace("Travel — ", "")
                .replace("JUNA — ", "")

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = "${viewModel.formatDateShort(expense.date)} · $cleanCat · ${expense.paidBy} paid",
                    style = MaterialTheme.typography.bodySmall,
                    color = WarmGreyMuted
                )
                if (!expense.tag.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .border(1.dp, BorderLight, RoundedCornerShape(4.dp))
                            .background(CreamBg)
                            .padding(horizontal = 7.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = expense.tag,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = CharcoalText
                        )
                    }
                }
            }
        }

        // Price block
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = fmtAmount(expense.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = CharcoalText
            )
            
            val balanceValue = calculateBalanceChangeValue(expense, u1, u2)
            val balanceText = when {
                isPersonal -> "Personal"
                balanceValue > 0 -> "+${fmtAmount(balanceValue)} to $u1"
                balanceValue < 0 -> "+${fmtAmount(balanceValue)} to $u2"
                else -> "—"
            }
            val balanceColor = when {
                isPersonal -> WarmGreyMuted
                balanceValue > 0 -> ForestPositive
                balanceValue < 0 -> ClayNegative
                else -> WarmGreyMuted
            }

            Text(
                text = balanceText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = balanceColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        var confirmDelete by remember { mutableStateOf(false) }
        val ctx = LocalContext.current

        // Delete check icon
        IconButton(
            onClick = { confirmDelete = true },
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterVertically)
        ) {
            Text("✕", fontSize = 14.sp, color = WarmGreyFaint)
        }

        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete this expense?") },
                text = { Text(expense.description) },
                confirmButton = {
                    TextButton(onClick = {
                        confirmDelete = false
                        viewModel.deleteExpense(expense) { ok ->
                            Toast.makeText(
                                ctx,
                                if (ok) "Deleted" else "Couldn't delete",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) { Text("Delete", color = ClayNegative) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun MonthFilterDropdown(viewModel: ExpenseViewModel, currentMonth: String) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    val months = remember(expenses) {
        val list = expenses.map { it.date.substring(0, 7) }.toMutableSet()
        list.add(viewModel.thisMonthKey())
        list.toList().sortedDescending()
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderStrong),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.formatMonthLabel(currentMonth),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalText
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow down",
                    tint = WarmGreyMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .background(SurfaceWhite)
        ) {
            months.forEach { m ->
                DropdownMenuItem(
                    text = { Text(viewModel.formatMonthLabel(m)) },
                    onClick = {
                        viewModel.selectedMonth.value = m
                        viewModel.txDisplayLimit.value = 10
                        expanded = false
                    }
                )
            }
            DropdownMenuItem(
                text = { Text("All time") },
                onClick = {
                    viewModel.selectedMonth.value = "all"
                    viewModel.txDisplayLimit.value = 10
                    expanded = false
                }
            )
        }
    }
}

@Composable
fun CategoryFilterDropdown(viewModel: ExpenseViewModel, currentCategory: String) {
    var expanded by remember { mutableStateOf(false) }

    val formattedLabel = when {
        currentCategory == "all" -> "All categories"
        currentCategory == "personal" -> "Personal only"
        currentCategory == "shared" -> "Shared only"
        currentCategory.startsWith("bucket:") -> "All " + currentCategory.substring(7)
        else -> currentCategory
    }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderStrong),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow down",
                    tint = WarmGreyMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.45f)
                .background(SurfaceWhite)
        ) {
            DropdownMenuItem(
                text = { Text("All categories") },
                onClick = {
                    viewModel.selectedCategory.value = "all"
                    expanded = false
                }
            )

            // Buckets group
            DropdownMenuItem(
                text = { Text("All Life Expenses", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                onClick = {
                    viewModel.selectedCategory.value = "bucket:Life Expenses"
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("All Travel", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                onClick = {
                    viewModel.selectedCategory.value = "bucket:Travel"
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("All JUNA", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)) },
                onClick = {
                    viewModel.selectedCategory.value = "bucket:JUNA"
                    expanded = false
                }
            )

            // Split Types group
            DropdownMenuItem(
                text = { Text("Personal only", style = MaterialTheme.typography.bodyMedium.copy(color = WarmGreyFaint)) },
                onClick = {
                    viewModel.selectedCategory.value = "personal"
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Shared only", style = MaterialTheme.typography.bodyMedium.copy(color = WarmGreyFaint)) },
                onClick = {
                    viewModel.selectedCategory.value = "shared"
                    expanded = false
                }
            )

            // Subcategories listing
            viewModel.categories.forEach { (bucket, cats) ->
                DropdownMenuItem(
                    text = { Text("--- $bucket ---", style = MaterialTheme.typography.bodySmall.copy(color = WarmGreyFaint)) },
                    onClick = {},
                    enabled = false
                )
                cats.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = {
                            viewModel.selectedCategory.value = c
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    viewModel: ExpenseViewModel,
    expense: Expense?,
    user1: String,
    user2: String,
    onClose: () -> Unit
) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()

    var activeType by remember { mutableStateOf(if (expense?.split == "Personal") "personal" else "shared") }
    var amount by remember { mutableStateOf(expense?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(expense?.description ?: "") }
    var date by remember { mutableStateOf(expense?.date ?: getCurrentDateISO()) }
    var category by remember { mutableStateOf(expense?.category ?: "") }
    var tag by remember { mutableStateOf(expense?.tag ?: "") }
    var paidBy by remember { mutableStateOf(expense?.paidBy ?: user1) }
    
    // Split drop configurations
    var splitVal by remember {
        mutableStateOf(
            if (activeType == "personal") "Personal"
            else if (expense?.split?.startsWith("Custom") == true) "custom"
            else expense?.split ?: "50/50"
        )
    }
    
    var customPct by remember {
        mutableStateOf(
            if (expense?.split?.startsWith("Custom") == true) {
                expense.split.filter { it.isDigit() }
            } else ""
        )
    }

    val mruCategories = remember(expenses) { viewModel.getMRUPending(expenses) }
    val localContext = LocalContext.current

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(CreamBg),
            color = CreamBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // Modal header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expense == null) "Add expense" else "Edit expense",
                        style = MaterialTheme.typography.headlineMedium,
                        color = CharcoalText
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = WarmGreyMuted)
                    }
                }

                // Field: Type (Segmented Toggle)
                Text(
                    text = "TYPE",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToggleOptionButton(
                        text = "Shared",
                        isActive = activeType == "shared",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeType = "shared"
                            if (splitVal == "Personal") splitVal = "50/50"
                        }
                    )
                    ToggleOptionButton(
                        text = "Personal",
                        isActive = activeType == "personal",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activeType = "personal"
                            splitVal = "Personal"
                        }
                    )
                }

                // Field: Amount
                Text(
                    text = "AMOUNT (₹)",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = CharcoalText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedIndicatorColor = CharcoalText,
                        unfocusedIndicatorColor = BorderStrong,
                        cursorColor = CharcoalText
                    ),
                    singleLine = true
                )

                // Field: Description
                Text(
                    text = "DESCRIPTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedIndicatorColor = CharcoalText,
                        unfocusedIndicatorColor = BorderStrong,
                        cursorColor = CharcoalText
                    ),
                    singleLine = true
                )

                // Field: Date
                Text(
                    text = "DATE",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedCard(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        try {
                            val parsed = sdf.parse(date)
                            if (parsed != null) calendar.time = parsed
                        } catch (e: Exception) {}

                        DatePickerDialog(
                            localContext,
                            { _, year, month, dayOfMonth ->
                                date = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, BorderStrong),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CharcoalText
                        )
                    }
                }

                // Field: Category
                Text(
                    text = "CATEGORY",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                CategorySpinner(
                    viewModel = viewModel,
                    category = category,
                    mruList = mruCategories,
                    onChoose = { selected ->
                        category = selected
                        // JUNA category auto default rules
                        if (viewModel.bucketFor(selected) == "JUNA" && activeType == "shared") {
                            splitVal = "$user2 owes full"
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Field: Tag
                Text(
                    text = "TAG (OPTIONAL)",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    placeholder = { Text("e.g. Goa Dec 2025") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedIndicatorColor = CharcoalText,
                        unfocusedIndicatorColor = BorderStrong,
                        cursorColor = CharcoalText
                    ),
                    singleLine = true
                )

                // Field: Paid By
                Text(
                    text = "PAID BY",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmGreyMuted,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToggleOptionButton(
                        text = user1,
                        isActive = paidBy == user1,
                        modifier = Modifier.weight(1f),
                        onClick = { paidBy = user1 }
                    )
                    ToggleOptionButton(
                        text = user2,
                        isActive = paidBy == user2,
                        modifier = Modifier.weight(1f),
                        onClick = { paidBy = user2 }
                    )
                }

                // Field Split (Only if shared)
                if (activeType == "shared") {
                    Text(
                        text = "SPLIT",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGreyMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    SplitSpinner(
                        u1 = user1,
                        u2 = user2,
                        value = splitVal,
                        onChoose = { splitVal = it },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (splitVal == "custom") {
                        Text(
                            text = "CUSTOM PERCENTAGE (% NON-PAYER OWES)",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarmGreyMuted,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = customPct,
                            onValueChange = { customPct = it },
                            placeholder = { Text("e.g. 30") },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite,
                                focusedIndicatorColor = CharcoalText,
                                unfocusedIndicatorColor = BorderStrong,
                                cursorColor = CharcoalText
                    ),
                            singleLine = true
                        )
                    }
                }

                // Form Action Buttons
                Button(
                    onClick = {
                        val parsedAmt = amount.toDoubleOrNull()
                        if (parsedAmt == null || parsedAmt <= 0) {
                            Toast.makeText(localContext, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (description.trim().isEmpty()) {
                            Toast.makeText(localContext, "Enter a description", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (category.isEmpty()) {
                            Toast.makeText(localContext, "Choose a category", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        var splitFinal = splitVal
                        if (activeType == "personal") {
                            splitFinal = "Personal"
                        } else if (splitVal == "custom") {
                            val pct = customPct.toIntOrNull()
                            if (pct == null || pct < 1 || pct > 99) {
                                Toast.makeText(localContext, "Enter custom percentage values 1-99", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            splitFinal = "Custom $pct%"
                        }

                        val resultHandler: (Boolean) -> Unit = { ok ->
                            val msg = if (ok) {
                                if (expense == null) "Added!" else "Updated!"
                            } else {
                                "Couldn't save"
                            }
                            Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show()
                        }

                        if (expense == null) {
                            viewModel.addExpense(
                                date = date,
                                description = description,
                                category = category,
                                tag = tag,
                                amount = parsedAmt,
                                paidBy = paidBy,
                                split = splitFinal,
                                onResult = resultHandler
                            )
                        } else {
                            viewModel.updateExpense(
                                id = expense.id,
                                date = date,
                                description = description,
                                category = category,
                                tag = tag,
                                amount = parsedAmt,
                                paidBy = paidBy,
                                split = splitFinal,
                                onResult = resultHandler
                            )
                        }
                        onClose()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CharcoalText, contentColor = CreamBg)
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (expense != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.deleteExpense(expense) { ok ->
                                Toast.makeText(
                                    localContext,
                                    if (ok) "Deleted" else "Couldn't delete",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            onClose()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ClayNegative),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ClayNegative)
                    ) {
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToggleOptionButton(
    text: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderStrong),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isActive) CharcoalText else SurfaceWhite,
            contentColor = if (isActive) CreamBg else CharcoalText
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CategorySpinner(
    viewModel: ExpenseViewModel,
    category: String,
    mruList: List<String>,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderStrong),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.ifEmpty { "Choose..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalText
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow",
                    tint = WarmGreyMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
        ) {
            if (mruList.isNotEmpty()) {
                DropdownMenuItem(
                    text = { Text("Recent", style = MaterialTheme.typography.bodySmall.copy(color = WarmGreyFaint)) },
                    onClick = {},
                    enabled = false
                )
                mruList.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = {
                            onChoose(c)
                            expanded = false
                        }
                    )
                }
            }

            viewModel.categories.forEach { (bucket, cats) ->
                DropdownMenuItem(
                    text = { Text(bucket, style = MaterialTheme.typography.bodySmall.copy(color = WarmGreyFaint)) },
                    onClick = {},
                    enabled = false
                )
                cats.forEach { c ->
                    DropdownMenuItem(
                        text = { Text(c) },
                        onClick = {
                            onChoose(c)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SplitSpinner(
    u1: String,
    u2: String,
    value: String,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val visualText = when {
        value == "50/50" -> "50/50 split"
        value == "$u1 owes full" -> "$u1 owes the full amount"
        value == "$u2 owes full" -> "$u2 owes the full amount"
        value == "custom" -> "Custom percentage…"
        else -> value
    }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, BorderStrong),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = visualText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CharcoalText
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Arrow",
                    tint = WarmGreyMuted
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceWhite)
        ) {
            DropdownMenuItem(
                text = { Text("50/50 split") },
                onClick = {
                    onChoose("50/50")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("$u1 owes full") },
                onClick = {
                    onChoose("$u1 owes full")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("$u2 owes full") },
                onClick = {
                    onChoose("$u2 owes full")
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Custom percentage…") },
                onClick = {
                    onChoose("custom")
                    expanded = false
                }
            )
        }
    }
}

// Helpers
fun getCurrentDateISO(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
}

fun fmtAmount(n: Double): String {
    val r = kotlin.math.round(n)
    val absVal = kotlin.math.abs(r)
    return "₹" + String.format(Locale("en", "IN"), "%,.0f", absVal)
}

fun fmtCompact(n: Double): String {
    val r = kotlin.math.round(n).toLong()
    return when {
        r == 0L -> ""
        r >= 100000L -> String.format(Locale.US, "%.1fL", r / 100000.0).replace(".0", "")
        r >= 10000L -> "${kotlin.math.round(r / 1000.0).toLong()}K"
        r >= 1000L -> String.format(Locale.US, "%.1fK", r / 1000.0).replace(".0", "")
        else -> r.toString()
    }
}

fun calculateBucketTotals(
    expenses: List<Expense>,
    monthKey: String,
    viewModel: ExpenseViewModel
): Map<String, Double> {
    val totals = mutableMapOf("Life Expenses" to 0.0, "Travel" to 0.0, "JUNA" to 0.0)
    expenses.forEach { e ->
        val m = e.date.substring(0, 7)
        val b = viewModel.bucketFor(e.category)
        if (monthKey == "all" || m == monthKey) {
            totals[b] = (totals[b] ?: 0.0) + e.amount
        }
    }
    return totals
}

fun getTrendTextForBucket(
    expenses: List<Expense>,
    monthKey: String,
    bucket: String,
    viewModel: ExpenseViewModel
): String {
    if (monthKey == "all") return ""
    val pm = viewModel.previousMonthOf(monthKey) ?: return ""
    
    val thisMonthAmt = expenses.filter {
        it.date.substring(0, 7) == monthKey && viewModel.bucketFor(it.category) == bucket
    }.sumOf { it.amount }

    val lastMonthAmt = expenses.filter {
        it.date.substring(0, 7) == pm && viewModel.bucketFor(it.category) == bucket
    }.sumOf { it.amount }

    return when {
        lastMonthAmt == 0.0 && thisMonthAmt == 0.0 -> "—"
        lastMonthAmt == 0.0 -> "New"
        else -> {
            val pct = kotlin.math.round(((thisMonthAmt - lastMonthAmt) / lastMonthAmt) * 100).toInt()
            if (pct > 0) "↑ $pct% vs last"
            else if (pct < 0) "↓ ${kotlin.math.abs(pct)}% vs last"
            else "— same as last"
        }
    }
}

fun calculateBalanceChangeValue(expense: Expense, u1: String, u2: String): Double {
    if (expense.split == "Personal") return 0.0
    val isU1Payer = expense.paidBy == u1
    return when {
        expense.split == "50/50" -> {
            if (isU1Payer) expense.amount / 2.0 else -expense.amount / 2.0
        }
        expense.split == "$u1 owes full" -> {
            if (isU1Payer) 0.0 else -expense.amount
        }
        expense.split == "$u2 owes full" -> {
            if (isU1Payer) expense.amount else 0.0
        }
        expense.split.startsWith("Custom") -> {
            val pct = expense.split.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            val change = expense.amount * (pct / 100.0)
            if (isU1Payer) change else -change
        }
        else -> 0.0
    }
}

fun filterExpenses(
    expenses: List<Expense>,
    monthKey: String,
    categoryKey: String,
    searchQuery: String,
    viewModel: ExpenseViewModel
): List<Expense> {
    var list = expenses

    if (monthKey != "all") {
        list = list.filter { it.date.substring(0, 7) == monthKey }
    }

    if (categoryKey == "personal") {
        list = list.filter { it.split == "Personal" }
    } else if (categoryKey == "shared") {
        list = list.filter { it.split != "Personal" }
    } else if (categoryKey.startsWith("bucket:")) {
        val b = categoryKey.substring(7)
        list = list.filter { viewModel.bucketFor(it.category) == b }
    } else if (categoryKey != "all") {
        list = list.filter { it.category == categoryKey }
    }

    if (searchQuery.isNotEmpty()) {
        val q = searchQuery.trim().lowercase()
        list = list.filter {
            it.description.lowercase().contains(q) || (it.tag?.lowercase()?.contains(q) ?: false)
        }
    }

    return list.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
}
