package com.student.task

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.student.task.ui.compose.HolidayComposeScreen
import com.student.task.ui.theme.TaskApplicationTheme
import com.student.task.ui.xml.HolidayXmlFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * Главная Activity приложения.
 *
 * Переключение между Compose и XML вариантами верстки осуществляется
 * через флаг [USE_COMPOSE].
 *
 * - true  → Jetpack Compose (HolidayComposeScreen)
 * - false → XML Fragment (HolidayXmlFragment)
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    companion object {
        /**
         * Флаг переключения между вариантами верстки.
         * true  = Jetpack Compose
         * false = XML + ViewBinding (Fragment)
         */
        private const val USE_COMPOSE = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (USE_COMPOSE) {
            setContent {
                TaskApplicationTheme {
                    HolidayComposeScreen()
                }
            }
        } else {
            setContentView(R.layout.activity_main_xml)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, HolidayXmlFragment())
                    .commit()
            }
        }
    }
}
