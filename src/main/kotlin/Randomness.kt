import org.openrndr.ExtensionStage
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.extra.noclear.NoClear
import org.openrndr.extra.noise.Random
import org.openrndr.math.Vector2
import org.openrndr.math.map
fun main() = application {
    configure {
        width = 900
        height = 900
    }
    program {
        backgroundColor = ColorRGBa.WHITE
//        val colors = listOf(
//            "FF0000",
//            "800000",
//            "FFFF00",
//            "808000",
//            "00FF00",
//            "008000",
//            "00FFFF",
//            "008080",
//            "0000FF",
//            "000080",
//            "FF00FF",
//            "800080",
//        ).map { ColorRGBa.fromHex(it) }
        val colors = listOf(
            "88ccf1",
            "c1dff0",
            "3587a4",
            "2d848a",
            "2d898b",
        ).map { ColorRGBa.fromHex(it) }
        var lastRandomUpdateSeconds = seconds
        extend(NoClear())
        extend {
            if (lastRandomUpdateSeconds + 0.1 < seconds) {
                lastRandomUpdateSeconds = seconds
                drawer.stroke = null
                for (x in 0..15) {
                    for (y in 0..15) {
                        if (Random.int(max = 20) != 0) continue
                        val pos = Vector2(
                            map(0.0, 15.0, 100.0, width - 100.0, x * 1.0),
                            map(0.0, 15.0, 100.0, height - 100.0, y * 1.0)
                        )
                        drawer.fill = Random.pick(colors)
                        drawer.circle(pos, 20.0)
                        if (Random.bool()) {
                            drawer.fill = Random.pick(colors)
                            drawer.circle(pos, 10.0)
                        }
                    }
                }
            }
        }
    }
}
