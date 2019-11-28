package edu.gemini.spModel.core

import java.time.{Instant, LocalDateTime}
import java.time.ZoneOffset.UTC

sealed abstract case class JulianDate(dayNumber: Int,
                                      nanoAdjustment: Long) {

  import JulianDate._

  // Guaranteed by the JulianDate constructors, double checked here.
  assert(dayNumber >= 0, s"dayNumber >= 0")
  assert(nanoAdjustment >= MinAdjustment, s"nanoAdjustment >= $MinAdjustment")
  assert(nanoAdjustment <= MaxAdjustment, s"nanoAdjustment <= $MaxAdjustment")
}

object JulianDate {

  /** JulianDate and related constants. */
  val SecondsPerDay: Int = // 86400
    24 * 60 * 60

  val SecondsPerHalfDay: Int = // 43200
    SecondsPerDay / 2

  val Billion: Long = 1000000000
  val NanoPerDay: Long = SecondsPerDay.toLong * Billion.toLong

  val MinAdjustment: Long = -SecondsPerHalfDay.toLong * Billion.toLong
  val MaxAdjustment: Long = SecondsPerHalfDay.toLong * Billion.toLong - 1

  /** J2000 reference epoch as Julian Date. */
  val J2000: JulianDate = // JulianDate(2451545,0)
    JulianDate.ofLocalDateTime(
      LocalDateTime.of(2000, 1, 1, 12, 0, 0)
    )

  /** Convert an `Instant` to a Julian Date.
   */
  def ofInstant(i: Instant): JulianDate =
    ofLocalDateTime(LocalDateTime.ofInstant(i, UTC))

  /** JulianDate from a `LocalDateTime` assumed to represent a time at UTC.
   */
  def ofLocalDateTime(ldt: LocalDateTime): JulianDate = {
    val y = ldt.getYear
    val m = ldt.getMonthValue
    val d = ldt.getDayOfMonth

    // Julian Day Number algorithm from:
    // Fliegel, H.F. and Van Flandern, T.C. (1968). "A Machine Algorithm for
    // Processing Calendar Dates" Communications of the Association of Computing
    // Machines ll, 6sT.

    // Yes, integer division.  -1 for Jan and Feb. 0 for Mar - Dec.
    val t = (m - 14) / 12

    // Julian Day Number (integer division).
    val jdn = (1461 * (y + 4800 + t)) / 4 +
      (367 * (m - 2 - 12 * t)) / 12 -
      (3 * ((y + 4900 + t) / 100)) / 4 +
      d - 32075

    // Whole seconds since midnight
    val secs = ldt.getHour * 3600 + ldt.getMinute * 60 + ldt.getSecond
    val adj = (secs - SecondsPerHalfDay).toLong * Billion + ldt.getNano

    new JulianDate(jdn, adj) {}
  }
}
