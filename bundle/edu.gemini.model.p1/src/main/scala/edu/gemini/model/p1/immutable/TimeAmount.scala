package edu.gemini.model.p1.immutable

import edu.gemini.model.p1.{ mutable => M }

object TimeAmount {

  val HoursPerNight = 10.0

  def apply(m: M.TimeAmount) = new TimeAmount(m)
  val empty = TimeAmount(0.0, M.TimeUnit.HR)

  // Basically this is just folding empty over the list summing as we go but
  // with the difference that we try to keep the time unit from changing if they
  // are all the same.
  def sum(times: Traversable[TimeAmount]): TimeAmount =
    if (times.isEmpty) empty else (times.head/:times.tail)(_ + _)
}

case class TimeAmount(value: Double, units: TimeUnit) {

  def isEmpty = value == 0

  def this(m: M.TimeAmount) = this(m.getValue.doubleValue, m.getUnits)

  def mutable = {
    val m = Factory.createTimeAmount
    m.setValue(java.math.BigDecimal.valueOf(value))
    m.setUnits(units)
    m
  }

  def hours = units match {
    case TimeUnit.HR    => value
    case TimeUnit.NIGHT => value * TimeAmount.HoursPerNight
  }

  def nights = units match {
    case TimeUnit.NIGHT => value
    case TimeUnit.HR    => value / TimeAmount.HoursPerNight
  }

  def toHours  = if (units == TimeUnit.HR) this else TimeAmount(hours, TimeUnit.HR)
  def toNights = if (units == TimeUnit.NIGHT) this else TimeAmount(nights, TimeUnit.HR)

  def +(that: TimeAmount): TimeAmount =
    if (units == that.units)
      TimeAmount(value + that.value, units)
    else
      TimeAmount(hours + that.hours, TimeUnit.HR)

  /**
   * Formats a time amount to the given precision (which is treated as 0 if
   * negative).
   */
  def format(prec: Int = 2): String = {
    val fmt = if (prec < 0) "%.0f" else "%%.%df".format(prec)
    val amt = fmt.format(value)
    val s   = if (amt == fmt.format(1.0)) "" else "s"
    "%s %s%s".format(amt, units.name.toLowerCase, s)
  }
}