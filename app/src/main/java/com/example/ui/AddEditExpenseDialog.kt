package com.example.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.outlinedButtonColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CATEGORIES
import com.example.data.Expense
import com.example.data.ExpenseViewModel
import com.example.data.bucketFor
import com.example.data.getCurrentDateISO
import com.example.data.getMRUPending
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.ClayNegative
import com.example.ui.theme.CreamBg
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmGreyFaint
import com.example.ui.theme.WarmGreyMuted
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    // Prefill new entries with the most-recent tag so multi-day trips don't
    // require retyping (e.g. "Goa Dec 2025" carries across all line items).
    var tag by remember {
        mutableStateOf(expense?.tag ?: expenses.firstOrNull()?.tag.orEmpty())
    }
    var paidBy by remember { mutableStateOf(expense?.paidBy ?: user1) }

    var splitVal by remember {
        mutableStateOf(
            when {
                activeType == "personal" -> "Personal"
                expense?.split?.startsWith("Custom") == true -> "custom"
                else -> expense?.split ?: "50/50"
            }
        )
    }

    var customPct by remember {
        mutableStateOf(
            if (expense?.split?.startsWith("Custom") == true) {
                expense.split.filter { it.isDigit() }
            } else ""
        )
    }

    val mruCategories = remember(expenses) { getMRUPending(expenses) }
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

                FieldLabel("TYPE")
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
                            splitVal = when {
                                bucketFor(category) == "JUNA" -> "$user2 owes full"
                                splitVal == "Personal" -> "50/50"
                                else -> splitVal
                            }
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

                FieldLabel("AMOUNT (₹)")
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
                    colors = formFieldColors(),
                    singleLine = true
                )

                FieldLabel("DESCRIPTION")
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors(),
                    singleLine = true
                )

                FieldLabel("DATE")
                OutlinedCard(
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        runCatching { sdf.parse(date)?.let { calendar.time = it } }

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
                        Text(text = date, style = MaterialTheme.typography.bodyLarge, color = CharcoalText)
                    }
                }

                FieldLabel("CATEGORY")
                CategorySpinner(
                    category = category,
                    mruList = mruCategories,
                    onChoose = { selected ->
                        category = selected
                        if (bucketFor(selected) == "JUNA" && activeType == "shared") {
                            splitVal = "$user2 owes full"
                        }
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                FieldLabel("TAG (OPTIONAL)")
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    placeholder = { Text("e.g. Goa Dec 2025") },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = CharcoalText),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = formFieldColors(),
                    singleLine = true
                )

                FieldLabel("PAID BY")
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

                if (activeType == "shared") {
                    FieldLabel("SPLIT")
                    SplitSpinner(
                        u1 = user1,
                        u2 = user2,
                        value = splitVal,
                        onChoose = { splitVal = it },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (splitVal == "custom") {
                        FieldLabel("CUSTOM PERCENTAGE (% NON-PAYER OWES)")
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
                            colors = formFieldColors(),
                            singleLine = true
                        )
                    }
                }

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

                        val splitFinal = when {
                            activeType == "personal" -> "Personal"
                            splitVal == "custom" -> {
                                val pct = customPct.toIntOrNull()
                                if (pct == null || pct < 1 || pct > 99) {
                                    Toast.makeText(localContext, "Enter custom percentage values 1-99", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                "Custom $pct%"
                            }
                            else -> splitVal
                        }

                        val resultHandler: (Boolean) -> Unit = { ok ->
                            val msg = if (ok) {
                                if (expense == null) "Added!" else "Updated!"
                            } else "Couldn't save"
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
                    Text(text = "Save", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
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
                        colors = outlinedButtonColors(contentColor = ClayNegative)
                    ) {
                        Text(text = "Delete", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = WarmGreyMuted,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun formFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    focusedIndicatorColor = CharcoalText,
    unfocusedIndicatorColor = BorderStrong,
    cursorColor = CharcoalText
)

@Composable
internal fun ToggleOptionButton(
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
private fun CategorySpinner(
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

            CATEGORIES.forEach { (bucket, cats) ->
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
private fun SplitSpinner(
    u1: String,
    u2: String,
    value: String,
    onChoose: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val visualText = when (value) {
        "50/50" -> "50/50 split"
        "$u1 owes full" -> "$u1 owes the full amount"
        "$u2 owes full" -> "$u2 owes the full amount"
        "custom" -> "Custom percentage…"
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
            DropdownMenuItem(text = { Text("50/50 split") }, onClick = {
                onChoose("50/50"); expanded = false
            })
            DropdownMenuItem(text = { Text("$u1 owes full") }, onClick = {
                onChoose("$u1 owes full"); expanded = false
            })
            DropdownMenuItem(text = { Text("$u2 owes full") }, onClick = {
                onChoose("$u2 owes full"); expanded = false
            })
            DropdownMenuItem(text = { Text("Custom percentage…") }, onClick = {
                onChoose("custom"); expanded = false
            })
        }
    }
}
