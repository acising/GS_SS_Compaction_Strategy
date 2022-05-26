package MarpleNu.FrameworkLable;

import MarpleNu.CDMLinuxFrameworkMain.LabelForCS;
import MarpleNu.FrameworkDataStruct.FrameworkFileInfo;
import MarpleNu.FrameworkDataStruct.FrameworkProcessInfo;
import MarpleNu.FrameworkSupportData.SupportData;
import MarpleNu.FrameworkTools.CDMDataTools;
import com.bbn.tc.schema.avro.cdm19.*;

import edu.umd.cs.findbugs.annotations.NonNull;

public class FrameworkLable {
    //file
    private static final String SensitiveFileLable = "FT2";
    private static final String SensitiveCommand = "PT6";
    /* by yjk*/
    private static final String UploadFile = "FT99";
    private static final String ApacheSensitiveCommand = "PT95";
    /* by yjk*/
    private static final String Deceptionlabel = "Deception";
    private static final String DeleteLoglabel = "DeleteLog";
    private static final String DownloadFileLable = "FT1";
    private static final String DownloadFileAndExecuteLable = "DownloadFileAndExecute";
    private static final String FileInfectionReplacementLabel = "FileInfectionReplacement";
    private static final String SubsystemsInitializationLable = "SubsystemsInitialization";
    private static final String TimeBasedExecutionLabel = "TimeBasedExecution";
    private static final String Top10ProcFileSystemAccessesLabel = "Top10ProcFileSystemAccesses";
    private static final String Top10EtcFileSystemAccessesLabel = "Top10EtcFileSystemAccesses";
    private static final String Top10SysFileSystemAccessLabel = "Top10SysFileSystemAccess";
    //process
    private static final String NetworkConnectionLabel = "PT1";
    private SupportData supportData;
    public FrameworkLable(SupportData supportData)
    {
        this.supportData = supportData;
    }


    private void label(UUID uuid,long time, String fileName)
    {
        if (FrameworkFileInfo.checkFileSensitive(fileName)){
            LabelForCS labelForCS = new LabelForCS(
                    fileName,
                    -1,
                    SensitiveFileLable,
                    time,
                    uuid.hashCode()
            );
            supportData.test.addLabel(labelForCS);
        }
        /*by yjk*/
        if (FrameworkFileInfo.isUploadDirectory(fileName)){
            LabelForCS labelForCS = new LabelForCS(
                    fileName,
                    -1,
                    UploadFile,
                    time,
                    uuid.hashCode()
            );
            supportData.test.addLabel(labelForCS);
        }
        /*by yjk*/
    }

    private void processLabel(TCCDMDatum tccdmDatum)
    {
        Subject subject = (Subject)tccdmDatum.getDatum();
        if (subject.getType() != SubjectType.SUBJECT_PROCESS)
            return;
        FrameworkProcessInfo processInfo = supportData.tid2ProcessMap.get(subject.getCid());
        if (processInfo.getName().equals("scp")||
                processInfo.getName().equals("wget")||processInfo.getName().equals("httpd")){
            processInfo.setNetworkConnect(true);
            LabelForCS labelForCS = new LabelForCS(
                    processInfo.getCmdline(),
                    processInfo.getTgid(),
                    NetworkConnectionLabel,
                    subject.getStartTimestampNanos(),
                    processInfo.getTgid()
            );
            //System.out.println(labelForCS.toString());
            supportData.test.addLabel(labelForCS);
        }
        if (processInfo.getCmdline().contains("chmod") ||
                processInfo.getCmdline().contains("chown") ||
                processInfo.getCmdline().contains("tcpdump") ||
                processInfo.getCmdline().contains("ifconfig")||
                processInfo.getCmdline().contains("sudo") ||
                processInfo.getCmdline().contains("whoami") ||
                processInfo.getCmdline().contains("netstate") ||
                processInfo.getCmdline().contains("insmod")||
                processInfo.getCmdline().startsWith("sh ")||
                processInfo.getCmdline().startsWith("/bin/sh")
        ) {
            LabelForCS labelForCS = new LabelForCS(
                    processInfo.getCmdline(),
                    processInfo.getTgid(),
                    SensitiveCommand,
                    subject.getStartTimestampNanos(),
                    processInfo.getTgid()
            );
            supportData.test.addLabel(labelForCS);
        }

    }
    private static long preTime = 0;
    public void label(TCCDMDatum tccdmDatum)
    {

        if (tccdmDatum.getType().equals(RecordType.RECORD_SUBJECT)){
            processLabel(tccdmDatum);
            return;
        }
        if (tccdmDatum.getType().equals(RecordType.RECORD_FILE_OBJECT)) {
            FileObject fileObject = (FileObject)tccdmDatum.getDatum();
            label(fileObject.getUuid(),preTime, CDMDataTools.getPropertiesValue(fileObject.getBaseObject(),"path"));
        }
        if (tccdmDatum.getType()!=RecordType.RECORD_EVENT)
            return;
        Event record = (Event)tccdmDatum.getDatum();
        preTime = record.getTimestampNanos();
        switch (record.getType()){
            case EVENT_LOADLIBRARY:
                //FileLabel(record,SensitiveFileLable);
                //FileLabel(record,DownloadFileAndExecuteLable);
                break;
            case EVENT_READ:
                //FileLabel(record,SensitiveFileLable);
                //FileLabel(record,Top10EtcFileSystemAccessesLabel);
                //FileLabel(record,Top10ProcFileSystemAccessesLabel);
                //FileLabel(record,Top10SysFileSystemAccessLabel);
                NetLabel(record,NetworkConnectionLabel);
                break;
            case EVENT_WRITE:
                //FileLabel(record,SensitiveFileLable);
                //FileLabel(record,Deceptionlabel);
                //FileLabel(record,DownloadFileLable);
                //FileLabel(record,FileInfectionReplacementLabel);
                //FileLabel(record,SubsystemsInitializationLable);
                //FileLabel(record,TimeBasedExecutionLabel);
                //FileLabel(record,Top10EtcFileSystemAccessesLabel);
                //FileLabel(record,Top10ProcFileSystemAccessesLabel);
                //FileLabel(record,Top10SysFileSystemAccessLabel);
                break;
            case EVENT_OPEN:
                NetLabel(record,NetworkConnectionLabel);
                break;
            case EVENT_CREATE_OBJECT:
                NetLabel(record,NetworkConnectionLabel);
                break;
            case EVENT_UNLINK:
                //FileLabel(record,DeleteLoglabel);
                break;
            case EVENT_EXECUTE:
                ProcessLabel(record.getThreadId(),"PT97");
                break;
            case EVENT_FORK:
                //ProcessLabel(record.getThreadId(),"PT96");
                break;
                default:
                    break;
        }
    }

    private void ProcessLabel(Integer subject, @NonNull String labelName)
    {
        FrameworkProcessInfo frameworkProcessInfo = supportData.tid2ProcessMap.get(subject);
        boolean flag = false;
        switch (labelName) {
            case "PT97":flag=true;break;
            case NetworkConnectionLabel:flag=frameworkProcessInfo.isNetworkConnect();break;
            default:break;
        }

        if (flag) {
            LabelForCS labelForCS = new LabelForCS(
                    frameworkProcessInfo.getCmdline(),
                    frameworkProcessInfo.getTgid(),
                    labelName,
                    SupportData.date.getTime(),
                    frameworkProcessInfo.getTgid()
            );
            supportData.test.addLabel(labelForCS);
        }

    }

    private void FileLabel(Event record,String labelName) {
        /*
        UUID uuid = record.getPredicateObject();
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(uuid);
        if (frameworkFileInfo==null || frameworkFileInfo.getProperty() != FrameworkFileInfo.file)
            return;

        boolean flag=false;
        switch (labelName) {
            case Deceptionlabel:flag=frameworkFileInfo.getDeception();break;
            case DeleteLoglabel:flag=frameworkFileInfo.getLog();break;
            case DownloadFileLable:
                UUID puuid = record.getSubject();
                if (supportData.uuid2ProcessMap.get(puuid)!=null)
                    flag = supportData.uuid2ProcessMap.get(puuid).isNetworkConnect();
                break;
            case DownloadFileAndExecuteLable: flag=frameworkFileInfo.getDownlaodFile();break;
            case FileInfectionReplacementLabel:flag=frameworkFileInfo.getFileInfectionAndReplacement();break;
            case SensitiveFileLable:flag=frameworkFileInfo.getSenstiveFile();break;
            case SubsystemsInitializationLable:flag=frameworkFileInfo.getRCScript();break;
            case TimeBasedExecutionLabel:flag=frameworkFileInfo.getTimeBaseExecution();break;
            case Top10ProcFileSystemAccessesLabel:flag=frameworkFileInfo.getTop10ProcFileSystemAccesses();break;
            case Top10EtcFileSystemAccessesLabel:flag=frameworkFileInfo.getTop10SEtcFileSystemAccesses();break;
            case Top10SysFileSystemAccessLabel:flag=frameworkFileInfo.getTop10SSysFileSystemAccesses();break;
            default:break;
        }
        if (flag) {
            LabelForCS labelForCS = new LabelForCS(
                    record.getPredicateObjectPath().toString(),
                    -1,
                    labelName,
                    record.getTimestampNanos(),
                    record.getUuid().hashCode()
                    );
            supportData.test.addLabel(labelForCS);
        }
        */
    }

    private void NetLabel(Event record,String labelName) {
        UUID uuid = record.getPredicateObject();
        FrameworkFileInfo frameworkFileInfo = supportData.uuid2FileMap.get(uuid);
        int tid = record.getThreadId();
        FrameworkProcessInfo processInfo = supportData.tid2ProcessMap.get(tid);
        if (frameworkFileInfo == null)
            return;
        if (frameworkFileInfo.getProperty()!=FrameworkFileInfo.socket)
            return;
        boolean flag=false;
        switch (labelName) {
            case NetworkConnectionLabel:
                if(processInfo!=null && !processInfo.isNetworkConnect()){
                    processInfo.setNetworkConnect(true);
                    flag=true;
                }
                break;
        }
        if (flag) {
            LabelForCS labelForCS = new LabelForCS(
                    processInfo.getCmdline(),
                    record.getThreadId(),
                    labelName,
                    record.getTimestampNanos(),
                    record.getThreadId()
            );
            supportData.test.addLabel(labelForCS);
        }
    }
}
