package experiments

import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.JPanel


object AwtExperiment {
	@JvmStatic fun main(args: Array<String>) {
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
}

//object SwtExperiment {
//	@JvmStatic fun main(args: Array<String>) {
//		val display = Display()
//		val shell = Shell(display)
//
//		val helloWorldTest = Text(shell, SWT.NONE)
//		helloWorldTest.text = "Hello World SWT"
//		helloWorldTest.pack()
//
//		shell.pack()
//		shell.open()
//		while (!shell.isDisposed) {
//			if (!display.readAndDispatch()) display.sleep()
//		}
//		display.dispose()
//	}
//}