package animations.v4

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noclear.NoClear
import org.openrndr.*
import org.openrndr.draw.Drawer
import org.openrndr.draw.LineCap
import org.openrndr.extensions.Screenshots
import org.openrndr.extra.noise.Random
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class Draw(override var enabled: Boolean = true) : Extension {

    private val baseColor: ColorRGBa

    init {
        Random.seed = Instant.now().nano.toString()
        baseColor = ColorRGBa.fromHex("#FF0000").toHSLa().shiftHue(Random.double(0.0, 360.0)).toRGBa()
    }

    override fun setup(program: Program) = with(program) { }

    var first = true

    @OptIn(ExperimentalTime::class)
    override fun beforeDraw(drawer: Drawer, program: Program): Unit = with(program) {

        if (first) {
            first = false
        } else {
            runBlocking {
                delay(Duration.milliseconds(100))
            }
            return@with
        }

        Random.resetState()

        drawer.stroke = null
        drawer.lineCap = LineCap.ROUND

        val xd = 160
        val yd = 90

        val w = width/xd.toDouble()
        val h = height/yd.toDouble()

        for (y in 0 until xd) {
            for (x in 0 until xd) {
                drawer.fill = baseColor.toHSLa().shiftHue(Random.double(0.0, 20.0)).saturate(Random.double(0.7, 1.0)).toRGBa()
                drawer.rectangle(x * w, y * h, w, h)
            }
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
