package edu.gemini.spdb.shell.osgi;

import edu.gemini.pot.sp.ISPNode;
import edu.gemini.pot.sp.ISPProgram;
import edu.gemini.pot.spdb.DBAbstractQueryFunctor;
import edu.gemini.pot.spdb.IDBDatabaseService;
import edu.gemini.pot.spdb.IDBQueryFunctor;
import edu.gemini.pot.spdb.IDBQueryRunner;
import edu.gemini.shared.util.immutable.ImEither;
import edu.gemini.spModel.core.SPBadIDException;
import edu.gemini.spModel.core.SPProgramID;
import edu.gemini.spModel.io.SpImportService;
import edu.gemini.spdb.shell.misc.EphemerisPurgeCommand;
import static edu.gemini.spdb.shell.misc.EphemerisPurgeCommand.*;
import edu.gemini.spdb.shell.misc.ExportXmlCommand;
import edu.gemini.spdb.shell.misc.ImportXmlCommand;
import edu.gemini.spdb.shell.misc.LsProgs;
import org.osgi.util.tracker.ServiceTracker;

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class Commands {

    private final ServiceTracker tracker;
    private final Set<Principal> user;

    Commands(final ServiceTracker tracker, Set<Principal> user) {
        this.tracker = tracker;
        this.user = user;
    }

    private IDBDatabaseService db() {
        final IDBDatabaseService db = (IDBDatabaseService) tracker.getService();
        if (db == null)
            throw new IllegalStateException("No database is available.");
        return db;
    }

    private interface Query {
        IDBQueryFunctor doQuery(IDBQueryRunner run, IDBQueryFunctor fun);
    }

    private static final Query PROG_QUERY = IDBQueryRunner::queryPrograms;

    private static final Query PLAN_QUERY = IDBQueryRunner::queryNightlyPlans;

    // return all the program ids formatted in columns
    public String lsprogs() {
        return lsRoots(PROG_QUERY);
    }

    public String lsplans() {
        return lsRoots(PLAN_QUERY);
    }

    private String lsRoots(final Query q) {
        final int cols = 5;
        final StringBuilder sb = new StringBuilder();
        int col = -1;
        for (final Object id : ((LsProgs) q.doQuery(db().getQueryRunner(user), new LsProgs())).ids()) {
            if (++col == cols) {
                sb.append('\n');
                col = 0;
            }
            sb.append(String.format("%-18s", id));
        }
        return sb.toString();
    }


    // import xml files
    public String importXml(final File path) throws Throwable {
        return importXml(path, "keep");
    }

    public String importXml(final File path, final String option) throws Throwable {
        try {
            final SpImportService.ImportDirective op;
            if ("keep".equals(option)) {
                op = SpImportService.Skip$.MODULE$;
            } else if ("replace".equals(option)) {
                op = SpImportService.Replace$.MODULE$;
            } else if ("copy".equals(option)) {
                op = SpImportService.Copy$.MODULE$;
            } else {
                return ("Option must be one of { copy, keep, replace }");
            }

            new ImportXmlCommand(db(), path, op).importXML();

            return "Done.";

        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }

    }

    public String exportXml(final File path, final String... progIdStrings) {
        if (!path.isDirectory()) return ("Not a directory: " + path);

        final List<SPProgramID> progIds = new ArrayList<>(progIdStrings.length);
        for (String id : progIdStrings) {
            try {
                progIds.add(SPProgramID.toProgramID(id));
            } catch (SPBadIDException e) {
                return "Could not parse '" + id + "' as a program ID.";
            }
        }

        try {
            new ExportXmlCommand(db(), path, user).exportXML(progIds);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
        return "Done.";
    }

    // export xml files
    public String exportXml(final File path) {
        return exportXml(path, new String[0]);
    }

    public String du() {
        return String.format("Total SPDB storage %,d bytes.", db().getDBAdmin().getTotalStorage());
    }

    private final String PURGE_CONFIRMATION = "" + System.currentTimeMillis();

    public String purge() {
        return String.format("This will remove all programs in the database.\nThis operation cannot be undone.\nTo confirm, enter:\n   purge %s", PURGE_CONFIRMATION);
    }

    public String purge(String confirm) {
        if (PURGE_CONFIRMATION.equals(confirm)) {
            final IDBDatabaseService db = db();
            db.getQueryRunner(user).queryPrograms(new DBAbstractQueryFunctor() {
                public void execute(IDBDatabaseService db, ISPNode node, Set<Principal> principals) {
                    if (node instanceof ISPProgram) {
                        final ISPProgram p = (ISPProgram) node;
                        System.out.format("Deleting %s %s\n", p.getNodeKey(), p.getProgramID());
                        db.remove(p);
                    }
                }
            });
            return null;
        } else {
           return String.format("Incorrect confirmation (expected %s)", PURGE_CONFIRMATION);
        }
    }

    private ImEither<String, ISPProgram> prog(String programId) {
        final IDBDatabaseService db = db();
        final SPProgramID pid;
        try {
            pid = SPProgramID.toProgramID(programId);
        } catch (SPBadIDException ex) {
            return ImEither.left(String.format("%s: illegal program id", programId));
        }

        final ISPProgram p = db.lookupProgramByID(pid);
        return (p == null) ? ImEither.left(String.format("%s: not in db", programId)) : ImEither.right(p);
    }

    public String rmprog(String programId) {
        final ImEither<String, ISPProgram> e = prog(programId);
        e.foreach(p -> db().remove(p));
        return e.fold(s -> s, p -> "");
    }

    public String rmprog(String[] programIds) {
        final StringBuilder buf = new StringBuilder();
        for (String pid : programIds) {
            String res = rmprog(pid);
            if (!"".equals(res)) buf.append(res).append("\n");
        }
        return buf.toString();
    }

    public String rmprog(List<String> programIds) {
        return rmprog(programIds.toArray(new String[programIds.size()]));
    }

    public String purgeEphemeris(String programId) {
        return purgeEphemeris(programId, ObservedOnly$.MODULE$.displayValue());
    }

    public String purgeEphemeris(String programId, String purgeOption) {
        final ImEither<String, ISPProgram> e = prog(programId);
        final scala.Option<PurgeOption>   po = PurgeOption$.MODULE$.fromDisplayValue(purgeOption);

        return ImEither.merge(e.flatMap(p -> po.isDefined() ?
            ImEither.<String, String>right(EphemerisPurgeCommand.apply(p, po.get())) :
            ImEither.<String, String>left("Usage: purgeEphemeris programId " + PurgeOption$.MODULE$.usageString())
        ));
    }
}
