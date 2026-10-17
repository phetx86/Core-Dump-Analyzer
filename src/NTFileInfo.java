public class NTFileInfo{
    public final int count;
    public final long pageSize;
    public final long[] start;
    public final long[] end;
    public final long[] fileOffset;
    public final String[] fileName;

    public NTFileInfo(int count, long pageSize,
                      long[] start, long[] end,
                      long[] fileOffset, String[] fileName){
        this.count = count;
        this.pageSize = pageSize;
        this.start = start;
        this.end = end;
        this.fileOffset = fileOffset;
        this.fileName = fileName;
    }
}
