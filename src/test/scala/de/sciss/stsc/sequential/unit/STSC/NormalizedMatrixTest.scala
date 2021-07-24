package de.sciss.stsc.sequential.unit.STSC

import breeze.linalg.DenseMatrix
import de.sciss.stsc.STSC
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class NormalizedMatrixTest extends AnyFlatSpec with Matchers {
  def roundAt5(n: Double): Double = {
    val s = math pow(10, 5); (math round n * s) / s
  } // We use round at as Double divisions can lead to minimal differences.

  "The normalized matrix" should "work" in {
    val scaledMatrix = DenseMatrix((0.0, 1.0, 3.0), (1.0, 0.0, 6.0), (3.0, 6.0, 0.0))
    val normalizedMatrix = STSC.normalizedAffinityMatrix(scaledMatrix)
    val correctResult = DenseMatrix((0.0, 1 / (math.sqrt(7) * 2), 0.5), (1 / (2 * math.sqrt(7)), 0.0, 2 / math.sqrt(7)), (0.5, 2 / math.sqrt(7), 0.0))
    for (row <- 0 until normalizedMatrix.rows) {
      for (col <- 0 until normalizedMatrix.cols) {
        roundAt5(normalizedMatrix(col, row)) should be(roundAt5(correctResult(row, col)))
      }
    }
  }
}
