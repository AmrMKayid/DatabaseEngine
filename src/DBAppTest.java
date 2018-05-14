import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

public class DBAppTest {

    /**
     * Class to test our DBApp
     */

    static DBApp ourDB;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void main(String[] args) throws DBAppException, IOException, ClassNotFoundException, ParseException {

        try {
            String strTableName = "Student";

            ourDB = new DBApp(strTableName);


            Hashtable htblColNameType = new Hashtable();
            htblColNameType.put("id", "java.lang.Integer");
            htblColNameType.put("name", "java.lang.String");
            htblColNameType.put("gpa", "java.lang.double");
            ourDB.createTable(strTableName, "id", htblColNameType);
            ourDB.createBRINIndex("Student", "id");


            Hashtable htblColNameValue = new Hashtable();
            htblColNameValue.put("id", new Integer(2343432));
            htblColNameValue.put("name", new String("Ahmed Noor"));
            htblColNameValue.put("gpa", new Double(0.95));
            ourDB.insertIntoTable(strTableName, htblColNameValue);

//			htblColNameValue.clear();
//			htblColNameValue.put("id", new Integer(2343432));
//			htblColNameValue.put("name", new String("Ahmed Noor"));
//			htblColNameValue.put("gpa", new Double(0.95));
//			ourDB.deleteFromTable(strTableName, htblColNameValue);

//			ourDB.deleteFromTable(strTableName, htblColNameValue);
            htblColNameValue.clear();
            htblColNameValue.put("id", new Integer(4253455));
            htblColNameValue.put("name", new String("Ahmed Ali"));
            htblColNameValue.put("gpa", new Double(0.95));
            ourDB.insertIntoTable(strTableName, htblColNameValue);

            htblColNameValue.clear();
            htblColNameValue.put("id", new Integer(453455));
            htblColNameValue.put("name", new String("Dalia Noor"));
            htblColNameValue.put("gpa", new Double(1.25));
            ourDB.insertIntoTable(strTableName, htblColNameValue);

            htblColNameValue.clear();
            htblColNameValue.put("id", new Integer(23498));
            htblColNameValue.put("name", new String("John Noor"));
            htblColNameValue.put("gpa", new Double(1.5));
            ourDB.insertIntoTable(strTableName, htblColNameValue);

            htblColNameValue.clear();
            htblColNameValue.put("id", new Integer(78452));
            htblColNameValue.put("name", new String("Zaky Noor"));
            htblColNameValue.put("gpa", new Double(0.88));
            ourDB.insertIntoTable(strTableName, htblColNameValue);

//			 htblColNameValue.clear();
//			 htblColNameValue.put("id", new Integer(23498));
//			 htblColNameValue.put("name", new String("John Noor"));
//			 htblColNameValue.put("gpa", new Double(1.5));
//			 ourDB.deleteFromTable(strTableName, htblColNameValue);
//
//			 htblColNameValue.clear();
//			 htblColNameValue.put("id", new Integer(78452));
//			 htblColNameValue.put("name", new String("3ala2 Noor"));
//			 htblColNameValue.put("gpa", new Double(1.08));
//			 ourDB.updateTable(strTableName,"78452", htblColNameValue);
//			 htblColNameValue.clear();
//
//			 ourDB.createBRINIndex("Student", "name");
//
//			htblColNameValue.put("id", new Integer(4253455));
//			htblColNameValue.put("name", new String("Ahmed Ali"));
//			htblColNameValue.put("gpa", new Double(0.95));
//			ourDB.deleteFromTable(strTableName, htblColNameValue);
//
//			htblColNameValue = new Hashtable();
//			htblColNameValue.put("id", new Integer(20432));
//			htblColNameValue.put("name", new String("AhmedasadNoor"));
//			htblColNameValue.put("gpa", new Double(0.95));
//			ourDB.insertIntoTable(strTableName, htblColNameValue);

            ourDB.createBRINIndex("Student", "gpa");

            Object[] objarrValues = new Object[2];
            objarrValues[0] = new Double(0.85);
            objarrValues[1] = new Double(1.0);
            String[] strarrOperators = new String[2];
            strarrOperators[0] = ">=";
            strarrOperators[1] = "<";
            Iterator resultSet = ourDB.selectFromTable(strTableName, "gpa",
                    objarrValues, strarrOperators);
            System.err.println("__________Select__________");
            while (resultSet.hasNext())
                System.out.println(resultSet.next().toString());

        } catch (DBAppException D) {
            System.out.println(D.getMessage());
        }

        TestSerialization();

    }

    public static void TestSerialization() throws IOException, ClassNotFoundException {
        File database = new File("databases/" + "Database" + ".class");
        InputStream file = new FileInputStream(database);
        InputStream buffer = new BufferedInputStream(file);
        ObjectInput input = new ObjectInputStream(buffer);

        DBApp DB = (DBApp) input.readObject();
        input.close();
        Set<String> names = DB.getTables().keySet();
        for (String name : names) {
            System.err.println("__________Table__________");
            File table1 = new File("databases/" + name + "/" + name + "/" + name + ".class");
            InputStream file1 = new FileInputStream(table1);
            InputStream buffer1 = new BufferedInputStream(file1);
            ObjectInput input1 = new ObjectInputStream(buffer1);

            Table ttt = (Table) input1.readObject();

            for (int i = 0; i <= ttt.getCurPageIndex(); i++) {
                File table = new File("databases/" + name + "/" + name + "/" + name + "_" + i + ".class");
                System.out.println("databases/" + name + "/" + name + "/" + name + "_" + i + ".class");
                InputStream file2 = new FileInputStream(table);
                InputStream buffer2 = new BufferedInputStream(file2);
                ObjectInput input2 = new ObjectInputStream(buffer2);
                try {

                    Page p = (Page) input2.readObject();

                    ArrayList<Tuple> t = p.getTuples();

                    for (Tuple tt : t) {
                        if (tt != null)
                            System.out.println(tt.toString());
                    }

                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                input2.close();
            }

            System.err.println("__________Index__________");
            input1.close();
            ArrayList<BrinIndex> b = ttt.fetchBRINindices();
            for (BrinIndex index : b) {
                System.out.println("\n" + index.getIndexColName());
                BrinLayer bi = index.fetchBrinLayer();
                DenseLayer di = index.fetchDenseLayer();

                System.out.println("__________Brin Layer__________");
                for (int i = 0; i <= bi.noPages; i++) {
                    File pagFile = new File(bi.BrinLayerPath + bi.indexkey + "brin_" + i + ".class");
                    InputStream file2 = new FileInputStream(pagFile);
                    InputStream buffer2 = new BufferedInputStream(file2);
                    ObjectInput input2 = new ObjectInputStream(buffer2);
                    try {

                        Page p = (Page) input2.readObject();

                        ArrayList<Tuple> t = p.getTuples();

                        for (Tuple tt : t) {
                            if (tt != null)
                                System.err.println(tt.toString());
                        }

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    input2.close();

                }

                System.out.println("__________Dense Layer__________");
                for (int i = 0; i <= di.noPages; i++) {
                    File pagFile = new File(di.DenseLayerPath + di.indexkey + "dense_" + i + ".class");
                    InputStream file2 = new FileInputStream(pagFile);
                    InputStream buffer2 = new BufferedInputStream(file2);
                    ObjectInput input2 = new ObjectInputStream(buffer2);
                    try {

                        Page p = (Page) input2.readObject();

                        ArrayList<Tuple> t = p.getTuples();

                        for (Tuple tt : t) {
                            if (tt != null)
                                System.err.println(tt.toString());
                        }

                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    input2.close();

                }
            }
        }

    }


}
