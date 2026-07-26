package org.gomap.android.features.presets

import android.content.Context
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

data class PresetItem(
    val id: String,
    val name: String,
    val geometry: Set<String>,
    val tags: Map<String, String>,
    val icon: String?,
    val terms: List<String>,
    val isNameSuggestion: Boolean
) {
    private val searchableText =
        (listOf(name, id) + terms + tags.keys + tags.values)
            .joinToString(" ")
            .lowercase(Locale.ROOT)

    fun matches(query: String): Boolean {
        val words = query.lowercase(Locale.ROOT).trim().split(Regex("\\s+"))
        return words.all(searchableText::contains)
    }

    fun supports(featureGeometry: String): Boolean = featureGeometry in geometry

    fun matchesTags(featureTags: Map<String, String>): Boolean =
        tags.isNotEmpty() && tags.all { (key, value) ->
            featureTags[key]?.let { value == "*" || it == value } == true
    }
}

data class PresetCategory(
    val id: String,
    val name: String,
    val members: List<String>
)

class PresetCatalog private constructor(
    val presets: List<PresetItem>,
    val categories: List<PresetCategory>,
    val suggestions: List<PresetItem>
) {
    private val presetsById = presets.associateBy(PresetItem::id)

    fun matchingPreset(
        tags: Map<String, String>,
        geometry: String
    ): PresetItem? = presets
        .asSequence()
        .filter { it.supports(geometry) && it.matchesTags(tags) }
        .maxByOrNull { preset ->
            preset.tags.count { it.value != "*" } * 100 + preset.tags.size
        }

    fun search(
        query: String,
        geometry: String,
        currentTags: Map<String, String>,
        limit: Int = 120
    ): List<PresetItem> {
        val trimmed = query.trim()
        val base = presets.asSequence()
            .filter { it.supports(geometry) }
            .filter { trimmed.isEmpty() || it.matches(trimmed) }
            .sortedWith(
                compareByDescending<PresetItem> { it.matchesTags(currentTags) }
                    .thenByDescending { it.name.startsWith(trimmed, ignoreCase = true) }
                    .thenBy { it.name }
            )
        val brands = if (trimmed.length >= 2) {
            suggestions.asSequence()
                .filter { it.supports(geometry) && it.matches(trimmed) }
                .sortedWith(
                    compareByDescending<PresetItem> {
                        it.name.startsWith(trimmed, ignoreCase = true)
                    }.thenBy { it.name }
                )
        } else {
            emptySequence()
        }
        return (base + brands).take(limit).toList()
    }

    fun categoriesFor(geometry: String): List<PresetCategory> =
        categories.filter { category ->
            category.members.any { presetsById[it]?.supports(geometry) == true }
        }

    fun categoryMembers(category: PresetCategory, geometry: String): List<PresetItem> =
        category.members.mapNotNull(presetsById::get).filter { it.supports(geometry) }

    fun preset(id: String): PresetItem? = presetsById[id]

    fun suggestion(name: String): PresetItem? =
        suggestions.firstOrNull { it.name.equals(name, ignoreCase = true) }

    companion object {
        @Volatile
        private var cached: PresetCatalog? = null

        fun load(context: Context): PresetCatalog =
            cached ?: synchronized(this) {
                cached ?: context.assets.open("presets/catalog.json")
                    .bufferedReader()
                    .use { reader -> parse(JSONObject(reader.readText())) }
                    .also { cached = it }
            }

        private fun parse(root: JSONObject): PresetCatalog = PresetCatalog(
            presets = root.getJSONArray("presets").toPresetItems(false),
            categories = root.getJSONArray("categories").toCategories(),
            suggestions = root.getJSONArray("suggestions").toPresetItems(true)
        )
    }
}

private fun JSONArray.toPresetItems(isSuggestion: Boolean): List<PresetItem> =
    buildList(length()) {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                PresetItem(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    geometry = item.optJSONArray("geometry")
                        ?.toStringSet()
                        ?: setOf("point", "area"),
                    tags = item.getJSONObject("tags").toStringMap(),
                    icon = if (item.isNull("icon")) null else item.getString("icon"),
                    terms = item.optJSONArray("terms")?.toStringList().orEmpty(),
                    isNameSuggestion = isSuggestion
                )
            )
        }
    }

private fun JSONArray.toCategories(): List<PresetCategory> =
    buildList(length()) {
        for (index in 0 until length()) {
            val item = getJSONObject(index)
            add(
                PresetCategory(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    members = item.getJSONArray("members").toStringList()
                )
            )
        }
    }

private fun JSONArray.toStringList(): List<String> =
    buildList(length()) {
        for (index in 0 until length()) add(getString(index))
    }

private fun JSONArray.toStringSet(): Set<String> = toStringList().toSet()

private fun JSONObject.toStringMap(): Map<String, String> =
    buildMap {
        this@toStringMap.keys().forEach { key ->
            put(key, this@toStringMap.get(key).toString())
        }
    }
