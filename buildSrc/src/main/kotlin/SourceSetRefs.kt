
data class SourceSetRef(
    val name: String,
    val parents: List<SourceSetRef>,
)

fun buildSourceSetRefs(parentsToChildren: Map<String, List<String>>): List<SourceSetRef> =
    SourceSetTreeBuilder(parentsToChildren).build()

fun List<SourceSetRef>.withAppendedNames(suffix: String) = map { it.withAppendedNames(suffix) }

private fun SourceSetRef.withAppendedNames(suffix: String): SourceSetRef =
    SourceSetRef("$name$suffix", parents.withAppendedNames(suffix))

private class SourceSetTreeBuilder(
    parentsToChildren: Map<String, List<String>>
) {
    private val childrenToParents = parentsToChildren.invert()

    private val refsByName = mutableMapOf<String, SourceSetRef>()

    fun build(): List<SourceSetRef> {
        val allBaseNames: Set<String> = childrenToParents.keys + childrenToParents.values.flatten()
        return allBaseNames.map { getOrCreate(it) }
    }

    private fun getOrCreate(baseName: String): SourceSetRef =
        refsByName.getOrPut(baseName) { create(baseName) }

    private fun create(baseName: String): SourceSetRef {
        val parentBaseNames = childrenToParents[baseName] ?: emptyList()
        val parents = parentBaseNames.map { getOrCreate(it) }
        return SourceSetRef(baseName, parents)
    }
}

private fun <K, V> Map<K, List<V>>.invert(): Map<V, List<K>> = entries
    .flatMap { (key, values) -> values.map { it to key } }
    .groupBy({ it.first }, { it.second })
