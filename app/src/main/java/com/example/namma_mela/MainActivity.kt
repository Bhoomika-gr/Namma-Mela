package com.example.namma_mela

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.bumptech.glide.integration.compose.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDatabase.getDatabase(this).appDao()
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)

        setContent {
            var currentUser by remember { mutableStateOf<User?>(null) }
            var isLoading by remember { mutableStateOf(true) }

            // AUTO-LOGIN LOGIC
            LaunchedEffect(Unit) {
                val savedUsername = sharedPref.getString("logged_username", null)
                if (savedUsername != null) {
                    currentUser = dao.getUser(savedUsername)
                }
                isLoading = false
            }

            if (!isLoading) {
                val primaryColor = Color(currentUser?.themeColor ?: 0xFF2ECC71.toInt())
                val isDarkTheme = when (currentUser?.themeMode ?: "System") {
                    "Dark" -> true
                    "Light" -> false
                    else -> isSystemInDarkTheme()
                }

                MaterialTheme(
                    colorScheme = if (isDarkTheme) {
                        darkColorScheme(primary = primaryColor, onSurface = Color.White, onBackground = Color.White)
                    } else {
                        lightColorScheme(primary = primaryColor)
                    }
                ) {
                    Surface(color = MaterialTheme.colorScheme.background) {
                        val navController = rememberNavController()
                        NavHost(
                            navController = navController,
                            startDestination = if (currentUser == null) "login" else "home"
                        ) {
                            composable("login") {
                                LoginScreen(dao, navController) { user ->
                                    currentUser = user
                                    sharedPref.edit().putString("logged_username", user.username).apply()
                                }
                            }
                            composable("home") { HomeScreen(navController) }
                            composable("booking") { BookingScreen(dao, navController, currentUser) }
                            composable("fanwall") { FanWallScreen(dao, navController, currentUser) }
                            composable("profile") {
                                ProfileScreen(dao, navController, currentUser,
                                    onUpdate = { currentUser = it },
                                    onLogout = {
                                        currentUser = null
                                        sharedPref.edit().remove("logged_username").apply()
                                        navController.navigate("login") { popUpTo(0) }
                                    })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoginScreen(dao: AppDao, nav: NavHostController, onLoginSuccess: (User) -> Unit) {
    var userText by remember { mutableStateOf("") }
    var passText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Namma Mela", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Traditional Drama Portal", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(userText, { userText = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(passText, { passText = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (userText.isBlank() || passText.isBlank()) return@Button
            scope.launch {
                val existing = dao.getUser(userText)
                if (existing != null) {
                    if (existing.password == passText) {
                        onLoginSuccess(existing)
                        nav.navigate("home") { popUpTo("login") { inclusive = true } }
                    } else Toast.makeText(context, "Wrong Password", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(userText, passText)
                    dao.registerUser(newUser)
                    onLoginSuccess(newUser)
                    nav.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            }
        }, Modifier.fillMaxWidth()) { Text("Login / Sign Up") }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen(nav: NavHostController) {
    val cast = listOf(
        Triple("Manjunath (Shiva)", "https://i.pravatar.cc/150?u=10", "Veteran of Shiva Leele."),
        Triple("Basavaraj (Narada)", "https://i.pravatar.cc/150?u=11", "Expert comedian."),
        Triple("Vijay (Arjuna)", "https://i.pravatar.cc/150?u=12", "Traditional Folk Dancer.")
    )
    Scaffold(bottomBar = { BottomNav(nav) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            item {
                Text("Tonight: Shiva Leele", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Card(Modifier.fillMaxWidth().height(220.dp).padding(vertical = 12.dp)) {
                    GlideImage(model = "https://images.unsplash.com/photo-1514302240736-b1fee5989260?w=500", contentDescription = null, contentScale = ContentScale.Crop)
                }
                Text("Description", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Text("A grand portrayal of Lord Shiva's divine miracles.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Spacer(Modifier.height(24.dp))
                Text("Casts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            items(cast) { (name, img, desc) ->
                Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    GlideImage(model = img, contentDescription = null, Modifier.size(60.dp).clip(CircleShape))
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
fun BookingScreen(dao: AppDao, nav: NavHostController, user: User?) {
    val bookings by dao.getAllBookings().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val preReservedSeats = listOf(3, 7, 10, 14, 21, 25)
    var seatToConfirm by remember { mutableStateOf<Int?>(null) }

    if (seatToConfirm != null) {
        AlertDialog(
            onDismissRequest = { seatToConfirm = null },
            title = { Text("Confirm Booking") },
            text = { Text("Reserve seat #$seatToConfirm for Tonight?") },
            confirmButton = {
                TextButton(onClick = {
                    if (user != null) {
                        scope.launch {
                            dao.addBooking(Booking(username = user.username, seatNumber = seatToConfirm!!, playName = "Shiva Leele"))
                            seatToConfirm = null
                        }
                    }
                }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { seatToConfirm = null }) { Text("Cancel") } }
        )
    }

    Scaffold(bottomBar = { BottomNav(nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Select a Seat", style = MaterialTheme.typography.headlineSmall)
            Text("Emerald: Available | Red: Reserved", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.fillMaxSize()) {
                items(30) { index ->
                    val seatNum = index + 1
                    val isReserved = bookings.any { it.seatNumber == seatNum } || preReservedSeats.contains(seatNum)
                    Button(
                        onClick = { if (!isReserved) seatToConfirm = seatNum },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isReserved) Color.Red else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(4.dp).height(50.dp)
                    ) { Text("$seatNum", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun FanWallScreen(dao: AppDao, nav: NavHostController, user: User?) {
    val comments by dao.getComments().collectAsState(initial = emptyList())
    var msg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(bottomBar = { BottomNav(nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Fan Wall", style = MaterialTheme.typography.headlineSmall)
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(msg, { msg = it }, placeholder = { Text("Applaud here...") }, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    if (msg.isNotBlank() && user != null) {
                        scope.launch { dao.addComment(FanWallComment(author = user.username, comment = msg)); msg = "" }
                    }
                }) { Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary) }
            }
            LazyColumn {
                items(comments) { c ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(8.dp)) {
                            Text(c.author, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(c.comment)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(dao: AppDao, nav: NavHostController, user: User?, onUpdate: (User) -> Unit, onLogout: () -> Unit) {
    if (user == null) return
    val scope = rememberCoroutineScope()
    val history by dao.getHistory(user.username).collectAsState(initial = emptyList())
    var langExpanded by remember { mutableStateOf(false) }
    var themeExpanded by remember { mutableStateOf(false) }

    Scaffold(bottomBar = { BottomNav(nav) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Profile: ${user.username}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))

            // Language Selection
            ExposedDropdownMenuBox(expanded = langExpanded, onExpandedChange = { langExpanded = !langExpanded }) {
                OutlinedTextField(value = user.preferredLanguage, onValueChange = {}, readOnly = true, label = { Text("Language") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(langExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                    listOf("English", "Kannada", "Hindi").forEach { lang ->
                        DropdownMenuItem(text = { Text(lang) }, onClick = {
                            scope.launch {
                                val u = user.copy(preferredLanguage = lang)
                                dao.updateUser(u); onUpdate(u)
                            }
                            langExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Theme Mode
            ExposedDropdownMenuBox(expanded = themeExpanded, onExpandedChange = { themeExpanded = !themeExpanded }) {
                OutlinedTextField(value = user.themeMode, onValueChange = {}, readOnly = true, label = { Text("Theme Mode") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(themeExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                    listOf("Light", "Dark", "System").forEach { mode ->
                        DropdownMenuItem(text = { Text(mode) }, onClick = {
                            scope.launch {
                                val u = user.copy(themeMode = mode)
                                dao.updateUser(u); onUpdate(u)
                            }
                            themeExpanded = false
                        })
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Text("Booking History", fontWeight = FontWeight.Bold)
            history.forEach { b ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Seat ${b.seatNumber} - ${b.playName}")
                    IconButton(onClick = { scope.launch { dao.deleteBooking(b) } }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                }
            }
            Spacer(Modifier.height(40.dp))
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.fillMaxWidth()) { Text("Logout", color = Color.White) }
        }
    }
}

@Composable
fun BottomNav(nav: NavHostController) {
    NavigationBar {
        NavigationBarItem(selected = false, onClick = { nav.navigate("home") }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
        NavigationBarItem(selected = false, onClick = { nav.navigate("booking") }, icon = { Icon(Icons.Default.ShoppingCart, null) }, label = { Text("Seats") })
        NavigationBarItem(selected = false, onClick = { nav.navigate("fanwall") }, icon = { Icon(Icons.Default.Favorite, null) }, label = { Text("Wall") })
        NavigationBarItem(selected = false, onClick = { nav.navigate("profile") }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
    }
}