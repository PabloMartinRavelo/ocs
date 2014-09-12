//
// $Id: ObsTimesService.java 46768 2012-07-16 18:58:53Z rnorris $
//

package edu.gemini.spModel.obs;

import edu.gemini.spModel.obsclass.ObsClass;
import edu.gemini.pot.sp.ISPObservation;
import edu.gemini.pot.sp.ISPObservationContainer;
import edu.gemini.spModel.obslog.ObsLog;
import edu.gemini.spModel.time.ObsTimeCharges;
import edu.gemini.spModel.time.ObsTimes;

import java.util.List;

/**
 * A service used to determine both raw and corrected {@link ObsTimes} for
 * particular observations.
 */
public final class ObsTimesService {

    private ObsTimesService() {
    }

    /**
     * Gets raw, uncorrected observation times as calculated from the events
     * received by the observation's {@link edu.gemini.spModel.obsrecord.ObsExecRecord} (if any).  These
     * times will not have had any manual corrections applied to them.
     *
     * @return uncorrected observation times
     */
    public static ObsTimes getRawObsTimes(ISPObservation obs)  {

        // First check the cache.
        ObsTimes res = SPObsCache.getRawObsTimes(obs);
        if (res != null) return res;

        // Default to 0 if the obs record could not be found.
        res = ObsTimes.ZERO_TIMES;

        // Ask the ObsRecord for the raw observation times.
        final ObsLog nodes = ObsLog.getIfExists(obs);
        if (nodes != null) {
            final ObsClass obsClass = ObsClassService.lookupObsClass(obs);
            res = nodes.getExecRecord().getTimes(nodes.getQaRecord(), obsClass.getDefaultChargeClass());
        }

        // Cache and return the results.
        SPObsCache.setRawObsTimes(obs, res);
        return res;
    }

    /**
     * Gets the corrected observation times as calculated from the events
     * received by the observation's {@link edu.gemini.spModel.obsrecord.ObsExecRecord} (if any) after
     * manual corrections have been applied.
     *
     * @return corrected observation times
     */
    public static ObsTimes getCorrectedObsTimes(ISPObservation obs) {

        // First check the cache
        ObsTimes res = SPObsCache.getCorrectedObsTimes(obs);
        if (res != null) return res;

        // Get the raw obs times.
        ObsTimes rawTimes = getRawObsTimes(obs);

        // Now get any corrections from the observation's data object.
        SPObservation dataObj = (SPObservation) obs.getDataObject();
        ObsTimeCharges corrections = dataObj.sumObsTimeCorrections();

        // Apply the corrections.
        ObsTimeCharges correctedCharges;
        correctedCharges = rawTimes.getTimeCharges().addTimeCharges(corrections);

        // Compute the corrected obs times.
        res = new ObsTimes(rawTimes.getTotalTime(), correctedCharges);

        // Cache the results.
        SPObsCache.setCorrectedObsTimes(obs, res);
        return res;
    }

    /**
     * Gets the corrected observation times (see
     * {@link #getCorrectedObsTimes(edu.gemini.pot.sp.ISPObservation)}) for all
     * the observations contained in a particular
     * {@link ISPObservationContainer}. These represent the sum of
     * corrected {@link ObsTimes} for each individual observation.
     *
     * @param container observation container whose observations are sought
     *
     * @return summed {@link ObsTimes} for all observations in the container
     */
    public static ObsTimes getCorrectedObsTimes(ISPObservationContainer container) {

        ObsTimeCharges charges = ObsTimeCharges.ZERO_CHARGES;
        long elapsedTime = 0;

        List obsList = container.getAllObservations();

        for (Object anObsList : obsList) {
            ISPObservation obs = (ISPObservation) anObsList;
            ObsTimes cur = getCorrectedObsTimes(obs);

            charges = charges.addTimeCharges(cur.getTimeCharges());
            elapsedTime += cur.getTotalTime();
        }

        return new ObsTimes(elapsedTime, charges);
    }
}
