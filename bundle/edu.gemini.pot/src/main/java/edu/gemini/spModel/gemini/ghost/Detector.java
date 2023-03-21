package edu.gemini.spModel.gemini.ghost;

import java.math.BigDecimal;

public enum Detector {
    BLUE("E2V_CCD231-84-1-G57", "Blue", Detector.PIXEL_SIZE, 4096, 4112, new BigDecimal("0.0003263888888888889")),
    RED ("E2V_CCD231-C6-1-G58", "Red", Detector.PIXEL_SIZE, 6144, 4096, new BigDecimal("0.00022916666666666666"));

    public static final double PIXEL_SIZE = 0.4; // arcsec/pixel

    public static final Detector DEFAULT = RED;

    private final String _displayValue;
    private final String _model;
    private final double _pixelSize;
    private final int _xSize;
    private final int _ySize;

    private final BigDecimal _darkCurrent;

    Detector(final String manufacter, final String displayValue,
             final double pixelSize, final int xSize, final int ySize, final BigDecimal darkCurrent) {
        this._model = manufacter;
        this._displayValue = displayValue;
        this._pixelSize = pixelSize;
        this._xSize = xSize;
        this._ySize = ySize;
        this._darkCurrent = darkCurrent;
    }

    public String displayValue() {
        return _displayValue;
    }

    public int getXsize() {
        return _xSize;
    }

    public BigDecimal getDarkCurrent() { return _darkCurrent;}

    public String getModel() {
        return _model;
    }

}
