package edu.gemini.spModel.gemini.ghost

import java.beans.PropertyDescriptor
import java.util
import java.util.logging.Logger
import java.util.{Collections, List => JList, Map => JMap, Set => JSet}

import edu.gemini.pot.sp._
import edu.gemini.shared.util.immutable.{None => JNone, Option => JOption}
import edu.gemini.skycalc.{Angle, CoordinateDiff, Coordinates}
import edu.gemini.spModel.config2.{Config, ItemKey}
import edu.gemini.spModel.core.Site
import edu.gemini.spModel.data.ISPDataObject
import edu.gemini.spModel.data.config.{DefaultParameter, DefaultSysConfig, ISysConfig, StringParameter}
import edu.gemini.spModel.data.property.{PropertyProvider, PropertySupport}
import edu.gemini.spModel.gemini.init.{ComponentNodeInitializer, ObservationNI}
import edu.gemini.spModel.inst.{ScienceAreaGeometry, VignettableScienceAreaInstrument}
import edu.gemini.spModel.obs.SPObservation
import edu.gemini.spModel.obs.context.ObsContext
import edu.gemini.spModel.obs.plannedtime.{CommonStepCalculator, PlannedTime}
import edu.gemini.spModel.obs.plannedtime.PlannedTime.{CategorizedTime, CategorizedTimeGroup, Category}
import edu.gemini.spModel.obscomp.{InstConfigInfo, InstConstants, SPInstObsComp}
import edu.gemini.spModel.pio.{ParamSet, Pio, PioFactory}
import edu.gemini.spModel.rich.shared.immutable._
import edu.gemini.spModel.seqcomp.SeqConfigNames
import edu.gemini.spModel.seqcomp.SeqConfigNames.INSTRUMENT_KEY
import edu.gemini.spModel.target.env.TargetEnvironment
import edu.gemini.spModel.target.obsComp.TargetObsComp
import edu.gemini.spModel.telescope.{IssPort, IssPortProvider}

import scala.collection.immutable.TreeMap
import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}
import scalaz._
import Scalaz._
import edu.gemini.spModel.target.SPSkyObject


/** The GHOST instrument SP model.
  * Note that we do not override clone since private variables are immutable.
  */
final class Ghost extends SPInstObsComp(GhostMixin.SP_TYPE) with PropertyProvider
  with GhostMixin with IssPortProvider with PlannedTime.StepCalculator with VignettableScienceAreaInstrument {
  override def getSite: JSet[Site] = {
    Site.SET_GS
  }

  override def getPhaseIResourceName: String = {
    "gemGHOST"
  }

  override def getProperties: JMap[String, PropertyDescriptor] = {
    Ghost.PropertyMap
  }

  /**
   * Paramset methods.
   * Note that we do not call the method on the superclass, SpInstObsComp, because unlike other instruments, GHOST
   * does not have a single exposure time: it instead has two exposure times, red and blue.
   */
  override def getParamSet(factory: PioFactory): ParamSet = {
    val paramSet = factory.createParamSet(getType.readableStr)
    paramSet.setKind(ISPDataObject.PARAM_SET_KIND)

    // Only write the title as a property if it has been changed.
    if (isValidTitle) Pio.addParam(factory, paramSet, ISPDataObject.TITLE_PROP, getTitle)

    Pio.addParam(factory, paramSet, InstConstants.POS_ANGLE_PROP, getPosAngleDegreesStr)
    Pio.addParam(factory, paramSet, Ghost.PORT_PROP, port.name())
    Pio.addBooleanParam(factory, paramSet, Ghost.ENABLE_FIBER_AGITATOR_1_PROP.getName, enableFiberAgitator1)
    Pio.addBooleanParam(factory, paramSet, Ghost.ENABLE_FIBER_AGITATOR_2_PROP.getName, enableFiberAgitator2)
    Pio.addDoubleParam(factory, paramSet, Ghost.RED_EXPOSURE_TIME_PROP.getName, redExposureTime)
    Pio.addParam(factory, paramSet, Ghost.RED_BINNING_PROP, redBinning.name)
    Pio.addParam(factory, paramSet, Ghost.RED_READ_NOISE_GAIN_PROP, redReadNoiseGain.name)
    Pio.addDoubleParam(factory, paramSet, Ghost.BLUE_EXPOSURE_TIME_PROP.getName, blueExposureTime)
    Pio.addParam(factory, paramSet, Ghost.BLUE_BINNING_PROP, blueBinning.name)
    Pio.addParam(factory, paramSet, Ghost.BLUE_READ_NOISE_GAIN_PROP, blueReadNoiseGain.name)
    paramSet
  }

  override def setParamSet(paramSet: ParamSet): Unit = {
    Option(Pio.getValue(paramSet, Ghost.PORT_PROP)).map(IssPort.valueOf).foreach(setIssPort)
    Option(Pio.getValue(paramSet, ISPDataObject.TITLE_PROP)).foreach(setTitle)
    Option(Pio.getValue(paramSet, InstConstants.POS_ANGLE_PROP)).map(_.toDouble).foreach(setPosAngleDegrees)
    setEnableFiberAgitator1(Pio.getBooleanValue(paramSet, Ghost.ENABLE_FIBER_AGITATOR_1_PROP.getName, true))
    setEnableFiberAgitator2(Pio.getBooleanValue(paramSet, Ghost.ENABLE_FIBER_AGITATOR_2_PROP.getName, true))
    setRedExposureTime(Pio.getDoubleValue(paramSet, Ghost.RED_EXPOSURE_TIME_PROP.getName, InstConstants.DEF_EXPOSURE_TIME))
    Option(Pio.getValue(paramSet, Ghost.RED_BINNING_PROP)).map(GhostBinning.valueOf).foreach(setRedBinning)
    Option(Pio.getValue(paramSet, Ghost.RED_READ_NOISE_GAIN_PROP)).map(GhostReadNoiseGain.valueOf).foreach(setRedReadNoiseGain)
    setBlueExposureTime(Pio.getDoubleValue(paramSet, Ghost.BLUE_EXPOSURE_TIME_PROP.getName, InstConstants.DEF_EXPOSURE_TIME))
    Option(Pio.getValue(paramSet, Ghost.BLUE_BINNING_PROP)).map(GhostBinning.valueOf).foreach(setBlueBinning)
    Option(Pio.getValue(paramSet, Ghost.BLUE_READ_NOISE_GAIN_PROP)).map(GhostReadNoiseGain.valueOf).foreach(setBlueReadNoiseGain)
  }

  override def getSysConfig: ISysConfig = {
    val sc = new DefaultSysConfig(SeqConfigNames.INSTRUMENT_CONFIG_NAME)
    sc.putParameter(StringParameter.getInstance(ISPDataObject.VERSION_PROP, getVersion))
    sc.putParameter(DefaultParameter.getInstance(Ghost.POS_ANGLE_PROP, getPosAngle))
    sc.putParameter(DefaultParameter.getInstance(Ghost.PORT_PROP, getIssPort))
    sc.putParameter(DefaultParameter.getInstance(Ghost.ENABLE_FIBER_AGITATOR_1_PROP.getName, isEnableFiberAgitator1))
    sc.putParameter(DefaultParameter.getInstance(Ghost.ENABLE_FIBER_AGITATOR_2_PROP.getName, isEnableFiberAgitator2))
    sc.putParameter(DefaultParameter.getInstance(Ghost.RED_EXPOSURE_TIME_PROP.getName, getRedExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Ghost.RED_BINNING_PROP.getName, getRedBinning))
    sc.putParameter(DefaultParameter.getInstance(Ghost.RED_READ_NOISE_GAIN_PROP, getRedReadNoiseGain))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_EXPOSURE_TIME_PROP.getName, getBlueExposureTime))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_BINNING_PROP.getName, getBlueBinning))
    sc.putParameter(DefaultParameter.getInstance(Ghost.BLUE_READ_NOISE_GAIN_PROP, getBlueReadNoiseGain))
    sc
  }

  /**
   * ISS Port
   */
  private var port: IssPort = IssPort.UP_LOOKING

  override def getIssPort: IssPort = port

  override def setIssPort(newValue: IssPort): Unit = {
    val oldValue = getIssPort
    if (oldValue != newValue) {
      port = newValue
      firePropertyChange(Ghost.PORT_PROP, oldValue, newValue)
    }
  }

  /**
   * Unsupported operations: GHOST has two exposure times, red and blue, and not a single exposure time like other
   * instruments do.
   */
  override def getExposureTime: Double = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def getExposureTimeAsString: String = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def getTotalExposureTime: Double = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def getTotalExposureTimeAsString: String = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def setExposureTime(newValue: Double): Unit = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def setExposureTimeAsString(newValue: String): Unit = {
    Ghost.LOG.severe(Ghost.GhostExposureTimeErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostExposureTimeErrorMessage)
  }

  override def setCoadds(newValue: Int): Unit = {
    Ghost.LOG.severe(Ghost.GhostCoaddsErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostCoaddsErrorMessage)
  }

  override def getCoadds: Int = {
    Ghost.LOG.severe(Ghost.GhostCoaddsErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostCoaddsErrorMessage)
  }

  override def getCoaddsAsString: String = {
    Ghost.LOG.severe(Ghost.GhostCoaddsErrorMessage)
    throw new UnsupportedOperationException(Ghost.GhostCoaddsErrorMessage)
  }

  /**
   * Fiber agitator 1: default is enabled.
   */
  private var enableFiberAgitator1: Boolean = true

  def isEnableFiberAgitator1: Boolean = enableFiberAgitator1

  def setEnableFiberAgitator1(newValue: Boolean): Unit = {
    val oldValue = isEnableFiberAgitator1
    if (oldValue != newValue) {
      enableFiberAgitator1 = newValue
      firePropertyChange(Ghost.ENABLE_FIBER_AGITATOR_1_PROP, oldValue, newValue)
    }
  }

  /**
   * Fiber agitator 2: default is enabled.
   */
  private var enableFiberAgitator2: Boolean = true

  def isEnableFiberAgitator2: Boolean = enableFiberAgitator2

  def setEnableFiberAgitator2(newValue: Boolean): Unit = {
    val oldValue = isEnableFiberAgitator2
    if (oldValue != newValue) {
      enableFiberAgitator2 = newValue
      firePropertyChange(Ghost.ENABLE_FIBER_AGITATOR_2_PROP, oldValue, newValue)
    }
  }

  /**
   * Detectors.
   */
  private var redExposureTime: Double = InstConstants.DEF_EXPOSURE_TIME

  def getRedExposureTime: Double = redExposureTime

  def setRedExposureTime(newValue: Double): Unit = {
    val oldValue = getRedExposureTime
    if (oldValue != newValue) {
      redExposureTime = newValue
      firePropertyChange(Ghost.RED_EXPOSURE_TIME_PROP, oldValue, newValue)
    }
  }

  private var redExposureCount: Int = InstConstants.DEF_REPEAT_COUNT

  def getRedExposureCount: Int = redExposureCount

  def setRedExposureCount(newValue: Int): Unit = {
    val oldValue = getRedExposureCount
    if (oldValue != newValue) {
      redExposureCount = newValue
      firePropertyChange(Ghost.RED_EXPOSURE_COUNT_PROP, oldValue, newValue)
    }
  }

  private var redBinning: GhostBinning = GhostBinning.DEFAULT

  def getRedBinning: GhostBinning = redBinning

  def setRedBinning(newValue: GhostBinning): Unit = {
    val oldValue = getRedBinning
    if (oldValue != newValue) {
      redBinning = newValue
      firePropertyChange(Ghost.RED_BINNING_PROP, oldValue, newValue)
    }
  }

  private var redReadNoiseGain: GhostReadNoiseGain = GhostReadNoiseGain.DEFAULT

  def getRedReadNoiseGain: GhostReadNoiseGain = redReadNoiseGain

  def setRedReadNoiseGain(newValue: GhostReadNoiseGain): Unit = {
    val oldValue = getRedReadNoiseGain
    if (oldValue != newValue) {
      redReadNoiseGain = newValue
      firePropertyChange(Ghost.RED_READ_NOISE_GAIN_PROP, oldValue, newValue)
    }
  }

  private var blueExposureTime: Double = InstConstants.DEF_EXPOSURE_TIME

  def getBlueExposureTime: Double = blueExposureTime

  def setBlueExposureTime(newValue: Double): Unit = {
    val oldValue = getBlueExposureTime
    if (oldValue != newValue) {
      blueExposureTime = newValue
      firePropertyChange(Ghost.BLUE_EXPOSURE_TIME_PROP, oldValue, newValue)
    }
  }


  private var blueExposureCount: Int = InstConstants.DEF_REPEAT_COUNT

  def getBlueExposureCount: Int = blueExposureCount

  def setBlueExposureCount(newValue: Int): Unit = {
    val oldValue = getBlueExposureCount
    if (oldValue != newValue) {
      blueExposureCount = newValue
      firePropertyChange(Ghost.BLUE_EXPOSURE_COUNT_PROP, oldValue, newValue)
    }
  }

  private var blueBinning: GhostBinning = GhostBinning.DEFAULT

  def getBlueBinning: GhostBinning = blueBinning

  def setBlueBinning(newValue: GhostBinning): Unit = {
    val oldValue = getBlueBinning
    if (oldValue != newValue) {
      blueBinning = newValue
      firePropertyChange(Ghost.BLUE_BINNING_PROP, oldValue, newValue)
    }
  }

  private var blueReadNoiseGain: GhostReadNoiseGain = GhostReadNoiseGain.DEFAULT

  def getBlueReadNoiseGain: GhostReadNoiseGain = blueReadNoiseGain

  def setBlueReadNoiseGain(newValue: GhostReadNoiseGain): Unit = {
    val oldValue = getBlueReadNoiseGain
    if (oldValue != newValue) {
      blueReadNoiseGain = newValue
      firePropertyChange(Ghost.BLUE_READ_NOISE_GAIN_PROP, oldValue, newValue)
    }
  }

  override def calc(cur: Config, prev: JOption[Config]): CategorizedTimeGroup = {
    val times: util.Collection[CategorizedTime] = new util.ArrayList[CategorizedTime]()

    // TODO-GHOST: Default values
    times.add(CategorizedTime.fromSeconds(Category.READOUT, 60))

    // TODO-GHOST: Have to handle exposure times differently. Max of exposure times?
    times.add(CategorizedTime.fromSeconds(Category.EXPOSURE, Math.max(redExposureTime * redExposureCount, blueExposureTime * blueExposureCount)))
    CommonStepCalculator.instance.calc(cur, prev).addAll(times)
  }

  override def getVignettableScienceArea: ScienceAreaGeometry = GhostScienceAreaGeometry

  override def pwfs2VignettingClearance: Angle = Angle.arcmins(5.5)
}

object Ghost {
  val LOG: Logger = Logger.getLogger(classOf[Ghost].getName)

  // Unfortunately we need a Java "Supplier" and "Function" which makes it
  // awkward to create the NodeInitializer via ComponentNodeInitializer.
  private val GhostSupplier: java.util.function.Supplier[Ghost] =
    new java.util.function.Supplier[Ghost] {
      def get(): Ghost = new Ghost()
    }

  private val GhostCbFactory: java.util.function.Function[ISPObsComponent, GhostCB] =
    new java.util.function.Function[ISPObsComponent, GhostCB] {
      def apply(oc: ISPObsComponent): GhostCB = new GhostCB(oc)
    }

  val NI: ISPNodeInitializer[ISPObsComponent, Ghost] =
    new ComponentNodeInitializer(SPComponentType.INSTRUMENT_GHOST, GhostSupplier, GhostCbFactory)

  val OBSERVATION_NI: ISPNodeInitializer[ISPObservation, SPObservation] = new ObservationNI(Instrument.Ghost.some()) {
    override protected def addTargetEnv(factory: ISPFactory, obsNode: ISPObservation): Unit = {
      Try {
        val p   = obsNode.getProgram
        val oc  = factory.createObsComponent(p, TargetObsComp.SP_TYPE, null)
        val toc = oc.getDataObject.asInstanceOf[TargetObsComp]

        // Create a single target GHOST asterism as the default.
        val a   = GhostAsterism.createEmptySingleTargetAsterism
        val env = TargetEnvironment.create(a)

        toc.setTargetEnvironment(env)
        oc.setDataObject(toc)
        obsNode.addObsComponent(oc)
      } match {
        case Success(_)               =>
          // Do nothing.
        case Failure(ex: SPException) =>
          throw new RuntimeException("Unable to create and initialize GHOST target environment", ex)
        case Failure(_)               =>
          // This should never happen.
          throw new RuntimeException("Unknown failure in creating GHOST target environment")
      }
    }
  }

  // The name of the Ghost instrument configuration.
  val INSTRUMENT_NAME_PROP: String = "GHOST"

  // GHOST-specific exposure times.
  val EXPOSURE_TIME_RED_PROP = "redExposureTime"
  val COUNT_RED = "redExposureCount"
  val BINNING_RED = "redBinning"
  val NOISE_GAIN_RED = "redReadNoiseGain"
  val DEF_EXPOSURE_TIME_RED = 10.0
  val EXPOSURE_TIME_RED_KEY = new ItemKey(INSTRUMENT_KEY, EXPOSURE_TIME_RED_PROP)
  val EXPOSURE_TIME_BLUE_PROP = "blueExposureTime"
  val COUNT_BLUE = "blueExposureCount"
  val BINNING_BLUE = "blueBinning"
  val NOISE_GAIN_BLUE = "blueReadNoiseGain"
  val DEF_EXPOSURE_TIME_BLUE = 10.0
  val EXPOSURE_TIME_BLUE_KEY = new ItemKey(INSTRUMENT_KEY, EXPOSURE_TIME_BLUE_PROP)

  // The names of the base position / IFUs.
  val BASE_RA_DEGREES: String  = "baseRADeg"
  val BASE_RA_HMS: String      = "baseRAHMS"
  val BASE_DEC_DEGREES: String = "baseDecDeg"
  val BASE_DEC_DMS: String     = "baseDecDMS"

  val FIBER_AGITATOR_1: String = "enableFiberAgitator1"
  val FIBER_AGITATOR_2: String = "enableFiberAgitator2"

  val SRIFU1_NAME: String      = "srifu1Name"
  val SRIFU1_RA_DEG: String    = "srifu1CoordsRADeg"
  val SRIFU1_DEC_DEG: String   = "srifu1CoordsDecDeg"
  val SRIFU1_RA_HMS: String    = "srifu1CoordsRAHMS"
  val SRIFU1_DEC_DMS: String   = "srifu1CoordsDecDMS"
  val SRIFU1_GUIDING: String   = "srifu1Guiding"

  val SRIFU2_NAME: String      = "srifu2Name"
  val SRIFU2_RA_DEG: String    = "srifu2CoordsRADeg"
  val SRIFU2_DEC_DEG: String   = "srifu2CoordsDecDeg"
  val SRIFU2_RA_HMS: String    = "srifu2CoordsRAHMS"
  val SRIFU2_DEC_DMS: String   = "srifu2CoordsDecDMS"
  val SRIFU2_GUIDING: String   = "srifu2Guiding"

  val HRIFU1_NAME: String      = "hrifu1Name"
  val HRIFU1_RA_DEG: String    = "hrifu1CoordsRADeg"
  val HRIFU1_DEC_DEG: String   = "hrifu1CoordsDecDeg"
  val HRIFU1_RA_HMS: String    = "hrifu1CoordsRAHMS"
  val HRIFU1_DEC_DMS: String   = "hrifu1CoordsDecDMS"
  val HRIFU1_GUIDING: String   = "hrifu1Guiding"

  val HRIFU2_NAME: String      = "hrifu2Name"
  val HRIFU2_RA_DEG: String    = "hrifu2CoordsRADeg"
  val HRIFU2_DEC_DEG: String   = "hrifu2CoordsDecDeg"
  val HRIFU2_RA_HMS: String    = "hrifu2CoordsRAHMS"
  val HRIFU2_DEC_DMS: String   = "hrifu2CoordsDecDMS"

  /** The properties supported by this class. */
  private def initProp(propName: String, query: Boolean, iter: Boolean): PropertyDescriptor = {
    PropertySupport.init(propName, classOf[Ghost], query, iter)
  }

  private val query_yes = true
  private val query_no  = false
  private val iter_yes  = true
  private val iter_no   = false

  val POS_ANGLE_PROP: PropertyDescriptor = initProp(InstConstants.POS_ANGLE_PROP, query = query_no, iter = iter_no)
  val PORT_PROP: PropertyDescriptor = initProp(IssPortProvider.PORT_PROPERTY_NAME, query = query_no, iter = iter_no)
  val ENABLE_FIBER_AGITATOR_1_PROP: PropertyDescriptor = initProp(FIBER_AGITATOR_1, query = query_no, iter = iter_no)
  val ENABLE_FIBER_AGITATOR_2_PROP: PropertyDescriptor = initProp(FIBER_AGITATOR_2, query = query_no, iter = iter_no)
  val RED_EXPOSURE_TIME_PROP: PropertyDescriptor = initProp(EXPOSURE_TIME_RED_PROP, query = query_no, iter = iter_yes)
  val RED_EXPOSURE_COUNT_PROP: PropertyDescriptor = initProp(COUNT_RED, query_no, iter_yes)
  val RED_BINNING_PROP: PropertyDescriptor = initProp(BINNING_RED, query = query_yes, iter = iter_no)
  val RED_READ_NOISE_GAIN_PROP: PropertyDescriptor = initProp(NOISE_GAIN_RED, query = query_no, iter = iter_no)
  val BLUE_EXPOSURE_TIME_PROP: PropertyDescriptor = initProp(EXPOSURE_TIME_BLUE_PROP, query = query_no, iter = iter_yes)
  val BLUE_EXPOSURE_COUNT_PROP: PropertyDescriptor = initProp(COUNT_BLUE, query_no, iter_yes)
  val BLUE_BINNING_PROP: PropertyDescriptor = initProp(BINNING_BLUE, query = query_yes, iter = iter_no)
  val BLUE_READ_NOISE_GAIN_PROP: PropertyDescriptor = initProp(NOISE_GAIN_BLUE, query = query_no, iter = iter_no)

  private val Properties: List[(String, PropertyDescriptor)] = List(
    POS_ANGLE_PROP,
    PORT_PROP,
    ENABLE_FIBER_AGITATOR_1_PROP,
    ENABLE_FIBER_AGITATOR_2_PROP,
    RED_EXPOSURE_TIME_PROP,
    RED_EXPOSURE_COUNT_PROP,
    RED_BINNING_PROP,
    RED_READ_NOISE_GAIN_PROP,
    BLUE_EXPOSURE_TIME_PROP,
    BLUE_EXPOSURE_COUNT_PROP,
    BLUE_BINNING_PROP,
    BLUE_READ_NOISE_GAIN_PROP
  ).map(p => (p.getName, p))

  private[ghost] val PropertyMap: JMap[String, PropertyDescriptor] = {
    Collections.unmodifiableMap(TreeMap(Properties: _*).asJava)
  }

  /** Currently, the instrument has no queryable configuration parameters. */
  val getInstConfigInfo: JList[InstConfigInfo] = List.empty[InstConfigInfo].asJava

  val GhostExposureTimeErrorMessage: String = "Error: tried to access single exposure time for GHOST."
  val GhostCoaddsErrorMessage: String = "Error: GHOST does not support coadds."
}
