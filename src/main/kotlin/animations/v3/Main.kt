package animations.v3

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
import org.openrndr.draw.TransformTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector2
import org.openrndr.shape.Rectangle
import java.time.Instant
import kotlin.math.*

fun Vector2.randomPointInRadius(radius: Double, tries: Int = 10): Vector2 {
    var point: Vector2
    var i = 0
    do {
        if (i >= tries) return this.copy()
        point = Vector2(
            x + Random.double(-radius, radius),
            y + Random.double(-radius, radius)
        )
        i++
    } while (distanceTo(point) >= radius)
    return point
}

data class Line(val center: Vector2, val radius: Double, val rotation: Double, val color: ColorRGBa) {
    val a: Vector2 get() = (center + Vector2(radius, 0.0)).rotate(rotation, center)
    val b: Vector2 get() = (center + Vector2(-radius, 0.0)).rotate(rotation, center)
}

@OptIn(ExperimentalStdlibApi::class)
var lines = buildList {
    repeat(60) {
        add(
            Line(
                Vector2.ZERO.randomPointInRadius(400.0),
                Random.double(16.0, 70.0),
                Random.double(0.0, 360.0),
                ColorRGBa.fromHex("#FF0000").toHSLa().shiftHue(Random.double(0.0, 360.0)).toRGBa()
            )
        )
    }
}

fun Program.loop() {

    drawer.translate(width/2.0, height/2.0, TransformTarget.VIEW)

    val xBound = width/2.0
    val yBound = height/2.0

    drawer.fill = ColorRGBa.WHITE.copy(a = 0.02)
    drawer.stroke = null
    drawer.pushStyle()
    drawer.drawStyle.blendMode = BlendMode.ADD
    drawer.rectangle(-xBound, -yBound, width.toDouble(), height.toDouble())
    drawer.popStyle()

    drawer.fill = null
    drawer.strokeWeight = 3.0
    drawer.lineCap = LineCap.ROUND
    lines.map {
        drawer.stroke = it.color
        drawer.lineSegment(it.a, it.b)
    }

    lines = lines.map {
        Line(
            it.center.randomPointInRadius(2.0),
            min(max(it.radius + Random.double(-5.0, 5.0), 16.0), 600.0),
            it.rotation.run {
                var new = this + Random.double(-3.0, 3.0)
                if (new > 360.0) {
                    new -= 360.0
                }
                if (new < 0.0) {
                    new += 360.0
                }
                new
            },
            it.color.toHSLa().shiftHue(Random.double(-10.0, 10.0)).toRGBa()
        )
    }
}

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {
        backgroundColor = ColorRGBa.BLACK
        extend(Screenshots())
        extend(NoClear())
        extend {
            loop()
        }
    }
}
