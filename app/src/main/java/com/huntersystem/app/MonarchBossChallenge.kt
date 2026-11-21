package com.huntersystem.app

import com.huntersystem.app.planning.ActivityMetadata
import com.huntersystem.app.planning.Category
import com.huntersystem.app.planning.EligibilityPolicy
import com.huntersystem.app.planning.Experience
import com.huntersystem.app.planning.TrainingProfile
import com.huntersystem.app.planning.VolumeUnit
import org.json.JSONObject
import java.time.Duration
import java.time.LocalDate
import kotlin.random.Random

/** Escala del desafío inspirada en los encuentros de Jinwoo, independiente del plan diario. */
internal enum class MonarchBoss(
    val title:String,
    val encounter:String,
    val difficulty:String,
    val repetitions:IntRange,
    val artwork:String
) {
    IGRIS("Igris","Guardia del Umbral","Inicial",75..110,"profile-monarca/training.webp"),
    BARUKA("Baruka","Puerta Roja","Intermedia",90..125,"monarch-boss/baruka.webp"),
    TUSK("Kargalgan (Tusk)","Chamán de los Altos Orcos","Avanzada",105..140,"monarch-boss/kargalgan.webp"),
    BARAN("Baran","Castillo Demoníaco","Élite",120..155,"monarch-boss/baran.webp"),
    BERU("Beru","Rey Hormiga de Jeju","Monarca",135..170,"monarch-boss/beru.webp");

    val rangeLabel get()="${repetitions.first}–${repetitions.last}"

    // Beru se guardaba con el rango de Igris antes de incorporar las dificultades.
    fun acceptsSavedTarget(target:Int)=target in repetitions || (this==BERU&&target in IGRIS.repetitions)
}

internal data class BossExerciseGoal(
    val id:String,val target:Int,val count:Int=0,val unit:VolumeUnit=VolumeUnit.REPETITIONS
) {
    val unitLabel get()=if(unit==VolumeUnit.SECONDS) "segundos" else "repeticiones"
    val shortUnit get()=if(unit==VolumeUnit.SECONDS) "s" else "reps"
}
internal data class BossOffer(val boss:MonarchBoss,val goals:List<BossExerciseGoal>)
internal enum class BossStatus { ACTIVE, COMPLETED, EXPIRED, ABANDONED }

internal val monarchBossDuration=Duration.ofDays(5).toMillis()

internal data class BossRun(
    val offer:BossOffer,
    val startedAt:Long,
    val closedStatus:BossStatus?=null
) {
    val deadline get()=startedAt+monarchBossDuration
    fun status(now:Long)=closedStatus ?: if(now>=deadline) BossStatus.EXPIRED else BossStatus.ACTIVE
    fun day(now:Long)=((now-startedAt)/Duration.ofDays(1).toMillis()+1).coerceIn(1L,5L).toInt()
}

/** Un sorteo para todo el ciclo. El calendario diario no interviene en la selección. */
internal fun planMonarchBoss(
    boss:MonarchBoss, profile:TrainingProfile, candidates:List<ActivityMetadata>, seed:Long,
    hasDemonstration:(String)->Boolean={true}
):BossOffer? {
    val random=Random(seed)
    val pool=candidates.filter { it.category!=Category.MOBILITY&&it.workUnit!=null&&EligibilityPolicy.compatible(it,profile) }
        .sortedBy { it.id }.shuffled(random)
    val selected=mutableListOf<ActivityMetadata>()
    val names=mutableSetOf<String>()
    val available=mutableMapOf<String,Boolean>()
    fun add(candidate:ActivityMetadata):Boolean {
        val name=normalize(candidate.displayName.ifBlank { candidate.id }).trim().replace(Regex("\\s+")," ")
        if(name in names||selected.any { it.id==candidate.id }||!available.getOrPut(candidate.id) { hasDemonstration(candidate.id) })return false
        selected+=candidate
        names+=name
        return true
    }
    // Give each compatible category a slot before filling with another movement type.
    for(group in pool.groupBy { it.category }.values)group.firstOrNull(::add)
    for(candidate in pool) {
        if(selected.size==5)break
        if(selected.none { it.typeId==candidate.typeId })add(candidate)
    }
    for(candidate in pool) {
        if(selected.size==5)break
        add(candidate)
    }
    if(selected.size<5)return null
    val limits=boss.repetitions
    val range=when(profile.experience){
        Experience.BEGINNER->limits.first..(limits.first+10)
        Experience.INTERMEDIATE->(limits.first+5)..(limits.last-10)
        Experience.ADVANCED->(limits.first+15)..limits.last
    }
    return BossOffer(boss,selected.map { BossExerciseGoal(it.id,range.random(random),unit=checkNotNull(it.workUnit)) })
}

internal fun Exercise.isSingleMonarchMovement(): Boolean =
    raw.a("sequenceParts").length()==0 && !raw.b("compositeSequence") &&
        !id.startsWith("sport-seq-")

internal class MonarchBossChallenges(private val store:NativeStore) {
    private val source by lazy { store.planner.planningSource(store.adjusted()) }
    private val profile=MonarchProfileAdapter.from(store)

    fun current():BossRun? {
        val row=store.state.optJSONObject(RUN_KEY)?:return null
        val boss=MonarchBoss.entries.firstOrNull { it.name==row.s("boss") }?:return null
        val start=row.optLong("startedAt")
        val goals=row.a("goals").objects().map {
            val unit=VolumeUnit.entries.firstOrNull { unit -> unit.name==it.s("unit",VolumeUnit.REPETITIONS.name) }
                ?:return null
            BossExerciseGoal(it.s("id"),it.i("target"),it.i("count"),unit)
        }
        if(start<=0||start>Long.MAX_VALUE-monarchBossDuration||goals.size!=5||
            goals.map { it.id }.distinct().size!=5||
            goals.any { it.id.isBlank()||!boss.acceptsSavedTarget(it.target)||it.count !in 0..it.target }) return null
        val closed=BossStatus.entries.firstOrNull { it!=BossStatus.ACTIVE&&it.name==row.s("closedStatus") }
        return BossRun(BossOffer(boss,goals),start,closed)
    }

    fun offer(date:LocalDate,boss:MonarchBoss=MonarchBoss.IGRIS):BossOffer? {
        val configured=profile?:return null
        val seed=date.toEpochDay() xor (current()?.startedAt?:0L) xor boss.name.hashCode().toLong()
        val individual=source.all().filter { store.catalog.byId[it.id]?.isSingleMonarchMovement()==true }
        return planMonarchBoss(boss,configured,individual,seed) { id ->
            ExerciseMotions.load(store.context,id)!=null
        }
    }

    fun compatible(id:String):Boolean {
        val configured=profile?:return false
        if(store.catalog.byId[id]?.isSingleMonarchMovement()!=true)return false
        val exercise=source.byId(id)?:return false
        return exercise.category!=Category.MOBILITY&&exercise.workUnit!=null&&
            EligibilityPolicy.compatible(exercise,configured)
    }

    fun start(offer:BossOffer, date:LocalDate=LocalDate.now(), now:Long=System.currentTimeMillis()) {
        synchronized(store){
            val previous=current()
            require(previous?.status(now)!=BossStatus.ACTIVE){"Ya hay un desafío Monarca activo."}
            // El usuario acepta exactamente los objetivos que vio en la propuesta.
            require(MonarchBossChallenges(store).offer(date,offer.boss)==offer){"La propuesta cambió. Vuelve a revisar el desafío."}
            save(BossRun(offer,now),previous?.copy(closedStatus=previous.status(now)))
        }
    }

    fun record(start:Long,id:String,amount:Int,expected:Int,now:Long=System.currentTimeMillis()) {
        synchronized(store){
            val run=requireActive(start,now)
            val target=run.offer.goals.firstOrNull { it.id==id }?:error("Ejercicio fuera del desafío.")
            require(MonarchBossChallenges(store).compatible(id)){"El ejercicio requiere un perfil compatible."}
            require(target.count==expected){"El avance cambió. Revisa el contador antes de guardar."}
            require(amount in 1..(target.target-target.count)){"Ingresa una cantidad válida de ${target.unitLabel}."}
            val updated=run.offer.goals.map { if(it.id==id) it.copy(count=it.count+amount) else it }
            save(run.copy(offer=run.offer.copy(goals=updated),
                closedStatus=BossStatus.COMPLETED.takeIf { updated.all { it.count==it.target } }))
        }
    }

    fun abandon(start:Long,now:Long=System.currentTimeMillis()) = synchronized(store) {
        save(requireActive(start,now).copy(closedStatus=BossStatus.ABANDONED))
    }

    private fun requireActive(start:Long,now:Long):BossRun =
        current()?.takeIf { it.startedAt==start&&it.status(now)==BossStatus.ACTIVE }
            ?: error("El desafío Monarca ya no está activo.")

    private fun save(run:BossRun,archive:BossRun?=null) {
        val oldRun=store.state.opt(RUN_KEY)
        val oldHistory=store.state.opt(HISTORY_KEY)
        if(archive!=null)store.state.put(HISTORY_KEY,arr(store.state.a(HISTORY_KEY).objects()+archive.json()))
        store.state.put(RUN_KEY,run.json())
        try {
            store.save()
        } catch(error:Exception) {
            store.state.put(RUN_KEY,oldRun)
            store.state.put(HISTORY_KEY,oldHistory)
            throw error
        }
    }

    private fun BossRun.json():JSONObject = obj(
        "boss" to offer.boss.name,"startedAt" to startedAt,"closedStatus" to closedStatus?.name.orEmpty(),
        "goals" to arr(offer.goals.map { obj("id" to it.id,"target" to it.target,"count" to it.count,"unit" to it.unit.name) })
    )

    private companion object {
        const val RUN_KEY="monarchBossChallenge"
        const val HISTORY_KEY="monarchBossHistory"
    }
}
