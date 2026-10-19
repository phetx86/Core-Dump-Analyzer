public class PrStatus{
    public final int pid;
    public final int ppid;
    public final long[] registers;
    public static final String[] REGISTER_NAMES = {
    "R15", "R14", "R13", "R12",
    "RBP", "RBX", "R11", "R10",
    "R9", "R8", "RAX", "RCX",
    "RDX", "RSI", "RDI", "ORIG_RAX",
    "RIP", "CS", "RFLAGS", "RSP",
    "SS", "FS_BASE", "GS_BASE",
    "DS", "ES", "FS", "GS"
    };


    public PrStatus(int pid, int ppid, long[] registers){
        this.pid = pid;
        this.ppid = ppid;
        this.registers = registers;
    }
}
