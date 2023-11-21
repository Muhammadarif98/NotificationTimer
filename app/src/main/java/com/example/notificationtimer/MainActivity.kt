package com.example.notificationtimer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val switchInfo = listOf(
        SwitchInfo("switch1", "Заголовок 1", "Описание 1",8),
        SwitchInfo("switch2", "Заголовок 2", "Описание 2",7),
        SwitchInfo("switch3", "Заголовок 3", "Описание 3",6)
    )

    Scaffold(modifier = Modifier.fillMaxSize()) {
        LazyColumn {
            itemsIndexed(switchInfo) { index, info ->
                val switchState = remember { mutableStateOf(getSwitchState(context, info.switchKey)) }
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
                                if(isChecked){
                                    sendNotification(context, info.title, info.description,info.time)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

data class SwitchInfo(val switchKey: String,
                      val title: String,
                      val description: String,
                      val time: Int)

fun getSwitchState(context: Context, switchKey: String): Boolean {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPreferences.getBoolean(switchKey, false)
}

fun saveSwitchState(context: Context, switchKey: String, state: Boolean) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPreferences.edit().putBoolean(switchKey, state).apply()
}
fun sendNotification(context: Context, titleKey: String,descriptionKey: String,delayInSeconds: Int) {
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

    val calendar = Calendar.getInstance()
    val time = "22:20"
    val hour = time.split(":")[0].toInt()
    val minute = time.split(":")[1].toInt()
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)

    val databaseTime = calendar.timeInMillis - (delayInSeconds) * 60 * 1000

    calendar.timeInMillis = databaseTime
    calendar.add(Calendar.SECOND, delayInSeconds)
    alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
    )

}

/*
    val timer = Timer()
    timer.schedule(object : TimerTask() {
        override fun run() {

        }
    }, 0, 5000) // Задача будет повторяться каждые 5 секунд
}*/


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainScreen()
}

const val PREFS_NAME = "com.example.app.PREFS"
