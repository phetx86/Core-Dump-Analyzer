public class PrStatus{
    public final int pid;
    public final int ppid;
    public final long[] registers;

    public PrStatus(int pid, int ppid, long[] registers){
        this.pid = pid;
        this.ppid = ppid;
        this.registers = registers;
    }
}
