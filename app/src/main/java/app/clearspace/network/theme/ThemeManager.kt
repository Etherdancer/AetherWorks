package app.clearspace.network.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(context: Context) : SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs: SharedPreferences = context.getSharedPreferences("aether_settings", Context.MODE_PRIVATE)
    
    private val _theme = MutableStateFlow(getSavedTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    init {
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit().putString("app_theme", theme.name).apply()
    }

    private fun getSavedTheme(): AppTheme {
        val saved = prefs.getString("app_theme", AppTheme.DEFAULT.name) ?: AppTheme.DEFAULT.name
        return try {
            AppTheme.valueOf(saved)
        } catch (e: Exception) {
            AppTheme.DEFAULT
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "app_theme") {
            _theme.value = getSavedTheme()
        }
    }
}
