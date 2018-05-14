import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

public class DBApp implements Serializable {

    /**
     * DataBase Engine Class that uses BRIN Index
     * You can insert, delete, update, and select from our DBApp
     * with or without using the BRIN Index
     */


    private String defaultPath = "databases/";
    private String name, dbPath;

    // TODO: Review below attributes.
    private static HashSet<String> IndexedColumns = new HashSet<>(); // add all
    // indexed
    // columns
    // here
    private Hashtable<String, String> htblColNameType;
    private HashMap<String, Table> tables = new HashMap<>();
    private transient FileWriter writer;

    private transient File metadata;
    private transient Properties properties;
    private Integer MaxRowsPerPage;

    public HashMap<String, Table> getTables() {
        return tables;
    }

    /**
     * Create New DataBase
     *
     * @param name name of the database
     * @throws IOException
     */
    public DBApp(String name) throws IOException {

        Configuration config = new Configuration();
        this.name = name;
        this.dbPath = defaultPath + this.name + '/';
        this.MaxRowsPerPage = config.getMaximumSize();
        File dbFolder = new File(dbPath);
        dbFolder.mkdir();


        // Meta data file
        File data = new File(dbPath + "data/");
        data.mkdirs();

        this.metadata = new File(dbPath + "data/" + "metadata.csv");
        if (!metadata.exists())
            metadata.createNewFile();
        savedatabase();

    }

    /**
     * Save DataBase into the secondary storage
     *
     * @throws IOException
     */
    private void savedatabase() throws IOException {
        File database = new File(defaultPath + "Database" + ".class");
        if (!database.exists())
            database.createNewFile();
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(database));
        oos.writeObject(this);
        oos.close();

    }

    /**
     * Create new table inside the Database
     *
     * @param strTableName           name of the table
     * @param strClusteringKeyColumn the primary key
     * @param htblColNameType        name and types of the values
     * @throws DBAppException
     * @throws IOException
     */
    public void createTable(String strTableName, String strClusteringKeyColumn,
                            Hashtable<String, String> htblColNameType) throws DBAppException, IOException {
        File db = new File("databases/" + name + "/" + strTableName);
        if (db.exists()) {
            throw new DBAppException("Table " + strTableName + " is already exists");
        }

        Table table = new Table(strTableName, dbPath, htblColNameType, strClusteringKeyColumn);
        tables.put(strTableName, table);

        this.htblColNameType = htblColNameType;
        Set<String> columns = htblColNameType.keySet();

        for (String column : columns) {
            boolean key = false;
            boolean indexed = false;
            if (strClusteringKeyColumn.equals(column))
                key = true;
            if (IndexedColumns.contains(column))
                indexed = true;

            WriteMetaData(strTableName, column, htblColNameType.get(column), key, indexed);
            savedatabase();
        }
    }


    /**
     * Create csv file for tables in the DataBase Engine
     *
     * @param strTableName name of the table
     * @param column       name of the column in the table
     * @param string
     * @param key          primary key of the table
     * @param indexed      check if the column is indexed or now
     * @throws IOException
     * @throws DBAppException
     */
    private void WriteMetaData(String strTableName, String column, String string, boolean key, boolean indexed)
            throws IOException, DBAppException {
        // TODO Auto-generated method stub

        writer = new FileWriter(metadata, true);
        writer.append(
                strTableName + "," + column + "," + htblColNameType.get(column) + "," + key + "," + indexed + '\n');

        writer.flush();
        writer.close();
    }

    /**
     * Insert new values in a certain table
     *
     * @param strTableName     name of the table
     * @param htblColNameValue the new values to be inserted
     * @throws DBAppException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void insertIntoTable(String strTableName, Hashtable<String, Object> htblColNameValue)
            throws DBAppException, IOException, ClassNotFoundException {

        Table table = getTable(strTableName);

        if (table == null)
            throw new DBAppException("Table: (" + strTableName + ") does not exist");
        if (!table.insert(htblColNameValue))
            throw new DBAppException("Insertion in table: (" + strTableName + ")failed");
        savedatabase();
    }

    /**
     * Delete values from the table
     *
     * @param strTableName     name of the table
     * @param htblColNameValue values that will be deleted
     * @throws DBAppException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public void deleteFromTable(String strTableName, Hashtable<String, Object> htblColNameValue)
            throws DBAppException, FileNotFoundException, ClassNotFoundException, IOException {

        Table table = getTable(strTableName);

        if (table == null)
            throw new DBAppException("Table: (" + strTableName + ") does not exist");
        if (!table.delete(htblColNameValue))
            throw new DBAppException("Deletion in table: (" + strTableName + ")failed");
        savedatabase();
    }

    /**
     * update specific table values
     *
     * @param strTableName     table to be updated
     * @param strKey           primary key of the table
     * @param htblColNameValue hashtable of the names and values of the new table's values
     * @throws DBAppException
     * @throws FileNotFoundException
     * @throws ClassNotFoundException
     * @throws IOException
     * @throws ParseException
     */
    public void updateTable(String strTableName, String strKey, Hashtable<String, Object> htblColNameValue)
            throws DBAppException, FileNotFoundException, ClassNotFoundException, IOException, ParseException {
        Table table = getTable(strTableName);

        if (table == null)
            throw new DBAppException("Table: (" + strTableName + ") does not exist");
        if (!table.update(strKey, htblColNameValue))
            throw new DBAppException("Update in table: (" + strTableName + ")failed");
        savedatabase();
    }

    /**
     * get the required table from the given name
     *
     * @param strTableName name of the reuired table
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private Table getTable(String strTableName) throws FileNotFoundException, IOException, ClassNotFoundException {

        File file = new File(dbPath + strTableName + "/" + strTableName + ".class");
        if (file.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
            Table table = (Table) ois.readObject();
            ois.close();
            return table;
        }
        return null;
    }

    /**
     * Create a BRIN Index for a given column
     *
     * @param strTableName table that contains the column
     * @param strColName   column to create the index on it
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    public void createBRINIndex(String strTableName, String strColName) throws FileNotFoundException, IOException, ClassNotFoundException, DBAppException {
        // TODO Find the table and create respective index
        Table table = getTable(strTableName);
        table.createBRINIndex(strColName);
    }

    public Iterator<Tuple> selectFromTable(String strTableName, String strColumnName,
                                           Object[] objarrValues,
                                           String[] strarrOperators)
            throws DBAppException, FileNotFoundException, ClassNotFoundException, IOException {
        writeIndexMetadata(strTableName, strColumnName);
        savedatabase();
        return getTable(strTableName).search(strColumnName, objarrValues, strarrOperators);

    }


    /**
     * Create a BRIN Index for a given column
     *
     * @param strTableName table that contains the column
     * @param strColName   column to create the index on it
     * @throws FileNotFoundException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws DBAppException
     */
    private void writeIndexMetadata(String strTableName, String strColumnName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(metadata));
        ArrayList<String> mdata = new ArrayList<>();
        while (reader.ready()) {
            String line = reader.readLine();

            StringTokenizer st = new StringTokenizer(line, ",");
            if (st.nextToken().equals(strTableName) & st.nextToken().equals(strColumnName)) {
                line = line.substring(0, line.length() - 5) + "true";

            }
            mdata.add(line);
        }
        reader.close();
        this.metadata = new File(dbPath + "data/" + "metadata.csv");
        metadata.delete();
        metadata.createNewFile();

        writer = new FileWriter(metadata, true);
        for (String line : mdata)
            writer.append(line + '\n');
        writer.flush();
        writer.close();


    }
}
