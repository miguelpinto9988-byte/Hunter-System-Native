package com.huntersystem.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** QA gate: no sustituir material existente si falta vídeo físicamente aprobado. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class Hunter120AssetGateTest{
 @Test fun originalSquatRemainsSvgWithoutApproval(){
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  assertNull(ExerciseVideoAssets.approvedVideo(ctx,"demos30/squat.svg"))
 }
 @Test fun pendingExpansionCannotBeUpgradedByNamingAlone(){
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  assertNull(ExerciseVideoAssets.approvedVideo(ctx,"demos30/hx_upper_0001.svg"))
 }
 @Test fun invalidIdIsRejected(){
  val ctx=ApplicationProvider.getApplicationContext<Context>()
  assertNull(ExerciseVideoAssets.approvedVideo(ctx,"../../invalid!.svg"))
 }
}
