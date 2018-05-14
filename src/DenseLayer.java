import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

public class DenseLayer implements Serializable {

    /**
     * Dense Layer that store the sorted values of the pages and point to them
     */
    public String primarykey;
    public String indexkey;
    public String tableName, dataPath, indexPath, DenseLayerPath;
    public Hashtable<String, String> htblColNameType;
    public Table myTable;
    public int noPages;

    /**
     * Create a Dense Layer for a certain table
     *
     * @param indexPath       path to store the dense layer
     * @param htblColNameType hashtable of the names and types of the values
     * @param indexkey        the key for the current index
     * @param primarykey      primary key of the table
     * @param dataPath        the path for the data of the dense layer
     * @param tableName       table name to make the dense layer on it
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public DenseLayer(String indexPath, Hashtable<String, String> htblColNameType, String indexkey, String primarykey, String dataPath, String tableName) throws IOException, ClassNotFoundException, DBAppException {
        this.primarykey = primarykey;
        this.indexkey = indexkey;
        this.htblColNameType = htblColNameType;
        this.indexPath = indexPath;
        this.dataPath = dataPath;
        this.tableName = tableName;
        DenseLayerPath = indexPath + "DenseLayer" + '/';
        noPages = -1;

        File tableSer = new File(dataPath + tableName + ".class");
        if (tableSer.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(tableSer));
            Table table = (Table) ois.readObject();
            ois.close();
            this.myTable = table;
        }

        createTDenseDirectory();
        load();
        saveindex();
    }

    /**
     * make directory for the dense layer
     */
    private void createTDenseDirectory() {
        File dense = new File(DenseLayerPath);
        dense.mkdir();
    }

    /**
     * Loads the data from the table
     * Sorts the data by the indexed column
     * Stores the data in the dense layer
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public void load() throws FileNotFoundException, IOException, ClassNotFoundException, DBAppException {
        ArrayList<Tuple> data = new ArrayList<Tuple>();
        int pageIndex = myTable.getCurPageIndex();
        for (int i = 0; i <= pageIndex; i++) {
            // Student_0.class


            String name = dataPath + tableName + "_" + i + ".class";

            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(name));
            Page page = (Page) ois.readObject();
            ois.close();
            for (Tuple t : page.getTuples()) {
                int indexKeyPos = t.getIndex(indexkey);
                int primaryKeyPos = t.getIndex(primarykey);
                Object[] values = new Object[3];
                values[0] = t.get()[indexKeyPos];
                values[1] = t.get()[primaryKeyPos];
                values[2] = i;

                String[] types = new String[3];
                types[0] = t.getTypes()[indexKeyPos];
                types[1] = t.getTypes()[primaryKeyPos];
                types[2] = "java.lang.integer";

                String[] colName = new String[3];
                colName[0] = t.colName[indexKeyPos];
                colName[1] = t.colName[primaryKeyPos];
                colName[2] = "page.number";

                Tuple newTuple = new Tuple(values, types, colName, 0);

                data.add(newTuple);
            }
        }

        Collections.sort(data);
        if (data.isEmpty())
            return;
        Page curPage = createPage();
        for (int i = 0; i < data.size(); i++) {
            if (curPage.isFull()) {
                curPage.savePage();
                curPage = createPage();
            }
            curPage.insert(data.get(i), true);
        }
        curPage.savePage();
    }

    /**
     * Create new Dense Layer Page
     *
     * @return
     * @throws IOException if an I/O error occur
     */
    private Page createPage() throws IOException {

        Page page = new Page(DenseLayerPath + indexkey + "dense_" + (++noPages) + ".class");
        saveindex();
        return page;
    }

    /**
     * saving the index file in the secondary storage
     *
     * @throws IOException if an I/O error occur
     */
    private void saveindex() throws IOException {
        File dense = new File(DenseLayerPath + "DenseLayer" + ".class");
        if (!dense.exists())
            dense.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dense));
        oos.writeObject(this);
        oos.close();
    }

    public Iterator<Tuple> search(Object min, Object max, boolean minEq, boolean maxEq, HashSet<Integer> pages) throws FileNotFoundException, IOException, ClassNotFoundException {
        ArrayList<Tuple> tuples = new ArrayList<>();
        for (Integer x : pages) {
            String name = DenseLayerPath + indexkey + "dense_" + x + ".class";
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(name));
            Page page = (Page) ois.readObject();
            for (Tuple t : page.getTuples()) {
                if (compare(t.getValues()[0], min) >= 0 && compare(t.getValues()[0], max) <= 0) {
                    if (!((compare(t.getValues()[0], min) == 0 && !minEq) || (compare(t.getValues()[0], max) == 0 && !maxEq)))
                        tuples.add(t);

                }
            }
            ois.close();

        }
        Iterator<Tuple> t = tuples.iterator();
        return t;
    }

    public int compare(Object x, Object y) {
        switch (y.getClass().getName()) {
            case "java.lang.Integer":
                return ((Integer) x).compareTo(((Integer) y));
            case "java.lang.String":
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

    public void delete(Tuple tupleToDelete, int pageNum) throws DBAppException, FileNotFoundException, IOException, ClassNotFoundException {
        if (pageNum > noPages)
            throw new DBAppException("Tuple doesn't exist in Dense-Layer index");
        File file = new File(DenseLayerPath + indexkey + "dense_" + pageNum + ".class");
        Page page = null;
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            page = (Page) ois.readObject();
            ois.close();
        }
        if (page == null)
            throw new DBAppException("Tuple doesn't exist in Dense-Layer index");

        int idx = 0;
        // Loops over all of the tuples in this page
        while (idx < page.getTupleCount()) {
            Tuple curTuple = page.getTuples().get(idx++);
            Object c1 = tupleToDelete.get()[1];
            Object c2 = curTuple.get()[1];

            // If the current tuple equals the tuple that we want to delete
            if (compare(c1, c2) == 0) {
                page.delete(curTuple);
                break;
            }
        }

        Page prevPage = page;
        page = loadPage(++pageNum);
        // Shift all of the next tuples
        while (page != null) {
            Tuple curTuple = page.getTuples().get(0);
            prevPage.insert(curTuple, false);
            page.delete(curTuple);
            page = loadPage(++pageNum);

        }

        // Save the changes
        saveindex();
    }

    public int insert(Tuple t, int pageNum, int pagetable) throws FileNotFoundException, IOException, ClassNotFoundException, DBAppException {
        int indexKeyPos = t.getIndex(indexkey);
        int primaryKeyPos = t.getIndex(primarykey);
        Object[] values = new Object[3];
        values[0] = t.get()[indexKeyPos];
        values[1] = t.get()[primaryKeyPos];
        values[2] = pagetable;

        String[] types = new String[3];
        types[0] = t.getTypes()[indexKeyPos];
        types[1] = t.getTypes()[primaryKeyPos];
        types[2] = "java.lang.integer";

        String[] colName = new String[3];
        colName[0] = t.colName[indexKeyPos];
        colName[1] = t.colName[primaryKeyPos];
        colName[2] = "page.number";

        Tuple newTuple = new Tuple(values, types, colName, 0);
        if (pageNum >= noPages) {
            File file = new File(DenseLayerPath + indexkey + "dense_" + (pageNum) + ".class");
            Page page = null;
            if (file.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                page = (Page) ois.readObject();
                ois.close();
            }
            if (page.isFull())
                page = createPage();
            page.insert(newTuple, true);
            saveindex();
            return noPages;
        }
        for (int i = pageNum; i <= noPages; i++) {
            File file2 = new File(DenseLayerPath + indexkey + "dense_" + i + ".class");
            Page curpage = null;
            if (file2.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file2));
                curpage = (Page) ois.readObject();
                ois.close();
            }
            if (curpage == null) break;
            if (curpage.isFull()) {
                Tuple tmp = curpage.getTuples().get(curpage.getTuples().size() - 1);
                curpage.getTuples().remove(tmp);
                curpage.insert(newTuple, true);
                newTuple = tmp;
                curpage.savePage();
            } else {
                curpage.insert(newTuple, true);
                curpage.savePage();
                break;
            }
        }
        saveindex();
        return pageNum;


    }


    public Page loadPage(int pageNum) throws ClassNotFoundException, IOException {
        File file = new File(DenseLayerPath + indexkey + "dense_" + pageNum + ".class");
        Page page = null;
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            page = (Page) ois.readObject();
            ois.close();
        }
        return page;
    }

    public void drop() throws IOException {
        File dir = new File(DenseLayerPath);
        for (File file : dir.listFiles())
            file.delete();

    }


}
