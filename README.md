[![Build Status](https://github.com/Sciss/Cluster/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/Cluster/actions?query=workflow%3A%22Scala+CI%22)

# Cluster

A Scala library for data clustering, utilizing Breeze.

This is an updated fork from [stsc, here](https://github.com/armandgrillet/stsc). The original author is
Armand Grillet. Updates, extensions and modifications by Hanns Holger Rutz.
The base package is now `de.sciss.cluster`. In the future, more clustering algorithms might be added.

For now, it contains 
an implementation of the Self-Tuning Spectral Clustering algorithm, based on the paper 
[*Self Tuning Spectral Clustering STSC*](http://www.vision.caltech.edu/lihi/Demos/SelfTuningClustering.html).

Some clusters found by the implementation on six different datasets with k in \[2, 6\]:

<p align="center">
<img src="results.png">
</p>

## Overview

The main class to use is the self-tuning spectral clustering algorithm, `de.sciss.cluster.STSC`:

```scala
import de.sciss.cluster.STSC

// Your code to load the dataset and make it a DenseMatrix.
val (cBest, costs, clusters) = STSC.cluster(dataset)
// Two optional parameters, the minimum and maximum value of k. By default: 2 and 6.
val bestGroupNumber = STSC.cluster(dataset, 2, 10)._1
```

cBest is the most likely number of clusters in the dataset. It will always be between the minimum and maximum 
value of k.
costs is a map with the cost returned for each possible number of clusters k.
clusters is an Array of Int with a length equals to the number of rows in the dataset, clusters(i) represents the 
cluster where should be the observation i in the dataset.

If you include `de.sciss.cluster.KDTree`, you can also create a k-d tree to divide a dataset:

```scala
scala >

import de.sciss.cluster.KDTree

scala >
val tree = KDTree.createWithMaxObservations(dataset, maxObservationsPerTile, tileBorderWidth)
```

The third class of the library is `Tile`, a list of tiles composing a k-d tree.
A Tile is composed of two DenseVectors representing the minimums and maximums coordinates in each dimension.

Copyright © 2016 Armand Grillet. All rights reserved.
