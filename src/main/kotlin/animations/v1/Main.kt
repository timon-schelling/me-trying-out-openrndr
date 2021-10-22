package animations.v1

import kotlinx.coroutines.channels.Channel
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noclear.NoClear
import org.openrndr.shape.LineSegment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.getOrElse
import org.openrndr.*
import org.openrndr.draw.BlendMode
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.Random
import java.time.Instant
import kotlin.math.*

fun Point.randomPointInRadius(radius: Double, tries: Int = 10): Point {
    var point: Point
    var i = 0
    do {
        if (i >= tries) return this.copy()
        point = Point(
            x + Random.double(-radius, radius),
            y + Random.double(-radius, radius)
        )
        i++
    } while (distance(point) >= radius)
    return point
}

data class Point(var x: Double = 0.0, var y: Double = 0.0) {
    fun distance(other: Point): Double = hypot(other.x - x, other.y - y)
}

data class Neighbours(val point: Point, var neighbours: List<Point> = emptyList())

class TrisolarExtension(
    val poins: Int,
    var color: ColorRGBa,
    val radius: Double,
    val travelRadius: Double,
    val origin: Point,
    val joints: Int,
    val flow: Boolean = true
) : Extension {

    val pointsChannel = Channel<List<Neighbours>>(20)

    override fun setup(program: Program): Unit = with(program) {
        GlobalScope.launch {
            var points = (1..poins).map { Neighbours(origin.randomPointInRadius(radius)) }
            while (true) {
                coroutineScope {
                    points.forEach { element ->
                        launch {
                            element.neighbours =
                                points.associateBy { element.point.distance(it.point) }.toSortedMap().values.drop(1)
                                    .take(joints).map { it.point }
                        }
                    }
                }
                pointsChannel.send(points)
                points = points.map {
                    Neighbours(it.point.run {
                        if (distance(origin) > radius) {
                            return@run if (flow) origin.copy() else origin.randomPointInRadius(radius)
                        }
                        var point: Point
                        var i = 0
                        do {
                            if (i >= 10) return@run this.copy()
                            point = randomPointInRadius(travelRadius)
                            i++
                        } while (flow && distance(origin) > point.distance(origin))
                        point
                    })
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun beforeDraw(drawer: Drawer, program: Program) = with(program) {
        val points = pointsChannel.tryReceive().getOrElse { return }
        drawer.fill = null
        drawer.strokeWeight = 3.0
        drawer.lineCap = LineCap.ROUND
        color = color.toHSLa().run { copy(l = min(max(l + Random.double(-.04, .04), 0.1), 0.9)) }.shiftHue(Random.double(-10.0, 10.0)).toRGBa()
        drawer.stroke = color
        val centerX = width / 2
        val centerY = height / 2
        drawer.lineSegments(buildList {
            points.forEach { element ->
                element.neighbours.forEach {
                    add(
                        LineSegment(
                            element.point.x + centerX,
                            element.point.y + centerY,
                            it.x + centerX,
                            it.y + centerY
                        )
                    )
                }
            }
        })
    }

    override fun afterDraw(drawer: Drawer, program: Program) = with(program) { }

    override var enabled: Boolean = true
}

class FadeoutExtension(val opacity: Double, val blendMode: BlendMode = BlendMode.ADD) : Extension {

    override fun setup(program: Program): Unit = with(program) { }

    override fun beforeDraw(drawer: Drawer, program: Program) = with(program) {
        drawer.fill = ColorRGBa.WHITE.copy(a = opacity)
        drawer.stroke = null
        drawer.pushStyle()
        drawer.drawStyle.blendMode = blendMode
        drawer.rectangle(drawer.bounds)
        drawer.popStyle()
    }

    override fun afterDraw(drawer: Drawer, program: Program) = with(program) { }

    override var enabled: Boolean = true
}

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
//    program {
//        backgroundColor = ColorRGBa.WHITE
//        extend(Screenshots())
//        extend(NoClear())
//        extend(FadeoutExtension(0.01))
//        val travelSpeed = 1.0
//        val radius = min(width, height).toDouble() / 4
//        val offset =  min(width, height).toDouble() / 5 * 1.5
//        extend(TrisolarExtension(150, ColorRGBa.fromHex("#673AB7"), radius, travelSpeed, Point(), 8))
//        extend(TrisolarExtension(150, ColorRGBa.fromHex("#C0392B"), radius, travelSpeed, Point(offset, offset), 7))
//        extend(TrisolarExtension(150, ColorRGBa.fromHex("#3498DB"), radius, travelSpeed, Point(offset, -offset), 6))
//        extend(TrisolarExtension(150, ColorRGBa.fromHex("#2ECC71"), radius, travelSpeed, Point(-offset, offset), 5))
//        extend(TrisolarExtension(150, ColorRGBa.fromHex("#F1C40F"), radius, travelSpeed, Point(-offset, -offset), 4))
//    }
    program {
        backgroundColor = ColorRGBa.BLACK
        extend(Screenshots())
        extend(NoClear())
        extend(FadeoutExtension(0.05, BlendMode.SUBTRACT))
        val travelSpeed = 1.0
        val radius = max(width, height).toDouble() / 1.6
        Random.seed = Instant.now().nano.toString()
        extend(TrisolarExtension(1000, ColorRGBa.fromHex("#00FF18").toHSLa().shiftHue(Random.double(0.0,360.0)).toRGBa(), radius, travelSpeed, Point(), 5, true))
    }
}
