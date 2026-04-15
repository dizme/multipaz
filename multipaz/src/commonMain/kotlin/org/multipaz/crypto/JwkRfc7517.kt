package org.multipaz.crypto

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
/**
 * Ensures a JWK matches [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517) before embedding
 * in JWT headers (e.g. OpenID4VCI proof-of-possession): `kty` is required; `key_ops` must be an
 * array of strings when present.
 */
object JwkRfc7517 {
    fun ensureCompliant(jwk: JsonObject): JsonObject {
        val normalized = jwk.toMutableMap()
        if ("kty" !in normalized) {
            val kty = when {
                "y" in normalized -> "EC"
                "x" in normalized -> "OKP"
                else -> return jwk
            }
            normalized["kty"] = JsonPrimitive(kty)
        }
        normalized["key_ops"]?.let { ko ->
            if (ko is JsonPrimitive && ko.isString) {
                normalized["key_ops"] = buildJsonArray { add(ko) }
            }
        }
        return JsonObject(normalized)
    }
}
