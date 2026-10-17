public class SigInfo{
    public final int signo;
    public final int errno;
    public final int code;
    public final long faultAddress;

    public SigInfo(int signo, int errno, int code, long faultAddress){
        this.signo = signo;
        this.errno = errno;
        this.code = code;
        this.faultAddress = faultAddress;
    }

}
