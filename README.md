# OpenID4VCI / OpenID4VP interoperability patches

This repository is a fork of [openwallet-foundation/multipaz](https://github.com/openwallet-foundation/multipaz). The **upstream project README** is preserved as [`README.upstream.md`](README.upstream.md).

The sections below describe **only the changes in this fork** aimed at interoperating with a **walt.id**-style issuer and **Verifier2** (OpenID4VCI / OpenID4VP). They do not replace Multipaz or protocol specifications.

---

## RFC 7517 JWK handling (`JwkRfc7517`, `EcPublicKey`)

**Issue:** OpenID4VCI proofs (and other JWTs carrying a `jwk` header, e.g. DPoP) must embed a **RFC 7517–compliant** JWK (`kty` required; `key_ops` as a string array when present). Fixing this only on the issuer with a permissive import is the wrong layer for client-side compliance.

**Change:**

- `multipaz/src/commonMain/kotlin/org/multipaz/crypto/JwkRfc7517.kt` — `ensureCompliant`: infers `kty` when missing (`y` → `EC`, `x` without `y` → `OKP`); normalizes `key_ops` from a single string to a `JsonArray`.
- `EcPublicKeyDoubleCoordinate.toJwk` / `EcPublicKeyOkp.toJwk`: after building the object, return `JwkRfc7517.ensureCompliant(jwk)`.

Excerpt:

```kotlin
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
```

---

## OpenID4VCI

### Issuer metadata cache removed

**Issue:** Stale issuer metadata across retries or after server-side changes.

**Upstream:** `IssuerConfiguration.get` could reuse a cached `IssuerConfiguration` when URL and preferences matched.

**Fork:** Cache removed — every `get` calls `fetchMetadata` again.

**File:** `multipaz/src/commonMain/kotlin/org/multipaz/provisioning/openid4vci/IssuerConfiguration.kt`

```kotlin
suspend fun get(
    url: String,
    httpClient: HttpClient,
    clientPreferences: OpenID4VCIClientPreferences
): IssuerConfiguration {
    val credentialMetadata = fetchMetadata(
        url = url,
        httpClient = httpClient,
        wellKnownName = "openid-credential-issuer"
    )
    // ...
}
```

### Per-entry issuer metadata parsing (safe skip)

**Behavior:** For each entry in `credential_configurations_supported`: run `extractFormat` (needs `vct` for `dc+sd-jwt`, `doctype` for `mso_mdoc`), then `extractKeyProofType`. If a step fails, **only that entry** is skipped with a log (including `credential_configuration_id`); other entries remain.

**Upstream:** One bad entry could fail the entire issuer load.

**Fork:**

- `catch (Exception)` around `extractFormat` (not only `IllegalArgumentException`).
- `vct` / `doctype` via `stringOrNull`; if missing → exception and skip (no invented defaults).

| Behavior | Effect |
| -------- | ------ |
| `catch (Exception)` on `extractFormat` | A broken entry does not block the rest |
| `vct` / `doctype` required for a valid entry | Incomplete entries are dropped, not “anonymous” credentials |
| Only `cwt` in `proof_types_supported` | Warning and `OpenidProofOfPossession` fallback (see below) |

Excerpt:

```kotlin
for ((id, config) in credentialMetadata.obj("credential_configurations_supported")) {
    val format = try {
        extractFormat(config)
    } catch (err: Exception) {
        Logger.e(TAG, "Skipping credential_configuration_id=\"$id\" (format parse failed)", err)
        continue
    }
    val keyProofType = try {
        extractKeyProofType(id, config, url, clientPreferences)
    } catch (err: IllegalArgumentException) {
        Logger.e(TAG, "Skipping credential_configuration_id=\"$id\" (key proof type not supported)", err)
        continue
    }
    // ... credentials[id] = CredentialMetadata(...)
}
```

### `cwt`-only proof metadata fallback

**Context:** OID4VCI allows `proof_types_supported` entries such as **`jwt`** and **`cwt`** for `mso_mdoc`; an issuer advertising **only `cwt`** is still spec-valid. The **gap** is between metadata (CWT proof) and the **local sample** using **JWT** proofs (`OpenidProofOfPossession`) without a CWT pipeline. This fallback is **client tolerance** to proceed; it does not implement native CWT proofs. Sending JWT when only `cwt` is advertised is **not** strictly aligned with metadata unless the issuer accepts JWT anyway (real-world stack behavior).

**Issue:** walt.id may declare `proof_types_supported.cwt` for `mso_mdoc` while the sample uses JWT proofs; without tolerance the configuration was unusable.

**Change:** In `IssuerConfiguration.kt`, if only `cwt` is present and not `jwt`, log a warning and still use `OpenidProofOfPossession` (same algorithm / client id as the operational JWT flow).

```kotlin
val jwt = proofTypes.objOrNull("jwt")
val cwt = proofTypes.objOrNull("cwt")
if (cwt != null && jwt == null) {
    Logger.w(
        TAG,
        "Issuer only advertises cwt proof; using OpenID JWT proof-of-possession as compatibility fallback"
    )
}
KeyBindingType.OpenidProofOfPossession(
    algorithm = alg,
    clientId = clientPreferences.clientId,
    aud = issuerId
)
```

### Credential response: `credential` vs `credentials[]`

**Issue:** walt.id may return a single **`credential`** field; the client assumed only `credentials[]`.

**Change:** `multipaz/src/commonMain/kotlin/org/multipaz/provisioning/openid4vci/OpenID4VCIProvisioningClient.kt`

```kotlin
val credentialElements = response["credentials"]?.jsonArray ?: listOfNotNull(
    response["credential"]?.let { credential ->
        buildJsonObject {
            put("credential", credential)
        }
    }
)
```

---

## OpenID4VP

### `direct_post` (plain) submit

**Issue:** The verifier expects `vp_token` and `state` in the POST body. The sample was sending `response` in a way that pushed the verifier toward the **encrypted** path instead of plain `direct_post`.

**Change:** `multipaz/src/commonMain/kotlin/org/multipaz/presentment/uriSchemePresentment.kt` — for `direct_post`, send `vp_token` (from the internal map, not a double wrapper) and `state`; use `response` only for `direct_post.jwt`.

```kotlin
Parameters.build {
    when (responseMode) {
        "direct_post" -> {
            append("vp_token", Json.encodeToString(responseObject.vpToken["vp_token"]!!.jsonObject))
            requestObject["state"]?.jsonPrimitive?.content?.let { append("state", it) }
        }
        "direct_post.jwt" -> {
            append("response", response["response"]!!.jsonPrimitive.content)
        }
        else -> throw IllegalArgumentException("Unexpected response_mode")
    }
}
```

### Verification response `Content-Type` and error bodies (wallet)

**Issue:** Strict string equality on `Content-Type` failed for `application/json; charset=utf-8`, etc.

**Change:** `uriSchemePresentment.kt` — use `ContentType.Application.Json` matching on the POST response to `response_uri`. If the verifier returns a non-2xx status (e.g. **500** when VP/DCQL validation fails on walt.id), the thrown exception now includes **status, URI, and response body** so logs are not only a generic “Check failed”.

### X.509 trust (`x509_san_dns`, single-certificate chain)

With a signed request and an X.509 chain reduced to a **single** certificate, `TrustManagerUtil.verifyX509TrustChain` may try to match a trust anchor via **Subject Key Identifier** (SKI). If the certificate has **no SKI** extension, the old code used `!!` and threw **NPE**; the lookup is now optional (`subjectKeyIdentifier?.toHex()`), and without a configured anchor the result is **untrusted** instead of a crash.

---

## Summary

These changes tighten **JWK** output, make **issuer metadata** loading more resilient and cache-free, accept **walt.id** credential response shapes, adjust **OpenID4VP presentment** for plain `direct_post`, improve **error visibility** from the verifier, and fix **X.509 trust** handling when SKI is absent—see [`README.upstream.md`](README.upstream.md) for the full upstream Multipaz overview.
