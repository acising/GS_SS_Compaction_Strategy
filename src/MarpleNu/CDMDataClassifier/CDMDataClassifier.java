package MarpleNu.CDMDataClassifier;

import MarpleNu.CDMDataDump.AllDataDump;
import MarpleNu.CDMDataDump.DataDump;
import MarpleNu.CDMDataDump.E5DataDump;
import MarpleNu.CDMDataParser.*;
import MarpleNu.CDMDataReader.BaseDataReader;
import MarpleNu.FrameworkLable.FrameworkLable;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;

import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.avro.cdm19.UUID;
import com.bbn.tc.schema.serialization.AvroGenericDeserializer;
import org.apache.avro.generic.GenericContainer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


public class CDMDataClassifier implements Runnable {
    private BaseParser eventParser;
    private BaseParser subjectParser;
    private BaseParser objectParser;
    private BaseParser principalParser;
    private FrameworkLable frameworkLable;
    private BaseDataReader baseDataReader;
    private Thread dataReader;
    private SupportData supportData = new SupportData();
    private DataDump dataDump;
    private boolean stopflag = true;
    private boolean dumpflag = true;
    private boolean tyflag = false;
    private static long startTime;
    private static long endTime;
    private static int usedTime;
    private boolean detectFlag;
    private long sum = 0;
    private static int DeadNumber = 10;
    private long deadSum = 0;
    public static String topic = "";
    public static String hostid;
    int flag = 1;

    public static long sum_event = 0;
    public static long sum_event_left = 0;

    public static Map<com.bbn.tc.schema.avro.cdm19.UUID, com.bbn.tc.schema.avro.cdm19.UUID> FindThread = new HashMap<>();

    private AvroGenericDeserializer deserializer;


    public CDMDataClassifier(BaseDataReader baseDataReader) {
        this.baseDataReader = baseDataReader;
        //dataReader = new Thread(baseDataReader);
        //dataReader.start()；
        eventParser = new EventParser(supportData);
        subjectParser = new SubjectParser(supportData);
        objectParser = new ObjectParser(supportData);
        principalParser = new PrincipalParser(supportData);
        frameworkLable = new FrameworkLable(supportData);
    }
    //计时器
//    class Timertest extends TimerTask{
//
//        @Override
//        public void run() {
//            tyflag = true;
//            System.out.println("=====计时结束=====");
//
//        }
//    }

    //写入文件的函数
    public void write_to_file(String s) {
        try {
            File f1 = new File("src/SS_json420.out");
            if (f1.exists() == false) {
                f1.getParentFile().mkdirs();
            }
            String s1 = s + "\n";
            FileWriter writer = new FileWriter("src/SS_json420.out", true);
            writer.write(s1);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        Queue<GenericContainer> queue = new LinkedList<GenericContainer>();
        dumpflag = FrameworkConfig.Search("dump_data").equals("true");
        detectFlag = FrameworkConfig.Search("detector_switch") == null || FrameworkConfig.Search("detector_switch").equals("true");
        String str = FrameworkConfig.Search("dump_way");
        switch (str) {
            case "cmdline":
                try {
                    dataDump = new E5DataDump();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "allInOne":
                try {
                    dataDump = new AllDataDump();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                dataDump = new DataDump();
                break;
        }
        //计时器
//        Timer t = new Timer();
//        t.schedule(new Timertest(),60000);
//        t.schedule(new Timertest(),180000);
//        t.schedule(new Timertest(),300000);
        //t.schedule(new Timertest(),600000);
        //这部分是当时为了实现实时压缩写的测试代码，如果不需要可以直接删掉，采用GS版本代码中的本地处理部分
        ServerSocket server = null;
        startTime = System.currentTimeMillis();
        try {
            server = new ServerSocket(9091);

            try {
                boolean flag = true;
                int count = 1;
                while (true) {
                    Socket client = server.accept();
                    try {
                        BufferedReader input =
                                new BufferedReader(new InputStreamReader(client.getInputStream()));
                        while (flag) {
                            String line = input.readLine();
                            if (line == null) {
                                flag = false;
                            } else {
                                System.out.println("客户端要开始说话了，这是第" + count + "次！");
                                count++;
                                System.out.println(line);
                            }

//                            if (line == null) {
//                                flag = false;
//                            } else {
//                                count++;
//                                endTime = System.currentTimeMillis();
//                                usedTime = (int)(endTime-startTime)/1000;
//                                switch (usedTime) {
//                                    case 1 :
//                                        System.out.println("第" + count + "次！");
//                                    case 30 :
//                                        System.out.println("第" + count + "次！");
//                                    case 60 :
//                                        System.out.println("第" + count + "次！");
//                                    case 120 :
//                                        System.out.println("第" + count + "次！");
//                                    case 180 :
//                                        System.out.println("第" + count + "次！");
//                                    case 300 :
//                                        System.out.println("第" + count + "次！");
//                                }

                            //System.out.println(genericContainer.toString());
                            //收到line（String）转jsonObject
                            //JSONObject jsonObject =  JSON.parseObject(line,Feature.OrderedField);
                            //jsonObject转TCCDMDatum
                            //TCCDMDatum tccdmDatum = JSONObject.toJavaObject(jsonObject,TCCDMDatum.class);
                            //jsonObject转GenericContainer
                            //GenericContainer genericContainer = JSONObject.toJavaObject(jsonObject,GenericContainer.class);
                            //System.out.println(genericContainer.toString());
                            //System.out.println(genericContainer instanceof TCCDMDatum);
                        }
//                        }
                    } finally {
                        client.close();
                        flag = true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}



/*old
package MarpleNu.CDMDataClassifier;

import MarpleNu.CDMDataDump.AllDataDump;
import MarpleNu.CDMDataDump.DataDump;
import MarpleNu.CDMDataDump.E5DataDump;
import MarpleNu.CDMDataParser.*;
import MarpleNu.CDMDataReader.BaseDataReader;
import MarpleNu.FrameworkLable.FrameworkLable;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.LinuxFrameworkConfig.FrameworkConfig;
import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.avro.cdm19.UUID;

import java.util.*;
import java.text.SimpleDateFormat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class CDMDataClassifier implements Runnable {
    private BaseParser eventParser;
    private BaseParser subjectParser;
    private BaseParser objectParser;
    private BaseParser principalParser;
    private FrameworkLable frameworkLable;
    private BaseDataReader baseDataReader;
    private Thread dataReader;
    private SupportData supportData = new SupportData();
    private DataDump dataDump;
    private boolean stopflag = true;
    private boolean dumpflag = true;
    private boolean detectFlag;
    private long sum = 0;
    private static int DeadNumber = 10;
    private long deadSum = 0;
    public static String topic = "";
    public static String hostid;

    */
/* wjy *//*

    int flag = 1;
    public static long sum_event = 0;
    public static long sum_event_left = 0;
    public static String s1 = null;
    */
/* wjy *//*


    public static Map<com.bbn.tc.schema.avro.cdm19.UUID, com.bbn.tc.schema.avro.cdm19.UUID> FindThread = new HashMap<>();

    */
/* wjy *//*

    public static ArrayList<String> path_list = new ArrayList<String>();
    */
/* wjy *//*


    public CDMDataClassifier(BaseDataReader baseDataReader) {
        this.baseDataReader = baseDataReader;
        //dataReader = new Thread(baseDataReader);
        //dataReader.start()；
        eventParser = new EventParser(supportData);
        subjectParser = new SubjectParser(supportData);
        objectParser = new ObjectParser(supportData);
        principalParser = new PrincipalParser(supportData);
        frameworkLable = new FrameworkLable(supportData);
    }


    //读取path
    private void read_path() {
        //每一个Event事件中都有path
        try {
            File f1 = new File("src/path.txt");
            if (f1.exists() == false) {
                f1.getParentFile().mkdirs();
            }
            FileInputStream fileInputStream = new FileInputStream("src/path.txt");
            Scanner scanner = new Scanner(fileInputStream, "utf-8");
            while (scanner.hasNext()) {
                String result = scanner.nextLine();
                path_list.add(result);
            }
            fileInputStream.close();
            scanner.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //写入文件的函数
    public void write_to_file(String s) {
        try {
            File f1 = new File("src/GS_json420.out");
            if (f1.exists() == false) {
                f1.getParentFile().mkdirs();
            }
            String s1 = s + "\n";
            FileWriter writer = new FileWriter("src/GS_json420.out", true);
            writer.write(s1);
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        dumpflag = FrameworkConfig.Search("dump_data").equals("true");
        detectFlag = FrameworkConfig.Search("detector_switch") == null || FrameworkConfig.Search("detector_switch").equals("true");

        */
/* wjy *//*

        read_path();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        System.out.println(df.format(new Date()));// new Date()为获取当前系统时间
        */
/* wjy *//*


        String str = FrameworkConfig.Search("dump_way");
        switch (str) {
            case "cmdline":
                try {
                    dataDump = new E5DataDump();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "allInOne":
                try {
                    dataDump = new AllDataDump();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                dataDump = new DataDump();
                break;
        }
        */
/* wjy *//*

        try {
            File f1 = new File("src/GS_json420.out");
            if (f1.exists() == false) {
                f1.getParentFile().mkdirs();
            }
            FileWriter writer = new FileWriter("src/GS_json420.out", true);
        */
/* wjy *//*

            while (stopflag) {
                TCCDMDatum tccdmDatum;
                */
/* wjy *//*

                flag = 1;
                */
/* wjy *//*

                try {
                    tccdmDatum = baseDataReader.getData();
                } catch (Exception e) {
                    continue;
                }
                if (tccdmDatum == null)
                    continue;
                ++sum;
            */
/*if (sum % 100000 == 0) {
                System.out.printf("consumer %dk messages.\n", sum / 1000);
            }*//*
//wjy
                if (detectFlag) {
                    Object datum = tccdmDatum.getDatum();
                    String s_w = tccdmDatum.toString();
                    if (datum instanceof Event) {
                        //再这一层进行读写判定
                        sum_event++;
                        flag = eventParser.parse(tccdmDatum);
                        if (flag == 1) {
                            sum_event_left++;
                        }
                        System.out.println("sum_event:" + sum_event + "; sum_event_left:" + sum_event_left);
                    } else if (datum instanceof Subject) {
                        subjectParser.parse(tccdmDatum);
                    } else if (datum instanceof FileObject ||
                            datum instanceof RegistryKeyObject ||
                            datum instanceof NetFlowObject ||
                            datum instanceof SrcSinkObject ||
                            datum instanceof IpcObject) {
                        objectParser.parse(tccdmDatum);
                    } else if (datum instanceof Principal) {
                        principalParser.parse(tccdmDatum);
                    } else if (datum instanceof Host) {
                        Host host = (Host) tccdmDatum.getDatum();
                        hostid = SupportData.byteArrayToHexString(host.getUuid().bytes());
                    }

                    if (datum instanceof EndMarker) {
                        System.out.println("EndMarker");
                        SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                        System.out.println(df2.format(new Date()));// new Date()为获取当前系统时间
                        break;
                    }
                    */
/* wjy *//*

                    if (flag == 1) {
                        s1 = s_w + "\n";
                        writer.write(s1);
                    }
                    */
/* wjy *//*

                }
                if (dumpflag) {
                    try {
                        dataDump.dump(tccdmDatum);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                //System.out.println(tccdmDatum.getDatum().toString());
                frameworkLable.label(tccdmDatum);
            }
            */
/* wjy *//*

            writer.close();
            */
/* wjy *//*

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //修改subject
    private TCCDMDatum ChangeDatum(TCCDMDatum tccdmDatum) {

        TCCDMDatum tmptccdmDatum = null;
        Event record = (com.bbn.tc.schema.avro.cdm19.Event) tccdmDatum.getDatum();
        if (record.getType().equals("EVENT_FORK")) {
            PutThread(record);
        }
        com.bbn.tc.schema.avro.cdm19.UUID PThread = record.getSubject();
        com.bbn.tc.schema.avro.cdm19.UUID NewThread = Find_Thread(PThread);
        record.setSubject(NewThread);
        tmptccdmDatum.setDatum(record);
        return tmptccdmDatum;
    }

    //当发生fork操作时，把父与子放入hashmap中
    private void PutThread(Event record) {
        com.bbn.tc.schema.avro.cdm19.UUID PThread = record.getSubject();
        com.bbn.tc.schema.avro.cdm19.UUID CThread = record.getPredicateObject();
        FindThread.put(CThread, PThread);
    }

    //利用递归的方法去找到最开始的原始进程
    private com.bbn.tc.schema.avro.cdm19.UUID Find_Thread(com.bbn.tc.schema.avro.cdm19.UUID Thread) {
        com.bbn.tc.schema.avro.cdm19.UUID tmpThread = FindThread.get(Thread);
        com.bbn.tc.schema.avro.cdm19.UUID tmp = null;
        while (tmpThread != null) {
            tmp = tmpThread;
            tmpThread = FindThread.get(tmp);
        }
        return tmp;
    }
}
*/
