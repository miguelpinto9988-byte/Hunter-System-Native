package com.huntersystem.app

import com.huntersystem.app.planning.Category
import com.huntersystem.app.planning.DailyMission
import com.huntersystem.app.planning.ProfileSignature
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class ExerciseTaxonomyTest {
    private val catalog = Catalog(loader = { File("src/main/assets", it).readText() })
    private val rules = Rules(catalog)
    private val today = LocalDate.parse("2026-11-16")
    private val profile = obj("name" to "Auditoría", "age" to 30, "difficulty" to "basic",
        "bodyAvailable" to true, "dailyMissions" to 3, "minutes" to 30)

    @Test fun catalogFocusSeparatesMobilityAndHoldsFromDynamicTraining() {
        fun includes(focus: CatalogFocus, id: String): Boolean {
            val exercise = checkNotNull(catalog.originalExercise(id))
            return focus.includes(exercise, catalog.classification(id)?.category)
        }
        assertTrue(includes(CatalogFocus.MOBILITY, "neck"))
        assertFalse(includes(CatalogFocus.DYNAMIC, "neck"))
        assertFalse(includes(CatalogFocus.DYNAMIC, "plank"))
        assertTrue(includes(CatalogFocus.ALL, "plank"))
        assertTrue(includes(CatalogFocus.DYNAMIC, "home-plankjack30"))
        assertTrue(includes(CatalogFocus.DYNAMIC, "goblethold"))
        assertTrue(includes(CatalogFocus.DYNAMIC, "farmerwalk"))
        assertTrue(includes(CatalogFocus.POWER, "new46-kbswing"))
        assertFalse(includes(CatalogFocus.IMPACT, "new46-kbswing"))
        assertTrue(includes(CatalogFocus.IMPACT, "squatjump"))
    }

    @Test fun performanceLibraryConsolidatesDuplicatesAndPreservesSavedIds() {
        val legacyBike = "gym-bike30-ecc30"
        val displayed = catalog.libraryExercises.mapTo(hashSetOf()) { it.id }
        val automatic = catalog.automaticExercises.mapTo(hashSetOf()) { it.id }
        assertEquals(16, catalog.originalExercises.size - displayed.size)
        assertEquals(612, catalog.libraryExercises.map { it.oldFamily }.distinct().size)
        assertNotNull(catalog.byId[legacyBike])
        assertFalse(legacyBike in displayed)
        assertFalse(legacyBike in automatic)
        assertTrue("gym-bike30" in automatic)
        assertTrue("pushup_slow" in displayed)
        assertFalse("seatedpunch" in automatic)
        assertFalse("plank" in automatic)
        assertTrue("neck" in automatic)
        val landing = catalog.byId.getValue("new46-droplanding")
        assertTrue(CatalogFocus.PREPARATION.includes(landing, null))
        assertFalse(CatalogFocus.IMPACT.includes(landing, null))
        assertTrue(CatalogFocus.POWER.includes(catalog.byId.getValue("new46-sprintbrake"), Category.CARDIO))
    }

    @Test fun onlyTwentyEssentialMobilityExercisesAreSelectable() {
        val active = catalog.exercises.groupBy { catalog.classification(it.id)!!.category }
        assertEquals(Category.entries.toSet(), active.keys)
        assertEquals(ExerciseTaxonomy.essentialMobility.keys,
            active.getValue(Category.MOBILITY).map { it.id }.toSet())
        assertEquals(20, active.getValue(Category.MOBILITY).size)
        assertNull(catalog.classification("sport-yoga-warrior3"))
        assertNotNull(catalog.byId["sport-yoga-warrior3"])
        assertNull(catalog.classification("pau-armworld"))
        assertNull(catalog.classification("wallslide"))
        assertNull(catalog.classification("sport-futbol-sole"))
        assertEquals(Category.CARDIO, catalog.classification("march")!!.category)
        assertEquals(Category.LOWER, catalog.classification("calf")!!.category)
        assertEquals("Empuje horizontal", catalog.classification("pushup")!!.family)
        assertEquals("Flexión abdominal", catalog.classification("curlup")!!.family)
        println(active.mapValues { it.value.size })
    }

    @Test fun activeSequencesNeverCombineCategoriesOrUseRetiredMovements() {
        for (exercise in catalog.exercises) {
            val category = catalog.classification(exercise.id)!!.category
            for (part in exercise.raw.a("sequenceParts").strings()) {
                assertEquals(exercise.id, category, catalog.classification(part)?.category)
            }
        }
        assertNull(catalog.classification("sport-seq-calistenia-wallplank-sport-ampl-wall-plus"))
    }

    @Test fun dailyPlansAndRoutineOffersStayInSelectedCategory() {
        val planner = Planner(catalog, rules)
        for (category in Category.entries) {
            val p = profile.copyJson().put("weeklyCategories", arr(List(7) { category.ordinal }))
                .put("custom_goal", "Quiero mejorar mi movilidad")
            val kind = when (category) {
                Category.MOBILITY -> "recovery"
                Category.CARDIO -> "cardio"
                else -> "strength"
            }
            val native = checkNotNull(MonarchProfileAdapter.from(p, today))
            val pinned = if (category == Category.UPPER) DailyMission(today, category,
                ProfileSignature.of(native), "catálogo anterior", listOf("wallslide"), "Propuesta antigua") else null
            val day = planner.choose(p, kind, "strict-taxonomy", date = today.toString(),
                trainingProfile = native, pinned = pinned)
            val items = day.a("items").objects()
            assertTrue("$category: ${day.s("blocked")}", items.isNotEmpty())
            assertTrue(items.all { catalog.classification(it.s("id"))?.category == category })
            val offers = planner.generateRoutineOptions("home", category.code, 0, 15, "strict-taxonomy", p,
                obj(), today)
            assertTrue("Sin rutinas para $category", offers.isNotEmpty())
            assertTrue(offers.flatMap { it.a("items").objects() }
                .all { catalog.classification(it.s("id"))?.category == category })
        }
    }
}
