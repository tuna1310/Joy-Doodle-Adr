package com.doodlestudio.app.domain.canvas

import com.doodlestudio.app.domain.model.ActionLog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Serialization and deserialization engine for ActionLog.
 * Generates compact JSON payloads suitable for persistence and network transfer.
 */
object ActionLogSerializer {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
        isLenient = true
    }

    /**
     * Serializes an ActionLog into a compact JSON string.
     */
    fun serialize(actionLog: ActionLog): String {
        return json.encodeToString(actionLog)
    }

    /**
     * Deserializes a JSON string back into an ActionLog object.
     */
    fun deserialize(jsonString: String): ActionLog {
        return json.decodeFromString(jsonString)
    }

    /**
     * Measures the size in bytes of the serialized JSON payload.
     */
    fun estimateByteSize(actionLog: ActionLog): Int {
        return serialize(actionLog).toByteArray(Charsets.UTF_8).size
    }
}

