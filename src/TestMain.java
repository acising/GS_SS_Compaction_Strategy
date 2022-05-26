import MarpleNu.CDMDataClassifier.CDMDataClassifier;
import MarpleNu.CDMDataReader.*;
import MarpleNu.CDMLinuxFrameworkMain.DetectionFramework;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import java.util.Scanner;
import java.util.UUID;
import java.io.IOException;
import java.util.HashMap;



public class TestMain {

    public static void main(String[] args) throws IOException {
        FrameworkConfig.init();
        BaseDataReader baseDataReader;
        switch (args[0]) {
            case "online":
                baseDataReader = new OnlineKafkaSSLReaderSingle();
                break;
            case "offline":
                baseDataReader = new OfflineAvroFileReaderSingle(args[1]);
                break;
            default:
                baseDataReader = new OnlineKafkaReader();
                break;
        }
        CDMDataClassifier cdmDataClassifier = new CDMDataClassifier(baseDataReader);
        Thread mainThread = new Thread(cdmDataClassifier);
        mainThread.start();
    }
}