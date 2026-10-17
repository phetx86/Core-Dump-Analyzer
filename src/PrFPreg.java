public class PrFPreg{
    public final int cwd;
    public final int swd;
    public final int twd;
    public final int mxcsr;
    public final byte[][] st_reg;
    public final byte[][] xmm_reg;

    public PrFPreg(int cwd,int swd,int twd,int mxcsr,
                   byte[][] st_reg ,byte[][] xmm_reg){
         this.cwd = cwd;
         this.swd = swd;
         this.twd = twd;
         this.mxcsr = mxcsr;
         this.st_reg = st_reg;
         this.xmm_reg = xmm_reg;
    }
}
