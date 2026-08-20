package com.lushaiedupls.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lushaiedupls.ui.auth.selectclass.SchoolClass
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.auth.selectsubject.SubjectOption

class SharedPrefsUserSessionStore(
    context: Context,
) : UserSessionStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getRole(): UserRole? {
        val raw = prefs.getString(KEY_ROLE, null) ?: return null
        return runCatching { UserRole.valueOf(raw) }.getOrNull()
    }

    override fun setRole(role: UserRole) {
        prefs.edit { putString(KEY_ROLE, role.name) }
    }

    override fun getDisplayName(): String =
        prefs.getString(KEY_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME

    override fun setDisplayName(name: String) {
        prefs.edit { putString(KEY_DISPLAY_NAME, name.trim().ifBlank { DEFAULT_DISPLAY_NAME }) }
    }

    override fun getSelectedClasses(): List<SchoolClass> {
        val raw = prefs.getString(KEY_SELECTED_CLASSES, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(LIST_SEP)
            .mapNotNull { token -> runCatching { SchoolClass.valueOf(token) }.getOrNull() }
            .distinct()
            .sortedBy { it.ordinal }
    }

    override fun setSelectedClasses(classes: List<SchoolClass>) {
        val ordered = classes.distinct().sortedBy { it.ordinal }
        prefs.edit {
            putString(KEY_SELECTED_CLASSES, ordered.joinToString(LIST_SEP) { it.name })
        }
    }

    override fun getClassSubjects(): Map<SchoolClass, List<SubjectOption>> {
        val raw = prefs.getString(KEY_CLASS_SUBJECTS, null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return raw.split(LIST_SEP).mapNotNull { pair ->
            val parts = pair.split(PAIR_SEP, limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val schoolClass = runCatching { SchoolClass.valueOf(parts[0]) }.getOrNull()
                ?: return@mapNotNull null
            val subjects = parts[1].split(SUBJECT_SEP)
                .mapNotNull { token -> runCatching { SubjectOption.valueOf(token) }.getOrNull() }
                .distinct()
                .sortedBy { it.ordinal }
            if (subjects.isEmpty()) return@mapNotNull null
            schoolClass to subjects
        }.toMap()
    }

    override fun setClassSubjects(assignments: Map<SchoolClass, List<SubjectOption>>) {
        val encoded = assignments.entries
            .sortedBy { it.key.ordinal }
            .mapNotNull { (schoolClass, subjects) ->
                val ordered = subjects.distinct().sortedBy { it.ordinal }
                if (ordered.isEmpty()) return@mapNotNull null
                "${schoolClass.name}$PAIR_SEP${ordered.joinToString(SUBJECT_SEP) { it.name }}"
            }
            .joinToString(LIST_SEP)
        prefs.edit { putString(KEY_CLASS_SUBJECTS, encoded) }
    }

    override fun getOnboardingState(): String? = prefs.getString(KEY_ONBOARDING_STATE, null)

    override fun setOnboardingState(state: String?) {
        prefs.edit {
            if (state.isNullOrBlank()) remove(KEY_ONBOARDING_STATE)
            else putString(KEY_ONBOARDING_STATE, state)
        }
    }

    override fun getClassId(): String? = getClassIds().firstOrNull()

    override fun setClassId(classId: String?) {
        setClassIds(listOfNotNull(classId?.takeIf { it.isNotBlank() }))
    }

    override fun getClassIds(): List<String> {
        val raw = prefs.getString(KEY_CLASS_IDS, null)
            ?: prefs.getString(KEY_CLASS_ID, null)
            ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(LIST_SEP).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    override fun setClassIds(ids: List<String>) {
        val cleaned = ids.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        prefs.edit {
            if (cleaned.isEmpty()) {
                remove(KEY_CLASS_IDS)
                remove(KEY_CLASS_ID)
            } else {
                putString(KEY_CLASS_IDS, cleaned.joinToString(LIST_SEP))
                putString(KEY_CLASS_ID, cleaned.first())
            }
        }
    }

    override fun getUserStatus(): String? = prefs.getString(KEY_USER_STATUS, null)

    override fun setUserStatus(status: String?) {
        prefs.edit {
            if (status.isNullOrBlank()) remove(KEY_USER_STATUS)
            else putString(KEY_USER_STATUS, status)
        }
    }

    override fun getSubjectIds(): List<String> {
        val raw = prefs.getString(KEY_SUBJECT_IDS, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(LIST_SEP).map { it.trim() }.filter { it.isNotEmpty() }
    }

    override fun setSubjectIds(ids: List<String>) {
        prefs.edit {
            putString(KEY_SUBJECT_IDS, ids.filter { it.isNotBlank() }.joinToString(LIST_SEP))
        }
    }

    override fun getPendingPhone(): String? = prefs.getString(KEY_PENDING_PHONE, null)
    override fun setPendingPhone(phone: String?) {
        prefs.edit {
            if (phone.isNullOrBlank()) remove(KEY_PENDING_PHONE) else putString(KEY_PENDING_PHONE, phone)
        }
    }

    override fun getPendingGender(): String? = prefs.getString(KEY_PENDING_GENDER, null)
    override fun setPendingGender(gender: String?) {
        prefs.edit {
            if (gender.isNullOrBlank()) remove(KEY_PENDING_GENDER) else putString(KEY_PENDING_GENDER, gender)
        }
    }

    override fun getPendingAddress(): String? = prefs.getString(KEY_PENDING_ADDRESS, null)
    override fun setPendingAddress(address: String?) {
        prefs.edit {
            if (address.isNullOrBlank()) remove(KEY_PENDING_ADDRESS) else putString(KEY_PENDING_ADDRESS, address)
        }
    }

    override fun getPendingInviteCode(): String? = prefs.getString(KEY_PENDING_INVITE, null)
    override fun setPendingInviteCode(code: String?) {
        prefs.edit {
            if (code.isNullOrBlank()) remove(KEY_PENDING_INVITE) else putString(KEY_PENDING_INVITE, code.trim())
        }
    }

    override fun isParentSignupFlow(): Boolean = prefs.getBoolean(KEY_PARENT_SIGNUP, false)

    override fun setParentSignupFlow(enabled: Boolean) {
        prefs.edit {
            if (enabled) putBoolean(KEY_PARENT_SIGNUP, true) else remove(KEY_PARENT_SIGNUP)
        }
    }

    override fun clear() {
        prefs.edit {
            remove(KEY_ROLE)
            remove(KEY_DISPLAY_NAME)
            remove(KEY_SELECTED_CLASSES)
            remove(KEY_CLASS_SUBJECTS)
            remove(KEY_ONBOARDING_STATE)
            remove(KEY_CLASS_ID)
            remove(KEY_CLASS_IDS)
            remove(KEY_SUBJECT_IDS)
            remove(KEY_USER_STATUS)
            remove(KEY_PENDING_PHONE)
            remove(KEY_PENDING_GENDER)
            remove(KEY_PENDING_ADDRESS)
            remove(KEY_PENDING_INVITE)
            remove(KEY_PARENT_SIGNUP)
        }
    }

    private companion object {
        const val PREFS_NAME = "lushai_user_session"
        const val KEY_ROLE = "role"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_SELECTED_CLASSES = "selected_classes"
        const val KEY_CLASS_SUBJECTS = "class_subjects"
        const val KEY_ONBOARDING_STATE = "onboarding_state"
        const val KEY_CLASS_ID = "class_id"
        const val KEY_CLASS_IDS = "class_ids"
        const val KEY_SUBJECT_IDS = "subject_ids"
        const val KEY_USER_STATUS = "user_status"
        const val KEY_PENDING_PHONE = "pending_phone"
        const val KEY_PENDING_GENDER = "pending_gender"
        const val KEY_PENDING_ADDRESS = "pending_address"
        const val KEY_PENDING_INVITE = "pending_invite"
        const val KEY_PARENT_SIGNUP = "parent_signup_flow"
        const val LIST_SEP = ","
        const val PAIR_SEP = ":"
        const val SUBJECT_SEP = "|"
        const val DEFAULT_DISPLAY_NAME = "V Lalfakea"
    }
}
