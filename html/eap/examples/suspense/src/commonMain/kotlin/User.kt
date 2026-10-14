/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

// Share this public user data with the browser. Keep the repository in jvmMain.
data class User(val id: Long, val name: String, val role: String, val location: String, val bio: String)

fun serializeUser(user: User): String = buildJsonObject {
    put("id", user.id.toString())
    put("name", user.name)
    put("role", user.role)
    put("location", user.location)
    put("bio", user.bio)
}.toString()

fun deserializeUser(payload: String): User {
    val json = Json.parseToJsonElement(payload).jsonObject
    fun field(name: String) = json.getValue(name).jsonPrimitive.content
    return User(field("id").toLong(), field("name"), field("role"), field("location"), field("bio"))
}

const val UserHydrationId = "user-details"
