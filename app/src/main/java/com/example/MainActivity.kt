package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.data.ExpenseViewModel
import com.example.ui.DashboardScreen
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.CharcoalText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SageLife
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.WarmGreyMuted

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val repository = remember(context) {
                    ExpenseRepository(AppDatabase.getDatabase(context.applicationContext).expenseDao())
                }
                val appViewModel: ExpenseViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ExpenseViewModel(repository) as T
                        }
                    }
                )

                val setupProfile by appViewModel.setupProfile.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        val profile = setupProfile
                        if (profile == null || !profile.isSetup) {
                            SetupScreen(viewModel = appViewModel)
                        } else {
                            DashboardScreen(viewModel = appViewModel, profile = profile)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupScreen(viewModel: ExpenseViewModel) {
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
            colors = setupFieldColors(),
            singleLine = true
        )

        Text(
            text = "PARTNER'S NAME",
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
            colors = setupFieldColors(),
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
private fun setupFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = SurfaceWhite,
    unfocusedContainerColor = SurfaceWhite,
    disabledContainerColor = SurfaceWhite,
    focusedIndicatorColor = CharcoalText,
    unfocusedIndicatorColor = BorderStrong,
    cursorColor = CharcoalText
)
