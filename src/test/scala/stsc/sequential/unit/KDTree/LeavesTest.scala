package stsc.sequential.unit.KDTree

import breeze.linalg.DenseVector
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import stsc.{KDTree, Node, Tile}

class LeavesTest extends AnyFlatSpec with Matchers {
  "Getting leaves" should "work in one dimension" in {
    val tree = new KDTree(Node(
      Tile(DenseVector(0.0), DenseVector(10.0)),
      Node(Tile(DenseVector(0.0), DenseVector(5.0))),
      Node(Tile(DenseVector(5.0), DenseVector(10.0)))
    ), 0.0) // border width)

    tree.leaves should be(Array(Tile(DenseVector(0.0), DenseVector(5.0)), Tile(DenseVector(5.0), DenseVector(10.0))))
  }

  "Getting leaves" should "work in two dimensions" in {
    val tree = new KDTree(Node(
      Tile(DenseVector(0.0, 0.0), DenseVector(10.0, 10.0)),
      Node(Tile(DenseVector(0.0, 0.0), DenseVector(10.0, 5.0))),
      Node(Tile(DenseVector(0.0, 5.0), DenseVector(10.0, 10.0)))
    ), 0.0) // border width)

    tree.leaves should be(Array(Tile(DenseVector(0.0, 0.0), DenseVector(10.0, 5.0)), Tile(DenseVector(0.0, 5.0), DenseVector(10.0, 10.0))))
  }
}
