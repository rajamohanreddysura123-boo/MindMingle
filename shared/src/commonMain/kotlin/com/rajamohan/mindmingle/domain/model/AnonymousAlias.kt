package com.rajamohan.mindmingle.domain.model

private val anonymousAdjectives = listOf(
    "Silent", "Neon", "Velvet", "Midnight", "Cobalt", "Amber",
    "Quiet", "Electric", "Hidden", "Lunar", "Crimson", "Drifting"
)

private val anonymousCreatures = listOf(
    "Falcon", "Comet", "Otter", "Cipher", "Nomad", "Ember",
    "Sparrow", "Vector", "Phantom", "Harbor", "Wren", "Signal"
)

private fun stableHash(seed: String): Int {
    var hash = 7
    for (character in seed) {
        hash = hash * 31 + character.code
        hash = hash and 0x7FFFFFFF
    }
    return hash
}

fun anonymousAliasFor(seed: String): String {
    val hash = stableHash(seed)
    val adjective = anonymousAdjectives[hash % anonymousAdjectives.size]
    val creature = anonymousCreatures[(hash / anonymousAdjectives.size) % anonymousCreatures.size]
    return "$adjective $creature"
}

fun anonymousAccentIndexFor(seed: String, paletteSize: Int): Int {
    if (paletteSize <= 0) return 0
    return stableHash(seed) % paletteSize
}
