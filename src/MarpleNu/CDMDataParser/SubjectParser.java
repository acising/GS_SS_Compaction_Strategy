package MarpleNu.CDMDataParser;


import com.bbn.tc.schema.avro.cdm19.*;

import MarpleNu.FrameworkDataStruct.FrameworkProcessInfo;
import MarpleNu.FrameworkSupportData.SupportData;


import java.util.List;

public class SubjectParser extends BaseParser{
    private SupportData supportData;

    public SubjectParser(SupportData supportData)
    {
        this.supportData = supportData;
    }

    @Override
    public int parse(TCCDMDatum tccdmDatum)
    {
        Subject record = (Subject) tccdmDatum.getDatum();
        if (record.getType() == SubjectType.SUBJECT_PROCESS) {
            String cwd="", name="";
            int ppid=1;
            int pid=record.getCid();
            PrivilegeLevel pLevel = record.getPrivilegeLevel();
            List<CharSequence> libraries = record.getImportedLibraries();
            if(record.getProperties()!=null) {
                for (CharSequence cs : record.getProperties().keySet()) {
                    switch (cs.toString()) {
                        case "ppid":
                            ppid = Integer.parseInt(record.getProperties().get(cs).toString());
                            break;
                        case "name":
                            name = record.getProperties().get(cs).toString();
                            break;
                        case "cwd":
                            cwd = record.getProperties().get(cs).toString();
                            break;
                    }

                }
                FrameworkProcessInfo processInfo = new FrameworkProcessInfo(record.getUuid(), pid, ppid, cwd, name);
                processInfo.setPrivilegeLevel(pLevel);
                processInfo.setImportedLibraries(libraries);
                if (record.getCmdLine() != null)
                    processInfo.setCmdline(record.getCmdLine().toString());
                supportData.tid2ProcessMap.put(record.getCid(), processInfo);
                supportData.ppid2ProcessMap.put(ppid,processInfo);
                //supportData.uuid2ProcessMap.put(record.getUuid(),processInfo);
            }
        }
        return 1;
    }
}
