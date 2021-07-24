package stsc.sequential.unit.KDTree

import breeze.linalg.DenseVector
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import stsc.{KDTree, Node, Tile}

class ToTextTest extends AnyFlatSpec with Matchers {
  "KDTree.toString() and fromString()" should "work" in {
    val tree = new KDTree(
      Node(Tile(DenseVector(0.0), DenseVector(10.0)),
      Node(Tile(DenseVector(0.0), DenseVector( 5.0))),
      Node(Tile(DenseVector(5.0), DenseVector(10.0)))
    ), 0.0) // border width)
    val treeText = tree.toString()
    val treeAgain = KDTree.fromString(treeText)

    tree.dimensions         should be(treeAgain.dimensions)
    tree.tiles.value        should be(treeAgain.tiles.value)
    tree.tiles.left.value   should be(treeAgain.tiles.left.value)
    tree.tiles.right.value  should be(treeAgain.tiles.right.value)
  }
}
