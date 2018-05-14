import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class Page implements Serializable {

    /**
     * A page is part of a table holding its tuples.
     * Tables are stored in many pages in a binary
     * file format (.class files)
     */

    private static final long serialVersionUID = 1L;

    private int maximumSize, tupleCount;
    private String path;
    private ArrayList<Tuple> tuples;


    /**
     * Create a new page specifying the path at which
     * the page will be stored relative to the executable files
     *
     * @param path the path at which the page is stored relative to the executable files
     * @throws IOException If an I/O error occurred
     */
    public Page(String path) throws IOException {
        Configuration config = new Configuration();
        this.maximumSize = config.getMaximumSize();
        this.path = path;
        tuples = new ArrayList<>(maximumSize);

        // TODO:
        savePage();
    }

    /**
     * Save & insert tuples into the page
     *
     * @param tuple tuple to be inserted
     * @param sort  boolean if the tuples need to be sorted after the insertion
     * @return true if the tuple was inserted successfully, false otherwise
     * @throws DBAppException If an DBAppException error occurred
     * @throws IOException    If an I/O error occurred
     */
    public boolean insert(Tuple tuple, boolean sort) throws DBAppException, IOException {

        if (isFull()) {
            return false;
        }
        tupleCount++;
        tuples.add(tuple);
        if (sort)
            Collections.sort(tuples);
        savePage();
        return true;
    }

    /**
     * delete tuple from the page
     *
     * @param tuple tuple to be deleted
     * @return true if the tuple was deleted successfully, false otherwise
     * @throws DBAppException If an DBAppException error occurred
     * @throws IOException    If an I/O error occurred
     */
    public boolean delete(Tuple tuple) throws DBAppException, IOException {
        if (isEmpty())
            return false;
        tupleCount--;
        tuples.remove(tuple);
        savePage();
        return true;

    }

    /**
     * save the page permanently on a secondary storage
     *
     * @throws IOException If an I/O error occurred
     */
    public void savePage() throws IOException {
        File file = new File(path);
        if (!file.exists())
            file.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
        oos.writeObject(this);
        oos.close();
    }

    public ArrayList<Tuple> getTuples() {
        return tuples;
    }

    public void setTuples(ArrayList<Tuple> tuples) {
        this.tuples = tuples;
    }

    public int getTupleCount() {
        return tupleCount;
    }

    public boolean isFull() {
        return tupleCount == maximumSize;
    }

    public void setTupleCount(int tupleCount) {
        this.tupleCount = tupleCount;

    }

    public boolean isEmpty() {
        return tupleCount == 0;
    }

    /**
     * check if a certain key exist
     *
     * @param objKey the search key
     * @return true if it was found, false otherwise
     */
    public boolean exist(Object objKey) {
        for (Tuple t : tuples) {
            t.get()[t.getKey()].equals(objKey);
            switch (t.getTypes()[t.getKey()].toLowerCase()) {
                case "java.lang.integer":
                    if (((Integer) t.get()[t.getKey()]).equals(objKey))
                        return true;
                case "java.lang.string":
                    if (((String) t.get()[t.getKey()]).equals(objKey))
                        return true;
                case "java.lang.double":
                    if (((Double) t.get()[t.getKey()]).equals(objKey))
                        return true;
                case "java.lang.boolean":
                    if (((Boolean) t.get()[t.getKey()]).equals(objKey))
                        return true;
                case "java.util.date":
                    if (((Date) t.get()[t.getKey()]).equals(objKey))
                        return true;
            }

        }
        return false;
    }

    /**
     * get a certain tuple according to the search key
     *
     * @param objKey the search key
     * @return true if it was found, false otherwise
     */
    public Tuple getThisTuple(Object objKey) {
        for (Tuple t : tuples) {
            t.get()[t.getKey()].equals(objKey);
            switch (t.getTypes()[t.getKey()].toLowerCase()) {
                case "java.lang.integer":
                    if (((Integer) t.get()[t.getKey()]).equals(objKey))
                        return t;
                case "java.lang.string":
                    if (((String) t.get()[t.getKey()]).equals(objKey))
                        return t;
                case "java.lang.double":
                    if (((Double) t.get()[t.getKey()]).equals(objKey))
                        return t;
                case "java.lang.boolean":
                    if (((Boolean) t.get()[t.getKey()]).equals(objKey))
                        return t;
                case "java.util.date":
                    if (((Date) t.get()[t.getKey()]).equals(objKey))
                        return t;
            }

        }
        return null;
    }
}
