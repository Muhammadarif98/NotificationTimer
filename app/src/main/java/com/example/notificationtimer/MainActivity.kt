package com.example.notificationtimer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notificationtimer.dataTimes.datesKhunzakhHatmu
import com.example.notificationtimer.dataTimes.datesKhunzakhSalawat
import com.example.notificationtimer.dataTimes.datesMakhachkalaHatmu
import com.example.notificationtimer.dataTimes.datesMakhachkalaSalawat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            MainScreen()
        }
    }

    override fun onStop() {
        super.onStop()
        val intent = Intent(this, NotificationService::class.java)
        startService(intent)
    }

}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val factory = DropDownViewModelFactory(sharedPreferences)
    val mainViewModel: DropDownViewModel = viewModel(factory = factory)


    val switchInfo = listOf(
        SwitchInfo("switch1", "Салават", "15 мин до Салавата", 15),
        SwitchInfo("switch1", "Салават", "30 мин до Салавата", 30),
        SwitchInfo("switch6", "Хатму", "15 мин до Хатму", 15),
        SwitchInfo("switch6", "Хатму", "30 мин до Хатму", 30),
    )

    Scaffold(modifier = Modifier.fillMaxSize()) {

        LazyColumn {
            item {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {

                    ShowTime(mainViewModel)
                }
            }
            itemsIndexed(switchInfo) { index, info ->
                val switchState =
                    remember { mutableStateOf(getSwitchState(context, info.switchKey)) }
                Card(modifier = Modifier.padding(8.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = info.title)
                        Text(text = info.description)
                        Switch(
                            checked = switchState.value,
                            onCheckedChange = { isChecked ->
                                switchState.value = isChecked
                                saveSwitchState(context, info.switchKey, isChecked)
                                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                                if (isChecked) {
                                    if (dayOfWeek == Calendar.THURSDAY && info.title == "Салават") {
                                        sendNotification(
                                            context,
                                            info.title,
                                            info.description,
                                            info.time,
                                            mainViewModel.currentLocation.value,
                                            sharedPreferences
                                        )
                                        mainViewModel.saveLocation(mainViewModel.currentLocation.value)
                                        sharedPreferences.edit().putString(
                                            "location",
                                            mainViewModel.currentLocation.value
                                        ).apply()
                                    } else if (dayOfWeek == Calendar.SUNDAY && info.title == "Хатму") {
                                        sendNotification(
                                            context,
                                            info.title,
                                            info.description,
                                            info.time,
                                            mainViewModel.currentLocation.value,
                                            sharedPreferences
                                        )
                                        mainViewModel.saveLocation(mainViewModel.currentLocation.value)
                                        sharedPreferences.edit().putString(
                                            "location",
                                            mainViewModel.currentLocation.value
                                        ).apply()
                                    }
                                }
                            }
                        )

                    }
                }
            }
        }
        DropdownSample(mainViewModel)
    }
}

data class SwitchInfo(
    val switchKey: String,
    val title: String,
    val description: String,
    val time: Int
)

fun getSwitchState(context: Context, switchKey: String): Boolean {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean(switchKey, false)
}

fun saveSwitchState(context: Context, switchKey: String, state: Boolean) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPreferences.edit().putBoolean(switchKey, state).apply()
}

fun saveSelectedText(sharedPreferences: SharedPreferences, selectedText: String) {
    with(sharedPreferences.edit()) {
        putString("selectedText", selectedText)
        apply()
    }
}

fun sendNotification(
    context: Context,
    titleKey: String,
    descriptionKey: String,
    delayInSeconds: Int,
    location: String,
    sharedPreferences: SharedPreferences
) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, NotificationReceiver::class.java).apply {
        putExtra("titleKey", titleKey)
        putExtra("descriptionKey", descriptionKey)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        titleKey.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
    val today = sdf.format(Date())
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    var timeForToday: String? = null

    if (location == "Хунзах") {
        if (dayOfWeek == Calendar.THURSDAY) {
            timeForToday = datesKhunzakhSalawat[today]
        } else if (dayOfWeek == Calendar.SUNDAY) {
            timeForToday = datesKhunzakhHatmu[today]
        }
        sharedPreferences.edit().putString("location", "Хунзах").apply()
    } else if (location == "Махачкала") {
        if (dayOfWeek == Calendar.THURSDAY) {
            timeForToday = datesMakhachkalaSalawat[today]
        } else if (dayOfWeek == Calendar.SUNDAY) {
            timeForToday = datesMakhachkalaHatmu[today]
        }
        sharedPreferences.edit().putString("location", "Махачкала").apply()
    }


    if (timeForToday != null) {
        val calendar = Calendar.getInstance()
        val hour = timeForToday.split(":")[0].toInt()
        val minute = timeForToday.split(":")[1].toInt()

        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)

        // Уведомление за delayInSeconds минут до заданного времени
        calendar.add(Calendar.MINUTE, -delayInSeconds)



        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
}


@Composable
fun ShowTime(mainViewModel: DropDownViewModel) {
    val location = mainViewModel.currentLocation.value
    val sdf = SimpleDateFormat("d MMMM", Locale("ru"))
    val today = sdf.format(Date())
    val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    var timeForToday: String? = null

    mainViewModel.saveLocation(mainViewModel.currentLocation.value)




    if (location == "Хунзах") {
        if (dayOfWeek == Calendar.THURSDAY) {
            timeForToday = datesKhunzakhSalawat[today]
        } else if (dayOfWeek == Calendar.SUNDAY) {
            timeForToday = datesKhunzakhHatmu[today]
        }
    } else if (location == "Махачкала") {
        if (dayOfWeek == Calendar.THURSDAY) {
            timeForToday = datesMakhachkalaSalawat[today]
        } else if (dayOfWeek == Calendar.SUNDAY) {
            timeForToday = datesMakhachkalaHatmu[today]
        }
    }

    if (timeForToday != null) {
        AnimateContent(shortText = "В $timeForToday", longText = " Сегодня в $timeForToday")
    } else {
        AnimateContent(shortText = "Сегодня нет", longText = "Сегодня $today, нет Салават и Хатму")
    }
}

@Composable
private fun AnimateContent(shortText: String, longText: String) {
    //  val shortText = stringResource(id = R.string.short_text)
    //  val longText = stringResource(id = R.string.long_text)
    var short by remember { mutableStateOf(true) }
    val st1 = Color(0xFFF4DBAD)
    val st2 = Color(0xFFFDFBCC)
    val st3 = Color(0xFFF9EBBD)
    val st4 = Color(0xFFF4DBAD)

    Box(
        modifier = Modifier
            .background(Color(0xFFF1E4D1), RoundedCornerShape(50.dp))
            .border(
                7.dp, brush = Brush.verticalGradient(
                    colors = listOf(
                        st1,
                        st2,
                        st3,
                        st4,
                    )
                ), shape = RoundedCornerShape(50.dp)
            )
            .clickable { short = !short }
            .padding(start = 25.dp, top = 10.dp, bottom = 10.dp, end = 25.dp)
            .wrapContentSize()
            .animateContentSize(
                tween(1000)
            ),

        ) {
        Text(
            if (short) {
                shortText
            } else {
                longText
            },

            softWrap = true,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = Color(0xFF604D2E),
                fontSize = 15.sp
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}

const val PREFS_NAME = "com.example.app.PREFS"

@Composable
fun DropdownSample(mainViewModel: DropDownViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("PREFERENCES_NAME", Context.MODE_PRIVATE)

    // Загрузка сохраненного значения при старте
    LaunchedEffect(Unit) {
        mainViewModel.selectedText.value =
            sharedPreferences.getString("selectedText", "Хунзах") ?: "Хунзах"
    }

    DropdownMenuSample(mainViewModel, sharedPreferences)
}


@Composable
fun DropdownMenuSample(mainViewModel: DropDownViewModel, sharedPreferences: SharedPreferences) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    LazyColumn() {
        item {
            Column {
                Text(
                    mainViewModel.selectedText.value,
                    modifier = Modifier.clickable { expanded = true },
                    color = Color(0xFF604D2E),
                    textDecoration = TextDecoration.Underline,
                    style = MaterialTheme.typography.bodyLarge
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Хунзах", color = Color(0xFF604D2E),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            mainViewModel.selectedText.value = "Хунзах"

                            mainViewModel.currentLocation.value = "Хунзах"
                            saveSelectedText(sharedPreferences, "Хунзах")
                            expanded = false
                            scope.launch {
                                Toast.makeText(
                                    context,
                                    "Выбрана локация Хунзах",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        })
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Махачкала", color = Color(0xFF604D2E),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        onClick = {
                            mainViewModel.selectedText.value = "Махачкала"
                            mainViewModel.currentLocation.value = "Махачкала"
                            saveSelectedText(sharedPreferences, "Махачкала")
                            expanded = false
                            scope.launch {
                                Toast.makeText(
                                    context,
                                    "Выбрана локация Махачкала",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        })
                }
            }
        }
    }
}
//                DropdownMenuItem(onClick = {
//                    mainViewModel.selectedText.value = "Хунзах"
//                    mainViewModel.currentLocation.value = "Хунзах"
//                    saveSelectedText(sharedPreferences, "Хунзах")
//                    expanded = false
//                    scope.launch {
//                        Toast.makeText(context, "Выбрана локация Хунзах", Toast.LENGTH_SHORT).show()
//                    }
//                }) {
//                    Text("Хунзах",color = Color(0xFF604D2E),style = MaterialTheme.typography.bodyLarge)
//                }
//                DropdownMenuItem(onClick = {
//                    mainViewModel.selectedText.value = "Махачкала"
//                    mainViewModel.currentLocation.value = "Махачкала"
//                    saveSelectedText(sharedPreferences, "Махачкала")
//                    expanded = false
//                    scope.launch {
//                        Toast.makeText(context, "Выбрана локация Махачкала", Toast.LENGTH_SHORT).show()
//                    }
//                }) {
//                    Text("Махачкала",color = Color(0xFF604D2E),style = MaterialTheme.typography.bodyLarge)
//                }
//            }