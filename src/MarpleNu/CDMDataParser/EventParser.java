package MarpleNu.CDMDataParser;

import java.io.*;

import MarpleNu.CDMDataClassifier.CDMDataClassifier;
import MarpleNu.CDMLinuxFrameworkMain.DetectionFramework;
import MarpleNu.CDMLinuxFrameworkMain.EventForCS;
import MarpleNu.FrameworkDataStruct.FrameworkFileInfo;
import MarpleNu.FrameworkDataStruct.FrameworkProcessInfo;
import MarpleNu.FrameworkSupportData.SupportData;
import com.bbn.tc.schema.avro.cdm19.*;
import com.bbn.tc.schema.avro.cdm19.UUID;
import org.apache.avro.JsonProperties;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class EventParser extends BaseParser {
    private SupportData supportData;
    private HashMap<UUID, String> stashFileMap = new HashMap<>();

    public EventParser(SupportData supportData) {
        this.supportData = supportData;
    }

    private ConcurrentHashMap<String, FrameworkProcessInfo> fileReadReduce = new ConcurrentHashMap<>();
    private static byte[] bt = {93, 64, -3, -5, 30, -124, 79, 22, -121, -12, 118, 106, -110, -14, 70, 15};
    private static UUID tmp = new UUID(bt);

    /*by wjy*/
    public static Map<String, EventLeft> MapEventLeft = new LinkedHashMap<String, EventLeft>();

    /*by wjy*/
    /*by yjk*/
    private static Map<UUID, UUID> clonelist = new HashMap<>();
    private static Map<Integer, Integer> threadlist = new HashMap<>();
    /*by yjk*/


    static long count_map = 1;
    static long tmp_sum = 0;

    int write_flag = 1;
    int path_flag = 1;
    int break_flag = 0;
    public static long time = 1;
    static long sum_read = 0;
    static long sum_read_left = 0;
    static long sum_write = 0;
    static long sum_write_left = 0;
    static long sum_net = 0;
    static long sum_net_left = 0;
    static long sum_fork = 0;
    static long sum_fork_left = 0;
    static long sum_clone = 0;
    static long sum_clone_left = 0;
    static long sum_rename = 0;
    static long sum_rename_left = 0;
    static long sum_image = 0;
    static long sum_image_left = 0;
    static long sum_path = 0;
    static long sum_path_left = 0;
    static long sum = 0;

    @Override
    public int parse(TCCDMDatum tccdmDatum) {
        Event record = (com.bbn.tc.schema.avro.cdm19.Event) tccdmDatum.getDatum();
        /*by yjk*/
        if (!(record.getType().equals(EventType.EVENT_CLONE) || record.getType().equals(EventType.EVENT_EXIT))) {
            record.setSubject(findSub(record.getSubject()));
            record.setThreadId(findThread(record.getThreadId()));
        }
        /*by yjk*/
        write_flag = 1;
        path_flag = 0;
        break_flag = 0;
        switch (record.getType()) {
            case EVENT_FORK:
                parseFork(record);
                break;
            case EVENT_CLONE:
                parseClone(record);
                break;
            case EVENT_EXECUTE:
                parseEventExecute(record);
                break; // ProcessStart
            case EVENT_EXIT:
                parseEventExit(record);
                break;  // ProcessEnd
            case EVENT_LOADLIBRARY:
                parseEventLoadLibrary(record);
                break; // ImageLoad
            case EVENT_SENDMSG:
                parseEventSendMsg(record);
                break; // Networkflow
            case EVENT_RECVMSG:
                parseEventRecvMsg(record);
                break; // Networkflow
            case EVENT_CONNECT:
                parseEventConnect(record);
                break; // ALPCALPC-Unwait, ALPCALPC-Wait-For-Reply, RegistryEnumerateKey, System call
            case EVENT_ACCEPT:
                parseEventAccept(record);
                break;
            case EVENT_WRITE:
                parseEventWrite(record);
                break; // DiskIoWrite
            case EVENT_READ:
                parseEventRead(record);
                break; // DiskIoWrite
            case EVENT_CREATE_OBJECT:
                parseCreateObject(record);
                break;
            case EVENT_RENAME:
                parseReName(record);
                break;
            case EVENT_MODIFY_FILE_ATTRIBUTES:
                parseModifyFileAttributes(record);
                break;
            case EVENT_UNLINK:
                parseUnlink(record);
                break;
            case EVENT_UPDATE:
                parseEventUpdate(record);
                break;
            case EVENT_OPEN:
                parseOpen(record);
                break;
            case EVENT_CLOSE:
                parseClose(record);
                break;
            /*by yjk*/
            case EVENT_MMAP:
                parseMmap(record);
                break;
            /*by yjk*/
            default:
                break;
        }
        judge_image(record);
        //一次重复性的优化
        //delete_map();
        return write_flag;
    }

    private void parseOpen(Event record) {
        sum_path++;
        write_flag = 0;
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();
        //judge_path(path1, path2);
        if (path_flag == 1) {
            write_flag = 1;
            sum_path_left++;
        }

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String flags = "";
        EventForCS eventForCS;
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (fileName != "") {
            stashFileMap.put(record.getPredicateObject(), fileName);
        }
        /*
        if (frameworkProcessInfo==null)
            return;
        if(record.getProperties()!=null) {
            for (CharSequence cs : record.getProperties().keySet()) {
                switch (cs.toString()) {
                    case "flags":
                        flags = record.getProperties().get(cs).toString();
                        break;
                }
            }
        }
        if (flags.contains("O_CLOEXEC")){
            eventForCS = new EventForCS(14,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    fileName,
                    ""
            );
        }
        else {
            eventForCS = new EventForCS(11,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    fileName,
                    ""
            );
        }
        supportData.test.addEvent(eventForCS);
        */

    }

    private void parseClose(Event record) {
        sum_path++;

        write_flag = 0;
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();
        judge_ss(path1, path2);
        //judge_path(path1, path2);
        if (write_flag == 1) {
            sum_path_left++;
        }

        if (record.getPredicateObject() != null) {
            stashFileMap.remove(record.getPredicateObject());
        }
        //FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        /*
        String fileName=record.getPredicateObjectPath()==null?"":record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo==null)
            return;
        EventForCS eventForCS = new EventForCS(12,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
        */

    }

    private void parseEventUpdate(Event record) {
    }

    private void parseFork(Event record) {
        sum_fork++;
        write_flag = 0;


        FrameworkProcessInfo frameworkProcessInfo = supportData.ppid2ProcessMap.get(record.getThreadId());
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(2,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                frameworkProcessInfo.getName() == null ? "" : frameworkProcessInfo.getName(),
                frameworkProcessInfo.getCmdline() == null ? "" : frameworkProcessInfo.getCmdline(),
                SupportData.byteArrayToHexString(frameworkProcessInfo.getUuid().bytes())
        );

        supportData.test.addEvent(eventForCS);
    }

    private void parseClone(Event record) {
        /*by yjk*/
        putThread(record);
        /*by yjk*/
        sum_clone++;
        sum_clone_left++;
        write_flag = 1;

        /*FrameworkProcessInfo frameworkProcessInfo = supportData.ppid2ProcessMap.get(record.getThreadId());
        String flags = "";
        EventForCS eventForCS;
        if (frameworkProcessInfo == null) return;
        if (record.getProperties() != null) {
            for (CharSequence cs : record.getProperties().keySet()) {
                switch (cs.toString()) {
                    case "flags":
                        flags = record.getProperties().get(cs).toString();
                        break;
                }
            }
        }
        if (flags.contains("CLONE_THREAD") && flags.contains("CLONE_FILES")) {
            FrameworkProcessInfo parentInfo = supportData.tid2ProcessMap.get(record.getThreadId());
            if (parentInfo == null) return;
            frameworkProcessInfo.setTgid(parentInfo.getTgid());
            frameworkProcessInfo.setPpid(parentInfo.getPpid());
        } else if (flags.contains("CLONE_FILES")) {
            eventForCS = new EventForCS(13,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    frameworkProcessInfo.getName() == null ? "" : frameworkProcessInfo.getName(),
                    frameworkProcessInfo.getCmdline() == null ? "" : frameworkProcessInfo.getCmdline(),
                    SupportData.byteArrayToHexString(frameworkProcessInfo.getUuid().bytes())
            );
            supportData.test.addEvent(eventForCS);
        } else {
            eventForCS = new EventForCS(2,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    frameworkProcessInfo.getName() == null ? "" : frameworkProcessInfo.getName(),
                    frameworkProcessInfo.getCmdline() == null ? "" : frameworkProcessInfo.getCmdline(),
                    SupportData.byteArrayToHexString(frameworkProcessInfo.getUuid().bytes())
            );
            supportData.test.addEvent(eventForCS);
        }*/
    }

    private void parseReName(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo == null) {
            write_flag = 1;
            return;
        }

        EventForCS eventForCS = new EventForCS(6,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);

        write_flag = 0;
        sum_rename++;
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();
        judge_ss(path1, path2);
        //judge_path(path1,path2);
        if (write_flag == 1) {
            sum_rename_left++;
        }
    }

    private void parseUnlink(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(5,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseModifyFileAttributes(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(8,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseCreateObject(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (fileName != "") {
            stashFileMap.put(record.getPredicateObject(), fileName);
        }
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(7,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                null
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventLoadLibrary(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();

        if (frameworkProcessInfo == null) {
            write_flag = 1;
            return;
        }
        EventForCS eventForCS;
        if (frameworkProcessInfo.getName() != null && fileName.contains(frameworkProcessInfo.getName())) {
            eventForCS = new EventForCS(10,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    fileName,
                    ""
            );
        } else {
            eventForCS = new EventForCS(4,
                    frameworkProcessInfo.getTgid(),
                    frameworkProcessInfo.getTgid(),
                    record.getTimestampNanos(),
                    frameworkProcessInfo.getPpid(),
                    fileName,
                    ""
            );
        }
        supportData.test.addEvent(eventForCS);

        sum_path++;
        write_flag = 0;
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();
        judge_ss(path1, path2);
        //judge_path(path1,path2);
        if (write_flag == 1) {
            sum_path_left++;
        }
    }

    private void parseEventExit(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(9,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                frameworkProcessInfo.getName() == null ? "" : frameworkProcessInfo.getName(),
                ""
        );
        supportData.test.addEvent(eventForCS);
        supportData.tid2ProcessMap.remove(record.getThreadId());
    }

    private void parseEventExecute(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        EventForCS eventForCS = new EventForCS(3,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                frameworkProcessInfo.getName() == null ? "" : frameworkProcessInfo.getName(),
                frameworkProcessInfo.getCmdline() == null ? "" : frameworkProcessInfo.getCmdline(),
                SupportData.byteArrayToHexString(frameworkProcessInfo.getUuid().bytes())
        );
        supportData.test.addEvent(eventForCS);
    }

    private void parseEventSendMsg(Event record) {

        sum_net++;

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(1,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );

        supportData.test.addEvent(eventForCS);


        String ObjectUUID = record.getPredicateObject() == null ? "" : record.getPredicateObject().toString();
        String ObjectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String ProcessUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String Time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();

        //遍历hashmap
        //第一层是找有没有与当前A、B、E相同的A'、B'、E'
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();
        //Iterator<Map.Entry<String, EventLeft>> iterator = set01.iterator();

        for (Map.Entry<String, EventLeft> entry01 : set01){
            //while(iterator.hasNext()){
                //Map.Entry<String, EventLeft> entry = iterator.next();
            EventLeft tmp01 = entry01.getValue();
                //EventLeft tmp01 = entry.getValue();
            if (tmp01.getSubjectID().equals(ProcessUUID) &&
                    tmp01.getObjectID().equals(ObjectUUID) &&
                    tmp01.getObjectID2().equals(ObjectUUID2) &&  //如果出错就删掉
                    tmp01.getEvent().equals("send") &&
                    (tmp01.getTime().compareTo("time") <= 0)) {
                write_flag = 0;
                //第二层是找有没有比A'、B'、E'时间更靠后的A''、B''、E''
                Set<Map.Entry<String, EventLeft>> set02 = MapEventLeft.entrySet();
                for (Map.Entry<String, EventLeft> entry02 : set02) {
                    EventLeft tmp02 = entry02.getValue();
                    if (tmp02.getObjectID().equals(ObjectUUID) &&
                            tmp02.getObjectID2().equals(ObjectUUID2) &&
                            tmp02.getEvent().equals("send") &&
                            (tmp02.getTime().compareTo(tmp01.getTime()) > 0)) {
                        tmp01 = tmp02;
                    }
                }
                //第三层使用最后时间的A''、B''、E''去找有没有read\rcev事件
                Set<Map.Entry<String, EventLeft>> set03 = MapEventLeft.entrySet();
                for (Map.Entry<String, EventLeft> entry03 : set03) {
                    EventLeft tmp03 = entry03.getValue();
                    if (tmp03.getSubjectID().equals(ProcessUUID) &&
                            tmp03.getEvent().equals("read") &&
                            (tmp03.getTime().compareTo(tmp01.getTime()) > 0)
                            ||
                            (tmp03.getSubjectID().equals(ProcessUUID) &&
                                    tmp03.getEvent().equals("recv") &&
                                    (tmp03.getTime().compareTo(tmp01.getTime()) >= 0))) {
                        judge_ss(path1, path2);
                        //write_flag = 1;
                        break_flag = 1;
                        break;
                    }
                }
            }
            if (break_flag == 1) {
                break;
            }
        }

        if (write_flag == 1) {
            sum_net_left++;
            add_To_Map("send", ProcessUUID, ObjectUUID, ObjectUUID2, Time);
        }
    }

    private void parseEventRecvMsg(Event record) {
        sum_net++;

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(0,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);

        String ObjectUUID = record.getPredicateObject() == null ? "" : record.getPredicateObject().toString();
        String ObjectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String ProcessUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String Time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();

        //寻找保留事件中时间值最大的recv，记录时间
        Set<Map.Entry<String, EventLeft>> set00 = MapEventLeft.entrySet();
        for (Map.Entry<String, EventLeft> entry00 : set00) {
            EventLeft tmp00 = entry00.getValue();
            if (tmp00.getSubjectID().equals(ProcessUUID) &&
                    tmp00.getObjectID().equals(ObjectUUID) &&
                    tmp00.getObjectID2().equals(ObjectUUID2) &&
                    tmp00.getEvent().equals("recv")) {
                long tmpTime = Long.parseLong(tmp00.getTime());
                if (tmpTime > time) {
                    time = tmpTime;
                }
            }
        }

        //读取文件F，对于文件中存在的IP地址，直接省略，不记录


        //遍历hashmap
        //第一层是找有没有与当前A、B、E相同的A'、B'、E'
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();
        for (Map.Entry<String, EventLeft> entry01 : set01) {
            EventLeft tmp01 = entry01.getValue();
            if (tmp01.getSubjectID().equals(ProcessUUID) &&
                    tmp01.getObjectID().equals(ObjectUUID) &&
                    tmp01.getObjectID2().equals(ObjectUUID2) &&
                    tmp01.getEvent().equals("recv") &&
                    //600的单位是妙
                    ((time - Long.parseLong(Time)) / 1000000 < 600)) {
                write_flag = 0;
            } else if (tmp01.getSubjectID().equals(ProcessUUID) &&
                    tmp01.getObjectID().equals(ObjectUUID) &&
                    tmp01.getObjectID2().equals(ObjectUUID2) &&
                    tmp01.getEvent().equals("recv") &&
                    ((time - Long.parseLong(Time)) / 1000000 >= 600)) {
                //judge_ss(path1, path2);
                write_flag = 1;
                break;
            }
        }

        if (write_flag == 1) {
            sum_net_left++;
            add_To_Map("recv", ProcessUUID, ObjectUUID, ObjectUUID2, Time);
            remove_redundancy_map1("recv", ProcessUUID);
        }
    }

    private void parseEventConnect(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(record.getPredicateObject());
        if (frameworkFileInfo == null || frameworkProcessInfo == null)
            return;
        if (frameworkFileInfo.getProperty() == FrameworkFileInfo.socket)
            frameworkProcessInfo.setNetworkConnect(true);
    }

    private void parseEventAccept(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(record.getPredicateObject());
        if (frameworkFileInfo == null || frameworkProcessInfo == null)
            return;
        if (frameworkFileInfo.getProperty() == FrameworkFileInfo.socket)
            frameworkProcessInfo.setNetworkConnect(true);
    }

    private void parseEventWrite(Event record) {

        sum_write++;


        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(record.getPredicateObject());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (fileName == "") {
            fileName = stashFileMap.get(record.getPredicateObject());
            if (fileName == null) fileName = "";
        }
        if (frameworkFileInfo != null || frameworkProcessInfo == null) {
            write_flag = 0;
            return;
        }
        fileReadReduce.remove(fileName);
        EventForCS eventForCS = new EventForCS(1,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);

        String ObjectUUID = record.getPredicateObject() == null ? "" : record.getPredicateObject().toString();
        String ObjectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String ProcessUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String Time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();

        //遍历hashmap
        //第一层是找有没有与当前A、B、E相同的A'、B'、E'
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();
        for (Map.Entry<String, EventLeft> entry01 : set01) {
            EventLeft tmp01 = entry01.getValue();
            if (tmp01.getSubjectID().equals(ProcessUUID) &&
                    tmp01.getObjectID().equals(ObjectUUID) &&
                    tmp01.getObjectID2().equals(ObjectUUID2) &&  //如果出错就删掉
                    tmp01.getEvent().equals("write") &&
                    (tmp01.getTime().compareTo("time") <= 0)) {
                write_flag = 0;
                //第二层是找有没有比A'、B'、E'时间更靠后的A''、B''、E''
                Set<Map.Entry<String, EventLeft>> set02 = MapEventLeft.entrySet();
                for (Map.Entry<String, EventLeft> entry02 : set02) {
                    EventLeft tmp02 = entry02.getValue();
                    if (tmp02.getObjectID().equals(ObjectUUID) &&
                            tmp02.getObjectID2().equals(ObjectUUID2) &&
                            tmp02.getEvent().equals("write") &&
                            (tmp02.getTime().compareTo(tmp01.getTime()) > 0)) {
                        tmp01 = tmp02;
                    }
                }
                //第三层使用最后时间的A''、B''、E''去找有没有read\rcev事件
                Set<Map.Entry<String, EventLeft>> set03 = MapEventLeft.entrySet();
                for (Map.Entry<String, EventLeft> entry03 : set03) {
                    EventLeft tmp03 = entry03.getValue();
                    if (tmp03.getSubjectID().equals(ProcessUUID) &&
                            tmp03.getEvent().equals("read") &&
                            (tmp03.getTime().compareTo(tmp01.getTime()) >= 0)
                            ||
                            (tmp03.getSubjectID().equals(ProcessUUID) &&
                                    tmp03.getEvent().equals("recv") &&
                                    (tmp03.getTime().compareTo(tmp01.getTime()) >= 0))) {
                        judge_ss(path1, path2);
                        //write_flag=1;
                        break_flag = 1;
                        break;
                    }
                }
            }
            if (break_flag == 1) {
                break;
            }
        }


        if (write_flag == 1) {
            sum_write_left++;
            add_To_Map("write", ProcessUUID, ObjectUUID, ObjectUUID2, Time);
            remove_redundancy_map2("write", ObjectUUID,ObjectUUID2);
        }

    }

    private void parseEventRead(Event record) {
        sum_read++;

        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (fileName == "") {
            fileName = stashFileMap.get(record.getPredicateObject());
            if (fileName == null) fileName = "";
        }
        if (frameworkProcessInfo == null) {
            write_flag = 0;
            return;
        }

        if (fileReadReduce.containsKey(fileName)) {
            if (fileReadReduce.get(fileName).equals(frameworkProcessInfo)) {
                write_flag = 0;
                return;
            } else
                fileReadReduce.replace(fileName, frameworkProcessInfo);
        } else {
            fileReadReduce.put(fileName, frameworkProcessInfo);
        }
        EventForCS eventForCS = new EventForCS(0,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );

        supportData.test.addEvent(eventForCS);

        String ObjectUUID = record.getPredicateObject() == null ? "" : record.getPredicateObject().toString();
        String ObjectUUID2 = record.getPredicateObject2() == null ? "" : record.getPredicateObject2().toString();
        String ProcessUUID = record.getSubject() == null ? "" : record.getSubject().toString();
        String Time = record.getTimestampNanos() == null ? "" : record.getTimestampNanos().toString();
        String Pid = record.getThreadId() == null ? "" : record.getThreadId().toString();
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();

        //判断语义是否改变
        //遍历hashmap
        //第一层是找有没有与当前A、B、E相同的A'、B'、E'
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();

        for (Map.Entry<String, EventLeft> entry01 : set01) {
            EventLeft tmp01 = entry01.getValue();
            if (tmp01.getSubjectID().equals(ProcessUUID) &&
                    tmp01.getObjectID().equals(ObjectUUID) &&
                    tmp01.getObjectID2().equals(ObjectUUID2) &&  //如果出错就删掉
                    tmp01.getEvent().equals("read") &&
                    (tmp01.getTime().compareTo("time") <= 0)) {
                write_flag = 0;
                //第二层是找有没有比A'、B'、E'时间更靠后的A''、B''、E''
                Set<Map.Entry<String, EventLeft>> set02 = MapEventLeft.entrySet();
                for (Map.Entry<String, EventLeft> entry02 : set02) {
                    EventLeft tmp02 = entry02.getValue();
                    if (tmp02.getObjectID().equals(ObjectUUID) &&

                            tmp02.getObjectID2().equals(ObjectUUID2) &&
                            tmp02.getEvent().equals("read") &&
                            (tmp02.getTime().compareTo(tmp01.getTime()) > 0)) {
                        tmp01 = tmp02;
                    }
                }
                //第三层使用最后时间的A''、B''、E''去找有没有write事件
                Set<Map.Entry<String, EventLeft>> set03 = MapEventLeft.entrySet();
                for (Map.Entry<String, EventLeft> entry03 : set03) {
                    EventLeft tmp03 = entry03.getValue();
                    if (tmp01.getObjectID().equals(tmp03.getObjectID()) &&
                            tmp01.getObjectID2().equals(tmp03.getObjectID2()) &&
                            tmp03.getEvent().equals("write") &&
                            (tmp01.getTime().compareTo(tmp03.getTime()) <= 0)) {
                        judge_ss(path1, path2);
                        //write_flag=1;
                        break_flag = 1;
                        break;
                    }
                }
            }
            if (break_flag == 1) {
                break;
            }
        }

        if (write_flag == 1) {
            sum_read_left++;
            add_To_Map("read", ProcessUUID, ObjectUUID, ObjectUUID2, Time);
            remove_redundancy_map1("read", ProcessUUID);
        }

    }


    private void judge_path(String path1, String path2) {
        //if ((path1.contains(result)) || path2.contains(result)) {
//        if (CDMDataClassifier.path_list.contains(path1) || CDMDataClassifier.path_list.contains(path2)) {
//            path_flag = 1;
//        }
    }


    //判断SS的压缩条件
    private void judge_ss(String path1, String path2) {
        judge_path(path1, path2);
        if ((path_flag == 1) || (DetectionFramework.ss_flag == 1)) {
            write_flag = 1;
        }
    }

    private void judge_image(Event record) {
        sum++;
        String path1 = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        String path2 = record.getPredicateObject2Path() == null ? "" : record.getPredicateObject2Path().toString();
        if (path1.contains(".so") || path2.contains(".so")) {
            sum_image++;
            if (write_flag == 1) {
                sum_image_left++;
            }
        }
        //System.out.println("sum_image:" + sum_image + "; sum_image_left:" + sum_image_left);
        System.out.println("sum_read:" + sum_read + "; sum_read_left:" + sum_read_left +
                "; sum_write:" + sum_write + "; sum_write_left:" + sum_write_left +
                "; sum_net:" + sum_net + "; sum_net_left:" + sum_net_left +
                "; sum_fork:" + sum_fork + "; sum_fork_left:" + sum_fork_left +
                "; sum_clone:" + sum_clone + "; sum_clone_left:" + sum_clone_left +
                "; sum_rename:" + sum_rename + "; sum_rename_left:" + sum_rename_left +
                "; sum_path:" + sum_path + "; sum_path_left:" + sum_path_left +
                "; sum_image:" + sum_image + "; sum_image_left:" + sum_image_left);

        /*long sum_thread = sum_clone+sum_fork;
        long sum_delete_rename_open_close = sum_rename + sum_path;
        long sum_thread_left = sum_clone+sum_fork_left;
        long sum_delete_rename_open_close_left = sum_rename_left + sum_path_left;
        String msg_sum = "w#" + CDMDataClassifier.s um_event + "*" +sum_read + "*" + sum_write + "*" + sum_net + "*" + sum_image + "*" + sum_thread + "*" + sum_delete_rename_open_close;
        String msg_left = CDMDataClassifier.sum_event_left + "*" + sum_read_left + "*" + sum_write_left  + "*" + sum_net_left + "*" + sum_image_left + "*" + sum_thread_left + "*" + sum_delete_rename_open_close_left;
        String msg_DR = (CDMDataClassifier.sum_event_left==0? "all":CDMDataClassifier.sum_event/CDMDataClassifier.sum_event_left)  + "*" + (sum_read_left==0?"all":sum_read/sum_read_left) + "*" + (sum_write_left==0?"all":sum_write/sum_write_left) + "*" +(sum_net_left==0?"all":sum_net/sum_net_left) + "*" + (sum_image_left==0?"all":sum_image/sum_image_left) +  "*" + (sum_thread_left==0?"all":sum_thread/sum_thread_left) + "*" + (sum_delete_rename_open_close_left==0?"all":sum_delete_rename_open_close/sum_delete_rename_open_close_left);
        String msg = msg_sum + "#" + msg_left + "#" + msg_DR;
        if(sum%100==0) {
            supportData.client.writeAndFlush(msg);
        }*/

    }

    /* wjy */
    private void add_To_Map(String Event, String SubjectID, String ObjectID, String ObjectID2, String Time) {
        count_map++;
        EventLeft e = new EventLeft();
        e.setEvent(Event);
        e.setSubjectID(SubjectID);
        e.setObjectID(ObjectID);
        e.setObjectID2(ObjectID2);
        e.setTime(Time);

        String s = String.valueOf(count_map);

        MapEventLeft.put(s, e);
    }



    private void remove_redundancy_map1(String event, String t) {
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();
        for (Map.Entry<String, EventLeft> entry01 : set01) {
            switch (event) {
                case "read":
                    if (entry01.getValue().getSubjectID().equals(t)) {
                        MapEventLeft.remove(entry01);
                    }
                case "recv":
                    if (entry01.getValue().getSubjectID().equals(t)) {
                        MapEventLeft.remove(entry01);
                    }
            }
        }
    }

    private void remove_redundancy_map2(String event, String t1,String t2) {
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();
        for (Map.Entry<String, EventLeft> entry01 : set01) {
            switch (event) {

                case "write":
                    if (entry01.getValue().getObjectID().equals(t1) || entry01.getValue().getObjectID2().equals(t2)) {
                        MapEventLeft.remove(entry01);
                    }
                case "send":
                    if (entry01.getValue().getObjectID().equals(t1) || entry01.getValue().getObjectID2().equals(t2)) {
                        MapEventLeft.remove(entry01);
                    }
            }
        }
    }

    private void delete_map() {
        EventLeft tmp = new EventLeft();
        String delete = "0";
        Set<Map.Entry<String, EventLeft>> set01 = MapEventLeft.entrySet();
        for (Map.Entry<String, EventLeft> entry01 : set01) {
            EventLeft tmp01 = entry01.getValue();
            if (tmp01.getSubjectID().equals(tmp.getSubjectID()) &&
                    tmp01.getObjectID().equals(tmp.getObjectID()) &&
                    tmp01.getObjectID2().equals(tmp.getObjectID2()) &&
                    tmp01.getEvent().equals(tmp.getEvent()) &&
                    (tmp01.getTime().compareTo(tmp.getTime()) > 0)) {
                if (tmp_sum == 0) {
                    tmp = tmp01;
                    delete = entry01.getKey();
                } else {
                    MapEventLeft.remove(delete);
                    tmp = tmp01;
                    delete = entry01.getKey();
                }
                tmp_sum++;
            }
        }
    }
    /* wjy */

    /*by yjk*/
    private void putThread(Event record) {
        UUID pSub = record.getSubject();
        UUID cObject = record.getPredicateObject();
        clonelist.put(cObject, pSub);

        FrameworkProcessInfo frameworkProcessInfo = supportData.ppid2ProcessMap.get(record.getThreadId());
        int cThread = frameworkProcessInfo.getTgid();
        int pThread = frameworkProcessInfo.getPpid();
        threadlist.put(cThread, pThread);
    }

    private UUID findSub(UUID thread) {
        UUID result = thread;
        while (thread != null) {
            result = thread;
            thread = clonelist.get(result);
        }
        return result;
    }

    private Integer findThread(Integer thread) {
        Integer result = thread;
        while (thread != null) {
            result = thread;
            thread = threadlist.get(result);
        }
        return result;
    }

    private void parseMmap(Event record) {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(record.getThreadId());
        String fileName = record.getPredicateObjectPath() == null ? "" : record.getPredicateObjectPath().toString();
        if (fileName == "") {
            fileName = stashFileMap.get(record.getPredicateObject());
            if (fileName == null) fileName = "";
        }
        if (frameworkProcessInfo == null)
            return;
        EventForCS eventForCS = new EventForCS(15,
                frameworkProcessInfo.getTgid(),
                frameworkProcessInfo.getTgid(),
                record.getTimestampNanos(),
                frameworkProcessInfo.getPpid(),
                fileName,
                ""
        );
        supportData.test.addEvent(eventForCS);
    }
    /*by yjk*/
}

