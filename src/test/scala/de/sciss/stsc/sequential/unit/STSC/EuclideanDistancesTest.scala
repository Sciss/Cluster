package de.sciss.stsc.sequential.unit.STSC

import breeze.linalg.DenseMatrix
import de.sciss.stsc.STSC
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class EuclideanDistancesTest extends AnyFlatSpec with Matchers {
  "The Euclidean distance " should "work with a simple 2x2 matrix" in {
    val initialMatrix = DenseMatrix((1.0, 3.0), (3.0, 3.0))
    val distanceMatrix = STSC.euclideanDistances(initialMatrix)
    val correctDistanceMatrix = DenseMatrix((0.0, 2.0), (2.0, 0.0))

    distanceMatrix should be(correctDistanceMatrix)
  }

  "The Euclidean distance " should "work with a 3x3 matrix" in {
    val initialMatrix = DenseMatrix((-2.0, -2.0), (2.0, -2.0), (2.0, 1.0))
    val distanceMatrix = STSC.euclideanDistances(initialMatrix)
    val correctDistanceMatrix = DenseMatrix((0.0, 4.0, 5.0), (4.0, 0.0, 3.0), (5.0, 3.0, 0.0))

    distanceMatrix should be(correctDistanceMatrix)
  }
}
