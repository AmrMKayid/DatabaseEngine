import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

class Configuration {

    Properties config;

    private int maximumSize;

    public Configuration() throws IOException {
        config = new Properties();
        FileInputStream inStream = null;

        inStream = new FileInputStream(new File("config/DBApp.config"));
        config.load(inStream);

        this.maximumSize = Integer.parseInt(config.getProperty("MaximumRowsCountinPage"));
    }

    public int getMaximumSize() {
        return maximumSize;
    }

    public void setMaximumSize(int maximumSize) {
        this.maximumSize = maximumSize;
    }
}
