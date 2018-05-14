import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;


public class Table implements Serializable {

    private static final long serialVersionUID = 1L;
    private ArrayList<Object> PrimaryKeyCheck;
    private transient ArrayList<BrinIndex> indexList;

    public ArrayList<Object> getPrimaryKeyCheck() {
        return PrimaryKeyCheck;
    }

    private Hashtable<String, String> htblColNameType;
    private String tableName, path, strClusteringKeyColumn;
    private int curPageIndex, maxPageSize, numOfCols, pagetmp;

    /**
     * Get the index of the current page
     *
     * @return
     */
    public int getCurPageIndex() {
        return curPageIndex;
    }


    public Table(String tableName, String path, Hashtable<String, String> htblColNameType,
                 String strClusteringKeyColumn) throws DBAppException, IOException {
        Configuration config = new Configuration();
        PrimaryKeyCheck = new ArrayList();
        this.maxPageSize = config.getMaximumSize();
        this.tableName = tableName;
        this.path = path + tableName + '/';
        this.htblColNameType = htblColNameType;
        this.strClusteringKeyColumn = strClusteringKeyColumn;
        numOfCols = htblColNameType.size();
        curPageIndex = -1;

        createTableDirectory();
        createPage();
        saveTable();

    }

    /**
     * Returns a new Tuple using the values of columns specified in the hashtable passed to the function
     *
     * @param htblColNameValue hashtable containing the values to be inserted
     * @return
     * @throws DBAppException
     */
    private Tuple makeTuple(Hashtable<String, Object> htblColNameValue) throws DBAppException {
        checkInsertedColumns(htblColNameValue);

        Object value = htblColNameValue.get(strClusteringKeyColumn);
        if (value == null)

            throw new DBAppException("Clustering key is not allowed to be null");

        Object[] values = new Object[numOfCols + 1];
        String[] types = new String[numOfCols + 1];
        String[] names = new String[numOfCols + 1];
        Set<String> columns = htblColNameValue.keySet();
        int i = 0;
        int keyIndex = numOfCols;
        for (String column : columns) {
            names[i] = column;
            if (column.equals(strClusteringKeyColumn))
                keyIndex = i;

            types[i] = htblColNameType.get(column);
            values[i] = htblColNameValue.get(column);
            i++;
        }
        Date d = Calendar.getInstance().getTime();
        values[numOfCols] = d;
        names[numOfCols] = "insert time";
        types[numOfCols] = "java.util.date";
        return new Tuple(values, types, names, keyIndex);
    }

    /**
     * Inserts a new tuple using the values specified by the hachtable and the tuple returnes by
     * passing the values to the function {@code maketuple}. Returns True if the value was inserted correctly
     * and false otherwise
     *
     * @param htblColNameValue the hashtable containing that would be inserted
     * @return
     * @throws DBAppException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public boolean insert(Hashtable<String, Object> htblColNameValue)
            throws DBAppException, ClassNotFoundException, IOException {
        Tuple t = makeTuple(htblColNameValue);
        Object value = htblColNameValue.get(strClusteringKeyColumn);
        if (PrimaryKeyCheck.contains(value))
            throw new DBAppException("Insertion in table failed. PrimaryKey value already exist in the table");
        int page = insertTuple(t);
        PrimaryKeyCheck.add(value);
        saveTable();
        rollBack();
        saveTable();

        return true;

    }

    /**
     * Deletes a Tuple with values specified by the
     * parameter hashtable and Returns True if the
     * deletion was successful
     *
     * @param htblColNameValue Hashtable containing the values to be deleted
     * @return
     * @throws DBAppException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public boolean delete(Hashtable<String, Object> htblColNameValue)
            throws DBAppException, ClassNotFoundException, IOException {
        Tuple t = makeTuple(htblColNameValue);
        Object value = htblColNameValue.get(strClusteringKeyColumn);
        deleteTuple(t);
        if (PrimaryKeyCheck.contains(value))
            PrimaryKeyCheck.remove(value);
        saveTable();
        rollBack();
        saveTable();

        return true;
    }

    /**
     * Updates the Tuple by using its Key specified in the parameters and the values from the hashtable
     *
     * @param strKey           The datatypes for all the variables in the tuple
     * @param htblColNameValue The updated values
     * @return
     * @throws DBAppException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ParseException
     */
    public boolean update(String strKey, Hashtable<String, Object> htblColNameValue)
            throws DBAppException, IOException, ClassNotFoundException, ParseException {
        Tuple t = makeTuple(htblColNameValue);
        Object value = htblColNameValue.get(strClusteringKeyColumn);
        Object oldKey = fromStringToObject(strKey, htblColNameType.get(strClusteringKeyColumn));
        if (!value.toString().equals(oldKey.toString()))
            throw new DBAppException("update in table failed. PrimaryKeys are not the same");
        Tuple old = updateTuple(oldKey, t);
        PrimaryKeyCheck.add(value);
        if (PrimaryKeyCheck.contains(oldKey))
            PrimaryKeyCheck.remove(oldKey);
        saveTable();
        rollBack();
        saveTable();

        return true;
    }

    /**
     * Updates the values of a specific Tuple using the values to be replaced
     * by deleting the old Tuple and inserting a new one in its place using the nes
     * values specified
     *
     * @param oldKey The old value identifying in the tuple to be updated
     * @param tuple  The new tuple to be inserted with all the updated values
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    private Tuple updateTuple(Object oldKey, Tuple tuple)
            throws FileNotFoundException, IOException, ClassNotFoundException, DBAppException {
        for (int i = 0; i <= curPageIndex; i++) {
            File file = new File(path + tableName + "_" + i + ".class");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            Page curPage = (Page) ois.readObject();

            if (curPage.exist(oldKey)) {
                Tuple t = curPage.getThisTuple(oldKey);
                curPage.delete(t);
                curPage.insert(tuple, true);
                ois.close();
                pagetmp = i;

                return t;
            }
            ois.close();
        }
        throw new DBAppException("This key does not exist in the table");

    }

    /**
     * Returns the Clustring key of the table
     *
     * @return
     */
    public String getStrClusteringKeyColumn() {
        return strClusteringKeyColumn;
    }

    /**
     * Delets a specific tuple passed as a parameter to the function by matching it with
     * the tuples in the table
     *
     * @param tuple Tuple to be deleted
     * @throws IOException
     * @throws DBAppException
     * @throws ClassNotFoundException
     */
    private void deleteTuple(Tuple tuple) throws IOException, DBAppException, ClassNotFoundException {
        for (int i = 0; i <= curPageIndex; i++) {
            File file = new File(path + tableName + "_" + i + ".class");
//            System.out.println(file.exists());
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            ObjectInputStream ois2 = null;
            Page curPage = (Page) ois.readObject();

            if (curPage.getTuples().contains((tuple))) {
                curPage.delete(tuple);
                if (curPage.getTuples().size() == 0) {
                    ois.close();
                    System.out.println("Delete: " + file.delete());
                    curPageIndex--;
                    return;
                } else {
                    int j = i + 1;
                    File file2 = new File(path + tableName + "_" + j + ".class");
                    while (file2.exists()) {
                        ois2 = new ObjectInputStream(new FileInputStream(file2));
                        Page nxtPage = (Page) ois2.readObject();
                        Tuple tuple1 = nxtPage.getTuples().get(0);
                        curPage.insert(tuple1, true);
                        nxtPage.delete(tuple1);
//                      deleteTuple(tuple1);
                        if (nxtPage.getTupleCount() == 0)
                            file2.delete();
                        j++;
                        curPage = nxtPage;
                        file2 = new File(path + tableName + "_" + j + ".class");

                    }
                    if (ois2 != null)
                        ois2.close();
                    ois.close();
                    return;
                }
            }
            ois.close();
        }
        throw new DBAppException("This row does not exist in the table");
    }

    /**
     * Inserts a new Tuple in the table using the specified tuple
     *
     * @param tuple Tuple to be inserted
     * @return
     * @throws IOException
     * @throws DBAppException
     * @throws ClassNotFoundException
     */
    public int insertTuple(Tuple tuple) throws IOException, DBAppException, ClassNotFoundException {

        for (int i = 0; i <= curPageIndex; i++) {
            File file = new File(path + tableName + "_" + i + ".class");
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            Page curPage = (Page) ois.readObject();
            if (!curPage.isFull()) {
                curPage.insert(tuple, true);
                ois.close();
                return i;
            } else {
                if (tuple.compareTo(curPage.getTuples().get(curPage.getTupleCount() - 1)) < 0) {
                    Tuple t = curPage.getTuples().get(curPage.getTupleCount() - 1);
                    curPage.setTupleCount(curPage.getTupleCount() - 1);
                    curPage.getTuples().remove(t);
                    curPage.insert(tuple, true);
                    ois.close();
                    insertTuple(t);

                    return i;
                }
            }

            ois.close();
        }
        Page curPage = createPage();
        curPage.insert(tuple, true);
        return curPageIndex;
    }

    /**
     * Checks if the inserted values matche the type of the columns or not
     *
     * @param htblColNameValue Values to be checked
     * @throws DBAppException
     */
    private void checkInsertedColumns(Hashtable<String, Object> htblColNameValue) throws DBAppException {
        for (Entry<String, Object> entry : htblColNameValue.entrySet()) {
            String colName = entry.getKey();
            if (!htblColNameType.containsKey(colName))
                throw new DBAppException("Column : " + colName + " doesn't exist");
            if (checkValueType(entry.getValue(), htblColNameType.get(colName)))
                throw new DBAppException("Type mismatch on column : " + colName);
        }
    }

    /**
     * Checks the type of the object specified
     * Could use getClass.getName() instead
     *
     * @param value object to be checked
     * @param type  Type to be matched
     * @return
     */
    public boolean checkValueType(Object value, String type) {

        switch (type.toLowerCase()) {
            case "java.lang.integer":
                if (!(value instanceof Integer))
                    return false;
            case "java.lang.string":
                if (!(value instanceof String))
                    return false;
            case "java.lang.double":
                if (!(value instanceof Double))
                    return false;
            case "java.lang.boolean":
                if (!(value instanceof Boolean))
                    return false;
            case "java.util.date":
                if (!(value instanceof Date))
                    return false;
            default:
                return true;
        }
    }

    /**
     * Converting {@code String} to an object
     *
     * @param strKey {@code String} to be converted to {@code Object}
     * @param type   Type of Object
     * @return
     * @throws ParseException
     * @throws DBAppException
     */
    private Object fromStringToObject(String strKey, String type) throws ParseException, DBAppException {
        Object key = new Object();
        try {
            switch (type.toLowerCase()) {
                case "java.lang.integer":
                    key = Integer.parseInt(strKey);
                    break;
                case "java.lang.string":
                    key = strKey;
                    break;
                case "java.lang.double":
                    key = Double.parseDouble(strKey);
                    break;
                case "java.lang.boolean":
                    key = Boolean.parseBoolean(strKey);
                    break;
                case "java.util.date":
                    SimpleDateFormat format = new SimpleDateFormat("EEE MMM DD HH:mm:ss zzz yyyy");
                    key = format.parse(strKey);
                    break;
            }
            return key;
        } catch (Exception e) {
            throw new DBAppException("this Key have the wrong syntax");
        }
    }

    /**
     * Creates a new Directory in the file path
     */
    private void createTableDirectory() {
        File table = new File(path);
        table.mkdir();
    }

    /**
     * Returns a new pages after creating it in the specific folder specified by the path
     *
     * @return
     * @throws IOException
     */
    private Page createPage() throws IOException {
        Page page = new Page(path + tableName + "_" + (++curPageIndex) + ".class");
        saveTable();
        return page;
    }

    /**
     * Saves the Table to the Disk in the specific path for that
     *
     * @throws IOException
     */
    private void saveTable() throws IOException {
        File table = new File(path + tableName + ".class");
        if (!table.exists())
            table.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(table));
        oos.writeObject(this);
        oos.close();
    }

    /**
     * Returns the Brin Index saved for this specific column
     *
     * @param strColName The column name that identifies its Brin index path
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public BrinIndex getIndex(String strColName) throws FileNotFoundException, IOException, ClassNotFoundException {
        File file = new File(path + strColName + ".class");
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            BrinIndex Index = (BrinIndex) ois.readObject();
            ois.close();
            return Index;
        }

        return null;
    }

    /**
     * Creates a new Brin index for the specified column
     *
     * @param strColName The column that the Brin index will be applied to
     * @throws DBAppException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void createBRINIndex(String strColName) throws DBAppException, IOException, ClassNotFoundException {
        if (!this.htblColNameType.containsKey(strColName))
            throw new DBAppException("this column does not exist");
        Hashtable<String, String> htblColNameType = new Hashtable<>();
        htblColNameType.put(strColName, this.htblColNameType.get(strColName));
        htblColNameType.put(strClusteringKeyColumn, this.htblColNameType.get(strClusteringKeyColumn));

        new BrinIndex(path, htblColNameType, strColName, strClusteringKeyColumn, tableName);
    }


    /**
     * Returns the maximum size of the page
     *
     * @return
     */
    public int getMaxPageSize() {
        return maxPageSize;
    }

    /**
     * Fetches the brin indices from the directory
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    /*
     * Returns all of the BRIN indices created on this table
     * */
    public ArrayList<BrinIndex> fetchBRINindices() throws FileNotFoundException, IOException, ClassNotFoundException {
        File dir = new File(path);
        ArrayList<BrinIndex> list = new ArrayList<BrinIndex>();
        for (File folder : dir.listFiles()) {
            if (folder.isDirectory()) {
                File file = new File(path + folder.getName() + "/" + folder.getName() + ".class");
                if (file.exists()) {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    BrinIndex index = (BrinIndex) ois.readObject();
                    ois.close();
                    list.add(index);
                }
            }
        }
        return indexList = list;
    }

    /**
     * Returns the Brin index for specified column if it was stored before
     *
     * @param strColName The Column name that the index was applied to
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public BrinIndex fetchBRINindex(String strColName) throws FileNotFoundException, IOException, ClassNotFoundException {
        File dir = new File(path);
        for (File folder : dir.listFiles()) {
            if (folder.isDirectory()) {
                File file = new File(path + folder.getName() + "/" + strColName + ".class");
                if (file.exists()) {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                    BrinIndex index = (BrinIndex) ois.readObject();
                    ois.close();
                    return index;
                }
            }
        }
        return null;
    }

    /**
     * Returns the matching tuples after Searching for an object using
     * Brin index
     *
     * @param strColumnName   The name of the column that the Brin index was applid to
     * @param objarrValues    values to be used in the search
     * @param strarrOperators Operators to be used in search for comparing the values
     * @return
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public Iterator<Tuple> search(String strColumnName, Object[] objarrValues, String[] strarrOperators)
            throws FileNotFoundException, ClassNotFoundException, IOException {


        Object min;
        Object max;
        switch (htblColNameType.get(strColumnName).toLowerCase()) {
            case "java.lang.integer":
                min = Integer.MAX_VALUE;
                max = Integer.MIN_VALUE;
                break;
            case "java.lang.string":
                min = "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";
                max = "";
                break;
            case "java.lang.double":
                min = Double.MAX_VALUE;
                max = Double.MIN_VALUE;
                break;

            case "java.util.date":
                min = new Date(Long.MAX_VALUE);
                max = new Date(Long.MIN_VALUE);
                break;
            default:
                min = Integer.MAX_VALUE;
                max = Integer.MIN_VALUE;
        }

        boolean mineq = false;
        boolean maxeq = false;
        for (int i = 0; i < objarrValues.length; i++) {
            switch (strarrOperators[i]) {
                case ">=":
                    min = min(min, objarrValues[i]);
                    mineq = true;
                    break;
                case ">":
                    min = min(min, objarrValues[i]);
                    break;
                case "<=":
                    max = max(max, objarrValues[i]);
                    maxeq = true;
                    break;
                case "<":
                    max = max(max, objarrValues[i]);
                    break;
                default:
                    break;
            }
        }

        ArrayList<Tuple> tabletubles = new ArrayList<Tuple>();

        for (int i = 0; i <= this.getCurPageIndex(); i++) {
            File table = new File(
                    "databases/" + tableName + "/" + tableName + "/" + tableName + "_" + i + ".class");
            System.out.println("databases/" + tableName + "/" + tableName + "/" + tableName + "_" + i + ".class");
            InputStream file = new FileInputStream(table);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            try {

                Page p = (Page) input.readObject();

                ArrayList<Tuple> t = p.getTuples();

                for (Tuple tt : t) {
                    if (tt != null) {
                        int myindex = tt.getIndex(strColumnName);
                        Object o = tt.getValues()[myindex];
                        if (compare(o, min) >= 0 && compare(o, max) <= 0) {
                            if (!((compare(o, min) == 0 && !mineq) || (compare(o, max) == 0 && !maxeq)))
                                tabletubles.add(tt);
                        }

                    }
                }

            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            input.close();
        }

        return tabletubles.iterator();



    /*        Iterator<Tuple> indextuples = index.search(min, max, mineq, maxeq);
            ArrayList<Tuple> tabletubles = new ArrayList<Tuple>();
            while (indextuples.hasNext()) {
                Tuple t = indextuples.next();
//                tabletubles.add(binarySearch(t.get()[t.getKey()]));
                File file = new File(path + tableName + "_" + t.getValues()[2] + ".class");
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                Page curPage = (Page) ois.readObject();
                tabletubles.add(curPage.getThisTuple(t.getValues()[1]));
                ois.close();
            }
            return tabletubles.iterator();

        */
    }

    /**
     * Returns the Maximum value among two objects
     *
     * @param max    Value to be compared
     * @param object Vlaue to be compared
     * @return
     */
    private Object max(Object max, Object object) {

        String type = object.getClass().toString();

        if (type.contains("Integer")) {
            return Math.max((Integer) max, (Integer) object);
        } else if (type.contains("Double")) {
            return Math.max((Double) max, (Double) object);

        } else if (type.contains("String")) {
            if (((String) max).compareTo((String) object) == 1)
                return max;
            else
                return object;
        } else if (type.contains("Date")) {
            if (((Date) max).compareTo((Date) object) == 1)
                return max;
            else
                return object;

        }

        return null;
    }

    /**
     * Returns the minumum value among two {@code Object} values
     *
     * @param min    Value to be compared
     * @param object Value to be compared
     * @return
     */
    private Object min(Object min, Object object) {
        String type = object.getClass().toString();

        if (type.contains("Integer")) {
            return Math.min((Integer) min, (Integer) object);
        } else if (type.contains("Double")) {
            return Math.min((Double) min, (Double) object);

        } else if (type.contains("String")) {
            if (((String) min).compareTo((String) object) == -1)
                return min;
            else
                return object;
        } else if (type.contains("Date")) {
            if (((Date) min).compareTo((Date) object) == -1)
                return min;
            else
                return object;

        }

        return null;
    }

    /**
     * Performing a binary search in  a page using a specific object
     *
     * @param key Value to be searched for
     * @return
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    private Tuple binarySearch(Object key) throws FileNotFoundException, ClassNotFoundException, IOException {
        int lo = 0, hi = curPageIndex, index = -1;

        while (lo <= hi) {
            int mid = (hi + lo) / 2;
            Page curPage = loadPage(mid);

            Tuple firstTuple = curPage.getTuples().get(0);
            Tuple lastTuple = curPage.getTuples().get(curPage.getTupleCount());

            Object p1 = firstTuple.get()[firstTuple.getKey()];
            Object p2 = lastTuple.get()[lastTuple.getKey()];

            if (compare(key, p1) >= 0 && compare(key, p2) <= 0) {
                index = mid;
                break;
            }
            if (compare(key, p2) > 0) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }

        if (index == -1)
            return null;
        return loadPage(index).getThisTuple(key);
    }

    /**
     * Returns a page using its Number that identifies its path
     *
     * @param pageNumber The Number of the Page you want to load
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public Page loadPage(int pageNumber) throws FileNotFoundException, IOException, ClassNotFoundException {
        File file = new File(path + tableName + "_" + pageNumber + ".class");
        Page page = null;
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            page = (Page) ois.readObject();
            ois.close();
        }
        return page;
    }

    /**
     * Compares two objects
     *
     * @param x Object to be compared
     * @param y Object to be compared
     * @return
     */
    public int compare(Object x, Object y) {
        switch (y.getClass().getName().toLowerCase()) {
            case "java.lang.integer":
                return ((Integer) x).compareTo(((Integer) y));
            case "java.lang.string":
                return ((String) x).compareTo(((String) y));
            case "java.lang.double":
                return ((Double) x).compareTo(((Double) y));
            case "java.lang.boolean":
                return ((Boolean) x).compareTo(((Boolean) y));
            case "java.util.date":
                return ((Date) x).compareTo(((Date) y));
        }
        return 0;

    }

    public void rollBack() throws IOException, ClassNotFoundException, DBAppException {
        fetchBRINindices();
        ArrayList<String> indexedColNames = new ArrayList<String>();

        for (BrinIndex index : indexList) {
//        	index.insertTuple(t,page);
            indexedColNames.add(index.getIndexColName());
//        	createBRINIndex(index.getIndexColName());
            index.drop();
        }
        indexList = new ArrayList<BrinIndex>();
        for (String col : indexedColNames)
            createBRINIndex(col);

    }
}
