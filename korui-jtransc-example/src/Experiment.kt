import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel

fun main(args: Array<String>) {
	val frame = JFrame()
	val cp = JPanel()
	cp.layout = null
	frame.add(cp)
	cp.preferredSize = Dimension(10, 10)
	frame.pack()
	//frame.layout = null
	//frame.preferredSize = Dimension(10, 10)
	//val insets = frame.insets
	//println(insets)
	//frame.preferredSize = Dimension(10 + insets.left, 10 + insets.top)
	//frame.pack()
	frame.isVisible = true
}