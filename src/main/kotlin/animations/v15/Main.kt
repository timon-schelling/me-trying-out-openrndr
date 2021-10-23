package animations.v15

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
import org.openrndr.shape.LineSegment
import java.text.FieldPosition

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

data class Particle(
    var position: Vector2,
    var radius: Double,
    var color: ColorRGBa,
    var type: Type,
    var teleport: Boolean = false,
    var neighbours: List<Particle> = emptyList(),
    var drawPosition: Vector2 = position
)

class Draw(override var enabled: Boolean = true) : Extension {

    val particleUpdateChannel = Channel<List<Particle>>(20)

    val radius = 1500.0

    override fun setup(program: Program): Unit = with(program) {
        GlobalScope.launch {
            val particleUnsafe = (1..100).map { Particle(Vector2.ZERO.randomPointInRadius(radius), 1.5, ColorRGBa.BLACK, Type.HUNTER) } +
                    (1..300).map { Particle(Vector2.ZERO.randomPointInRadius(radius), 1.5, ColorRGBa.fromHex("#910000"), Type.RUNNER) }
            while (true) {
                val particles = particleUnsafe.map { it.copy() }
                val runnerParticles = particles.filter { it.type == Type.RUNNER }
                val hunterParticles = particles.filter { it.type == Type.HUNTER }
                coroutineScope {
                    particleUnsafe.forEach { element ->
                        launch {
                            element.drawPosition = element.position
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
                                element.neighbours = emptyList()
                                return@launch
                            }
                            when (element.type) {
                                Type.RUNNER -> {
                                    val alieRanking = runnerParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val alie = alieRanking.entries.drop(1).first().value
                                    val alieDistance = alieRanking.entries.drop(1).first().key

                                    element.neighbours = alieRanking.values.drop(1).take(5)

                                    val enemyRanking = hunterParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val enemy = enemyRanking.values.first()
                                    val enemyDistance = enemyRanking.keys.first()

                                    val originDistance = element.position.distanceTo(Vector2.ZERO)

                                    if (enemyDistance < enemy.radius + element.radius || originDistance > radius) {
                                        element.teleport = true
                                    }

                                    if (element.teleport) return@launch

                                    element.position += ((element.position - enemy.position).normalized * 0.5) + Vector2.ZERO.randomPointInRadius(0.2)
                                }
                                Type.HUNTER -> {
                                    val alieRanking = hunterParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val alie = alieRanking.entries.drop(1).first().value
                                    val alieDistance = alieRanking.entries.drop(1).first().key

                                    element.neighbours = alieRanking.values.drop(1).take(5)

                                    val enemyRanking = runnerParticles.associateBy { element.position.distanceTo(it.position) }.toSortedMap()
                                    val enemy = enemyRanking.values.first()
                                    val enemyDistance = enemyRanking.keys.first()

                                    val originDistance = element.position.distanceTo(Vector2.ZERO)

                                    if (originDistance > radius || alieDistance < 2.0) {
                                        element.teleport = true
                                    }

                                    if (element.teleport) return@launch

                                    element.position -= ((element.position - enemy.position).normalized * 0.4) + Vector2.ZERO.randomPointInRadius(0.2)
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

        drawer.fill = ColorRGBa.WHITE.copy(a = 0.01)
        drawer.stroke = null
        drawer.pushStyle()
        drawer.drawStyle.blendMode = BlendMode.ADD
        drawer.rectangle(-width.toDouble()/2.0, -height.toDouble()/2.0, width.toDouble(), height.toDouble())
        drawer.popStyle()

        drawer.fill = null
        drawer.strokeWeight = 5.0
        drawer.lineCap = LineCap.ROUND
        particle.forEach {
            drawer.stroke = it.color
            drawer.lineSegments(buildList {
                it.neighbours.forEach { other ->
                    add(LineSegment(it.drawPosition, other.position))
                }
            })
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
