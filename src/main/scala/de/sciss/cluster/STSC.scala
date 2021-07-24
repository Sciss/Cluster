package de.sciss.cluster

import breeze.linalg.functions.euclideanDistance
import breeze.linalg.{*, DenseMatrix, DenseVector, argmax, max, sum, svd}
import breeze.numerics.{cos, pow, sin, sqrt}

import java.io.File
import scala.collection.immutable.SortedMap
import scala.collection.mutable
import scala.math.exp
import scala.util.control.Breaks.{break, breakable}

object STSC {
  /** Clustering result.
    *
    * @param numClusters  the number of clusters detected
    * @param costs        a Map of costs (key = number of clusters, value = cost for this number of clusters)
    * @param clusters     the clusters obtained for the input nodes (running from zero until `numClusters`)
    */
  case class Result(numClusters: Int, costs: SortedMap[Int, Double], clusters: Array[Int])

  /** Clusters a given dataset using a self-tuning spectral clustering algorithm.
    *
    * @param matrix      the dataset to cluster as a DenseMatrix, each row being an observation with each column
    *                    representing one dimension
    * @param minClusters the minimum number of clusters in the dataset
    * @param maxClusters the maximum number of clusters in the dataset
    * @return the best possible number of clusters, a Map of costs (key = number of clusters,
    *         value = cost for this number of clusters) and the clusters obtained for the best possible number.
    */
  def cluster(matrix: DenseMatrix[Double], minClusters: Int = 2, maxClusters: Int = 6): Result = {
    checkParams   (matrix, minClusters = minClusters, maxClusters = maxClusters)
    clusterMatrix (matrix, minClusters = minClusters, maxClusters = maxClusters)
  }

  /** Clusters a matrix of distances. using a self-tuning spectral clustering algorithm. Private function used by
    * public functions to cluster a matrix or a CSV.
    *
    * @param distances    square matrix of distances between nodes
    * @param minClusters  the minimum number of clusters in the dataset
    * @param maxClusters  the maximum number of clusters in the dataset
    * @return the best possible number of clusters, a Map of costs (key = number of clusters,
    *         value = cost for this number of clusters) and the clusters obtained for the best possible number.
    */
  def clusterDistances(distances: DenseMatrix[Double], minClusters: Int = 2, maxClusters: Int = 6): Result = {
    if (distances.rows != distances.cols) throw new IllegalArgumentException("The distance matrix is not square.")
    checkParams(distances, minClusters = minClusters, maxClusters = maxClusters)
    clusterImpl(distances, minClusters = minClusters, maxClusters = maxClusters)
  }

  private def checkParams(matrix: DenseMatrix[Double], minClusters: Int, maxClusters: Int): Unit = {
    // Three possible exceptions: empty dataset, minClusters less than 0, minClusters more than maxClusters.
    if (matrix.rows == 0)           throw new IllegalArgumentException("The dataset does not contains any observations.")
    if (minClusters < 0)            throw new IllegalArgumentException("The minimum number of clusters has to be positive.")
    if (minClusters > maxClusters)  throw new IllegalArgumentException("The minimum number of clusters has to be inferior to the maximum number of clusters.")
  }

  /** Clusters a given dataset using a self-tuning spectral clustering algorithm.
    *
    * @param path        the path of the dataset to cluster, each row being an observation with each column
    *                    representing one dimension
    * @param minClusters the minimum number of clusters in the dataset
    * @param maxClusters the maximum number of clusters in the dataset
    * @return the best possible number of clusters, a Map of costs (key = number of clusters,
    *         value = cost for this number of clusters) and the clusters obtained for the best possible number.
    */
  def clusterCSV(path: String, minClusters: Int = 2, maxClusters: Int = 6): Result = {
    val matrix = readCSV(path)
    cluster(matrix, minClusters = minClusters, maxClusters = maxClusters)
  }

  /** Reads a matrix from a comma-separated file.
    *
    * @param path        the path of the dataset to cluster, each row being an observation with each column
    *                    representing one dimension
    * @return the corresponding DenseMatrix
    */
  def readCSV(path: String): DenseMatrix[Double] = {
    val file = new File(path)
    breeze.linalg.csvread(file)
  }

  /** Clusters a matrix. using a self-tuning spectral clustering algorithm. Private function used by public
    * functions to cluster a matrix or a CSV.
    *
    * @param matrix      the dataset to cluster as a DenseMatrix, each row being an observation with each column
    *                    representing one dimension
    * @param minClusters the minimum number of clusters in the dataset
    * @param maxClusters the maximum number of clusters in the dataset
    * @return the best possible number of clusters, a Map of costs (key = number of clusters,
    *         value = cost for this number of clusters) and the clusters obtained for the best possible number.
    */
  private[cluster] def clusterMatrix(matrix: DenseMatrix[Double], minClusters: Int, maxClusters: Int): Result = {
    val distances = euclideanDistances(matrix)
    clusterImpl(distances, minClusters = minClusters, maxClusters = maxClusters)
  }

  /** Clusters a matrix. using a self-tuning spectral clustering algorithm. Private function used by public
    * functions to cluster a matrix or a CSV.
    *
    * @param distances   square matrix of distances between nodes
    * @param minClusters the minimum number of clusters in the dataset
    * @param maxClusters the maximum number of clusters in the dataset
    * @return the best possible number of clusters, a Map of costs (key = number of clusters,
    *         value = cost for this number of clusters) and the clusters obtained for the best possible number.
    */
  private[cluster] def clusterImpl(distances: DenseMatrix[Double], minClusters: Int, maxClusters: Int): Result = {
    // Compute local scale (step 1).
    val scale = localScale(distances, 7) // In the original paper we use the 7th neighbor to create a local scale.

    // Build locally scaled affinity matrix (step 2).
    val scaledMatrix = locallyScaledAffinityMatrix(distances, scale)

    // Build the normalized affinity matrix (step 3)
    val normalizedMatrix = normalizedAffinityMatrix(scaledMatrix)

    // Compute the largest eigenvectors (step 4)
    val largestEigenvectors = svd(normalizedMatrix).leftVectors(::, 0 until maxClusters)

    var cBest = minClusters // The best group number.
    // We only take the eigenvectors needed for the number of clusters.
    var currentEigenvectors = largestEigenvectors(::, 0 until minClusters)
    // val tzero = System.nanoTime()
    var (cost, rotatedEigenvectors) = bestRotation(currentEigenvectors)
    // val tone = System.nanoTime()
    val costs = mutable.Map(minClusters -> cost) // List of the costs.
    var bestRotatedEigenvectors = rotatedEigenvectors // The matrix of rotated eigenvectors having the minimal cost.

    for (k <- minClusters until maxClusters) { // We get the cost of stsc for each possible number of clusters.
      val eigenvectorToAdd = largestEigenvectors(::, k).toDenseMatrix.t // One new eigenvector at each turn.
      // We add it to the already rotated eigenvectors.
      currentEigenvectors = DenseMatrix.horzcat(rotatedEigenvectors, eigenvectorToAdd)
      // val t0 = System.nanoTime()
      val (tempCost, tempRotatedEigenvectors) = bestRotation(currentEigenvectors)
      // val t1 = System.nanoTime()
      costs += (k + 1 -> tempCost) // Add the cost to the map.
      rotatedEigenvectors = tempRotatedEigenvectors // We keep the new rotation of the eigenvectors.

      if (tempCost <= cost * 1.0001) {
        bestRotatedEigenvectors = rotatedEigenvectors
        cBest = k + 1
      }
      if (tempCost < cost) {
        cost = tempCost
      }
    }

    val orderedCosts = SortedMap(costs.toSeq: _*) // Order the costs.
    val absoluteRotatedEigenvectors = Util.getSomeAbs(bestRotatedEigenvectors)
    // The alignment result (step 8), conversion to array due to https://issues.scala-lang.org/browse/SI-9578
    val z = argmax(absoluteRotatedEigenvectors(*, ::)).toArray
    Result(cBest, orderedCosts, z)
  }

  /** Returns the euclidean distances of a given dense matrix.
    *
    * @param matrix the matrix that needs to be analyzed, each row being an observation with each column
    *               representing one dimension
    * @return the euclidean distances as a dense matrix
    */
  def euclideanDistances(matrix: DenseMatrix[Double]): DenseMatrix[Double] = {
    val distanceMatrix = DenseMatrix.zeros[Double](matrix.rows, matrix.rows) // Distance matrix, size rows x rows.

    for (i <- 0 until matrix.rows) {
      for (j <- i + 1 until matrix.rows) {
        // breeze.linalg.functions.euclideanDistance
        distanceMatrix(i, j) = euclideanDistance(matrix(i, ::).t, matrix(j, ::).t)
        distanceMatrix(j, i) = distanceMatrix(i, j) // Symmetric matrix.
      }
    }

    distanceMatrix
  }

  /** Returns the local scale as defined in the original paper, a vector containing the Kth nearest neighbor for
    * each observation.
    *
    * @param distanceMatrix the distance matrix used to get the Kth nearest neighbor
    * @param k              k, always 7 in the original paper
    * @return the local scale, the distance of the Kth nearest neighbor for each observation as a dense vector
    */
  private[cluster] def localScale(distanceMatrix: DenseMatrix[Double], k: Int): DenseVector[Double] =
    if (k > distanceMatrix.cols - 1) {
      throw new IllegalArgumentException("Not enough observations (" + distanceMatrix.cols + ") for k (" + k + ").")
    } else {
      val localScale = DenseVector.zeros[Double](distanceMatrix.cols)

      for (i <- 0 until distanceMatrix.cols) {
        val sortedDistances = distanceMatrix(::, i).toArray.sorted // Ordered distances.
        // Kth nearest distance, the 0th neighbor is always 0 and sortedVector(1) is the first neighbor
        localScale(i) = sortedDistances(k)
      }

      localScale
    }

  /** Returns a locally scaled affinity matrix using a distance matrix and a local scale
    *
    * @param distanceMatrix the distance matrix
    * @param localScale     the local scale, the distance of the Kth nearest neighbor for each observation as
    *                       a dense vector
    * @return the locally scaled affinity matrix
    */
  private[cluster] def locallyScaledAffinityMatrix(distanceMatrix: DenseMatrix[Double],
                                                   localScale: DenseVector[Double]): DenseMatrix[Double] = {
    // Distance matrix, size rows x cols.
    val affinityMatrix = DenseMatrix.zeros[Double](distanceMatrix.rows, distanceMatrix.cols)

    for (i <- 0 until distanceMatrix.rows) {
      for (j <- i + 1 until distanceMatrix.rows) {
        affinityMatrix(i, j) = -pow(distanceMatrix(i, j), 2)    // -d(si, sj)²
        affinityMatrix(i, j) /= (localScale(i) * localScale(j)) // -d(si, sj)² / lambi * lambj
        affinityMatrix(i, j) = exp(affinityMatrix(i, j))        // exp(-d(si, sj)² / lambi * lambj)
        affinityMatrix(j, i) = affinityMatrix(i, j)
      }
    }

    affinityMatrix
  }

  /** Returns the euclidean distance of a given dense matrix.
    *
    * @param scaledMatrix the matrix that needs to be normalized
    * @return the normalized matrix
    */
  private[cluster] def normalizedAffinityMatrix(scaledMatrix: DenseMatrix[Double]): DenseMatrix[Double] = {
    // Sum of each row, then power -0.5.
    val normalizedMatrix  = DenseMatrix.zeros[Double](scaledMatrix.rows, scaledMatrix.cols)
    val diagonalVector    = DenseVector.tabulate(scaledMatrix.rows)(i => 1 / sqrt(sum(scaledMatrix(i, ::))))

    for (i <- 0 until normalizedMatrix.rows) {
      for (j <- i + 1 until normalizedMatrix.cols) {
        normalizedMatrix(i, j) = diagonalVector(i) * scaledMatrix(i, j) * diagonalVector(j)
        normalizedMatrix(j, i) = normalizedMatrix(i, j)
      }
    }

    normalizedMatrix
  }

  /** Step 5 of the self-tuning spectral clustering algorithm, recover the rotation R which best aligns the
    * eigenvectors.
    *
    * @param eigenvectors the eigenvectors
    * @return the cost of the best rotation and the rotation as a DenseMatrix.
    */
  private[cluster] def bestRotation(eigenvectors: DenseMatrix[Double]): (Double, DenseMatrix[Double]) = {
    var nablaJ, newCost = 0.0
    var cost, old1Cost, old2Cost = j(eigenvectors)

    val bigK = eigenvectors.cols * (eigenvectors.cols - 1) / 2
    val theta, thetaNew = DenseVector.zeros[Double](bigK)

    breakable {
      for (i <- 0 until 200) { // Max iterations = 200, as in the original paper code.
        for (k <- 0 until theta.length) { // kth entry in the list composed of the (i, j) indexes
          val alpha = 0.001
          nablaJ = numericalQualityGradient(eigenvectors, theta, k, alpha)
          thetaNew(k) = theta(k) - alpha * nablaJ
          newCost = j(givensRotation(eigenvectors, thetaNew))

          if (newCost < cost) {
            theta(k) = thetaNew(k)
            cost = newCost
          } else {
            thetaNew(k) = theta(k)
          }
        }

        // If the new cost is not that better, we end the rotation.
        // If the new cost is not that better, we end the rotation.
        if (i > 2 && (old2Cost - cost) < (0.0001 * old2Cost)) {
          break()
        }
        old2Cost = old1Cost
        old1Cost = cost
      }
    }

    val rotatedEigenvectors = givensRotation(eigenvectors, thetaNew) // The rotation using the "best" theta we found.
    (cost, rotatedEigenvectors)
  }

  /** Returns the cost of a given rotation, follow the computation in the original paper code.
    *
    * @param matrix the rotation to analyze
    * @return the cost, the bigger the better (generally less than 1)
    */
  private[cluster] def j(matrix: DenseMatrix[Double]): Double = {
    val squareMatrix = matrix *:* matrix
    val sumVector = Util.sumCols(squareMatrix)
    val maxVector = max(squareMatrix(*, ::))
    sum(sumVector / maxVector) // Sum of the sum of each row divided by the max of each row.
  }

  private[cluster] def numericalQualityGradient(matrix: DenseMatrix[Double], theta: DenseVector[Double],
                                                k: Int, h: Double): Double = {
    theta(k) = theta(k) + h
    val upRotation = givensRotation(matrix, theta)
    theta(k) = theta(k) - 2 * h // -2 * because we cancel the previous operation.
    val downRotation = givensRotation(matrix, theta)
    (j(upRotation) - j(downRotation)) / (2 * h)
  }

  /** Givens rotation of a given matrix
    *
    * @param matrix the matrix to rotate
    * @param theta  the angle of the rotation
    * @return the Givens rotation
    */
  private[cluster] def givensRotation(matrix: DenseMatrix[Double], theta: DenseVector[Double]): DenseMatrix[Double] = {
    // Find the coordinate planes (i, j).
    var i, j = 0
    val ij = List.fill(theta.length) {
      j += 1
      if (j >= matrix.cols) {
        i += 1
        j = i + 1
      }
      (i, j)
    }

    val g = DenseMatrix.eye[Double](matrix.cols) // Create an empty identity matrix.

    var tt, uIk = 0.0
    for (k <- 0 until theta.length) {
      tt = theta(k)
      for (i <- 0 until matrix.cols) {
        uIk = g(i, ij(k)._1) * cos(tt) - g(i, ij(k)._2) * sin(tt)
        g(i, ij(k)._2) = g(i, ij(k)._1) * sin(tt) + g(i, ij(k)._2) * cos(tt)
        g(i, ij(k)._1) = uIk
      }
    }
    matrix * g
  }
}
