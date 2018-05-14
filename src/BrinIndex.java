import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;


public class BrinIndex implements Serializable {

    /**
     * BRIN Index that store the max and min of each page
     * to make the Database Engine Operation faster and easier
     */

    private String indexPath, dataPath, tableName, indexColName;
    private transient DenseLayer denseLayer;
    private transient BrinLayer brinLayer;

    /**
     * Constructor that creates the BRIN index for a given table
     *
     * @param dataPath        path of the data stored
     * @param htblColNameType names and types of values in the table
     * @param indexkey        index key of the table
     * @param primarykey      primary key of the table
     * @param tableName       name of table
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public BrinIndex(String dataPath, Hashtable<String, String> htblColNameType, String indexkey, String primarykey, String tableName) throws IOException, ClassNotFoundException, DBAppException {
        this.dataPath = dataPath;
        indexPath = dataPath + indexkey + '/';
        this.tableName = tableName;
        indexColName = indexkey;

        createTIndexDirectory();
        createDenseIndex(indexPath, htblColNameType, indexkey, primarykey);
        createBrinIndex(indexkey);
        save();

    }

    /**
     * Create Dense Layer for the BRIN Index
     *
     * @param indexPath       path of the index
     * @param htblColNameType names and types of values in the table
     * @param indexkey        index key of the table
     * @param primarykey      primary key of the table
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public void createDenseIndex(String indexPath, Hashtable<String, String> htblColNameType, String indexkey, String primarykey) throws IOException, ClassNotFoundException, DBAppException {
        denseLayer = new DenseLayer(indexPath, htblColNameType, indexkey, primarykey, dataPath, tableName);
    }

    /**
     * Create BRIN Layer for the BRIN Index
     *
     * @param indexkey index key of the table
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public void createBrinIndex(String indexkey) throws IOException, ClassNotFoundException, DBAppException {
        brinLayer = new BrinLayer(indexPath, indexkey);
    }

    /**
     * Create Directory for the BRIN Index
     */
    private void createTIndexDirectory() {
        File brin = new File(indexPath);
        brin.mkdir();
    }

    /**
     * Get the Indexed Column name
     *
     * @return
     */
    public String getIndexColName() {
        return indexColName;
    }

    /**
     * Fetch the dense layer class and assign it to denseLayer variable
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public DenseLayer fetchDenseLayer() throws FileNotFoundException, IOException, ClassNotFoundException {
        File file = new File(indexPath + "DenseLayer/DenseLayer.class");
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            DenseLayer layer = (DenseLayer) ois.readObject();
            ois.close();
            return denseLayer = layer;
        }
        return null;
    }

    /**
     * Fetch the BRIN Layer Class and assign it to brinLayer variable
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public BrinLayer fetchBrinLayer() throws FileNotFoundException, IOException, ClassNotFoundException {
        File file = new File(indexPath + "BrinLayer/BrinLayer.class");
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            BrinLayer layer = (BrinLayer) ois.readObject();
            ois.close();
            return brinLayer = layer;
        }
        return null;
    }

    /**
     * Save the BRIN Index into .class files
     *
     * @throws IOException
     */
    public void save() throws IOException {
        File index = new File(indexPath + indexColName + ".class");
        if (!index.exists())
            index.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(index));
        oos.writeObject(this);
        oos.close();
    }

    public Iterator<Tuple> search(Object min, Object max, boolean minEq, boolean maxEq) throws FileNotFoundException, ClassNotFoundException, IOException {
        fetchBrinLayer();
        fetchDenseLayer();
        HashSet<Integer> pages = brinLayer.search(min, max, minEq, maxEq);

        return denseLayer.search(min, max, minEq, maxEq, pages);

    }

    public void deleteTuple(Tuple tupleToDelete) throws FileNotFoundException, ClassNotFoundException, IOException, DBAppException {
        fetchBrinLayer();
        fetchDenseLayer();
        // Read page number from brin layer
        HashSet<Integer> list = new HashSet<>();
        Object idx = tupleToDelete.get()[tupleToDelete.getIndex(indexColName)];
        list = brinLayer.search(idx, idx, true, true);
        if (list.isEmpty()) {
            System.err.println("Delete-trace: Tuple doesn't exist in index");
            return;
        }
        int pageNumber = list.iterator().next();

        denseLayer.delete(tupleToDelete, pageNumber);
        brinLayer.refresh(pageNumber, denseLayer.noPages);
    }

    public void insertTuple(Tuple t, int pagetable) throws FileNotFoundException, ClassNotFoundException, IOException, DBAppException {
        fetchBrinLayer();
        fetchDenseLayer();
        HashSet<Integer> list = new HashSet<>();
        Object idx = t.get()[t.getIndex(indexColName)];
        list = brinLayer.search(idx, idx, true, true);
        int page;
        if (list.isEmpty())
            page = denseLayer.noPages;
        else
            page = list.iterator().next();
        page = denseLayer.insert(t, page, pagetable);
        brinLayer.refresh(page, denseLayer.noPages);
    }

    public void drop() throws IOException, ClassNotFoundException {
        fetchBrinLayer();
        fetchDenseLayer();
        denseLayer.drop();
        brinLayer.drop();
        File dir = new File(indexPath);
        for (File file : dir.listFiles())
            file.delete();

    }

}
