package stsc

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf

object Job {
    def main(args: Array[String]) {
        val conf = new SparkConf().setAppName("STSCSparkJob")
        val sc = new SparkContext(conf)
        val dataset = getClass.getResource("/dataset.csv").getPath()
        val kdtree = getClass.getResource("/kdtree.csv").getPath()
        sc.addJar("/Users/Armand/Code/stsc/target/scala-2.11/stsc-assembly-1.0.jar")
        // STSC.sparkCluster(sc, dataset, kdtree, "clusters")
        STSC.sparkCluster(sc, dataset, kdtree, "clusterCenters.csv", true, 2, 12)
        sc.stop()

        // val dataPath = getClass.getResource("/dataset.csv").getPath()
        // val t0 = System.nanoTime()
        // val (bestK, clustersQualities, correctClusters) = STSC.clusterCSV(dataPath, 30, 40)
        // val t1 = System.nanoTime()
        // println("Elapsed time: " + (t1 - t0) / 1000000000 + "s")
        // println(bestK)
        // println(clustersQualities)
        // println(correctClusters)

    }
}
