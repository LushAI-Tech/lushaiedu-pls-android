package com.lushaiedupls.data.remote.api

import com.lushaiedupls.data.remote.dto.AdminFeedbackUpdateRequest
import com.lushaiedupls.data.remote.dto.AdminFeeSummaryOut
import com.lushaiedupls.data.remote.dto.AdminParentLinkCreateRequest
import com.lushaiedupls.data.remote.dto.AdminUserCreateRequest
import com.lushaiedupls.data.remote.dto.AdminUserCreateResponse
import com.lushaiedupls.data.remote.dto.AdminUserEditRequest
import com.lushaiedupls.data.remote.dto.ClassCreate
import com.lushaiedupls.data.remote.dto.ClassOut
import com.lushaiedupls.data.remote.dto.ClassReassignRequest
import com.lushaiedupls.data.remote.dto.ClassUpdate
import com.lushaiedupls.data.remote.dto.DeletedResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerBulkDeleteRequest
import com.lushaiedupls.data.remote.dto.FeeLedgerBulkDeleteResponse
import com.lushaiedupls.data.remote.dto.FeeLedgerOut
import com.lushaiedupls.data.remote.dto.FeeLedgerUpdateRequest
import com.lushaiedupls.data.remote.dto.InstitutionCreate
import com.lushaiedupls.data.remote.dto.InstitutionOut
import com.lushaiedupls.data.remote.dto.InstitutionUpdate
import com.lushaiedupls.data.remote.dto.InviteCodeCreate
import com.lushaiedupls.data.remote.dto.InviteCodeOut
import com.lushaiedupls.data.remote.dto.LedgerGenerationRequest
import com.lushaiedupls.data.remote.dto.MessageResponse
import com.lushaiedupls.data.remote.dto.PaginatedUsersResponse
import com.lushaiedupls.data.remote.dto.ParentFeedbackOut
import com.lushaiedupls.data.remote.dto.ParentLinkOut
import com.lushaiedupls.data.remote.dto.StemBindingRequest
import com.lushaiedupls.data.remote.dto.StemNode
import com.lushaiedupls.data.remote.dto.SubjectCreate
import com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeOut
import com.lushaiedupls.data.remote.dto.SubjectMonthlyFeeUpsertRequest
import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.SubjectUpdate
import com.lushaiedupls.data.remote.dto.TeacherInstitutionAssignmentRequest
import com.lushaiedupls.data.remote.dto.UserOut
import com.lushaiedupls.data.remote.dto.UserStatusUpdate
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AdminApi {
    @GET("api/v1/admin/institutions")
    suspend fun listInstitutions(
        @Query("include_inactive") includeInactive: Boolean = false,
    ): List<InstitutionOut>

    @POST("api/v1/admin/institutions")
    suspend fun createInstitution(@Body body: InstitutionCreate): InstitutionOut

    @PATCH("api/v1/admin/institutions/{institution_id}")
    suspend fun updateInstitution(
        @Path("institution_id") institutionId: String,
        @Body body: InstitutionUpdate,
    ): InstitutionOut

    @DELETE("api/v1/admin/institutions/{institution_id}")
    suspend fun deleteInstitution(
        @Path("institution_id") institutionId: String,
    ): DeletedResponse

    @GET("api/v1/admin/classes")
    suspend fun listClasses(
        @Query("institution_id") institutionId: String,
        @Query("include_inactive") includeInactive: Boolean = false,
    ): List<ClassOut>

    @POST("api/v1/admin/classes")
    suspend fun createClass(@Body body: ClassCreate): ClassOut

    @PATCH("api/v1/admin/classes/{class_id}")
    suspend fun updateClass(
        @Path("class_id") classId: String,
        @Query("institution_id") institutionId: String,
        @Body body: ClassUpdate,
    ): ClassOut

    @DELETE("api/v1/admin/classes/{class_id}")
    suspend fun deleteClass(@Path("class_id") classId: String): DeletedResponse

    @GET("api/v1/admin/classes/{class_id}/subjects")
    suspend fun listSubjects(
        @Path("class_id") classId: String,
        @Query("institution_id") institutionId: String,
        @Query("include_inactive") includeInactive: Boolean = true,
    ): List<SubjectOut>

    @POST("api/v1/admin/subjects")
    suspend fun createSubject(@Body body: SubjectCreate): SubjectOut

    @PATCH("api/v1/admin/subjects/{subject_id}")
    suspend fun updateSubject(
        @Path("subject_id") subjectId: String,
        @Query("institution_id") institutionId: String,
        @Body body: SubjectUpdate,
    ): SubjectOut

    @DELETE("api/v1/admin/subjects/{subject_id}")
    suspend fun deleteSubject(@Path("subject_id") subjectId: String): DeletedResponse

    @PUT("api/v1/admin/subjects/{subject_id}/stem-binding")
    suspend fun bindStem(
        @Path("subject_id") subjectId: String,
        @Body body: StemBindingRequest,
    ): SubjectOut

    @GET("api/v1/admin/stem/boards")
    suspend fun stemBoards(): List<StemNode>

    @GET("api/v1/admin/stem/grades")
    suspend fun stemGrades(@Query("board_id") boardId: String): List<StemNode>

    @GET("api/v1/admin/stem/subjects")
    suspend fun stemSubjects(@Query("grade_id") gradeId: String): List<StemNode>

    @POST("api/v1/admin/invite-codes")
    suspend fun createInvite(@Body body: InviteCodeCreate): InviteCodeOut

    @GET("api/v1/admin/invite-codes")
    suspend fun listInvites(
        @Query("only_redeemable") onlyRedeemable: Boolean = false,
    ): List<InviteCodeOut>

    @DELETE("api/v1/admin/invite-codes/{invite_id}")
    suspend fun revokeInvite(@Path("invite_id") inviteId: String): MessageResponse

    @GET("api/v1/admin/users")
    suspend fun listUsers(
        @Query("role") role: String? = null,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null,
        @Query("include_deleted") includeDeleted: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): PaginatedUsersResponse

    @GET("api/v1/admin/users/{user_id}")
    suspend fun getUser(@Path("user_id") userId: String): UserOut

    @POST("api/v1/admin/users")
    suspend fun createUser(@Body body: AdminUserCreateRequest): AdminUserCreateResponse

    @PATCH("api/v1/admin/users/{user_id}")
    suspend fun editUser(
        @Path("user_id") userId: String,
        @Body body: AdminUserEditRequest,
    ): UserOut

    @PATCH("api/v1/admin/users/{user_id}/status")
    suspend fun updateUserStatus(
        @Path("user_id") userId: String,
        @Body body: UserStatusUpdate,
    ): UserOut

    @POST("api/v1/admin/users/{user_id}/approve")
    suspend fun approveUser(@Path("user_id") userId: String): UserOut

    @POST("api/v1/admin/users/{user_id}/reject")
    suspend fun rejectUser(@Path("user_id") userId: String): UserOut

    @PATCH("api/v1/admin/users/{user_id}/class")
    suspend fun reassignClass(
        @Path("user_id") userId: String,
        @Body body: ClassReassignRequest,
    ): UserOut

    @POST("api/v1/admin/teachers/{teacher_id}/institution-assignments")
    suspend fun assignTeacherInstitution(
        @Path("teacher_id") teacherId: String,
        @Body body: TeacherInstitutionAssignmentRequest,
    ): UserOut

    @POST("api/v1/admin/parent-links")
    suspend fun createParentLink(@Body body: AdminParentLinkCreateRequest): ParentLinkOut

    @GET("api/v1/admin/feedback")
    suspend fun listFeedback(
        @Query("status") status: String? = null,
    ): List<ParentFeedbackOut>

    @PATCH("api/v1/admin/feedback/{feedback_id}")
    suspend fun updateFeedback(
        @Path("feedback_id") feedbackId: String,
        @Body body: AdminFeedbackUpdateRequest,
    ): ParentFeedbackOut

    @POST("api/v1/admin/fees/subject-monthly")
    suspend fun upsertSubjectMonthlyFee(
        @Body body: SubjectMonthlyFeeUpsertRequest,
    ): SubjectMonthlyFeeOut

    @GET("api/v1/admin/fees/subject-monthly")
    suspend fun listSubjectMonthlyFees(
        @Query("month") month: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("subject_id") subjectId: String? = null,
    ): List<SubjectMonthlyFeeOut>

    @GET("api/v1/admin/fees/subject-monthly/{fee_id}")
    suspend fun getSubjectMonthlyFee(
        @Path("fee_id") feeId: String,
    ): SubjectMonthlyFeeOut

    @DELETE("api/v1/admin/fees/subject-monthly/{fee_id}")
    suspend fun deleteSubjectMonthlyFee(
        @Path("fee_id") feeId: String,
    ): MessageResponse

    @POST("api/v1/admin/fees/generate-ledgers")
    suspend fun generateLedgers(@Body body: LedgerGenerationRequest): MessageResponse

    @POST("api/v1/admin/fees/ledgers/delete-bulk")
    suspend fun deleteLedgersBulk(
        @Body body: FeeLedgerBulkDeleteRequest,
    ): FeeLedgerBulkDeleteResponse

    @GET("api/v1/admin/fees/ledgers")
    suspend fun listLedgers(
        @Query("month") month: String? = null,
        @Query("class_id") classId: String? = null,
        @Query("student_id") studentId: String? = null,
        @Query("subject_id") subjectId: String? = null,
        @Query("payment_status") paymentStatus: String? = null,
    ): List<FeeLedgerOut>

    @PATCH("api/v1/admin/fees/ledgers/{ledger_id}")
    suspend fun updateLedger(
        @Path("ledger_id") ledgerId: String,
        @Body body: FeeLedgerUpdateRequest,
    ): FeeLedgerOut

    @GET("api/v1/admin/fees/summary")
    suspend fun feeSummary(
        @Query("month") month: String,
        @Query("class_id") classId: String? = null,
    ): AdminFeeSummaryOut
}
