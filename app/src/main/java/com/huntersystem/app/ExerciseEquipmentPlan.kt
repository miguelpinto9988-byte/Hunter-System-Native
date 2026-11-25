package com.huntersystem.app

internal enum class DemoGear(val asset: String, val label: String) {
    DUMBBELL("dumbbell", "Mancuerna"), KETTLEBELL("kettlebell", "Kettlebell"),
    BARBELL("barbell", "Barra con discos"), TRAP_BAR("trapbar", "Barra hexagonal"),
    PLATE("plate", "Disco de pesas"), TOWEL("towel", "Toalla"), BAND("band", "Banda elástica"),
    CABLE("weight-stack", "Polea"), SMITH("smith", "Máquina Smith"), LANDMINE("barbell", "Barra landmine"),
    LEG_PRESS("platform", "Prensa de piernas"), GUIDED_SQUAT("smith", "Máquina de sentadillas"),
    LEG_LEVER("roller", "Máquina con rodillo"), TIBIALIS("roller", "Máquina de tibial anterior"),
    ARM_LEVER("handle", "Máquina con palancas"), HIP_LEVER("roller", "Máquina de cadera"),
    ASSISTED("bar", "Máquina asistida"), RACK("smith", "Rack"), ANCHOR("anchor", "Anclaje"),
    BIKE("wheel", "Bicicleta estática"), AIR_BIKE("fan", "Bicicleta de aire"),
    RECUMBENT("wheel", "Bicicleta reclinada"), ROWER("console", "Ergómetro de remo"),
    TREADMILL("console", "Cinta de correr"), ELLIPTICAL("fan", "Elíptica"),
    STEP_MILL("stepmill", "Escaladora"), SKI_ERG("weight-stack", "Ergómetro de esquí"),
    BALL("ball", "Balón"), MED_BALL("ball", "Balón medicinal"), SWISS_BALL("ball", "Pelota suiza"),
    SOCCER("soccer", "Balón de fútbol"), BASKETBALL("basketball", "Balón de baloncesto"),
    VOLLEYBALL("volleyball", "Balón de voleibol"), RACKET("racket", "Raqueta"),
    JUMP_ROPE("handle", "Cuerda de saltar"), BATTLE_ROPE("handle", "Cuerdas de batalla"),
    ROPE("handle", "Cuerda"), STRAP("handle", "Correa"), RINGS("ring", "Anillas"),
    PULL_BAR("bar", "Barra de dominadas"), LOW_BAR("bar", "Barra baja"),
    PARALLETTES("bar", "Paralelas"), STICK("bar", "Bastón"), SLIDER("slider", "Disco deslizante"),
    FOAM_ROLLER("roller", "Rodillo de espuma"), AB_WHEEL("wheel", "Rueda abdominal"),
    BENCH("bench", "Banco"), INCLINED_SUPPORT("incline", "Banco inclinado"), CHAIR("chair", "Silla"), TABLE("bench", "Mesa de apoyo"),
    STEP("platform", "Escalón"), BOX("platform", "Cajón"), MAT("mat", "Colchoneta"),
    WALL("wall", "Pared"), CONES("cone", "Conos"), LADDER("ladder", "Escalera de agilidad"),
    HURDLE("hurdle", "Valla"), SLED("sled", "Trineo"), HARNESS("harness", "Arnés");
}

internal enum class GearMount { BOTH, NEAR, FAR, SHARED, ANKLE, ANKLE_LOOP, KNEE, HIP, FLOOR, FOOT_TETHER, FOREHEAD }
internal enum class LegRollerContact { FRONT_SHIN, BACK_SHIN }
internal enum class ArmLeverContact { HAND, HIGH_HAND, CHEST_SUPPORT, ELBOW_PAD, OVERHEAD_ELBOW }

internal data class DemoApparatus(
    val gear: DemoGear,
    val mount: GearMount = GearMount.SHARED,
    val label: String = gear.label,
    val pulleyHeight: Float = .52f,
    val bilateralTether: Boolean = false,
    val ropeGrip: Boolean = false,
    val legRollerContact: LegRollerContact = LegRollerContact.FRONT_SHIN,
    val armLeverContact: ArmLeverContact = ArmLeverContact.HAND
)

internal object ExerciseEquipmentPlan {
    private val materialGear = mapOf(
        "db" to DemoGear.DUMBBELL, "kb" to DemoGear.KETTLEBELL, "barbell" to DemoGear.BARBELL,
        "safetybar" to DemoGear.BARBELL, "trapbar" to DemoGear.TRAP_BAR, "plate" to DemoGear.PLATE,
        "landmine" to DemoGear.LANDMINE, "smith" to DemoGear.SMITH, "rack" to DemoGear.RACK,
        "cable" to DemoGear.CABLE, "band" to DemoGear.BAND, "towel" to DemoGear.TOWEL,
        "anchor" to DemoGear.ANCHOR, "footanchor" to DemoGear.ANCHOR,
        "ball" to DemoGear.BALL, "medball" to DemoGear.MED_BALL, "slamball" to DemoGear.MED_BALL,
        "swissball" to DemoGear.SWISS_BALL, "soccer_ball" to DemoGear.SOCCER,
        "basketball" to DemoGear.BASKETBALL, "volleyball" to DemoGear.VOLLEYBALL,
        "tennis_racket" to DemoGear.RACKET, "jumprope" to DemoGear.JUMP_ROPE, "light_rope" to DemoGear.JUMP_ROPE,
        "rope" to DemoGear.ROPE, "battlerope" to DemoGear.BATTLE_ROPE, "suspension" to DemoGear.STRAP,
        "strap" to DemoGear.STRAP, "rings" to DemoGear.RINGS, "pullbar" to DemoGear.PULL_BAR,
        "pullup_bar" to DemoGear.PULL_BAR, "lowbar" to DemoGear.LOW_BAR, "parallels" to DemoGear.PARALLETTES,
        "parallettes" to DemoGear.PARALLETTES, "stick" to DemoGear.STICK, "lightlever" to DemoGear.STICK,
        "slider" to DemoGear.SLIDER, "foamroller" to DemoGear.FOAM_ROLLER, "abwheel" to DemoGear.AB_WHEEL,
        "bench" to DemoGear.BENCH, "inclinebench" to DemoGear.INCLINED_SUPPORT, "romanchair" to DemoGear.INCLINED_SUPPORT,
        "chair" to DemoGear.CHAIR, "table" to DemoGear.TABLE, "step" to DemoGear.STEP, "plyobox" to DemoGear.BOX,
        "mat" to DemoGear.MAT, "wall" to DemoGear.WALL, "throwwall" to DemoGear.WALL, "doorframe" to DemoGear.WALL,
        "cones" to DemoGear.CONES, "ladder" to DemoGear.LADDER, "hurdle" to DemoGear.HURDLE,
        "sled" to DemoGear.SLED, "harness" to DemoGear.HARNESS, "support" to DemoGear.TABLE
    )
    private val motionGear = mapOf(
        "ball" to DemoGear.SOCCER, "basketball" to DemoGear.BASKETBALL, "volleyball" to DemoGear.VOLLEYBALL,
        "rope" to DemoGear.JUMP_ROPE, "bar" to DemoGear.PULL_BAR, "barbell" to DemoGear.BARBELL,
        "dumbbell" to DemoGear.DUMBBELL, "dumbbells" to DemoGear.DUMBBELL, "goblet-weight" to DemoGear.DUMBBELL,
        "racket" to DemoGear.RACKET, "parallettes" to DemoGear.PARALLETTES, "bike" to DemoGear.BIKE,
        "wall" to DemoGear.WALL, "rear-wall" to DemoGear.WALL, "tibialis-machine" to DemoGear.TIBIALIS
    )
    private val props = mapOf("wall" to DemoGear.WALL, "step" to DemoGear.STEP, "chair" to DemoGear.CHAIR,
        "bench" to DemoGear.BENCH, "table" to DemoGear.TABLE, "incline" to DemoGear.INCLINED_SUPPORT)

    fun forProp(name: String): DemoGear? = props[name]

    fun forExercise(exercise: Exercise, motion: ExerciseMotion): List<DemoApparatus> {
        val name = normalize(exercise.name)
        val family = exercise.oldFamily
        val material = (exercise.raw.a("materialRequirements").strings() +
            exercise.raw.a("materials46").strings() + exercise.gymRequirements).toSet()
        val kinds = material.mapNotNull(materialGear::get).toMutableSet()
        if ("cardio" in material) cardio(family,name)?.let(kinds::add)
        if ("machine" in material || "maquina" in name && ("gym" in material || name.startsWith("prensa de piernas")))
            kinds += machine(name)
        motionGear[motion.equipment]?.let(kinds::add)
        if (exercise.equipment=="db" && kinds.none { it in setOf(DemoGear.DUMBBELL,DemoGear.KETTLEBELL,DemoGear.CABLE) })
            kinds += DemoGear.DUMBBELL
        if (DemoGear.KETTLEBELL in kinds || DemoGear.CABLE in kinds && "mancuerna" !in name)
            kinds -= DemoGear.DUMBBELL
        if ("toalla" in name || motion.authoredFloor?.isHamstringSlide == true) kinds += DemoGear.TOWEL
        if (DemoGear.SOCCER in kinds) kinds -= DemoGear.BALL
        if (DemoGear.TRAP_BAR in kinds) kinds -= DemoGear.PLATE
        if (DemoGear.SMITH in kinds || DemoGear.LANDMINE in kinds) kinds -= DemoGear.BARBELL
        if (DemoGear.BAND in kinds || DemoGear.STRAP in kinds || DemoGear.BATTLE_ROPE in kinds)
            kinds -= DemoGear.ANCHOR
        if (motion.equipment=="tibialis-machine") kinds -= DemoGear.LEG_LEVER
        if (kinds.any { it in cardioMachines }) kinds.removeAll(setOf(DemoGear.BENCH,DemoGear.CHAIR))
        val represented = motion.props.mapNotNull { forProp(it.kind) }.toSet()
        val supports = setOf(DemoGear.BENCH,DemoGear.CHAIR,DemoGear.TABLE,DemoGear.INCLINED_SUPPORT)
        if (represented.any { it in supports }) kinds.removeAll(supports)
        if (DemoGear.BOX in kinds && DemoGear.STEP in represented) kinds -= DemoGear.BOX
        kinds.removeAll(represented)

        val single = if (name.contains("unilateral") || name.contains("un brazo") || "maleta" in name ||
            family in setOf("row","tricep","concentration","suitcase","diagonalraise","home-renegade23")) GearMount.NEAR
            else GearMount.SHARED
        val pulleyHeight = when {
            name.contains("baja") || "tobillo" in name || "tobillera" in name ||
                family in setOf("gym-hipflexc30","gym-abductc30","gym-adductc30","gym-kickc30","gym-pullthrough30") -> .14f
            name.contains("alta") || "jalon" in name || "skierg" in family ||
                "triceps" in name && "cruzada" !in name || "arrodillado" in name && "abdominal" in name -> .96f
            else -> .57f
        }
        return kinds.sortedBy(DemoGear::ordinal).map { gear ->
            val mount = when (gear) {
                DemoGear.DUMBBELL, DemoGear.KETTLEBELL -> when {
                    family == "home-renegade23" || family == "home-renegade-knee23" -> GearMount.BOTH
                    family in setOf("dbbridge","hipthrust") || "mancuerna sobre cadera" in name -> GearMount.HIP
                    single == GearMount.NEAR -> single
                    motion.equipment=="goblet-weight" || name.contains("con mancuerna ") ||
                        family in setOf("goblet","goblethold","dbsumo","sumodead","pullover") -> GearMount.SHARED
                    else -> GearMount.BOTH
                }
                DemoGear.TOWEL -> when {
                    family in setOf("prone","reverse") -> GearMount.FOREHEAD
                    "remo" in name -> GearMount.BOTH
                    else -> GearMount.FLOOR
                }
                DemoGear.BAND -> when {
                    "plantar" in name || "inversion de tobillo" in name || "eversion de tobillo" in name -> GearMount.FOOT_TETHER
                    "minibanda" in name && ("tobillo" in name || "pies" in name) -> GearMount.ANKLE_LOOP
                    "tobillo" in name || "tobillera" in name || "tibial" in name || "tobillo" in family ||
                        "minibanda en pies" in name -> GearMount.ANKLE
                    "rodillas" in name || "terminal de rodilla" in name || "minibanda" in name -> GearMount.KNEE
                    "cadera" in name && "arrastre" in name -> GearMount.HIP
                    else -> single
                }
                DemoGear.CABLE -> if (pulleyHeight == .14f &&
                    ("cadera" in name || "tobillo" in name || "tobillera" in name || "femoral" in name)) GearMount.ANKLE
                    else single
                DemoGear.SLIDER -> when {
                    "apertura de manos" in name -> GearMount.BOTH
                    family == "new46-slidereverse" -> GearMount.NEAR
                    else -> GearMount.FLOOR
                }
                DemoGear.BALL, DemoGear.MED_BALL -> if ("rodillas" in name) GearMount.KNEE else GearMount.SHARED
                DemoGear.SWISS_BALL -> if("pies en balon" in name || "femoral" in name) GearMount.ANKLE else GearMount.SHARED
                DemoGear.RACKET -> GearMount.NEAR
                DemoGear.ARM_LEVER -> if (single == GearMount.NEAR) single else GearMount.BOTH
                DemoGear.LEG_LEVER -> if ("talones" in name) GearMount.KNEE else GearMount.ANKLE
                DemoGear.HIP_LEVER -> if ("aduccion" in name || "abduccion" in name) GearMount.KNEE else GearMount.HIP
                DemoGear.GUIDED_SQUAT -> if ("cinturon" in name) GearMount.HIP else GearMount.SHARED
                else -> GearMount.SHARED
            }
            val label = if (gear in leverMachines && " · " in exercise.name) exercise.name.substringBefore(" · ")
                else if (gear in leverMachines) exercise.name else gear.label
            DemoApparatus(gear,mount,label,pulleyHeight,
                bilateralTether=gear==DemoGear.CABLE && ("desde poleas" in name || "en poleas" in name),
                ropeGrip=gear==DemoGear.CABLE && "con cuerda" in name,
                legRollerContact=if (gear==DemoGear.LEG_LEVER && "femoral" in name) LegRollerContact.BACK_SHIN
                    else LegRollerContact.FRONT_SHIN,
                armLeverContact=when (family) {
                    "gym-highrow30" -> ArmLeverContact.HIGH_HAND
                    "gym-supportrow30" -> ArmLeverContact.CHEST_SUPPORT
                    "gym-lateralm30" -> ArmLeverContact.ELBOW_PAD
                    "gym-pulloverm30" -> ArmLeverContact.OVERHEAD_ELBOW
                    else -> ArmLeverContact.HAND
                })
        }
    }

    val cardioMachines = setOf(DemoGear.BIKE,DemoGear.AIR_BIKE,DemoGear.RECUMBENT,DemoGear.ROWER,
        DemoGear.TREADMILL,DemoGear.ELLIPTICAL,DemoGear.STEP_MILL,DemoGear.SKI_ERG)
    private val leverMachines = setOf(DemoGear.LEG_PRESS,DemoGear.GUIDED_SQUAT,DemoGear.LEG_LEVER,DemoGear.ARM_LEVER,
        DemoGear.HIP_LEVER,DemoGear.ASSISTED)

    private fun cardio(id: String, name: String): DemoGear? = when {
        "recumbent" in id || "reclinada" in name -> DemoGear.RECUMBENT
        "airbike" in id || "bicicleta de aire" in name -> DemoGear.AIR_BIKE
        "bike" in id || "cycling" in id || "bicicleta" in name -> DemoGear.BIKE
        "row-erg" in id || "ergometro de remo" in name -> DemoGear.ROWER
        "treadmill" in id || "en cinta" in name -> DemoGear.TREADMILL
        "elliptical" in id || "eliptica" in name -> DemoGear.ELLIPTICAL
        "stepmill" in id || "escaladora" in name -> DemoGear.STEP_MILL
        "skierg" in id || "ergometro de esqui" in name -> DemoGear.SKI_ERG
        else -> null
    }

    private fun machine(name: String): DemoGear = when {
        "prensa de piernas" in name -> DemoGear.LEG_PRESS
        "hack" in name || "pendulo" in name || "cinturon" in name || "palanca en v" in name -> DemoGear.GUIDED_SQUAT
        "femoral" in name || "extension de rodilla" in name || "tibial" in name || "talones" in name -> DemoGear.LEG_LEVER
        "asistida" in name -> DemoGear.ASSISTED
        "cadera" in name || "gluteo" in name || "tronco" in name || "torso" in name || "abdominal" in name -> DemoGear.HIP_LEVER
        else -> DemoGear.ARM_LEVER
    }
}
