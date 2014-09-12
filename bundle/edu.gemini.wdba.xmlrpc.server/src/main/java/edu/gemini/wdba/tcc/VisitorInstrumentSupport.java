package edu.gemini.wdba.tcc;

import edu.gemini.spModel.gemini.gpi.Gpi;
import edu.gemini.spModel.gemini.visitor.VisitorInstrument;

/**
 * TCC support for Visitor Instruments
 */
public class VisitorInstrumentSupport implements ITccInstrumentSupport {
    private ObservationEnvironment _oe;

    private VisitorInstrumentSupport(ObservationEnvironment oe) {
        if (oe == null) throw new IllegalArgumentException("Observation environment can not be null");
        _oe = oe;
    }

    /**
     * Factory for creating a new Visitor Instrument Support.
     */
    static public ITccInstrumentSupport create(ObservationEnvironment oe) {
        return new VisitorInstrumentSupport(oe);
    }

    @Override
    public String getWavelength() {
        VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();
        return inst.getWavelengthStr();
    }

    @Override
    public String getPositionAngle() {
        VisitorInstrument inst = (VisitorInstrument) _oe.getInstrument();
        return inst.getPosAngleDegreesStr();
    }

    @Override
    public String getTccConfigInstrument() {
        return "VISITOR";
    }

    @Override
    public String getTccConfigInstrumentOrigin() {
        return "visitor";
    }

    @Override
    public String getFixedRotatorConfigName() {
        return null;
    }

    @Override
    public String getChopState() {
        return TccNames.NOCHOP;
    }

    @Override
    public void addGuideDetails(ParamSet p) {

    }
}
