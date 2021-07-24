package de.sciss.stsc.sequential.unit.STSC

import breeze.linalg.DenseVector
import de.sciss.stsc.STSC
import de.sciss.stsc.STSC.Result
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.io.File

class STSCTest extends AnyFlatSpec with Matchers {
  def compressCorrectClusters(correctClusters: Array[Int], max: Int): Array[Int] = {
    val differentValues = Array.fill[Int](max)(0)
    correctClusters.foreach(differentValues(_) += 1)
    differentValues.filterNot(elm => elm == 0).sorted
  }

  // Unit tests/
  "The algorithm" should "work when the minimum and maximum number of clusters are the same" in {
    val dataPath = getClass.getResource("/near.csv").getPath
    val dataset = new File(dataPath)
    val matrix = breeze.linalg.csvread(dataset)
    val Result(bestK, _ /*clustersQualities*/, correctClusters) = STSC.cluster(matrix, 2, 2)

    bestK should be(2)
    correctClusters should not be DenseVector.zeros[Int](matrix.rows)
    correctClusters should not be DenseVector.ones[Int](matrix.rows)
  }

  // Global tests.
  "The dataset 0" should "be correctly clustered" in {
    val dataPath = getClass.getResource("/0.csv").getPath
    val Result(bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath)
    println(clustersQualities)
    bestK should be(3)
    compressCorrectClusters(correctClusters, 6) should be(Array(61, 99, 139))
  }

  "The dataset 1" should "be correctly clustered" in {
    val dataPath = getClass.getResource("/1.csv").getPath
    val Result(bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath)
    println(clustersQualities)
    bestK should be(3)
    compressCorrectClusters(correctClusters, 6) should be(Array(95, 102, 106))
  }

  "The dataset 2" should "be correctly clustered" in {
    val dataPath = getClass.getResource("/2.csv").getPath
    val Result(bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath)
    println(clustersQualities)
    bestK should be(3)
    compressCorrectClusters(correctClusters, 6) should be(Array(73, 75, 118))
  }

  "The dataset 3" should "be correctly clustered" in {
    val dataPath = getClass.getResource("/3.csv").getPath
    val Result(bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath)
    println(clustersQualities)
    bestK should be(5)
    compressCorrectClusters(correctClusters, 6) should be(Array(109, 111, 116, 136, 150))
  }

  "The dataset 4" should "be correctly clustered" in {
    val dataPath = getClass.getResource("/4.csv").getPath
    val Result(bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath)
    println(clustersQualities)
    bestK should be(4)
    compressCorrectClusters(correctClusters, 6) should be(Array(117, 122, 123, 150))
  }

  "The dataset 5" should "be correctly clustered" in {
    val dataPath = getClass.getResource("/5.csv").getPath
    val Result(bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath)
    println(clustersQualities)
    bestK should be(3)
    compressCorrectClusters(correctClusters, 6) should be(Array(56, 82, 100))
  }
}
