package de.sciss.cluster

import breeze.linalg.{DenseMatrix, DenseVector, Transpose, sum}

import scala.math.abs

object Util {
  /** Sums the columns to only have one DenseVector at the end. */
  def sumCols(dm: DenseMatrix[Double]): DenseVector[Double] =
    DenseVector.tabulate(dm.rows) { i => sum(dm(i, ::)) }

  /** Sums the rows to only have one Transpose at the end. */
  def sumRows(dm: DenseMatrix[Double]): Transpose[DenseVector[Double]] =
    DenseVector.tabulate(dm.cols) { i => sum(dm(::, i)) }.t

  /** Returns the absolute version of the DenseMatrix. */
  def getSomeAbs(pullIn: DenseMatrix[Double]): DenseMatrix[Double] =
    DenseMatrix.tabulate(pullIn.rows, pullIn.cols) { case (i, j) => abs(pullIn(i, j)) }
}
