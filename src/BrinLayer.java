import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;


public class BrinLayer implements Serializable {


    public String indexPath, BrinLayerPath, denseLayerPath;
    public String indexkey;
    public int noPages;

    /**
     * Created a Brin Index for the specified column
     *
     * @param indexPath The path to save the index
     * @param indexkey  The Key on which the index will be applied
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public BrinLayer(String indexPath, String indexkey) throws IOException, ClassNotFoundException, DBAppException {
        this.indexkey = indexkey;
        this.indexPath = indexPath;
        BrinLayerPath = indexPath + "BrinLayer" + '/';
        denseLayerPath = indexkey + "DenseLayer/";
        noPages = -1;
        createTBrineDirectory();
        load();
        saveindex();
    }

    /**
     * Creates the Directory for the index.
     */
    private void createTBrineDirectory() {
        File brin = new File(BrinLayerPath);
        brin.mkdir();
    }

    /**
     * Loads the index from path
     *
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public void load() throws FileNotFoundException, IOException, ClassNotFoundException, DBAppException {
        File dense = new File(indexPath + "DenseLayer" + '/' + "DenseLayer" + ".class");
        if (!dense.exists())
            throw new DBAppException("dense does not exist");
        ObjectInputStream ois2 = new ObjectInputStream(new FileInputStream(dense));
        DenseLayer ddense = (DenseLayer) ois2.readObject();
        ois2.close();

        Page curPage = createPage();
        for (int i = 0; i <= ddense.noPages; i++) {
            // Student_0.class
            String name = indexPath + "DenseLayer" + '/' + indexkey + "dense" + "_" + i + ".class";
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(name));
            Page page = (Page) ois.readObject();
            ois.close();

            int length = page.getTuples().size();
            if (length == 0) continue;

            Object[] values = new Object[3];
            values[0] = page.getTuples().get(0).get()[0];
            values[1] = page.getTuples().get(length - 1).get()[0];
            values[2] = i;

            String[] types = new String[3];
            types[0] = page.getTuples().get(0).getTypes()[0];
            types[1] = page.getTuples().get(length - 1).getTypes()[0];
            types[2] = "java.lang.integer";

            String[] colName = new String[3];
            colName[0] = page.getTuples().get(0).colName[0];
            colName[1] = page.getTuples().get(0).colName[0];
            colName[2] = "page.number";
            Tuple tuple = new Tuple(values, types, colName, 0);
            if (curPage.isFull()) {
                curPage.savePage();
                curPage = createPage();
            }

            curPage.insert(tuple, false);
        }
        curPage.savePage();
        saveindex();


    }

    /**
     * Returns a new page
     *
     * @return
     * @throws IOException
     */
    private Page createPage() throws IOException {
        Page page = new Page(BrinLayerPath + indexkey + "brin_" + (++noPages) + ".class");
        saveindex();
        return page;
    }

    /**
     * Saves Index to the Disk
     *
     * @throws IOException
     */
    private void saveindex() throws IOException {
        File brin = new File(BrinLayerPath + "BrinLayer" + ".class");
        if (!brin.exists())
            brin.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(brin));
        oos.writeObject(this);
        oos.close();
    }

    public HashSet<Integer> search(Object min, Object max, boolean minEq, boolean maxEq) throws FileNotFoundException, IOException, ClassNotFoundException {
        HashSet<Integer> pages = new HashSet<>();
        for (int i = 0; i <= noPages; i++) {
            String name = BrinLayerPath + indexkey + "brin_" + i + ".class";
            ObjectInputStream ois2 = new ObjectInputStream(new FileInputStream(name));
            Page brin = (Page) ois2.readObject();
            for (Tuple t : brin.getTuples()) {
                if (compare(max, t.getValues()[0]) >= 0 && compare(min, t.getValues()[1]) <= 0)
                    pages.add((Integer) t.getValues()[2]);
                if (compare(min, t.getValues()[1]) > 0)
                    break;
            }
            ois2.close();
        }
        return pages;
    }

    /**
     * Compares two objects
     *
     * @param x First object to be compared
     * @param y The Other object to be compared
     * @return
     */
    public int compare(Object x, Object y) {
        switch (x.getClass().getName().toLowerCase()) {
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

    /**
     * Refreshes the index after any change
     *
     * @param densePageNumber    The page number among index pages
     * @param maxDensePageNumber Max number of pages in the index
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void refresh(int densePageNumber, int maxDensePageNumber) throws FileNotFoundException, IOException, ClassNotFoundException {
        int tuplesPerPage = (new Configuration()).getMaximumSize();
        int brinPageNumber = densePageNumber / tuplesPerPage;
        int tuplePointer = densePageNumber % tuplesPerPage;

        while (densePageNumber <= maxDensePageNumber) {

            File file = new File(indexPath + indexkey + "index_" + brinPageNumber + ".class");
            Page brinPage = null;
            if (file.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                brinPage = (Page) ois.readObject();
                ois.close();
            }
            if (brinPage == null)
                brinPage = createPage();


            file = new File(denseLayerPath + indexkey + "dense_" + densePageNumber + ".class");
            Page densePage = null;
            if (file.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
                densePage = (Page) ois.readObject();
                ois.close();
            }
            if (densePage == null) return;


            Object[] values = new Object[3];
            values[0] = densePage.getTuples().get(0).get()[0];
            values[1] = densePage.getTuples().get(densePage.getTupleCount() - 1).get()[0];
            values[2] = densePageNumber;

            String[] types = new String[3];
            types[0] = densePage.getTuples().get(0).getTypes()[0];
            types[1] = densePage.getTuples().get(densePage.getTupleCount() - 1).getTypes()[0];
            // TODO Fix types[2]
            types[2] = "java.lang.integer";

            String[] colName = new String[3];
            colName[0] = densePage.getTuples().get(0).colName[0];
            colName[1] = densePage.getTuples().get(0).colName[0];
            colName[2] = "page.number";

            Tuple tuple = new Tuple(values, types, colName, 0);
            brinPage.getTuples().set(tuplePointer++, tuple);

            densePageNumber++;
            if (tuplePointer >= tuplesPerPage)
                brinPageNumber++;
        }
        saveindex();
    }


    /**
     * Delets the index file
     *
     * @throws IOException
     */
    public void drop() throws IOException {
        File dir = new File(indexPath);
        for (File file : dir.listFiles())
            file.delete();

    }

}
