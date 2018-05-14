import java.io.Serializable;
import java.util.Date;

public class Tuple implements Serializable, Comparable<Tuple> {

    /**
     * A Tuple represents a row/relation in a table.
     * It consists of an array of values.
     */

    private static final long serialVersionUID = 1L;

    public Object[] getValues() {
        return values;
    }

    private Object[] values;
    public String[] types, colName;
    private int key;

    /**
     * Creates a new tuple
     *
     * @param values  array of values which will be in the tuple
     * @param types   array of the types of values which will be in the tuple
     * @param colName array of columns name in the tuple
     * @param key     Primary key in the tuple
     */
    public Tuple(Object[] values, String[] types, String[] colName, int key) {
        this.values = values;
        this.types = types;
        this.key = key;
        this.colName = colName;
    }

    /**
     * Get the index of the given string
     *
     * @param s string to search for it's index
     * @return given string index
     */
    public int getIndex(String s) {

        for (int i = 0; i < colName.length; i++) {
            if (colName[i] != null && colName[i].equals(s))
                return i;
        }
        return -1;
    }

    /**
     * display tuple values
     *
     * @return
     */
    @Override
    public String toString() {
        String result = "";
        for (Object o : values) {
            if (o != values[(values.length - 1)]) {
                result += o.toString() + ", ";
            } else {
                result += o.toString();
            }
        }
        return result;
    }

    /**
     * add new value to specific index
     *
     * @param index location for inserted value
     * @param value the object value needed to be inserted
     */
    public void add(int index, Object value) {
        values[index] = value;
    }

    /**
     * get the tuple's values array
     *
     * @return
     */
    public Object[] get() {
        return values;
    }

    /**
     * check if two objects are equals
     *
     * @param o objects to be compared with
     * @return true if the are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        Tuple t = (Tuple) o;
        boolean equal = true;
        for (int i = 0; i < t.values.length - 1; i++) {
            switch (types[i].toLowerCase()) {
                case "java.lang.integer":
                    if (!((Integer) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.lang.string":
                    if (!((String) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.lang.double":
                    if (!((Double) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.lang.boolean":
                    if (!((Boolean) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.util.date":
                    if (!((Date) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
            }
        }
        return equal;
    }

    /**
     * comparing tuples
     *
     * @param t tuple to be compared with
     * @return
     */
    @Override
    public int compareTo(Tuple t) {
        boolean equal = true;
        for (int i = 0; i < t.values.length - 1; i++) {
            switch (types[i].toLowerCase()) {
                case "java.lang.integer":
                    if (!((Integer) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.lang.string":
                    if (!((String) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.lang.double":
                    if (!((Double) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.lang.boolean":
                    if (!((Boolean) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
                case "java.util.date":
                    if (!((Date) values[i]).equals(t.values[i]))
                        equal = false;
                    break;
            }
        }
        if (equal)
            return 0;
        switch (types[key].toLowerCase()) {
            case "java.lang.integer":
                return ((Integer) values[key]).compareTo(((Integer) t.values[t.key]));
            case "java.lang.string":
                return ((String) values[key]).compareTo(((String) t.values[t.key]));
            case "java.lang.double":
                return ((Double) values[key]).compareTo(((Double) t.values[t.key]));
            case "java.lang.boolean":
                return ((Boolean) values[key]).compareTo(((Boolean) t.values[t.key]));
            case "java.util.date":
                return ((Date) values[key]).compareTo(((Date) t.values[t.key]));
        }
        return 0;
    }

    /**
     * get the types of the values inside the tuple
     *
     * @return the types of the values
     */
    public String[] getTypes() {
        return types;
    }

    /**
     * get the primary key
     *
     * @return primary key
     */
    public int getKey() {
        return key;
    }


}
