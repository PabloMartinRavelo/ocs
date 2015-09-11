// Copyright 2000
// Association for Universities for Research in Astronomy, Inc.
// Observatory Control System, Gemini Telescopes Project.
// See the file LICENSE for complete details.
//
// $Id: SeqRepeatCoaddExpCB.java 38365 2011-11-03 20:37:20Z swalker $
//

package edu.gemini.spModel.seqcomp;

import edu.gemini.pot.sp.ISPSeqComponent;

import edu.gemini.spModel.data.AbstractDataObject;
import edu.gemini.spModel.data.config.IConfig;
import edu.gemini.spModel.data.config.StringParameter;
import edu.gemini.spModel.data.config.DefaultParameter;
import edu.gemini.spModel.config.AbstractSeqComponentCB;
import edu.gemini.spModel.obscomp.InstConstants;

import java.util.Map;


/**
 * A configuration builder for the science object observe sequence
 * component that include coadds and expsosure time.
 */
public class SeqRepeatCoaddExpCB extends AbstractSeqComponentCB {

    private static final String SYSTEM_NAME = SeqConfigNames.OBSERVE_CONFIG_NAME;

    // for serialization
    private static final long serialVersionUID = 1L;

    private transient int _curCount;
    private transient int _max;
    private transient int _limit;
    private transient String _objectName;
    private transient Map _options;

    public SeqRepeatCoaddExpCB(ISPSeqComponent seqComp) {
        super(seqComp);
    }

    public Object clone() {
        SeqRepeatCoaddExpCB result = (SeqRepeatCoaddExpCB) super.clone();
        result._curCount   = 0;
        result._max        = 0;
        result._limit      = 0;
        result._objectName = null;
        result._options    = null;
        return result;
    }

    protected void thisReset(Map options) {
        _curCount = 0;
        ICoaddExpSeqComponent c = (ICoaddExpSeqComponent) getDataObject();
        _max = c.getStepCount();
        _limit = SeqRepeatCbOptions.getCollapseRepeat(options) ? 1 : _max;
        _objectName = ((AbstractDataObject) c).getType().readableStr;
        _options = options;
    }

    protected boolean thisHasNext() {
        return _curCount < _limit;
    }

    protected void thisApplyNext(IConfig config, IConfig prevFull) {
        ++_curCount;
        ICoaddExpSeqComponent c = (ICoaddExpSeqComponent) getDataObject();
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance(InstConstants.OBSERVE_TYPE_PROP,
                                                        c.getObserveType()));

        config.putParameter(SYSTEM_NAME, StringParameter.getInstance(InstConstants.OBJECT_PROP,
                                                                     _objectName));

        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP, c.getExposureTime()));
//                            StringParameter.getInstance(InstConstants.EXPOSURE_TIME_PROP,
//                                                        String.valueOf(c.getExposureTime())));
        config.putParameter(SYSTEM_NAME,
                DefaultParameter.getInstance(InstConstants.COADDS_PROP, c.getCoaddsCount()));
//                            StringParameter.getInstance(InstConstants.COADDS_PROP,
//                                                        String.valueOf(c.getCoaddsCount())));
        config.putParameter(SYSTEM_NAME,
                            StringParameter.getInstance(InstConstants.OBS_CLASS_PROP,
                                                        c.getObsClass().sequenceValue()));

        if (!SeqRepeatCbOptions.getAddObsCount(_options)) return;
        config.putParameter(SYSTEM_NAME,
           DefaultParameter.getInstance(InstConstants.REPEAT_COUNT_PROP, _max));
    }
}

