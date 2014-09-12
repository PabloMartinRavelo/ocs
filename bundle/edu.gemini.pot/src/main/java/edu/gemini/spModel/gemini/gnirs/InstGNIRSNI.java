// Copyright 1999 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
// See the file COPYRIGHT for complete details.
//
// $Id: InstGNIRSNI.java 39256 2011-11-22 17:42:49Z swalker $
//
package edu.gemini.spModel.gemini.gnirs;

import edu.gemini.pot.sp.ISPObsComponent;

import edu.gemini.pot.sp.SPComponentType;
import edu.gemini.spModel.config.IConfigBuilder;
import edu.gemini.spModel.gemini.inst.DefaultInstNodeInitializer;
import edu.gemini.spModel.obscomp.SPInstObsComp;


/**
 * Initializes <code>{@link ISPObsComponent}</code> nodes of type GNIRS.
 */
public final class InstGNIRSNI extends DefaultInstNodeInitializer {
    @Override public SPComponentType getType() { return InstGNIRS.SP_TYPE; }

    @Override protected IConfigBuilder createConfigBuilder(ISPObsComponent node) {
        return new InstGNIRSCB(node);
    }

    @Override public SPInstObsComp createDataObject() {
        return new InstGNIRS();
    }

}
