package stsc

import breeze.linalg._
import breeze.numerics._
import breeze.stats._

import java.io.File
import scala.util.control.Breaks._
import scala.collection.mutable.ListBuffer

/** A tessellation tree to divide a daset into multiple tiles.
*
* @constructor create a new tessellation tree containing a list of Tiles and a dimension.
* @param mins the minimums of the tile in every dimension.
* @param maxs the maximums of the tile in every dimension.
* @param borderWidth the border width used when an observation is between two tiles.
*/
class TessellationTree(val dimension: Int, val tiles: List[Tile]) {
    /** Write the tessellation tree as a CSV file.
    *
    * @param filePath the path where the tessellation tree has to be written.
    */
    def toCSV(filePath: String) = {
        var tilesDenseMatrix = DenseMatrix.zeros[Double](this.tiles.length + 1, dimension * 2)
        tilesDenseMatrix(0, 0) = tiles(0).borderWidth
        var i = 0
        for (i <- 0 until tiles.length) {
            tilesDenseMatrix(i + 1, ::) := tiles(i).asTranspose()
        }
        csvwrite(new File(filePath), tilesDenseMatrix, separator = ',')
    }

    /** Returns the tile(s) owning a given observation.
    *
    * @param observation the observation to check, represented as a DenseVector.
    * @return a list of the tiles having the tiles. They can be more than 1 tile due to the border width.
    */
    def owningTiles(observation: DenseVector[Double]): List[Tile] = {
        var owningTiles = ListBuffer.empty[Tile]
        for (tile <- tiles) {
            if (tile.has(observation)) {
                owningTiles += tile
                if (owningTiles.length == 1 && tile.hasDeeply(observation)) {
                    return owningTiles.toList
                }
            }
        }
        return owningTiles.toList
    }
}

/** Factory for gr.armand.stsc.TessellationTree instances. */
object TessellationTree {
    /** Initialize a tessellation tree with a given maximum number of observations per tile.
    *
    * @param dataset the dataset to use to create the tessellation tree.
    * @param maxObservations the maximum number of observations per tile.
    * @param tileBorderWidth the border width of the tiles in the tessellation tree.
    * @param cutFunction the function used to cut a tile in two. The default is to cut the tiles using the parent tile dimensions.
    * @return the tessellation tree.
    */
    def createWithMaxObservations(dataset: DenseMatrix[Double], maxObservations: Int, tileBorderWidth: Double, cutFunction: (Tile, DenseMatrix[Double]) => (Tile, Tile) = stsc.cutUsingTileDimensions): TessellationTree = {
        if (tileBorderWidth < 0) { throw new IndexOutOfBoundsException("Tile radius must be a positive number. It was " + tileBorderWidth + ".") }
        if (maxObservations > dataset.rows) { throw new IndexOutOfBoundsException("The maximum number of observations in a tile must be less than the number of observations.") }
        val firstTile = Tile(DenseVector.fill(dataset.cols){scala.Double.NegativeInfinity}, DenseVector.fill(dataset.cols){scala.Double.PositiveInfinity}, tileBorderWidth)
        val tiles: List[Tile] = cutWithMaxObservations(dataset, firstTile, maxObservations, cutFunction)
        return new TessellationTree(dataset.cols, tiles)
    }

    /** Initialize a tessellation tree with a given maximum number of tiles in the tessellation tree.
    *
    * @param dataset the dataset to use to create the tessellation tree.
    * @param tilesNumber the number of tiles in the tessellation tree.
    * @param tileBorderWidth the border width of the tiles in the tessellation tree.
    * @param cutFunction the function used to cut a tile in two. The default is to cut the tiles using the parent tile dimensions.
    * @return the tessellation tree.
    */
    def createWithTilesNumber(dataset: DenseMatrix[Double], tilesNumber: Int, tileBorderWidth: Double = 0, cutFunction: (Tile, DenseMatrix[Double]) => (Tile, Tile) = stsc.cutUsingTileDimensions): TessellationTree = {
        if (tileBorderWidth < 0) { throw new IndexOutOfBoundsException("Tile radius must be a positive number. It was " + tileBorderWidth + ".") }
        if (tilesNumber > dataset.rows) { throw new IndexOutOfBoundsException("The number of tiles must be less than the number of observations.") }
        val maxObservations = math.ceil(dataset.rows / tilesNumber).toInt
        val firstTile = Tile(DenseVector.fill(dataset.cols){scala.Double.NegativeInfinity}, DenseVector.fill(dataset.cols){scala.Double.PositiveInfinity}, tileBorderWidth)
        val tiles: List[Tile] = cutWithMaxObservations(dataset, firstTile, maxObservations, cutFunction)
        return new TessellationTree(dataset.cols, tiles)
    }

    /** Initialize a tessellation tree using a given CSV. The CSV must have a specific structure that can be done using toCSV().
    *
    * @param filePath the path of the CSV file containing the tessellation tree.
    * @return the tessellation tree.
    */
    def fromCSV(filePath: String): TessellationTree = {
        var tilesDenseMatrix = csvread(new File(filePath))
        if (tilesDenseMatrix.cols % 2 != 0) { throw new IndexOutOfBoundsException("The file is not formatted to be a tessellation tree.") }
        if (tilesDenseMatrix(0, 0) != sum(tilesDenseMatrix(0, ::))) { throw new IndexOutOfBoundsException("The file is not formatted to be a tessellation tree.") }

        val borderWidth = tilesDenseMatrix(0, 0)
        val dimension = tilesDenseMatrix.cols / 2
        val tiles = ListBuffer.empty[Tile]
        var i = 0
        for (i <- 1 until tilesDenseMatrix.rows) {
            val tile = tilesDenseMatrix(::, i)
            tiles += Tile(tile(0 until dimension), tile(dimension to -1), borderWidth)
        }
        return new TessellationTree(dimension, tiles.toList)
    }

    private def observationsInTile(dataset: DenseMatrix[Double], tile: Tile, axis: Int): DenseMatrix[Double] = {
        var observations = DenseMatrix.zeros[Double](dataset.rows, dataset.cols)
        var numberOfObservations = 0
        for (row <- dataset(*,::)) {
            if (tile.has(row)) {
                observations(numberOfObservations, 0) = row(0)
                observations(numberOfObservations, 1) = row(1)
                numberOfObservations += 1
            }
        }
        return observations(::, 0 until numberOfObservations)
    }

    private def cutWithMaxObservations(dataset: DenseMatrix[Double], parentTile: Tile, maxObservations: Int, cutFunction: (Tile, DenseMatrix[Double]) => (Tile, Tile)): List[Tile] = {
        val observations = observationsInTile(dataset, parentTile, 0)
        if (observations.rows > maxObservations) {
            val childrenTiles = cutFunction(parentTile, observations)
            return List.concat(cutWithMaxObservations(observations, childrenTiles._1, maxObservations, cutFunction), cutWithMaxObservations(observations, childrenTiles._2, maxObservations, cutFunction))
        } else {
            return List(parentTile)
        }
    }
}
