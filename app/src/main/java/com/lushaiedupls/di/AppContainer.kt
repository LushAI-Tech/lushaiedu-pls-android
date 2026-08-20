package com.lushaiedupls.di

import android.content.Context
import com.lushaiedupls.BuildConfig
import com.lushaiedupls.data.mock.StudentMockRepository
import com.lushaiedupls.data.mock.TeacherMockRepository
import com.lushaiedupls.data.remote.ApiClient
import com.lushaiedupls.data.remote.api.AiApi
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.AuthApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.ClassesApi
import com.lushaiedupls.data.remote.api.MeApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.ParentApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.remote.token.SharedPrefsTokenProvider
import com.lushaiedupls.data.remote.token.TokenProvider
import com.lushaiedupls.data.repository.AuthRepository
import com.lushaiedupls.data.repository.ParentRepository
import com.lushaiedupls.data.repository.SessionRepository
import com.lushaiedupls.data.repository.StudentRepository
import com.lushaiedupls.data.repository.TeacherRepository
import com.lushaiedupls.data.session.SharedPrefsUserSessionStore
import com.lushaiedupls.data.session.UserSessionStore
import retrofit2.Retrofit

/**
 * Manual composition root. Swap this for Hilt/Koin later without changing call sites much.
 */
class AppContainer(context: Context) {
    val tokenProvider: TokenProvider = SharedPrefsTokenProvider(context)

    val sessionRepository = SessionRepository(tokenProvider)

    val userSessionStore: UserSessionStore = SharedPrefsUserSessionStore(context)

    val deviceIdProvider = DeviceIdProvider(context)

    val studentMockRepository = StudentMockRepository()

    val teacherMockRepository = TeacherMockRepository()

    val retrofit: Retrofit = ApiClient.create(
        baseUrl = BuildConfig.BASE_URL,
        tokenProvider = tokenProvider,
        deviceIdProvider = deviceIdProvider,
        appVersion = BuildConfig.VERSION_NAME,
        isDebug = BuildConfig.DEBUG,
        onUnauthorized = sessionRepository::onUnauthorized,
    )

    inline fun <reified T> createService(): T = ApiClient.createService(retrofit)

    val authApi: AuthApi = createService()
    val meApi: MeApi = createService()
    val classesApi: ClassesApi = createService()
    val overviewApi: OverviewApi = createService()
    val attendanceApi: AttendanceApi = createService()
    val calendarApi: CalendarApi = createService()
    val timetableApi: TimetableApi = createService()
    val notificationsApi: NotificationsApi = createService()
    val aiApi: AiApi = createService()
    val parentApi: ParentApi = createService()
    val teachingUnitsApi: TeachingUnitsApi = createService()

    val authRepository = AuthRepository(
        authApi = authApi,
        sessionRepository = sessionRepository,
        deviceIdProvider = deviceIdProvider,
        userSessionStore = userSessionStore,
    )

    val studentRepository = StudentRepository(
        overviewApi = overviewApi,
        attendanceApi = attendanceApi,
        calendarApi = calendarApi,
        timetableApi = timetableApi,
        notificationsApi = notificationsApi,
        meApi = meApi,
        classesApi = classesApi,
        aiApi = aiApi,
        parentApi = parentApi,
        teachingUnitsApi = teachingUnitsApi,
        deviceIdProvider = deviceIdProvider,
    )

    val parentRepository = ParentRepository(
        parentApi = parentApi,
        overviewApi = overviewApi,
        attendanceApi = attendanceApi,
    )

    val teacherRepository = TeacherRepository(
        overviewApi = overviewApi,
        teachingUnitsApi = teachingUnitsApi,
        attendanceApi = attendanceApi,
        calendarApi = calendarApi,
        timetableApi = timetableApi,
        notificationsApi = notificationsApi,
    )
}
