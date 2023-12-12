package com.example.notificationtimer


import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DropDownViewModel(private val sharedPreferences: SharedPreferences) : ViewModel() {
    var selectedText = mutableStateOf("Хунзах")
    var currentLocation = mutableStateOf(
        sharedPreferences.getString("location", "Хунзах") ?: "Хунзах"
    )

    init {
        selectedText.value = sharedPreferences.getString("location", "Хунзах") ?: "Хунзах"
    }

    fun saveLocation(location: String) {
        sharedPreferences.edit().putString("location", location).apply()
        currentLocation.value = location
    }
}
class DropDownViewModelFactory(private val sharedPreferences: SharedPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DropDownViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DropDownViewModel(sharedPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
