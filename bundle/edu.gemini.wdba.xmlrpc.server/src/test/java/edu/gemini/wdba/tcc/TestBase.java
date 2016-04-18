//
// $
//

package edu.gemini.wdba.tcc;

import edu.gemini.pot.sp.*;
import edu.gemini.pot.spdb.DBLocalDatabase;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.core.Site;
import edu.gemini.spModel.ext.ObservationNode;
import edu.gemini.spModel.ext.ObservationNodeFunctor;
import edu.gemini.util.security.principal.StaffPrincipal;
import edu.gemini.wdba.glue.WdbaGlueService;
import edu.gemini.wdba.glue.api.WdbaContext;
import edu.gemini.wdba.glue.api.WdbaDatabaseAccessService;
import edu.gemini.wdba.xmlrpc.ITccXmlRpc;
import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.Reader;
import java.io.StringReader;
import java.security.Principal;
import java.util.*;

/**
 * A base class for creating unit test code for the TCC configuration file
 * creator.  It handles making the test database, putting a test program in it,
 * and cleaning up afterwords.  It also has convenience methods for working
 * with the configuration file and for adding targets etc. to observations.
 *
 * <p>
 * <b>The property <code>HLPG_PROJECT_BASE</code> must be set to OCS
 * installation dir.</b>
 */
public abstract class TestBase extends TestCase {

    protected IDBDatabaseService odb;

    protected ISPProgram prog;
    protected SPNodeKey progKey;
    protected SPProgramID progId;

    protected ISPObservation obs;
    protected SPObservationID obsId;
    protected WdbaDatabaseAccessService databaseAccessService;

    // We will run as superuser
    final Set<Principal> user = Collections.<Principal>singleton(StaffPrincipal.Gemini());

    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    protected void setUp() throws Exception {
        odb = DBLocalDatabase.createTransient();

        progId  = SPProgramID.toProgramID("GS-2009B-Q-1");
        prog = odb.getFactory().createProgram(null, progId);
        odb.put(prog);
        progKey = prog.getProgramKey();

        obs = odb.getFactory().createObservation(prog, null);
        obsId = obs.getObservationID();
        prog.addObservation(obs);

        databaseAccessService = new WdbaGlueService(odb, user);
    }

    protected void tearDown() throws Exception {
        odb.getDBAdmin().shutdown();
    }

    protected ITccXmlRpc getHandler(final Site site) {
        return new TccHandler(new WdbaContext(site, databaseAccessService, user));
    }

    protected ObservationNode getObsNode() throws Exception {
        return ObservationNodeFunctor.getObservationNode(odb, obs, user);
    }

    protected Document parse(String tccConfigXml) throws Exception {
        final SAXReader sax = new SAXReader();

        final Document doc;
        try (Reader rdr = new StringReader(tccConfigXml)) {
            doc = sax.read(rdr);
        }
        return doc;
    }

    protected Document getResults(Site site) throws Exception {
        String xmlStr = getHandler(site).getCoordinates(obsId.toString());
//        System.out.println(xmlStr);
        return parse(xmlStr);
    }

    protected Document getNorthResults() throws Exception {
        return getResults(Site.GN);
    }

    protected Document getSouthResults() throws Exception {
        return getResults(Site.GS);
    }

    protected Element getSubconfig(Document doc, String type) {
        return (Element) doc.selectSingleNode("/" + TccNames.ROOT + "/paramset[@type='" + type + "']");
    }

    protected Element getTccFieldConfig(Document doc) {
        return getSubconfig(doc, TccNames.FIELD);
    }

    @SuppressWarnings({"unchecked"})
    protected List<Element> getTccFieldContainedParamSet(Document doc, String type) {
        Element tccFieldConfig = getTccFieldConfig(doc);
        if (tccFieldConfig == null) return Collections.emptyList();

        return (List<Element>) tccFieldConfig.selectNodes("//paramset[@type='" + type + "']");
    }

    protected List<Element> getTargetGroups(Document doc) {
        return getTccFieldContainedParamSet(doc, TargetGroupConfig.TYPE_VALUE);
    }

    protected List<Element> getTargets(Document doc) {
        List<Element> res = getTccFieldContainedParamSet(doc, "hmsdegTarget");
        res.addAll(getTccFieldContainedParamSet(doc, "conicTarget"));
        return res;
    }

    protected Element getTcsConfiguration(Document doc) {
        return getSubconfig(doc, TccNames.TCS_CONFIGURATION);
    }

    protected Map<String, String> getTcsConfigurationMap(Document doc) throws Exception {
        final Map<String, String> res = new HashMap<>();

        final Element psetElement = getTcsConfiguration(doc);
        @SuppressWarnings({"unchecked"}) List<Element> params = (List<Element>) psetElement.elements();
        for (Element paramElement : params) {
            final String name  = paramElement.attributeValue("name");
            final String value = paramElement.attributeValue("value");
            res.put(name, value);
        }

        return Collections.unmodifiableMap(res);
    }
}
