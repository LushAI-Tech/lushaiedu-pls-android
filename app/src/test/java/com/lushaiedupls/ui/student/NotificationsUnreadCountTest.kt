package com.lushaiedupls.ui.student

import com.lushaiedupls.data.remote.api.AiApi
import com.lushaiedupls.data.remote.api.AttendanceApi
import com.lushaiedupls.data.remote.api.CalendarApi
import com.lushaiedupls.data.remote.api.ClassesApi
import com.lushaiedupls.data.remote.api.MeApi
import com.lushaiedupls.data.remote.api.NotificationsApi
import com.lushaiedupls.data.remote.api.OverviewApi
import com.lushaiedupls.data.remote.api.ParentApi
import com.lushaiedupls.data.remote.api.TeachingUnitsApi
import com.lushaiedupls.data.remote.api.TimetableApi
import com.lushaiedupls.data.remote.device.DeviceIdProvider
import com.lushaiedupls.data.repository.StudentRepository
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationsUnreadCountTest {

    private inline fun <reified T> createDummyProxy(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, _, _ -> null } as T
    }

    private fun createDummyDeviceIdProvider(): DeviceIdProvider {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(DeviceIdProvider::class.java) as DeviceIdProvider
    }

    private val studentRepository = StudentRepository(
        overviewApi = createDummyProxy<OverviewApi>(),
        attendanceApi = createDummyProxy<AttendanceApi>(),
        calendarApi = createDummyProxy<CalendarApi>(),
        timetableApi = createDummyProxy<TimetableApi>(),
        notificationsApi = createDummyProxy<NotificationsApi>(),
        meApi = createDummyProxy<MeApi>(),
        classesApi = createDummyProxy<ClassesApi>(),
        aiApi = createDummyProxy<AiApi>(),
        parentApi = createDummyProxy<ParentApi>(),
        teachingUnitsApi = createDummyProxy<TeachingUnitsApi>(),
        deviceIdProvider = createDummyDeviceIdProvider(),
    )

    @Test
    fun unreadNotificationCount_startsNull_updatesAndDecrementsProperly() {
        assertEquals(null, studentRepository.unreadNotificationCount.value)

        studentRepository.setUnreadNotificationCount(3)
        assertEquals(3, studentRepository.unreadNotificationCount.value)

        studentRepository.decrementUnreadNotificationCount()
        assertEquals(2, studentRepository.unreadNotificationCount.value)

        studentRepository.decrementUnreadNotificationCount()
        assertEquals(1, studentRepository.unreadNotificationCount.value)

        studentRepository.decrementUnreadNotificationCount()
        assertEquals(0, studentRepository.unreadNotificationCount.value)

        // Does not go negative
        studentRepository.decrementUnreadNotificationCount()
        assertEquals(0, studentRepository.unreadNotificationCount.value)
    }

    @Test
    fun unreadNotificationCount_setZero_clearsCount() {
        studentRepository.setUnreadNotificationCount(5)
        assertEquals(5, studentRepository.unreadNotificationCount.value)

        studentRepository.setUnreadNotificationCount(0)
        assertEquals(0, studentRepository.unreadNotificationCount.value)
    }
}
