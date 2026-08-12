// Verifies enum-like values against the generated model and coverage ledgers.
import kotlin.test.Test
import kotlin.test.assertEquals

// Checks discovered, emitted, and skipped enum-like values against the generator ledgers.
class GeneratedEnumLikeValueLedgerTest {
    private val coverage = GeneratedCoverageReport.read()
    private val model = GeneratedModelReport.read()

    // Requires every top-level extension on a selected classifier to be emitted.
    @Test
    fun everyTopLevelExtensionIsPorted() {
        val entries = coverage.of("top-level-extension")

        assertEquals(emptyList(), entries.filterNot { it.ported }.map { it.subject })
        assertEquals(model.counts.getValue("extensions") + model.counts.getValue("values"), entries.size)
    }

    /** The values, as the model ledger records them under the classifier they hang off. */
    @Test
    fun theValuesAreRecordedAgainstTheirClassifier() {
        assertEquals(73, model.counts.getValue("values"))
        assertEquals(73, model.declarations.sumOf { it.values.size })

        val behavior = model.byName.getValue("org.w3c.dom.ScrollBehavior")
        assertEquals(
            listOf(
                "ScrollBehavior.Companion.AUTO",
                "ScrollBehavior.Companion.INSTANT",
                "ScrollBehavior.Companion.SMOOTH",
            ),
            behavior.values,
        )
        // The classifier itself stays what the browser makes it: an empty interface with a companion.
        assertEquals("interface", behavior.kind)
        assertEquals(0, behavior.memberCount)
        assertEquals(emptyList(), behavior.members)

        assertEquals(
            listOf("WorkerType.Companion.CLASSIC", "WorkerType.Companion.MODULE"),
            model.byName.getValue("org.w3c.dom.WorkerType").values,
        )
        assertEquals(
            listOf(
                "CanvasLineCap.Companion.BUTT",
                "CanvasLineCap.Companion.ROUND",
                "CanvasLineCap.Companion.SQUARE",
            ),
            model.byName.getValue("org.w3c.dom.CanvasLineCap").values,
        )
    }

    // Keeps every emitted value on the classifier matching its type.
    @Test
    fun everyValueBelongsToTheClassifierItHangsOff() {
        model.declarations.forEach { declaration ->
            declaration.values.forEach { value ->
                assertEquals(
                    "${declaration.simpleName}.Companion.",
                    value.substringBeforeLast('.') + '.',
                    "${declaration.name} records $value, which is not one of its own values",
                )
            }
        }
    }

    // Cross-checks emitted values between the model and coverage ledgers.
    @Test
    fun theCoverageLedgerAgreesWithTheModel() {
        val emitted = coverage.of("top-level-extension")
            .filter { it.detail.startsWith("emitted as ") && ".Companion." in it.detail }
            .map { it.detail.removePrefix("emitted as ") }
            .sorted()

        assertEquals(model.declarations.flatMap { it.values }.sorted(), emitted)
    }

}
