package com.huntersystem.app

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.isActive
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource
import kotlin.math.min

/** Vector rig fitted to the ORIGINAL media2d art, never to removed demos30 SVGs. */
internal data class OriginalVectorRig(
    val frames:List<List<Offset>>,
    val offsets:List<Offset>,
    val footVectors:Map<Int,Offset>,
    val translate:Offset,
    val anatomyScale:Float,
    val viewWidth:Float,
    val viewHeight:Float,
    val calibratedAnchors:Int
){
    fun sample(phase:Float):List<Offset>{
        val t=phase.coerceIn(0f,1f)*frames.size
        val a=t.toInt().coerceIn(0,frames.lastIndex)
        val b=(a+1)%frames.size
        val k=(t-a).coerceIn(0f,1f)
        return frames[a].indices.map{i->
            val p=frames[a][i];val q=frames[b][i];val o=offsets[i]
            Offset(p.x+(q.x-p.x)*k+o.x,p.y+(q.y-p.y)*k+o.y)
        }
    }
}

/** Parses the 11 original joint tracks plus semantic anchors embedded in media2d. */
internal object OriginalVectorRigAssets{
    private val allowed=Regex("media2d/[A-Za-z0-9_-]{1,100}\\.svg")
    private val numeric=Regex("[-+]?(?:[0-9]*\\.)?[0-9]+")

    private fun floatPair(value:String):Offset?{
        val n=numeric.findAll(value).mapNotNull{it.value.toFloatOrNull()}.toList()
        return if(n.size>=2) Offset(n[0],n[1]) else null
    }
    private fun nodes(element:Element,name:String):List<Element>{
        val found=element.getElementsByTagName(name)
        return (0 until found.length).mapNotNull{found.item(it) as? Element}
    }
    private fun group(element:Element,attribute:String,value:String?=null):Element?=
        nodes(element,"g").firstOrNull{
            it.hasAttribute(attribute)&&(value==null||it.getAttribute(attribute)==value)
        }
    fun load(context:Context,asset:String):OriginalVectorRig?{
        if(!allowed.matches(asset))return null
        return runCatching{
            val id=asset.substringAfter('/').removeSuffix(".svg")
            val source=context.assets.open("demos30/$id.json").bufferedReader().use{it.readText()}
            val json=JSONObject(source)
            val poses=json.getJSONArray("frames")
            require(poses.length() in 2..240)
            val frames=List(poses.length()){index->
                val joints=poses.getJSONObject(index).getJSONArray("p")
                require(joints.length()==11)
                List(11){j->
                    val point=joints.getJSONArray(j)
                    require(point.length()>=2)
                    val v=Offset(point.getDouble(0).toFloat(),point.getDouble(1).toFloat())
                    require(v.x.isFinite()&&v.y.isFinite())
                    v
                }
            }
            val svg=context.assets.open(asset).bufferedReader().use{it.readText()}
            require(!Regex("<!\\s*(DOCTYPE|ENTITY)|<\\s*(script|foreignObject)\\b",RegexOption.IGNORE_CASE).containsMatchIn(svg))
            val factory=DocumentBuilderFactory.newInstance().apply{
                isNamespaceAware=true
                isExpandEntityReferences=false
                runCatching{setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)}
                runCatching{setFeature("http://xml.org/sax/features/external-general-entities",false)}
                runCatching{setFeature("http://xml.org/sax/features/external-parameter-entities",false)}
            }
            val root=factory.newDocumentBuilder().parse(InputSource(StringReader(svg))).documentElement
            require(root.localName=="svg")
            val view=numeric.findAll(root.getAttribute("viewBox")).mapNotNull{it.value.toFloatOrNull()}.toList()
            require(view.size==4&&view[2]>0f&&view[3]>0f)
            val anatomy=group(root,"data-anatomy22","true")?:error("No anatomy anchors")
            val anchors=frames[0].toMutableList()
            var calibrated=0
            fun anchor(index:Int,value:Offset?){
                if(value!=null&&value.x.isFinite()&&value.y.isFinite()){
                    anchors[index]=value;calibrated++
                }
            }
            val head=group(anatomy,"data-head-view")
            val headEllipse=head?.let{nodes(it,"ellipse").firstOrNull()}
            anchor(0,headEllipse?.let{Offset(it.getAttribute("cx").toFloat(),it.getAttribute("cy").toFloat())})
            for(index in listOf(7,9)){
                val knee=group(anatomy,"data-knee22",index.toString())
                val circle=knee?.let{nodes(it,"circle").firstOrNull()}
                anchor(index,circle?.let{Offset(it.getAttribute("cx").toFloat(),it.getAttribute("cy").toFloat())})
            }
            val feet=mutableMapOf<Int,Offset>()
            for(index in listOf(8,10)){
                val foot=group(anatomy,"data-foot22",index.toString())
                val heel=foot?.let{floatPair(it.getAttribute("data-heel22"))}
                val toe=foot?.let{floatPair(it.getAttribute("data-toe22"))}
                anchor(index,heel)
                if(heel!=null&&toe!=null)feet[index]=toe-heel
            }
            // Hands are direct-child ellipses; the nested head ellipse is excluded.
            val palms=(0 until anatomy.childNodes.length).mapNotNull{
                anatomy.childNodes.item(it) as? Element
            }.filter{
                it.localName=="ellipse" &&
                (it.getAttribute("rx").toFloatOrNull()?:0f) in 7f..14f
            }.map{Offset(it.getAttribute("cx").toFloat(),it.getAttribute("cy").toFloat())}
            val assigned=mutableSetOf<Int>()
            for(index in listOf(4,6)){
                val pick=palms.indices.filter{it !in assigned}
                    .minByOrNull{(palms[it]-frames[0][index]).getDistance()}
                if(pick!=null&&(palms[pick]-frames[0][index]).getDistance()<90f){
                    assigned+=pick;anchor(index,palms[pick])
                }
            }
            val transform=anatomy.getAttribute("transform")
            fun numbersFor(name:String):List<Float>{
                val inside=Regex("$name\\(([^)]*)\\)").find(transform)?.groupValues?.get(1)?:""
                return numeric.findAll(inside).mapNotNull{it.value.toFloatOrNull()}.toList()
            }
            val translate=numbersFor("translate")
            val scale=numbersFor("scale")
            val translation=Offset(translate.getOrElse(0){0f},translate.getOrElse(1){0f})
            val artScale=scale.firstOrNull()?:1f
            require(artScale>0f)
            OriginalVectorRig(
                frames=frames,
                offsets=anchors.indices.map{anchors[it]-frames[0][it]},
                footVectors=feet,
                translate=translation,
                anatomyScale=artScale,
                viewWidth=view[2],
                viewHeight=view[3],
                calibratedAnchors=calibrated
            )
        }.getOrNull()
    }
}

private val originalBones=listOf(
    0 to 1, 1 to 2, 1 to 3, 3 to 4, 1 to 5, 5 to 6,
    2 to 7, 7 to 8, 2 to 9, 9 to 10
)

/** Independent structural layer, sharing the exact SVG viewBox and anatomy transform. */
@Composable
internal fun OriginalVectorRigCanvas(
    rig:OriginalVectorRig,
    modifier:Modifier=Modifier,
    play:Boolean=false,
    speed:Float=1f,
    mirror:Boolean=false,
    scrub:Float=0f,
    restartToken:Int=0
){
    val colors=LocalHunter.current
    var phase by remember(rig){mutableFloatStateOf(0f)}
    LaunchedEffect(rig,restartToken){phase=0f}
    LaunchedEffect(scrub){if(!play)phase=scrub.coerceIn(0f,1f)}
    LaunchedEffect(rig,play,speed,restartToken){
        if(play){
            var last=withFrameNanos{it}
            while(isActive){
                val now=withFrameNanos{it}
                val delta=(now-last)/1_000_000f
                last=now
                if(delta in 0f..150f)
                    phase=(phase+delta*speed.coerceIn(.25f,2f)/4800f)%1f
            }
        }
    }
    val points=rig.sample(phase)
    Canvas(modifier){
        val fit=min(size.width/rig.viewWidth,size.height/rig.viewHeight)
        val left=(size.width-rig.viewWidth*fit)/2f
        val top=(size.height-rig.viewHeight*fit)/2f
        fun map(p:Offset):Offset{
            val a=p*rig.anatomyScale+rig.translate
            return Offset(left+(if(mirror)rig.viewWidth-a.x else a.x)*fit,
                top+a.y*fit)
        }
        val screen=points.map(::map)
        for((index,edge) in originalBones.withIndex()){
            drawLine(
                color=if(index in listOf(4,5,8,9))colors.muted else colors.accent,
                start=screen[edge.first],end=screen[edge.second],
                strokeWidth=if(index in listOf(4,5,8,9))5.5f*fit else 8f*fit,
                cap=StrokeCap.Round
            )
        }
        for((index,shift) in rig.footVectors){
            drawLine(colors.text,screen[index],map(points[index]+shift),
                strokeWidth=5f*fit,cap=StrokeCap.Round)
        }
        for((index,point) in screen.withIndex()){
            drawCircle(
                color=if(index<=2)colors.text else colors.accent,
                radius=if(index<=2)10f*fit else 7f*fit,
                center=point
            )
        }
    }
}
