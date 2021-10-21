import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgba
import org.openrndr.extra.noclear.NoClear
import org.openrndr.extra.noise.Random
import org.openrndr.math.Polar

fun main() = application {
    configure { width = 1800; height = 1000 }
    program {
        val zoom = 0.01
        backgroundColor = ColorRGBa.WHITE
        extend(NoClear())
        extend {
            drawer.fill = rgba(0.0, 0.0, 0.0, 0.05)
            drawer.points(generateSequence(Random.point(drawer.bounds)) {
                it + Polar(
                    180 * if (it.x < width / 2)
                        Random.value(it * zoom)
                    else
                        Random.simplex(it * zoom)
                ).cartesian
            }.take(5000).toList())
        }
    }
}
