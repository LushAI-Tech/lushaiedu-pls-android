package com.lushaiedupls.data.session

import com.lushaiedupls.ui.auth.selectclass.SchoolClass
import com.lushaiedupls.ui.auth.selectrole.UserRole
import com.lushaiedupls.ui.auth.selectsubject.SubjectOption

/**
 * Persists the selected app role, display profile, and class/subject choices for navigation.
 */
interface UserSessionStore {
    fun getRole(): UserRole?
    fun setRole(role: UserRole)
    fun getDisplayName(): String
    fun setDisplayName(name: String)
    fun getAvatarUrl(): String?
    fun setAvatarUrl(url: String?)
    fun getSelectedClasses(): List<SchoolClass>
    fun setSelectedClasses(classes: List<SchoolClass>)
    fun getClassSubjects(): Map<SchoolClass, List<SubjectOption>>
    fun setClassSubjects(assignments: Map<SchoolClass, List<SubjectOption>>)
    fun getOnboardingState(): String?
    fun setOnboardingState(state: String?)
    fun getClassId(): String?
    fun setClassId(classId: String?)
    fun getClassIds(): List<String>
    fun setClassIds(ids: List<String>)
    fun getSubjectIds(): List<String>
    fun setSubjectIds(ids: List<String>)
    fun getUserStatus(): String?
    fun setUserStatus(status: String?)
    fun getPendingPhone(): String?
    fun setPendingPhone(phone: String?)
    fun getPendingGender(): String?
    fun setPendingGender(gender: String?)
    fun getPendingAddress(): String?
    fun setPendingAddress(address: String?)
    fun getPendingInviteCode(): String?
    fun setPendingInviteCode(code: String?)
    fun getInstitutionId(): String?
    fun setInstitutionId(institutionId: String?)
    fun isParentSignupFlow(): Boolean
    fun setParentSignupFlow(enabled: Boolean)
    /** After Google OAuth: collect name/phone/password/address/gender before role. */
    fun needsProfileSetup(): Boolean
    fun setNeedsProfileSetup(enabled: Boolean)
    fun clear()
}
