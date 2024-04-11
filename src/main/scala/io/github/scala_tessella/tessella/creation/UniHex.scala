package io.github.scala_tessella.tessella
package creation

/** Contains fast methods to create a [[Tiling]] from an hexagonal net with different uniformity values */
trait UniHex extends Reticulate:

  /** 2-uniform tessellation [(3⁶);(3².6²)] (t=2, e=3) */
  def twoUniform(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)(_ % 3 == _ % 3)

  /** 2-uniform tessellation [(3⁶);(3⁴.6)] (t=3, e=3)
   * 
   * @note obtainable also with a triangleNetVariant(_, _)(_ % 3 == 0 && _ % 3 == 0)
   */
  def twoUniform2(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)(_ % 3 != _ % 3)

  /** 3-uniform tessellation [(3⁶);(3².6²;(6³)] (t=2, e=3) */
  def threeUniformOneOneOne(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)(_ % 2 == 0 && _ % 2 == 0)

  /** 3-uniform tessellation [(3⁶);(3⁴.6);(3².6²)] (t=5, e=8) */
  def threeUniformOneOneOne2(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)((i, j) => (i + 2 * j) % 4 < 2)

  /** 3-uniform tessellation [(3⁶);(3⁴.6);(3².6²)] (t=3, e=5) */
  def threeUniformOneOneOne3(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)((_, j) => j % 2 == 0)

  /** 3-uniform tessellation [2x(3⁶);(3⁴.6)] (t=3, e=4) */
  def threeUniformTwoOne(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)(_ % 2 == 0 || _ % 2 == 0)

  /** 7-uniform tessellation [(3⁶);2x(3².6²);4x(6³)] */
  def sevenUniformFourTwoOne(width: Int, height: Int): Either[String, Tiling] =
    hexagonRect(width, height)((i, j) => i % 10 == (j * 8) % 10)
