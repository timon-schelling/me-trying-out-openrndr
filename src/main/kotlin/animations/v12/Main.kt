package animations.v12

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

data class Particle(var position: Vector2, var radius: Double, var color: ColorRGBa, var type: Type, var teleport: Boolean = false)

class Draw(override var enabled: Boolean = true) : Extension {

    val particleUpdateChannel = Channel<List<Particle>>(20)

    override fun setup(program: Program): Unit = with(program) {
        GlobalScope.launch {
            val particleUnsafe = (1..30).map { Particle(Vector2.ZERO.randomPointInRadius(500.0), 15.0, ColorRGBa.BLUE, Type.HUNTER) } +
                    (1..100).map { Particle(Vector2.ZERO.randomPointInRadius(500.0), 15.0, ColorRGBa.RED, Type.RUNNER) }
            while (true) {
                val particles = particleUnsafe.map { it.copy() }
                val runnerParticles = particles.filter { it.type == Type.RUNNER }
                val hunterParticles = particles.filter { it.type == Type.HUNTER }
                coroutineScope {
                    particleUnsafe.forEach { element ->
                        launch {
                            if (element.teleport) {
                                element.teleport = false
                                var position: Vector2
                                do {
                                    position = Vector2.ZERO.randomPointInRadius(400.0)
                                    val ranking = particles.associateBy { position.distanceTo(it.position) }.toSortedMap()
                                    val other = ranking.entries.drop(1).first().value
                                    val distance = ranking.entries.drop(1).first().key
                                } while (distance <= other.radius + element.radius)
                                element.position = position
                            }
                            when (element.type) {
                                Type.RUNNER -> {
                                    val alieRanking = runnerParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val alie = alieRanking.entries.drop(1).first().value
                                    val alieDistance = alieRanking.entries.drop(1).first().key

                                    val enemyRanking = hunterParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val enemy = enemyRanking.values.first()
                                    val enemyDistance = enemyRanking.keys.first()

                                    val originDistance = element.position.distanceTo(Vector2.ZERO)

                                    when {
                                        alieDistance < 4.0 && !alie.teleport -> {
                                            element.teleport = true
                                        }
                                        enemyDistance < enemy.radius + element.radius || originDistance > 500 -> {
                                            element.teleport = true
                                        }
                                    }

                                    if (element.teleport) return@launch

                                    element.position += ((element.position - enemy.position).normalized * 1.2) + Vector2.ZERO.randomPointInRadius(0.4)
                                }
                                Type.HUNTER -> {
                                    val alieRanking = hunterParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val alie = alieRanking.entries.drop(1).first().value
                                    val alieDistance = alieRanking.entries.drop(1).first().key

                                    val enemyRanking = runnerParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val enemy = enemyRanking.values.first()
                                    val enemyDistance = enemyRanking.keys.first()

                                    val originDistance = element.position.distanceTo(Vector2.ZERO)

                                    when {
                                        alieDistance < 4.0 && !alie.teleport -> {
                                            element.teleport = true
                                        }
                                        originDistance > 500 -> {
                                            element.teleport = true
                                        }
                                    }

                                    if (element.teleport) return@launch

                                    element.position -= ((element.position - enemy.position).normalized * 0.9) + Vector2.ZERO.randomPointInRadius(0.4)
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

        drawer.fill = ColorRGBa.WHITE.copy(a = 0.016)
        drawer.stroke = null
        drawer.pushStyle()
        drawer.drawStyle.blendMode = BlendMode.ADD
        drawer.rectangle(-width.toDouble()/2.0, -height.toDouble()/2.0, width.toDouble(), height.toDouble())
        drawer.popStyle()

        drawer.stroke = null
        drawer.lineCap = LineCap.ROUND
        particle.forEach {
            drawer.fill = it.color
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
