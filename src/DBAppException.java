public class DBAppException extends Exception {

    /**
     * Any errors related to the DB App can be detected using this exceptions
     */
    private static final long serialVersionUID = 1L;
    int x;

    public DBAppException(String string) {
        super(string);
    }

}
