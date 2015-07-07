// Copyright 2001 Association for Universities for Research in Astronomy, Inc.,
// Observatory Control System, Gemini Telescopes Project.
//
// $Id: PosMapOffsetEntry.java 45596 2012-05-29 21:50:05Z swalker $
//

package jsky.app.ot.gemini.tpe;

import edu.gemini.spModel.guide.GuideProbe;
import edu.gemini.spModel.target.SPTarget;
import edu.gemini.spModel.target.offset.OffsetPosBase;
import edu.gemini.shared.util.immutable.Option;
import edu.gemini.spModel.target.offset.OffsetPosList;
import jsky.app.ot.tpe.PosMapEntry;
import jsky.app.ot.tpe.TpePositionMap;


/**
 * A Simple utility class that combines a PosMapEntry with a corresponding
 * OffsetPosBase, along with some usefull utility methods.
 */
public final class PosMapOffsetEntry {
    private PosMapEntry<SPTarget> _pme;
    private OffsetPosBase _offsetPos;

    private PosMapOffsetEntry(PosMapEntry<SPTarget> pme, OffsetPosBase offsetPos) {
        _pme = pme;
        _offsetPos = offsetPos;
    }

    public PosMapEntry<SPTarget> getPosMapEntry() {
        return _pme;
    }

    public OffsetPosBase getOffsetPos() {
        return _offsetPos;
    }


    /**
     * Return the PosMapEntry for the named guide star as used by the given
     * offset position.
     *
     * If the guide tag is set to PARKED, return null.
     * If it is set to FROZEN, use the guide star used by the previous offset position,
     * or return null if there is none.
     *
     * @param pm position map to use
     * @param opl offset position list to use (if offset not null)
     * @param op offset position to use if tag is FROZEN
     */
    public static PosMapOffsetEntry getPosMapOffsetEntry(TpePositionMap pm, OffsetPosList<OffsetPosBase> opl, OffsetPosBase op, GuideProbe guider, Option<SPTarget> targetOpt) {
        SPTarget target = targetOpt.isEmpty() ? null : targetOpt.getValue();

        if ((op == null) || (guider == null)) {
            return new PosMapOffsetEntry(pm.getPositionMapEntry(target), op);
        }

        if (op.isFrozen(guider)) {
            int index = opl.getPositionIndex(op);
            if (index > 0) {
                return getPosMapOffsetEntry(pm, opl, opl.getPositionAt(index - 1), guider, targetOpt);
            }
            return new PosMapOffsetEntry(null, op);
        } else if (!op.isActive(guider)) {
            return new PosMapOffsetEntry(null, op);
        }

        return new PosMapOffsetEntry(pm.getPositionMapEntry(target), op);
    }
}

