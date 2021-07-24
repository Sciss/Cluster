package de.sciss.stsc

case class Node(value: Tile, left: Node = null, right: Node = null) {
  require(left  == null || left .value.mins.length == value.mins.length, "The value of the tree and its left child must be in the same dimensions")
  require(right == null || right.value.mins.length == value.mins.length, "The value of the tree and its right child must be in the same dimensions")

  /* Returns if the node is a leaf. */
  def isLeaf: Boolean =
    left == null && right == null

  /* Returns the number of leaves from this Node. */
  def leaves: Int =
    if (isLeaf) 1 else left.leaves + right.leaves

  /* Returns the depth from this Node. */
  def length: Int =
    if (isLeaf) 1 else 1 + left.length + right.length
}
