package de.sciss.cluster.test

import breeze.linalg.DenseMatrix
import de.sciss.cluster.STSC
import de.sciss.cluster.STSC.Result

import java.awt.geom.Ellipse2D
import java.awt.{Color, Dimension, EventQueue, Graphics, Graphics2D, GridLayout, RenderingHints}
import java.io.File
import javax.swing.{BorderFactory, JComponent, JFrame, JPanel, WindowConstants}
import scala.math.{max, min}

object Visualize {
  case class Data(input: DenseMatrix[Double], numClusters: Int, clusters: Array[Int])

  def main(args: Array[String]): Unit = {
    val dataSq = Seq.tabulate(6) { di =>
      val dataPath = getClass.getResource(s"/$di.csv").getPath
      val file = new File(dataPath)
      val matrix = breeze.linalg.csvread(file)
      val Result(numClusters, _, clusters) = STSC.cluster(matrix)
      Data(matrix, numClusters, clusters)
    }

    EventQueue.invokeLater(() => run(dataSq))
  }

  def run(dataSq: Seq[Data]): Unit = {
    val p = new JPanel(new GridLayout(2, 3, 16, 16))
    p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8))
    dataSq.foreach { d =>
      import d._
      val cOff = if (numClusters == 3) 2 else 0
      val colors = Array.tabulate(numClusters)(i => Color.getHSBColor((i + cOff).toFloat / numClusters, 1f, 1f))
      var minX = Double.PositiveInfinity
      var maxX = Double.NegativeInfinity
      var minY = Double.PositiveInfinity
      var maxY = Double.NegativeInfinity
      for (ri <- 0 until input.rows) {
        val x = input(ri, 0)
        val y = input(ri, 1)
        minX = min(minX, x)
        maxX = max(maxX, x)
        minY = min(minY, y)
        maxY = max(maxY, y)
      }
      // println(s"minX $minX, minY $minY, maxX $maxX, maxY $maxY")

      val comp: JComponent = new JComponent {
        private val w = 320
        private val h = 320
        setPreferredSize(new Dimension(w + 5, h + 5))

        private val e = new Ellipse2D.Double

        override def paintComponent(g: Graphics): Unit = {
          super.paintComponent(g)
          val g2 = g.asInstanceOf[Graphics2D]
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
          g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
          for (ri <- 0 until input.rows) {
            val x = (input(ri, 0) - minX) / (maxX - minX)
            val y = (input(ri, 1) - minY) / (maxY - minY)
            val c = clusters(ri)
            val xi = (x * w).toInt
            val yi = ((1.0 - y) * h).toInt
            g2.setColor(colors(c))
            e.setFrame(xi, yi, 5, 5)
            // g2.fillOval(xi /*- 2*/, yi /*- 2*/, 5, 5)
            g2.fill(e)
          }
        }
      }
      p.add(comp)
    }

    new JFrame {
      getContentPane.add(p)
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
