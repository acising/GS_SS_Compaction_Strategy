package MarpleNu.CDMLinuxFrameworkMain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

class FileNode extends Node{
    String filePath;
    ArrayList<String> originFilePath = new ArrayList<>();
    ConcurrentHashMap<ProcessNode, HashSet<Operator>> visitedProcess  = new ConcurrentHashMap<>();
    FileNode(String filepath){
        filePath = filepath;
    }
    @Override
    public boolean equals(Object fileNode){
        if (fileNode == this) return true;
        if (!(fileNode instanceof LabelForCS)) {
            return false;
        }
        FileNode f = (FileNode) fileNode;
        return filePath.equals(f.filePath) && pcid.equals(f.pcid);
    }
    @Override
    public int hashCode() {
        return (filePath + pcid).hashCode();
    }

    public ConcurrentHashMap<ProcessNode, HashSet<Operator>> getVisitedProcess() {
        return visitedProcess;
    }
}
