package de.sciss.cluster.test.st

import breeze.linalg.{DenseMatrix, DenseVector}
import de.sciss.cluster.STSC
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LocalScaleTest extends AnyFlatSpec with Matchers {
  "The local scale " should "work with a 4x4 matrix and k = 5 (k is voluntarily too big)" in {
    val distanceMatrix = DenseMatrix((0.0, 4.0, 5.0, 3.0), (4.0, 0.0, 3.0, 5.0), (5.0, 3.0, 0.0, 4.0), (3.0, 5.0, 4.0, 0.0))
    val e = intercept[IllegalArgumentException] {
      STSC.localScale(distanceMatrix, 5)
    }
    e.getMessage should equal("Not enough observations (" + distanceMatrix.cols + ") for k (5).")
  }

  "The local scale " should "work with a 4x4 matrix and k = 4 (k is still too big)" in {
    val distanceMatrix = DenseMatrix((0.0, 4.0, 5.0, 3.0), (4.0, 0.0, 3.0, 5.0), (5.0, 3.0, 0.0, 4.0), (3.0, 5.0, 4.0, 0.0))
    val e = intercept[IllegalArgumentException] {
      STSC.localScale(distanceMatrix, 4)
    }
    e.getMessage should equal("Not enough observations (" + distanceMatrix.cols + ") for k (4).")
  }

  "The local scale " should "work with a 4x4 matrix and k = 3" in {
    val distanceMatrix = DenseMatrix((0.0, 4.0, 5.0, 3.0), (4.0, 0.0, 3.0, 5.0), (5.0, 3.0, 0.0, 4.0), (3.0, 5.0, 4.0, 0.0))
    val scale = STSC.localScale(distanceMatrix, 3)
    val correctScale = DenseVector(5.0, 5.0, 5.0, 5.0)
    scale should be(correctScale)
  }

  "The local scale " should "work with a 4x4 matrix and k = 2" in {
    val distanceMatrix = DenseMatrix((0.0, 4.0, 5.0, 3.0), (4.0, 0.0, 3.0, 5.0), (5.0, 3.0, 0.0, 4.0), (3.0, 5.0, 4.0, 0.0))
    val scale = STSC.localScale(distanceMatrix, 2)
    val correctScale = DenseVector(4.0, 4.0, 4.0, 4.0)
    scale should be(correctScale)
  }
}
