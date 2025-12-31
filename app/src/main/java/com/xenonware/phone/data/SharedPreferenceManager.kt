//package com.xenonware.phone.data
//
//import android.content.Context
//import android.content.SharedPreferences
//import androidx.appcompat.app.AppCompatDelegate
//import androidx.compose.ui.unit.IntSize
//import androidx.core.content.edit
//import com.xenonware.notes.viewmodel.NotesLayoutType
//import com.xenonware.notes.viewmodel.SortOption
//import com.xenonware.notes.viewmodel.SortOrder
//import com.xenonware.notes.viewmodel.ThemeSetting
//import com.xenonware.notes.viewmodel.classes.Label
//import com.xenonware.notes.viewmodel.classes.NotesItems
//import kotlinx.serialization.json.Json
//import kotlin.math.max
//import kotlin.math.min
//
//class SharedPreferenceManager(context: Context) {
//
//    private val prefsName = "AppPrefs"
//    private val themeKey = "app_theme"
//    private val coverThemeEnabledKey = "cover_theme_enabled"
//    private val coverDisplayDimension1Key = "cover_display_dimension_1"
//    private val coverDisplayDimension2Key = "cover_display_dimension_2"
//    private val sortOptionKey = "task_sort_option"
//    private val sortOrderKey = "task_sort_order"
//    private val listKey = "task_list_json"
//    private val labelsListKey = "labels_list_json"
//    private val blackedOutModeKey = "blacked_out_mode_enabled"
//    private val developerModeKey = "developer_mode_enabled"
//    private val layoutTypeKey = "notes_layout_type"
//    private val isUserLoggedInKey = "is_user_logged_in"
//
//
//    internal val sharedPreferences: SharedPreferences =
//        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
//
//    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
//
//    var theme: Int
//        get() = sharedPreferences.getInt(themeKey, ThemeSetting.SYSTEM.ordinal)
//        set(value) = sharedPreferences.edit { putInt(themeKey, value) }
//
//    val themeFlag: Array<Int> = arrayOf(
//        AppCompatDelegate.MODE_NIGHT_NO,
//        AppCompatDelegate.MODE_NIGHT_YES,
//        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
//    )
//
//    var coverThemeEnabled: Boolean
//        get() = sharedPreferences.getBoolean(coverThemeEnabledKey, false)
//        set(value) = sharedPreferences.edit { putBoolean(coverThemeEnabledKey, value) }
//
//    var coverDisplaySize: IntSize
//        get() {
//            val dim1 = sharedPreferences.getInt(coverDisplayDimension1Key, 0)
//            val dim2 = sharedPreferences.getInt(coverDisplayDimension2Key, 0)
//            return IntSize(dim1, dim2)
//        }
//        set(value) {
//            sharedPreferences.edit {
//                putInt(coverDisplayDimension1Key, min(value.width, value.height))
//                putInt(coverDisplayDimension2Key, max(value.width, value.height))
//            }
//        }
//
//    var blackedOutModeEnabled: Boolean
//        get() = sharedPreferences.getBoolean(blackedOutModeKey, false)
//        set(value) = sharedPreferences.edit { putBoolean(blackedOutModeKey, value) }
//
//    var developerModeEnabled: Boolean
//        get() = sharedPreferences.getBoolean(developerModeKey, false)
//        set(value) = sharedPreferences.edit { putBoolean(developerModeKey, value) }
//
//    var isUserLoggedIn: Boolean
//        get() = sharedPreferences.getBoolean(isUserLoggedInKey, false)
//        set(value) = sharedPreferences.edit { putBoolean(isUserLoggedInKey, value) }
//
//    var notesItems: List<NotesItems>
//        get() {
//            val jsonString = sharedPreferences.getString(listKey, null)
//            return if (jsonString != null) {
//                try {
//                    json.decodeFromString<List<NotesItems>>(jsonString)
//                } catch (e: Exception) {
//                    System.err.println("Error decoding task items, deleting old data: ${e.localizedMessage}")
//                    sharedPreferences.edit { remove(listKey) }
//                    emptyList()
//                }
//            } else {
//                emptyList()
//            }
//        }
//        set(value) {
//            try {
//                val jsonString = json.encodeToString(value)
//                sharedPreferences.edit { putString(listKey, jsonString) }
//            } catch (e: Exception) {
//                System.err.println("Error encoding task items: ${e.localizedMessage}")
//            }
//        }
//
//    var labels: List<Label>
//        get() {
//            val jsonString = sharedPreferences.getString(labelsListKey, null)
//            return if (jsonString != null) {
//                try {
//                    json.decodeFromString<List<Label>>(jsonString)
//                } catch (e: Exception) {
//                    System.err.println("Error decoding labels, deleting old data: ${e.localizedMessage}")
//                    sharedPreferences.edit { remove(labelsListKey) }
//                    emptyList()
//                }
//            } else {
//                emptyList()
//            }
//        }
//        set(value) {
//            try {
//                val jsonString = json.encodeToString(value)
//                sharedPreferences.edit { putString(labelsListKey, jsonString) }
//            } catch (e: Exception) {
//                System.err.println("Error encoding labels: ${e.localizedMessage}")
//            }
//        }
//
//    var sortOption: SortOption
//        get() {
//            val option = sharedPreferences.getString(sortOptionKey, null)
//            try {
//                if (option != null) return SortOption.valueOf(option)
//            } catch (_: Exception) {
//            }
//            return SortOption.FREE_SORTING
//        }
//        set(value) {
//            sharedPreferences.edit { putString(sortOptionKey, value.name) }
//        }
//
//    var sortOrder: SortOrder
//        get() {
//            val order = sharedPreferences.getString(sortOrderKey, null)
//            try {
//                if (order != null) return SortOrder.valueOf(order)
//            } catch (_: Exception) {
//            }
//            return SortOrder.ASCENDING
//        }
//        set(value) {
//            sharedPreferences.edit { putString(sortOrderKey, value.name) }
//        }
//    var notesLayoutType: NotesLayoutType
//        get() {
//            val type = sharedPreferences.getString(layoutTypeKey, null)
//            try {
//                if (type != null) return NotesLayoutType.valueOf(type)
//            } catch (_: Exception) {
//            }
//            return NotesLayoutType.LIST
//        }
//        set(value) {
//            sharedPreferences.edit { putString(layoutTypeKey, value.name) }
//        }
//
//    var smoothness: Float
//        get() = sharedPreferences.getFloat(lineSmoothnessKey, 0.1f)
//        set(value) = sharedPreferences.edit { putFloat(lineSmoothnessKey, value) }
//
//    var gridColumnCount: Int
//        get() = sharedPreferences.getInt(gridColumnCountKey, 2)
//        set(value) = sharedPreferences.edit { putInt(gridColumnCountKey, value) }
//
//    var listItemLineCount: Int
//        get() = sharedPreferences.getInt(listItemLineCountKey, 3)
//        set(value) = sharedPreferences.edit { putInt(listItemLineCountKey, value) }
//
//    var showLocalOnlyNotes: Boolean
//        get() = sharedPreferences.getBoolean("show_local_only_notes", false)
//        set(value) = sharedPreferences.edit { putBoolean("show_local_only_notes", value) }
//
//    fun isCoverThemeApplied(currentDisplaySize: IntSize): Boolean {
//        if (!coverThemeEnabled) return false
//        val storedDimension1 = sharedPreferences.getInt(coverDisplayDimension1Key, 0)
//        val storedDimension2 = sharedPreferences.getInt(coverDisplayDimension2Key, 0)
//        if (storedDimension1 == 0 || storedDimension2 == 0) return false
//        val currentMatchesStoredOrder =
//            (currentDisplaySize.width == storedDimension1 && currentDisplaySize.height == storedDimension2)
//        val currentMatchesSwappedOrder =
//            (currentDisplaySize.width == storedDimension2 && currentDisplaySize.height == storedDimension1)
//
//        return currentMatchesStoredOrder || currentMatchesSwappedOrder
//    }
//
//    fun clearSettings() {
//        sharedPreferences.edit {
//            putInt(themeKey, ThemeSetting.SYSTEM.ordinal)
//            putBoolean(coverThemeEnabledKey, false)
//            remove(coverDisplayDimension1Key)
//            remove(coverDisplayDimension2Key)
//            putBoolean(blackedOutModeKey, false)
//            putBoolean(developerModeKey, false)
//            remove(gridColumnCountKey)
//            remove(layoutTypeKey)
//            remove(listItemLineCountKey)
//            remove(labelsListKey)
//        }
//    }
//}