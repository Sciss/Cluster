package stsc

import breeze.linalg.{*, BitVector, DenseMatrix, DenseVector, argmax, csvread, csvwrite, max, min, sum}
import breeze.numerics.abs
import breeze.stats.median

import java.io.File
import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

/** A kd tree to divide a dataset into multiple tiles.
*
* @constructor create a new kd tree containing a list of tiles and a border-width.
* @param tiles the tiles in the kd tree, represented as a binary tree.
* @param borderWidth the border of each tile that is used to know in which tile(s) is an observation.
*/
class KDTree(val tiles: Node, val borderWidth: Double = 0.0) {
  require(borderWidth >= 0)

  val dimensions: Int = tiles.value.mins.length // The dimensionality of the kd tree.

  /* Returns the leaves of kd tree as an array. */
  def leaves: Array[Tile] = {
    val leaves = Array.fill[Tile](tiles.leaves)(Tile(DenseVector(0.0), DenseVector(0.0)))
    var count = 0

    def leavesHelper(tree: Node): Unit =
      if (tree.isLeaf) {
        leaves(count) = tree.value
        count += 1
      } else {
        leavesHelper(tree.left)
        leavesHelper(tree.right)
      }

    leavesHelper(tiles)
    leaves
  }

  /** Write the kd tree as a CSV file.
    *
    * @param path the path where the kd tree has to be written.
    */
  def toCSV(path: String): Unit = {
    val tilesDM = DenseMatrix.zeros[Double](tiles.length + 1, dimensions * 2)
    tilesDM(0, 0) = borderWidth

    def toCSVHelper(tree: Node, position: Int): Unit = {
      tilesDM(position + 1, ::) := tree.value.toTranspose // +1 because the 1st row is for the border width.
      if (!tree.isLeaf) {
        toCSVHelper(tree.left , 2 * position + 1)
        toCSVHelper(tree.right, 2 * position + 2)
      }
    }

    toCSVHelper(tiles, 0)

    csvwrite(new File(path), tilesDM, separator = ',')
  }

  /* Returns the kd tree as a String, useful for debugging. */
  override def toString: String = {
    val tilesArray = Array.fill[String](tiles.length)("")

    def toStringHelper(tree: Node, position: Int): Unit = {
      tilesArray(position) = tree.value.toString
      if (!tree.isLeaf) {
        toStringHelper(tree.left , 2 * position + 1)
        toStringHelper(tree.right, 2 * position + 2)
      }
    }

    toStringHelper(tiles, 0)

    borderWidth.toString + "," + Array.fill[String](dimensions * 2 - 1)("0.0").mkString(",") + "\n" + tilesArray.mkString("\n")
  }

  /** Returns the tile(s) owning a given observation.
    *
    * @param observation the observation to check, represented as a DenseVector where every value is the coordinates in a dimension.
    * @return an array of the tiles having the observation. There can be more than one tile due to the border width.
    */
  def owningTiles(observation: DenseVector[Double]): Array[Tile] = {
    val owningTiles = ArrayBuffer.empty[Tile]

    def owningTilesHelper(tree: Node): Unit =
      if (tree.isLeaf) {
        owningTiles += tree.value
      } else {
        // The observation can be in multiple tiles thus we test for the left and the right tile.
        if (tree.left.value.has(observation, borderWidth)) {
          owningTilesHelper(tree.left)
        }
        if (tree.right.value.has(observation, borderWidth)) {
          owningTilesHelper(tree.right)
        }
      }

    owningTilesHelper(tiles)
    owningTiles.toArray
  }

  /** Returns the tile owning a given observation, must be within the strict edges of the tile.
    *
    * @param observation the observation to check, represented as a DenseVector where every value is the coordinates in a dimension.
    * @return the tile owning the observation.
    */
  def owningTile(observation: DenseVector[Double]): Tile = {
    @tailrec
    def owningTileHelper(tree: Node): Tile = {
      if (tree.isLeaf) {
        tree.value
      } else {
        if (tree.left.value.has(observation, 0)) {
          owningTileHelper(tree.left)
        } else {
          owningTileHelper(tree.right)
        }
      }
    }

    owningTileHelper(tiles)
  }
}

/** Factory for gr.armand.stsc.KDTree instances. */
object KDTree {
  /** Initialize a kd tree with a given maximum number of observations per tile.
    *
    * @param dataset             the dataset to use to create the kd tree.
    * @param tileMaxObservations the maximum number of observations per tile.
    * @param tileBorderWidth     the border width of the tiles in the kd tree.
    * @param cutFunction         the function used to cut a tile in two. The default is to cut the tiles using the parent tile dimensions.
    * @return the kd tree.
    */
  def createWithMaxObservations(dataset: DenseMatrix[Double], tileMaxObservations: Int, tileBorderWidth: Double = 0,
                                cutFunction: (Tile, DenseMatrix[Double]) => (Tile, Tile) = cutUsingTileDimensions): KDTree = {
    if (tileBorderWidth < 0) throw new IndexOutOfBoundsException("Tile radius must be a positive number. It was " + tileBorderWidth + ".")
    if (tileMaxObservations > dataset.rows) throw new IndexOutOfBoundsException("The maximum number of observations in a tile must be less than the number of observations.")
    val firstTile = Tile(DenseVector.fill(dataset.cols)(Double.NegativeInfinity), DenseVector.fill(dataset.cols)(Double.PositiveInfinity))
    val tilesTree = cutWithMaxObservations(dataset, firstTile, tileMaxObservations, cutFunction)
    new KDTree(tilesTree, tileBorderWidth)
  }

  /** Initialize a kd tree with a given maximum number of tiles in the kd tree.
    *
    * @param dataset         the dataset to use to create the kd tree.
    * @param leaves          the number of leaves in the kd tree.
    * @param tileBorderWidth the border width of the tiles in the kd tree.
    * @param cutFunction     the function used to cut a tile in two. The default is to cut the tiles using the parent tile dimensions.
    * @return the kd tree.
    */
  def createWithLeavesNumber(dataset: DenseMatrix[Double], leaves: Int, tileBorderWidth: Double = 0,
                             cutFunction: (Tile, DenseMatrix[Double]) => (Tile, Tile) = cutUsingTileDimensions): KDTree = {
    if (tileBorderWidth < 0)    throw new IndexOutOfBoundsException("Tile radius must be a positive number. It was " + tileBorderWidth + ".")
    if (leaves < 1)             throw new IndexOutOfBoundsException("The number of tiles must be positive.")
    if (leaves > dataset.rows)  throw new IndexOutOfBoundsException("The number of tiles must be less than the number of observations.")
    val maxObservations = math.ceil(dataset.rows / leaves).toInt
    val firstTile = Tile(DenseVector.fill(dataset.cols)(Double.NegativeInfinity),
                         DenseVector.fill(dataset.cols)(Double.PositiveInfinity))
    val tilesTree = cutWithMaxObservations(dataset, firstTile, maxObservations, cutFunction)
    new KDTree(tilesTree, tileBorderWidth)
  }

  /** Initialize a kd tree using a given CSV. The CSV must have a specific structure created by toCSV().
    *
    * @param filePath the path of the CSV file containing the kd tree.
    * @return the kd tree.
    */
  def fromCSV(filePath: String): KDTree =
    fromMatrix(csvread(new File(filePath)))

  /** Initialize a kd tree using a given String. The String must have a specific structure created by toCSV().
    *
    * @param text the string representing the kd tree.
    * @return the kd tree.
    */
  def fromString(text: String): KDTree = {
    val textArr = text.split("\n").map(l => l.split(","))
    fromMatrix(DenseMatrix.tabulate(textArr.length, textArr.head.length)((i, j) => textArr(i)(j).toDouble))
  }

  /** Initialize a kd tree using a given Matrix. The matrix must have a firts row corresponding to its border width.
    *
    * @param tilesDM the matrix representing the kd tree.
    * @return the kd tree.
    */
  private[stsc] def fromMatrix(tilesDM: DenseMatrix[Double]): KDTree = {
    if (tilesDM.cols % 2 != 0) {
      throw new IndexOutOfBoundsException("The file is not formatted to be a kd tree.")
    }
    if (tilesDM(0, 0) != sum(tilesDM(0, ::))) throw new IndexOutOfBoundsException("The file is not formatted to be a kd tree.")

    val borderWidth = tilesDM(0, 0)
    val dimensions = tilesDM.cols / 2
    val tilesDenseVector = DenseVector.fill(tilesDM.rows - 1) {
      Tile(DenseVector.zeros[Double](0), DenseVector.zeros[Double](0))
    }
    for (i <- 1 until tilesDM.rows) {
      val rowAsTile = tilesDM(i, ::).t
      tilesDenseVector(i - 1) = Tile(rowAsTile(0 until dimensions), rowAsTile(dimensions to -1))
    }

    def fromMatrixHelper(i: Int): Node = {
      if (2 * i + 2 < tilesDenseVector.length) {
        Node(tilesDenseVector(i), fromMatrixHelper(2 * i + 1), fromMatrixHelper(2 * i + 2))
      } else {
        Node(tilesDenseVector(i))
      }
    }

    val tiles = fromMatrixHelper(0)

    new KDTree(tiles, borderWidth)
  }

  // Anonymous functions to find on which dimension should the cut of the tile be made. Can be used with the create() functions.

  /** Cut a tile in two depending on its dimensions, returns two new tiles.
    *
    * If the tile is in two dimensions and the distance between maxs(0) and mins(0) is bigger than
    * maxs(1) and mins(1) the cut direction will be 0 as we need to cut in the first dimension (0).
    *
    * - `tile` the parent tile.
    * - `observations` the observations in the tile.
    */
  val cutUsingTileDimensions: (Tile, DenseMatrix[Double]) => (Tile, Tile) = (parent: Tile, observations: DenseMatrix[Double]) => {
    val parentSizes = parent.sizes
    // We check if the parent sites contains some infinities.
    if ((parentSizes :== DenseVector.fill(parentSizes.length)(Double.PositiveInfinity)) == BitVector()) {
      val cutDirection = argmax(parentSizes)
      val observationsMedian = median(observations(::, cutDirection))

      val firstTileMaxs = parent.maxs.copy
      firstTileMaxs(cutDirection) = observationsMedian
      val firstTile = Tile(parent.mins, firstTileMaxs) // The children parents will be similar as the parent.

      val secondTileMins = parent.mins.copy
      secondTileMins(cutDirection) = observationsMedian
      val secondTile = Tile(secondTileMins, parent.maxs)

      (firstTile, secondTile)
    } else {
      cutUsingContentDistances(parent, observations)
    }
  }: (Tile, Tile)

  /** Cut a tile in two depending on the observations in a tile, returns two new tiles.
    *
    * - `tile` the parent tile.
    * - `observations` the observations in the tile.
    */
  val cutUsingContentDistances: (Tile, DenseMatrix[Double]) => (Tile, Tile) = (parent: Tile, observations: DenseMatrix[Double]) => {
    val minCols = min(observations(::, *)).t
    val maxCols = max(observations(::, *)).t
    val dists   = abs(maxCols - minCols)
    val cutDirection = argmax(dists)
    val observationsMedian = median(observations(::, cutDirection))

    val firstTileMaxs = parent.maxs.copy
    firstTileMaxs(cutDirection) = observationsMedian
    val firstTile = Tile(parent.mins, firstTileMaxs) // The children parents will be similar as the parent.

    val secondTileMins = parent.mins.copy
    secondTileMins(cutDirection) = observationsMedian
    val secondTile = Tile(secondTileMins, parent.maxs)

    (firstTile, secondTile)
  }: (Tile, Tile)

  /** The main function to create the kd tree, used by both create functions.
    *
    * @param dataset         the dataset used to create the kdtree.
    * @param parentTile      the parent tile that is going to be cut.
    * @param maxObservations the maximum number of observations per tile.
    * @param cutFunction     the anonymous function used to cut the paren tile.
    * @return the value of the KDTree (thus a Node)
    */
  private[stsc] def cutWithMaxObservations(dataset: DenseMatrix[Double], parentTile: Tile, maxObservations: Int,
                                           cutFunction: (Tile, DenseMatrix[Double]) => (Tile, Tile)): Node = {
    val observations = parentTile.filter(dataset, 0)
    if (observations.rows > maxObservations) {
      val childrenTiles = cutFunction(parentTile, observations)
      Node(parentTile, cutWithMaxObservations(observations, childrenTiles._1, maxObservations, cutFunction),
                       cutWithMaxObservations(observations, childrenTiles._2, maxObservations, cutFunction))
    } else {
      Node(parentTile)
    }
  }
}
