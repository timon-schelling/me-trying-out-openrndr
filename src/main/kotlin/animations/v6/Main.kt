package animations.v6

import animations.v3.randomPointInRadius
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.getOrElse
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noclear.NoClear
import org.openrndr.*
import org.openrndr.draw.BlendMode
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.draw.TransformTarget
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector2
import org.openrndr.shape.Circle

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

data class Particle(val position: Vector2, val radius: Double, val color: ColorRGBa, var neighbours: List<Particle> = emptyList())

class Draw(override var enabled: Boolean = true) : Extension {

    val particleUpdateChannel = Channel<List<Particle>>(20)

    override fun setup(program: Program): Unit = with(program) {
        GlobalScope.launch {
            var particle = (1..50).map { Particle(Vector2.ZERO.randomPointInRadius(300.0), 40.0, ColorRGBa.BLACK) }
            while (true) {
                coroutineScope {
                    particle.forEach { element ->
                        launch {
                            element.neighbours =
                                particle.associateBy { element.position.distanceTo(it.position) }.toSortedMap().values.drop(1)
                        }
                    }
                }
                particleUpdateChannel.send(particle)
                particle = particle.map {
                    Particle(
                        it.run {
                            if (position.distanceTo(Vector2.ZERO) > 300) {
                                return@run Vector2.ZERO
                            }
                            var position = this.position
                            var i = 0
                            do {
                                if (i >= 10) return@run position
                                position = position.randomPointInRadius(4.0)
                                i++
                            } while (neighbours.first().position.distanceTo(position) < this.radius*2 && neighbours.first().position.distanceTo(position) > neighbours.first().position.distanceTo(this.position))
                            position
                        },
                        it.radius,
                        it.color
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun beforeDraw(drawer: Drawer, program: Program) = with(program) {
        val points = particleUpdateChannel.tryReceive().getOrElse { return }

        drawer.translate(width/2.0, height/2.0, TransformTarget.VIEW)

        drawer.fill = ColorRGBa.WHITE.copy(a = 0.02)
        drawer.stroke = null
        drawer.pushStyle()
        drawer.drawStyle.blendMode = BlendMode.ADD
        drawer.rectangle(-width.toDouble()/2.0, -height.toDouble()/2.0, width.toDouble(), height.toDouble())
        drawer.popStyle()

        drawer.fill = null
        drawer.strokeWeight = 3.0
        drawer.lineCap = LineCap.ROUND
        drawer.stroke = ColorRGBa.BLACK
        drawer.circles(buildList {
            points.forEach { element ->
                element.neighbours.forEach {
                    add(Circle(it.position, it.radius))
                }
            }
        })
    }

    override fun afterDraw(drawer: Drawer, program: Program) = with(program) { }
}

fun main() = application {
    configure {
        fullscreen = Fullscreen.CURRENT_DISPLAY_MODE
    }
    program {
        backgroundColor = ColorRGBa.BLACK
        extend(Screenshots())
        extend(NoClear())
        extend(Draw())
    }
}
