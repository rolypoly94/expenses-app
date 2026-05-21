package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CATEGORIES
import com.example.data.Expense
import com.example.data.ExpenseViewModel
import com.example.data.SetupProfile
import com.example.data.bucketFor
import com.example.data.calculateBalanceChange
import com.example.data.calculateBucketTotals
import com.example.data.filterExpenses
import com.example.data.fmtAmount
import com.example.data.fmtCompact
import com.example.data.formatDateShort
import com.example.data.formatMonthLabel
import com.example.data.getTrendMonths
import com.example.data.getTrendTextForBucket
import com.example.data.thisMonthKey
import com.example.ui.theme.BorderLight
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.ClayNegative
import com.example.ui.theme.CreamBg
import com.example.ui.theme.DeepGreenOnAccent
import com.example.ui.theme.ForestPositive
import com.example.ui.theme.GoldTravel
import com.example.ui.theme.RoseJUNA
import com.example.ui.theme.SageLife
import com.example.ui.theme.SoftGreenAccent
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TonalCardBg
import com.example.ui.theme.WarmGreyFaint
import com.example.ui.theme.WarmGreyMuted

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

    // Cache the heavy per-frame calculations so a typing keystroke in the
    // search bar doesn't re-walk every expense for buckets + trends + travel.
    val totals = remember(expenses, selectedMonth) {
        calculateBucketTotals(expenses, selectedMonth)
    }
    val lifeTrend = remember(expenses, selectedMonth) {
        getTrendTextForBucket(expenses, selectedMonth, "Life Expenses")
    }
    val travelTrend = remember(expenses, selectedMonth) {
        getTrendTextForBucket(expenses, selectedMonth, "Travel")
    }
    val junaTrend = remember(expenses, selectedMonth) {
        getTrendTextForBucket(expenses, selectedMonth, "JUNA")
    }
    val travelExpensesInMonth = remember(expenses, selectedMonth) {
        expenses.filter {
            (selectedMonth == "all" || it.date.substring(0, 7) == selectedMonth) &&
                bucketFor(it.category) == "Travel"
        }
    }
    val filteredList = remember(expenses, selectedMonth, selectedCategory, searchQuery) {
        filterExpenses(expenses, selectedMonth, selectedCategory, searchQuery)
    }

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

            // Running balance
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
                    val metaText = if (kotlin.math.abs(runningBalance) < 0.5) {
                        "No outstanding balance."
                    } else {
                        "Across $unsettledCount unsettled transactions."
                    }

                    Text(
                        text = directionText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarmGreyMuted,
                        modifier = Modifier.padding(bottom = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = fmtAmount(runningBalance),
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

            // Monthly buckets heading
            Text(
                text = formatMonthLabel(selectedMonth),
                style = MaterialTheme.typography.labelMedium,
                color = WarmGreyMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BucketItem(
                    name = "Life",
                    amount = totals["Life Expenses"] ?: 0.0,
                    trendColor = SageLife,
                    trendText = lifeTrend,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectedCategory.value = "bucket:Life Expenses" }
                )
                BucketItem(
                    name = "Travel",
                    amount = totals["Travel"] ?: 0.0,
                    trendColor = GoldTravel,
                    trendText = travelTrend,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectedCategory.value = "bucket:Travel" }
                )
                BucketItem(
                    name = "JUNA",
                    amount = totals["JUNA"] ?: 0.0,
                    trendColor = RoseJUNA,
                    trendText = junaTrend,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.selectedCategory.value = "bucket:JUNA" }
                )
            }

            Text(
                text = "Monthly trends",
                style = MaterialTheme.typography.labelMedium,
                color = WarmGreyMuted,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )

            TrendsCard(expenses = expenses)

            if (travelExpensesInMonth.isNotEmpty()) {
                TravelSnapshotCard(
                    travelExpenses = travelExpensesInMonth,
                    selectedMonthLabel = formatMonthLabel(selectedMonth)
                )
            }

            Text(
                text = "Transactions",
                style = MaterialTheme.typography.titleLarge,
                color = CharcoalText,
                modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp)
            )

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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MONTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGreyFaint,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    MonthFilterDropdown(viewModel = viewModel, currentMonth = selectedMonth)
                }
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
                                text = if (searchQuery.isNotEmpty())
                                    "No matches for \"$searchQuery\""
                                else
                                    "No expenses match these filters.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = WarmGreyMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(72.dp)) // FAB buffer
        }

        // Sticky FAB
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
private fun BucketItem(
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
private fun TrendsCard(expenses: List<Expense>) {
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
            buckets.forEachIndexed { index, (bucket, barColor, _) ->
                val trendMonths = remember(expenses, bucket) { getTrendMonths(expenses, bucket) }
                val maxVal = trendMonths.maxOfOrNull { it.amount } ?: 1.0
                val safeMax = if (maxVal == 0.0) 1.0 else maxVal
                val avgVal = if (trendMonths.isNotEmpty())
                    trendMonths.sumOf { it.amount } / trendMonths.size
                else 0.0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = if (index < buckets.size - 1) 18.dp else 0.dp)
                ) {
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(78.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        trendMonths.forEach { trendMonth ->
                            val heightPercent = (trendMonth.amount / safeMax).toFloat()
                            val adjustedHeightPercent =
                                if (trendMonth.amount > 0 && heightPercent < 0.05f) 0.05f else heightPercent

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Bottom,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
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
private fun TravelSnapshotCard(travelExpenses: List<Expense>, selectedMonthLabel: String) {
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
                    text = "Total · ${fmtAmount(totalSum)} across ${travelExpenses.size} " +
                        if (travelExpenses.size == 1) "transaction" else "transactions",
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
private fun TransactionRow(
    expense: Expense,
    u1: String,
    u2: String,
    viewModel: ExpenseViewModel,
    onEditClick: () -> Unit
) {
    val isPersonal = expense.split == "Personal"
    val bucket = bucketFor(expense.category)
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
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )

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
                    text = "${formatDateShort(expense.date)} · $cleanCat · ${expense.paidBy} paid",
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

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = fmtAmount(expense.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = CharcoalText
            )

            val balanceValue = calculateBalanceChange(expense, u1, u2)
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
private fun MonthFilterDropdown(viewModel: ExpenseViewModel, currentMonth: String) {
    val expenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    val months = remember(expenses) {
        val list = expenses.map { it.date.substring(0, 7) }.toMutableSet()
        list.add(thisMonthKey())
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
                    text = formatMonthLabel(currentMonth),
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
                    text = { Text(formatMonthLabel(m)) },
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
private fun CategoryFilterDropdown(viewModel: ExpenseViewModel, currentCategory: String) {
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

            CATEGORIES.forEach { (bucket, cats) ->
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
