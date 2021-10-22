package animations.v2

import kotlinx.coroutines.channels.Channel
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noclear.NoClear
import org.openrndr.shape.LineSegment
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.getOrElse
import org.openrndr.*
import org.openrndr.color.ColorHSLa
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
    var poins: Int,
    var color: ColorRGBa,
    var radius: Double,
    var travelRadius: Double,
    var origin: Point,
    var joints: Int,
    var flow: Boolean = true
) : Extension {

    private val pointsChannel = Channel<List<Neighbours>>(20)

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
    override fun beforeDraw(drawer: Drawer, program: Program): Unit = with(program) {
        if (seconds < 1.0 || seconds > 3.0) return@with
        val points = pointsChannel.tryReceive().getOrElse { return }
        drawer.fill = ColorRGBa.WHITE.copy(a = 0.02)
        drawer.stroke = null
        drawer.pushStyle()
        drawer.drawStyle.blendMode = BlendMode.SUBTRACT
        drawer.rectangle(drawer.bounds)
        drawer.popStyle()
        drawer.fill = null
        drawer.strokeWeight = 3.0
        drawer.lineCap = LineCap.ROUND
        color = color.toHSLa().shiftHue(-((seconds-1.0) * 6.5)).toRGBa()
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


fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {
        backgroundColor = ColorRGBa.BLACK
        extend(Screenshots())
        extend(NoClear())
        val travelSpeed = 4.0
        val radius = min(width, height).toDouble() * 2.2
        Random.seed = Instant.now().nano.toString()
        extend(TrisolarExtension(1500, ColorRGBa.fromHex("#FFDF00"), radius, travelSpeed, Point(), 5, false))
    }
}
