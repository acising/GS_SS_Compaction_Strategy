

import java.io.*;

import MarpleNu.CDMDataClassifier.CDMDataClassifier;
import MarpleNu.CDMDataReader.BaseDataReader;
import MarpleNu.CDMDataReader.OfflineAvroFileReaderSingle;
import MarpleNu.CDMDataReader.OnlineKafkaReader;
import MarpleNu.CDMDataReader.OnlineKafkaSSLReaderSingle;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;


public class compress_test {


    public static boolean write_flag=true;

    public static void main(String[] args) throws Exception {



        FrameworkConfig.init();
        BaseDataReader baseDataReader;

        //解析avro成json输入到baseDataReader
        baseDataReader = new OfflineAvroFileReaderSingle(args[1]);


        //把json传递到cdmDataClassifier进行分类操作
        CDMDataClassifier cdmDataClassifier = new CDMDataClassifier(baseDataReader);
        Thread mainThread = new Thread(cdmDataClassifier);
        mainThread.start();
    }
}
