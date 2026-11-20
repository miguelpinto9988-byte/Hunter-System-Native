package com.huntersystem.app.planning

import java.time.LocalDate

/** Orquesta decisiones; los historiales y las fuentes se leen fuera del motor. */
data class TrainingHistory(
    val efforts: List<EffortObservation> = emptyList(),
    val participation: List<Participation> = emptyList(),
    val interruptions: List<SessionInterruption> = emptyList(),
    val phases: Map<Category, TechnicalPhase> = emptyMap()
)

data class CoordinatedTrainingPlan(
    val mission: DailyMission,
    val adaptation: Adaptation,
    val checkpoint: TechnicalCheckpoint?
)

object TrainingSessionCoordinator {
    fun plan(
        date: LocalDate,
        profile: TrainingProfile,
        source: ActivityRepositoryInterface,
        history: TrainingHistory,
        pinned: DailyMission? = null,
        historicalPriority: Map<String, Int> = emptyMap(),
        recentWeekIds: Set<String> = emptySet(),
        variation: String = ""
    ): CoordinatedTrainingPlan {
        val category = profile.category(date)
        val adaptation = category?.let {
            CategoryEffortPolicy.decide(history.efforts, date, it, history.interruptions)
        } ?: Adaptation.MAINTAIN
        val mission = DailyMissionPlanner.plan(date, profile, source, history.efforts, history.phases,
            pinned, historicalPriority, recentWeekIds, variation, history.participation, history.interruptions)
        val checkpoint = category?.let {
            TechnicalCheckpointPolicy.review(profile, it, date, history.efforts,
                history.phases[it] ?: TechnicalPhase(), history.interruptions)
        }
        return CoordinatedTrainingPlan(mission, adaptation, checkpoint)
    }

    fun reviewVolume(
        date: LocalDate,
        activity: ActivityMetadata,
        source: VolumeProfileInterface,
        observations: List<VolumeObservation>,
        blockId: String,
        history: TrainingHistory
    ): VolumeRecommendation = VolumeProgressionEngine.review(
        date, activity, source.find(activity.id), observations, blockId, history.efforts, history.interruptions
    )
}
