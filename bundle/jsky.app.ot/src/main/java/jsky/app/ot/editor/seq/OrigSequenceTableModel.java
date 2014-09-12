//
// $Id$
//

package jsky.app.ot.editor.seq;

import static jsky.app.ot.editor.seq.Keys.*;

import edu.gemini.spModel.config.map.ConfigValMapInstances;
import edu.gemini.spModel.config2.*;
import edu.gemini.spModel.gemini.calunit.calibration.CalDictionary;
import edu.gemini.spModel.gemini.seqcomp.smartgcal.SmartgcalSysConfig;
import edu.gemini.spModel.obsclass.ObsClass;

import javax.swing.table.AbstractTableModel;
import java.util.*;

public class OrigSequenceTableModel extends AbstractTableModel {
    private static ItemKey[] SYS_KEYS = new ItemKey[] {
            TELESCOPE_KEY,
            INSTRUMENT_KEY,
            CALIBRATION_KEY,
    };

    private static final ItemKey[] ADD = new ItemKey[] {
            OBS_CLASS_KEY,
            OBS_COADDS_KEY,
            OBS_EXP_TIME_KEY,
            OBS_OBJECT_KEY,
            OBS_TYPE_KEY,
    };

    private static final ItemKey[] REMOVE = new ItemKey[] {
            CAL_CLASS_KEY,
            CalDictionary.BASECAL_DAY_ITEM.key,
            CalDictionary.BASECAL_NIGHT_ITEM.key,
            CAL_VERSION_KEY,
            INST_EXP_TIME_KEY,
            INST_COADDS_KEY,
            INST_VERSION_KEY,
            OBS_STATUS_KEY,
            OBS_ELAPSED_KEY,
            TEL_BASE_NAME,
            TEL_VERSION_KEY,
    };

    enum RowType {
        title,
        system,
        data,
        empty
    }

    public enum TitleColumns {
        label() {
            public String map(Config c) { return SequenceTabUtil.shortDatasetLabel(c); }
        },
        obsClass() {
            public String map(Config c) {
                String s = (String) c.getItemValue(OBS_CLASS_KEY);
                if (s == null) return ObsClass.SCIENCE.logValue();
                return ObsClass.parseType(s).logValue();
            }
        },
        obsObject() {
            public String map(Config c) { return toString(c, OBS_OBJECT_KEY); }
        },
        obsType() {
            public String map(Config c) { return toString(c, OBS_TYPE_KEY); }
        },
        empty() {
            public String map(Config c) { return ""; }
        },
        obsExposure() {
            private int coadds(Config c) {
                Object coadds = c.getItemValue(OBS_COADDS_KEY);
                if (coadds instanceof String) {
                    return Integer.parseInt(coadds.toString());
                }
                return (coadds == null) ? 1 : (Integer) coadds;
            }

            private String formatExposureTime(Config c) {
                Object time = c.getItemValue(OBS_EXP_TIME_KEY);
                if (time instanceof String) {
                    String s = time.toString();
                    try {
                        Double.parseDouble(s);
                        return s + "s";
                    } catch (NumberFormatException ex) {
                        return s;
                    }
                }
                if (time instanceof Double) {
                    return time.toString() + "s";
                }
                return "";
            }

            public String map(Config c) {
                int coadds = coadds(c);
                String exp = formatExposureTime(c);
                return 1==coadds ? exp : coadds + "x" + exp;
            }
        },

        ;

        public abstract String map(Config c);

        protected String toString(Config c, ItemKey key) {
            Object val = c.getItemValue(key);
            return (val == null) ? "" : val.toString();
        }
    }


    private interface Row {
        RowType type();
        Object  value(int col);
        String  datasetLabel();
        boolean hasMappingError();
    }

    private static class TitleRow implements Row {
        private final Config config;
        TitleRow(Config c) { this.config = c; }

        public RowType type() { return RowType.title; }
        public Object value(int col) {
            return (col < TitleColumns.values().length) ?
                TitleColumns.values()[col].map(config) : "";
        }

        public boolean hasMappingError() {
            Object val = config.getItemValue(SmartgcalSysConfig.MAPPING_ERROR_KEY);
            if (val == null) return false;
            return (val == Boolean.TRUE) || Boolean.parseBoolean(val.toString());
        }

        public String datasetLabel() {
            return config.getItemValue(Keys.DATALABEL_KEY).toString();
        }
    }

    private static ItemKey systemKey(int col) {
        int index = col/2;
        return (index < SYS_KEYS.length) ? SYS_KEYS[index] : null;
    }

    private static class SystemRow implements Row {
        private final Map<ItemKey, List<ItemEntry>> sysConfigs;
        private final String datasetLabel;

        SystemRow(Map<ItemKey, List<ItemEntry>> sysConfigs, String datasetLabel) {
            this.sysConfigs   = sysConfigs;
            this.datasetLabel = datasetLabel;
        }

        public RowType type() { return RowType.system; }
        public Object value(int col) {
            if (col%2 == 1) return "";  // title goes over the attr name col

            ItemKey key = systemKey(col);
            if (key == null) return "";
            if (sysConfigs.get(key) == null) return "";

            return titleCase(key);
        }

        private String titleCase(ItemKey key) {
            String s = key.getName();
            if (s.length() == 0) return "";
            String f = s.substring(0, 1).toUpperCase();
            return s.length() == 1 ? f : f + s.substring(1);
        }

        public boolean hasMappingError() { return false; }

        public String datasetLabel() { return datasetLabel; }
    }


    private static class DataRow implements Row {
        private final Map<ItemKey, List<ItemEntry>> sysConfigs;
        private final int index;
        private final String datasetLabel;

        DataRow(Map<ItemKey, List<ItemEntry>> sysConfigs, int index, String datasetLabel) {
            this.sysConfigs = sysConfigs;
            this.index      = index;
            this.datasetLabel = datasetLabel;
        }

        public RowType type() { return RowType.data; }
        public Object value(int col) {
            ItemKey key = systemKey(col);
            if (key == null) return "";

            List<ItemEntry> lst = sysConfigs.get(key);
            if (lst == null) return "";

            if (index >= lst.size()) return "";
            ItemEntry ie = lst.get(index);

            return (col%2 == 0) ? label(ie.getKey()) : strValue(ie.getItemValue());
        }

        private String strValue(Object value) {
            return ConfigValMapInstances.TO_DISPLAY_VALUE.apply(value).toString();
        }

        private String label(ItemKey key) {
            return key.splitPath().tail().mkString("", " ", "");
        }

        public boolean hasMappingError() { return false; }
        public String datasetLabel() { return datasetLabel; }
    }

    private static final class EmptyRow implements Row {
        public RowType type() { return RowType.empty; }
        public Object value(int col) { return ""; }
        public boolean hasMappingError() { return false; }
        public String datasetLabel() { return null; }
    }

    private static class SplitConfig {
        private final Map<ItemKey, List<ItemEntry>> m = new HashMap<ItemKey, List<ItemEntry>>();
        private final Row datasetRow;
        private final String datasetLabel;

        SplitConfig(Config c) {
            datasetRow = new TitleRow(c);
            datasetLabel = c.getItemValue(Keys.DATALABEL_KEY).toString();
            for (ItemEntry ie : c.itemEntries()) {
                ItemKey root = ie.getKey().getRoot();
                List<ItemEntry> lst = m.get(root);
                if (lst == null) {
                    lst = new ArrayList<ItemEntry>();
                    m.put(root, lst);
                }
                lst.add(ie);
            }
        }

        List<Row> rows() {
            List<Row> rows = new ArrayList<Row>();
            rows.add(datasetRow);

            int max = 0;
            for (ItemKey key : SYS_KEYS) {
                List<ItemEntry> lst = m.get(key);
                if ((lst != null) && (lst.size() > max)) {
                    max = lst.size();
                }
            }

            if (max > 0) rows.add(new SystemRow(m, datasetLabel));
            for (int i=0; i<max; ++i) {
                rows.add(new DataRow(m, i, datasetLabel));
            }

            rows.add(new EmptyRow());

            return rows;
        }
    }

    static final class RowInterval {
        public int start;
        public int end;

        RowInterval(int start, int end) {
            this.start = start;
            this.end   = end;
        }
    }

    private List<Row> rows = new ArrayList<Row>();
    private Map<String, RowInterval> datasetMap = new HashMap<String, RowInterval>();

    private static Config[] extractConfigs(ConfigSequence cs) {
        Config[] configs   = cs.getCompactView();
        Map<ItemKey, Object[]> m = new HashMap<ItemKey, Object[]>();

        for (ItemKey key : ADD) {
            m.put(key, cs.getItemValueAtEachStep(key));
        }

        for (int i=0; i<configs.length; ++i) {
            Config c = configs[i];
            for (ItemKey key : REMOVE) c.remove(key);
            for (ItemKey key : ADD) c.putItem(key, m.get(key)[i]);
        }

        return configs;
    }

    public void setSequence(ConfigSequence sequence) {
        rows.clear();
        datasetMap.clear();

        for (Config c : extractConfigs(sequence)) {
            SplitConfig sc = new SplitConfig(c);
            int start = rows.size();
            rows.addAll(sc.rows());

            RowInterval i = new RowInterval(start, rows.size()-1);
            datasetMap.put(sc.datasetLabel, i);

        }
        fireTableStructureChanged();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < rows.size()) {
            Row row = rows.get(rowIndex);
            return row.value(columnIndex);
        }
        return "";
    }

    public int getRowCount() { return rows.size(); }
    public int getColumnCount() { return SYS_KEYS.length * 2; }
    public Class getColumnClass(int columnIndex) { return String.class; }
    public String getColumnName(int columnIndex) { return ""; }
    public RowType getType(int row) { return rows.get(row).type(); }
    public boolean hasMappingError(int row) { return rows.get(row).hasMappingError(); }
    public String getDatasetLabel(int row) { return rows.get(row).datasetLabel(); }
    public RowInterval getRowInterval(String datasetLabel) { return datasetMap.get(datasetLabel); }
}
