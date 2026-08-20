package com.lushaiedupls

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lushaiedupls.ui.navigation.AppNavGraph
import com.lushaiedupls.ui.theme.LushAIEdu_PLSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LushAIEduApp).container
        setContent {
            LushAIEdu_PLSTheme {
                AppNavGraph(
                    userSessionStore = container.userSessionStore,
                    authRepository = container.authRepository,
                    studentRepository = container.studentRepository,
                    teacherRepository = container.teacherRepository,
                    parentRepository = container.parentRepository,
                )
            }
        }
    }
}
