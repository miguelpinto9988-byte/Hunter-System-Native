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
        val withdrawn = catalog.originalExercises.filter { catalog.isWithdrawn(it.id) }
        assertEquals(78, withdrawn.count { it.isBaseVariant })
        assertEquals(132, withdrawn.size)
        assertEquals(148, catalog.originalExercises.count { it.id !in displayed })
        assertEquals(545, catalog.libraryExercises.map { it.oldFamily }.distinct().size)
        assertTrue(withdrawn.none { it.id in displayed || it.id in automatic })
        assertTrue(withdrawn.all { catalog.byId.containsKey(it.id) })
        assertTrue(catalog.isWithdrawn("heelcontrolled"))
        assertTrue(catalog.isWithdrawn("heel_slow"))
        assertFalse(catalog.isWithdrawn("ankle"))
        assertNotNull(catalog.byId[legacyBike])
        assertFalse(legacyBike in displayed)
        assertFalse(legacyBike in automatic)
        assertTrue("gym-bike30" in automatic)
        assertTrue("pushup_slow" in displayed)
        assertFalse("seatedpunch" in automatic)
        assertFalse("plank" in automatic)
        assertFalse("neck" in automatic)
        assertTrue("ankle" in automatic)
        val landing = catalog.byId.getValue("new46-droplanding")
        assertTrue(CatalogFocus.PREPARATION.includes(landing, null))
        assertFalse(CatalogFocus.IMPACT.includes(landing, null))
        assertTrue(CatalogFocus.POWER.includes(catalog.byId.getValue("new46-sprintbrake"), Category.CARDIO))
    }

    @Test fun intervalsUseDeclaredEquipmentAndFitTheTimeWindow() {
        val planner = Planner(catalog, rules)
        val home = profile.copyJson().put("difficulty", "intermediate")
            .put("materials46", arr(listOf("space", "jumprope")))
            .put("capabilities46", arr(listOf("landing")))
        fun offers(p: org.json.JSONObject, mode: String, avoidImpact: Boolean = false, intervals: Boolean = true) =
            planner.generateRoutineOptions(mode, "cardio", 1, 15, "test", p, obj(), today,
                avoidImpact = avoidImpact, cardioIntervals = intervals)
        val outdoors = offers(home, "offline")
        val ids = outdoors.flatMap { it.a("items").objects() }.map { it.s("id") }
        assertTrue("performance-outdoor-run" in ids)
        assertTrue("new46-jumpropealt" in ids)
        assertTrue(offers(home, "offline", avoidImpact = true).isEmpty())
        assertTrue(offers(home.copyJson().put("capabilities46", arr(emptyList<String>())), "offline").isEmpty())
        val gym = home.copyJson().put("equipment", "gym").put("gymEquipment23", arr(listOf("rower")))
        val gymIntervals = offers(gym, "gym", avoidImpact = true)
        assertEquals(listOf("gym-row-erg23"), gymIntervals.flatMap { it.a("items").objects() }.map { it.s("id") })
        assertTrue(offers(gym, "gym", intervals = false).isNotEmpty())
        for (routine in outdoors + gymIntervals) {
            val steps = planner.steps(routine, home)
            assertEquals("preparation", steps.first().s("kind"))
            assertEquals("cooldown", steps.last().s("kind"))
            assertTrue(steps.count { it.s("kind") == "exercise" } >= 2)
            assertTrue(steps.filter { it.s("kind") == "exercise" }.all { it.s("unit") == "sec" })
            assertTrue(planner.minutes(routine, home) <= 15)
        }
        assertEquals(Category.MOBILITY, catalog.classification("new46-bentcalfstretch")?.category)
        assertEquals(Category.MOBILITY, catalog.classification("new46-hip9090lift")?.category)
        assertTrue(catalog.originalExercise("performance-weighted-pullup")!!.recordsExternalLoad)
    }

    @Test fun calisthenicsProgressionsStayInTheLibraryAndContinuousRunningIsOptIn() {
        val ids = listOf("performance-ring-false-grip", "performance-ring-transition",
            "performance-strict-ring-muscle-up", "performance-advanced-tuck-lever", "performance-one-leg-lever")
        assertTrue(ids.all { id -> catalog.libraryExercises.any { it.id == id } })
        assertTrue(ids.none { id -> catalog.automaticExercises.any { it.id == id } })
        assertEquals(listOf("performance-ring-transition"),
            performanceProgressions.getValue("performance-ring-false-grip").nextIds.toList())
        assertEquals(listOf("performance-advanced-tuck-lever"),
            performanceProgressions.getValue("new46-tucklever").nextIds.toList())
        assertTrue(performanceProgressions.values.flatMap { it.nextIds.toList() }.all {
            catalog.originalExercise(it) != null
        })
        val planner = Planner(catalog, rules)
        val home = profile.copyJson().put("difficulty", "intermediate")
            .put("materials46", arr(listOf("space"))).put("capabilities46", arr(listOf("landing")))
        fun running(level: Int, profile: org.json.JSONObject, avoidImpact: Boolean = false) =
            planner.generateRoutineOptions("offline", "cardio", level, 15, "test", profile,
                obj(), today, avoidImpact = avoidImpact, cardioContinuous = true)
        val routine = running(1, home).single()
        val steps = planner.steps(routine, home)
        assertEquals(listOf("preparation", "exercise", "cooldown"), steps.map { it.s("kind") })
        assertEquals("performance-continuous-run", steps[1].s("id"))
        assertEquals(420, steps[1].i("seconds"))
        assertEquals(15, planner.minutes(routine, home))
        assertTrue(running(0, home).isEmpty())
        assertTrue(running(1, home, avoidImpact = true).isEmpty())
        assertTrue(running(1, home.copyJson().put("materials46", arr(emptyList<String>()))).isEmpty())
    }

    @Test fun gymAndDumbbellAdditionsRequireTheirActualEquipment() {
        val planner = Planner(catalog, rules)
        val dumbbells = profile.copyJson().put("difficulty", "intermediate").put("equipment", "db")
            .put("dumbbellsAvailable", true).put("bench", true).put("step", true)
        val row = checkNotNull(catalog.originalExercise("performance-db-chest-supported-row"))
        val step = checkNotNull(catalog.originalExercise("performance-db-step-up"))
        val deadlift = checkNotNull(catalog.originalExercise("performance-high-handle-trapbar-deadlift"))
        assertFalse(rules.compatible(row, dumbbells))
        dumbbells.put("materials46", arr(listOf("inclinebench")))
        assertTrue(rules.compatible(row, dumbbells))
        assertTrue(rules.compatible(step, dumbbells))
        assertFalse(rules.compatible(step, dumbbells.copyJson().put("step", false)))
        assertFalse(rules.compatible(row, dumbbells.copyJson().put("dumbbellsAvailable", false)))
        assertFalse(rules.compatible(deadlift, dumbbells))
        val gym = profile.copyJson().put("difficulty", "intermediate").put("equipment", "gym")
        gym.put("materials46", arr(listOf("trapbar")))
        assertFalse(rules.compatible(deadlift, gym))
        gym.put("materials46", arr(listOf("trapbar", "plate")))
        assertTrue(rules.compatible(deadlift, gym))
        assertTrue(catalog.byId.getValue("hx_lower_0002").raw.b("sourceOnly"))
        val technical = checkNotNull(MonarchProfileAdapter.from(gym, today))
        val metadata = checkNotNull(planner.planningSource(gym).byId(deadlift.id))
        assertTrue(com.huntersystem.app.planning.EligibilityPolicy.compatible(metadata, technical, 2))
        gym.put("gymEquipment23", arr(listOf("skierg")))
        fun intervals(p: org.json.JSONObject) =
            planner.generateRoutineOptions("gym", "cardio", 1, 15, "test", p, obj(), today,
                avoidImpact = true, cardioIntervals = true)
        val routine = intervals(gym).single()
        assertEquals("gym-skierg30", routine.a("items").objects().single().s("id"))
        assertTrue(planner.steps(routine, gym).any { it.s("id") == "gym-skierg30" && it.s("unit") == "sec" })
        assertTrue(intervals(gym.copyJson().put("gymEquipment23", arr(listOf("bike")))).none {
            it.a("items").objects().any { item -> item.s("id") == "gym-skierg30" }
        })
    }

    @Test fun onlyPublishedEssentialMobilityExercisesAreSelectable() {
        val active = catalog.exercises.groupBy { catalog.classification(it.id)!!.category }
        assertEquals(Category.entries.toSet(), active.keys)
        assertEquals(ExerciseTaxonomy.essentialMobility.keys.filterNot(catalog::isWithdrawn).toSet(),
            active.getValue(Category.MOBILITY).map { it.id }.toSet())
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
