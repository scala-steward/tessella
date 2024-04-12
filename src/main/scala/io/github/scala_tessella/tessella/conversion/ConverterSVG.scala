package io.github.scala_tessella.tessella
package conversion

import ConverterSVG.Description
import SharedML.*

import math.geom2d.Point2D.createPolar
import math.geom2d.line.LineSegment2D
import math.geom2d.polygon.{Polyline2D, SimplePolygon2D}
import math.geom2d.{Box2D, Point2D}

import scala.jdk.CollectionConverters.*
import scala.xml.{Elem, Null, UnprefixedAttribute}

/** Generic methods for producing .SVG file from [[math.geom2d]]
 *
 * @see https://en.wikipedia.org/wiki/Scalable_Vector_Graphics
 */
trait ConverterSVG extends UtilsXML:

  /** scale multiplier for all elements */
  val scale: Double =
    50.0

  /** round accuracy for scaling */
  val svgAccuracy: Double =
    1000000.0

  private def rescale(double: Double): Double =
    Math.round(double * svgAccuracy * scale) / svgAccuracy

  extension (point: Point2D)

    private def scaled: (Double, Double) = (rescale(point.x), rescale(point.y))

  private def formatPoints(points: Iterable[Point2D]): String =
    points.map(_.scaled).map((x, y) => s"$x,$y").mkString(" ")

  extension (elem: Elem)

    /** add `points` attribute for `polyline` and `polygon` */
    private def withPoints(points: Iterable[Point2D]): Elem =
      elem.addAttributes(Attribute.create("points")(formatPoints(points)))

  /** `fill` attribute */
  val fill: String => Attribute =
    Attribute.create("fill")

  /** `stroke` attribute */
  val stroke: String => Attribute =
    Attribute.create("stroke")

  private def framedViewBox(box2D: Box2D): String =
    val enlarged: Box2D =
      box2D.enlarge(0.5)
    val newMin: Point2D =
      Point2D(enlarged.getMinX, enlarged.getMinY)
    val (minX, minY): (Double, Double) =
      newMin.scaled
    val (maxX, maxY): (Double, Double) =
      Point2D(enlarged.getMaxX, enlarged.getMaxY).minus(newMin).scaled
    s"$minX $minY $maxX $maxY"

  /** `svg` element with `viewBox` to fit a given box
   *
   * @param box2D box area with width and height to fit
   * @param elems placed in `svg`
   */
  def svg(box2D: Box2D, elems: Elem *): Elem =
    <svg viewBox={ s"${framedViewBox(box2D)}" } xmlns="http://www.w3.org/2000/svg">{ elems.toNodeBuffer }</svg>

  /** `metadata` element */
  def metadata(elems: Elem *): Elem =
    <metadata>{ elems.toNodeBuffer }</metadata>

  /** `group` element with optional title and description
   *
   * @param title optional title
   * @param desc optional description
   * @param elems placed in `group`
   */
  def group(title: Option[Title],
            desc: Option[Description],
            elems: Elem *): Elem =
    val titleDesc: Seq[Elem] =
      Seq(title.map(_.toElem), desc.map(_.toElem)).filter(_.isDefined).map(_.get)
    <g>{ (titleDesc ++ elems).toNodeBuffer }</g>

  /** `text` element
   *
   * @param point spatial coordinates
   * @param s     text
   */
  def text(point: Point2D, s: String): Elem =
    val (x, y) = point.scaled
    <text x={ s"$x" } y={ s"$y" } >{ s }</text>

  /** `rect` element
   *
   * @param box2D spatial coordinates
   */
  def rect(box2D: Box2D): Elem =
    val methods: List[Box2D => Double] =
      List(_.getWidth, _.getHeight, _.getMinX, _.getMinY)
    (methods.map(_.apply(box2D)).map(rescale): @unchecked) match
      case width :: height :: x :: y :: Nil =>
        <rect width={ s"$width" } height={ s"$height" } x={ s"$x" } y={ s"$y" }></rect>

  /** `polygon` element
   *
   * @param points spatial coordinates
   * @param elems placed in `polygon`
   */
  def polygon(points: Iterable[Point2D], elems: Elem *): Elem =
    (if elems.isEmpty then
      <polygon/>
    else
      <polygon>{ elems.toNodeBuffer }</polygon>
    ).withPoints(points)

  /** `polygon` element from `Polygon2D` */
  def polygon(polygon2D: SimplePolygon2D): Elem =
    polygon(polygon2D.vertices().asScala)

  /** `polyline` element
   *
   * @param points spatial coordinates
   * @param elems placed in `polygon`
   */
  def polyline(points: Iterable[Point2D], elems: Elem *): Elem =
    (if elems.isEmpty then
      <polyline/>
    else
      <polyline>{ elems.toNodeBuffer }</polyline>
    ).withPoints(points)

  /** `polyline` element from `Polyline2D` */
  def polyline(polyline2D: Polyline2D, elems: Elem *): Elem =
    polyline(polyline2D.vertices().asScala, elems *)

  /** `line` element
   *
   * @param point1 spatial coordinates of one endpoint
   * @param point2 spatial coordinates of other endpoint
   */
  def line(point1: Point2D, point2: Point2D): Elem =
    val (x1, y1): (Double, Double) =
      point1.scaled
    val (x2, y2): (Double, Double) =
      point2.scaled
    <line x1={ s"$x1" } y1={ s"$y1" } x2={ s"$x2" } y2={ s"$y2" } />

  /** `line` element from `LineSegment2D` */
  def line(segment: LineSegment2D): Elem =
    line(segment.firstPoint(), segment.lastPoint())

  /** `circle` element
   *
   * @param center spatial coordinates
   * @param radius length
   */
  def circle(center: Point2D, radius: Double): Elem =
    val (cx, cy): (Double, Double) =
      center.scaled
    <circle cx={ s"$cx" } cy={ s"$cy" } r={ s"${rescale(radius)}"} />

  /** `animate` element with attributes */
  def animate(attributes: Attribute *): Elem =
    val element: Elem =
      <animate />
    element.addAttributes(attributes *)

  private def pointsAnimation(from: Iterable[Point2D],
                              to: Iterable[Point2D],
                              values: List[Iterable[Point2D]]): Elem =
    animate(List(
      Attribute.create("attributeName")("points"),
      Attribute.create("dur")("5s"),
      Attribute.create("from")(formatPoints(from)),
      Attribute.create("to")(formatPoints(to)),
      Attribute.create("values")(values.map(formatPoints).mkString(";"))
    ) *)

  /** Animated polygon
   *
   * @param points spatial coordinates
   */
  def animatedPolygon(pointsSeries: List[Iterable[Point2D]]): Elem =
    val animation: Elem =
      pointsAnimation(
        pointsSeries.head,
        pointsSeries.last,
        pointsSeries.tail.init
      )
    polygon(pointsSeries.last, animation)

  /** Animated polyline developing sequentially from start to end
   *
   * @param points spatial coordinates
   */
  def animatedPolyline(points: Iterable[Point2D]): Elem =
    val scanned: Iterable[List[Point2D]] =
      points.tail.scanLeft(List(points.head))(_ ++ List(_))
    val animation: Elem =
      pointsAnimation(
        scanned.head,
        scanned.last,
        scanned.tail.init.toList
      )
    polyline(points, animation)

  private def arrowHeadPoints(tip: Point2D, origin: Point2D): List[Point2D] =
    val angle = LineSegment2D(tip, origin).horizontalAngle
    val delta = 0.5

    def vertex(upper: Boolean): Point2D =
      val variation = if upper then delta else -delta
      tip.plus(Point2D(createPolar(0.1, angle + variation)))

    List(tip, vertex(true), vertex(false))

  /** Arrow head as a triangle
   *
   * @param tip spatial coordinates of endpoint with arrow
   * @param point2 spatial coordinates of other endpoint
   */
  def arrowHead(tip: Point2D, origin: Point2D, elems: Elem *): Elem =
    polygon(arrowHeadPoints(tip, origin), elems *)

  /** Animated arrow head
   *
   * @param points spatial coordinates of the arrow movements
   */
  def animatedArrowHead(points: Iterable[Point2D]): Elem =
    val listed = points.toList
    val slided: List[List[Point2D]] = (listed.head :: listed).sliding(2).toList
    val animation: Elem =
      pointsAnimation(
        arrowHeadPoints(slided.head.last, slided.head.head),
        arrowHeadPoints(slided.last.last, slided.last.head),
        slided.tail.init.map(ps => arrowHeadPoints(ps.last, ps.head))
      )
    arrowHead(slided.last.last, slided.last.head, animation)

  /** Animated arrow and polyline
   *
   * @param points spatial coordinates of the polyline
   * @param arrowHeadStyle arrow head style
   * @param polylineStyle polyline style
   */
  def animatedPolylineArrow(points: Iterable[Point2D],
                            arrowHeadStyle: Style = Style(Nil *),
                            polylineStyle: Style = Style(Nil *)): Elem =
    group(None, None, elems = List(
      animatedPolyline(points).withStyle(polylineStyle),
      animatedArrowHead(points).withStyle(arrowHeadStyle)
    ) *)

/** Companion methods for producing .SVG file from [[math.geom2d]] */
object ConverterSVG:

  /** A description `desc` element
   *
   * @param underlying `String`
   */
  class Description(val underlying: String) extends AnyVal:

    def toElem: Elem =
      <desc>{underlying}</desc>