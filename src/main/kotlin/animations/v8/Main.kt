package animations.v8

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

enum class Type {
    RUNNER,
    HUNTER
}

data class Particle(var position: Vector2, var radius: Double, var color: ColorRGBa, var type: Type)

class Draw(override var enabled: Boolean = true) : Extension {

    val particleUpdateChannel = Channel<List<Particle>>(20)

    override fun setup(program: Program): Unit = with(program) {
        GlobalScope.launch {
            val particleUnsafe = (1..10).map { Particle(Vector2.ZERO.randomPointInRadius(300.0), 40.0, ColorRGBa.BLUE, Type.HUNTER) } +
                    (1..10).map { Particle(Vector2.ZERO.randomPointInRadius(300.0), 40.0, ColorRGBa.RED, Type.RUNNER) }
            while (true) {
                val particles = particleUnsafe.map { it.copy() }
                val runnerParticles = particles.filter { it.type == Type.RUNNER }
                val hunterParticles = particles.filter { it.type == Type.HUNTER }
                coroutineScope {
                    particleUnsafe.forEach { element ->
                        launch {
                            when (element.type) {
                                Type.RUNNER -> {
                                    val enemy = hunterParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap().values.first()
                                    if (element.position.distanceTo(Vector2.ZERO) > 600) {
                                        element.position = Vector2.ZERO
                                    }
//                                    var position = element.position
//                                    var i = 0
//                                    do {
//                                        if (i >= 10) {
//                                            element.position = position
//                                            break
//                                        }
//                                        position = position.randomPointInRadius(4.0)
//                                        i++
//                                    } while (position.distanceTo(enemy.position) < element.position.distanceTo(enemy.position))
//                                    element.position = position
                                    element.position += (element.position - enemy.position).normalized * 1.2
                                }
                                Type.HUNTER -> {
                                    val target = runnerParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap().values.first()
                                    if (element.position.distanceTo(Vector2.ZERO) > 400) {
                                        element.position = Vector2.ZERO
                                    }
                                    element.position -= (element.position - target.position).normalized
                                }
                            }
                        }
                    }
                }
                particleUpdateChannel.send(particleUnsafe)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    override fun beforeDraw(drawer: Drawer, program: Program) = with(program) {
        val particle = particleUpdateChannel.tryReceive().getOrElse { return }

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
        particle.forEach {
            drawer.stroke = it.color
            drawer.circle(it.position, it.radius)
        }
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
