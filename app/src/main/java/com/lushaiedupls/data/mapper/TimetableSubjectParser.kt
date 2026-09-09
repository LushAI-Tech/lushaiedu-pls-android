package com.lushaiedupls.data.mapper

import com.lushaiedupls.data.remote.dto.SubjectOut
import com.lushaiedupls.data.remote.dto.TeachingUnitOut
import com.lushaiedupls.ui.teacher.overlays.SessionSubjectOption
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

object TimetableSubjectParser {

    fun parse(element: JsonElement): List<SubjectOut> {
        val array = subjectArray(element) ?: return emptyList()
        return array.mapNotNull { item ->
            val obj = item as? JsonObject ?: return@mapNotNull null
            val id = string(obj, "subject_id", "id") ?: return@mapNotNull null
            val name = string(obj, "name", "subject_name", "title", "code") ?: return@mapNotNull null
            SubjectOut(
                id = id,
                class_id = string(obj, "class_id").orEmpty(),
                name = name,
                code = string(obj, "code"),
                sort_order = (obj["sort_order"] as? JsonPrimitive)?.intOrNull ?: 0,
                is_active = (obj["is_active"] as? JsonPrimitive)?.booleanOrNull ?: true,
                stem_subject_id = string(obj, "stem_subject_id"),
            )
        }
    }

    fun sessionOptions(
        apiSubjects: List<SubjectOut>,
        units: List<TeachingUnitOut>,
        classId: String?,
        className: String? = null,
    ): List<SessionSubjectOption> {
        val fromApi = apiSubjects
            .filter { it.is_active }
            .sortedBy { it.sort_order }
            .mapNotNull { subject ->
                val id = subject.id.ifBlank { return@mapNotNull null }
                val name = subject.name.ifBlank { subject.code.orEmpty() }.ifBlank { return@mapNotNull null }
                SessionSubjectOption(id = id, name = name)
            }
        val fromUnits = subjectsFromUnits(units, classId, className)
        return (fromApi + fromUnits).distinctBy { it.id }
    }

    fun subjectsFromUnits(
        units: List<TeachingUnitOut>,
        classId: String?,
        className: String? = null,
    ): List<SessionSubjectOption> {
        val matched = units.filter { unit ->
            (classId != null && unit.class_id == classId) ||
                (className != null && unit.class_name.equals(className, ignoreCase = true))
        }
        val scoped = if (classId != null || className != null) matched else units
        return scoped
            .mapNotNull { unit ->
                val id = unit.subject_id.ifBlank { unit.id }
                val name = unit.subject_name.ifBlank { return@mapNotNull null }
                SessionSubjectOption(id = id, name = name)
            }
            .distinctBy { it.id }
    }

    private fun subjectArray(element: JsonElement): JsonArray? {
        when (element) {
            is JsonArray -> return element
            is JsonObject -> {
                listOf("items", "subjects", "data", "results").forEach { key ->
                    val value = element[key]
                    if (value is JsonArray) return value
                }
            }
            else -> Unit
        }
        return null
    }

    private fun string(obj: JsonObject, vararg keys: String): String? {
        keys.forEach { key ->
            val value = (obj[key] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }
            if (value != null) return value
        }
        return null
    }
}
