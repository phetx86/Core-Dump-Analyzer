public class CoreDumpData{
    public final PrStatus prStatus;
    public final SigInfo sigInfo;
    public final NTFileInfo ntFileInfo;
    public final PrFPreg prFPreg; 


    public CoreDumpData(PrStatus prStatus,
                        SigInfo sigInfo,
                        NTFileInfo ntFileInfo,
                        PrFPreg prFPreg){
        this.prStatus = prStatus;
        this.sigInfo = sigInfo;
        this.ntFileInfo = ntFileInfo;
        this.prFPreg = prFPreg;
    }

    public String code_detail(){
        return switch(sigInfo.signo){
            case 31 -> analysis_SigSYS(sigInfo.code);
            case 29 -> analysis_SigPOLL(sigInfo.code);
            case 17 -> analysis_SigCHLD(sigInfo.code);
            case 11 -> analysis_SigSEGV(sigInfo.code);
            case 8 -> analysis_SigFPE(sigInfo.code);
            case 7 -> analysis_SigBUS(sigInfo.code);
            case 5 -> analysis_SigTRAP(sigInfo.code);
            case 4 -> analysis_SigILL(sigInfo.code);
            default -> "UnSupported Signal";
        };
    }
    private String analysis_SigILL(int code){
        return switch(code){
            case 11 -> "ILL_BNDMOD(11): bundle-update (modification) in progress";
            case 10 -> "ILL_BREAK(10): illegal break";
            case 9 -> "ILL_BADIADDR(9): unimplemented instruction address";
            case 8 -> "ILL_BADSTK(8): internal stack error";
            case 7 -> "ILL_COPROC(7): coprocessor error";
            case 6 -> "ILL_PRVREG(6): privileged register";
            case 5 -> "ILL_PRVOPC(5): privileged opcode";
            case 4 -> "ILL_ILLTRP(4): illegal trap";
            case 3 -> "ILL_ILLADR(3): illegal addressing mode";
            case 2 -> "ILL_ILLOPN(2): illegal operand";
            case 1 -> "ILL_ILLOPC(1): illegal opcode";
            default -> "UnSupported SigILL code";
        };
    }
    private String analysis_SigTRAP(int code){
        return switch(code){
            case 6 -> "TRAP_PERF(6): perf event with sigtrap = 1";
            case 5 -> "TRAP_UNK(5): undiagnosed trap";
            case 4 -> "TRAP_HWBKPT(4): hardware breakpoint/watchpoint";
            case 3 -> "TRAP_BRANCH(3): process taken branch trap";
            case 2 -> "TRAP_TRACE(2): process trace trap";
            case 1 -> "TRAP_BRKPT(1): process breakpoint";
            default -> "UnSupported SigTRAP code";
        };
    }
    private String analysis_SigBUS(int code){
        return switch(code){
            case 5 -> "BUS_MCEERR_AO(5): hardware memory error detected in process but not consumed: action optional";
            case 4 -> "BUS_MCEERR_AR(4): hardware memory error consumed on a machine check: action required";
            case 3 -> "BUS_OBJERR(3): object specific hardware error";
            case 2 -> "BUS_ADRERR(2): non-existent physical address";
            case 1 -> "BUS_ADRALN(1): invalid address alignment";
            default -> "UnSupported sigBUS code";
        };
    }
    private String analysis_SigFPE(int code){
        return switch(code){
            case 15 -> "FPE_CONDTRAP(15): trap on condition";
            case 14 -> "FPE_FLTUNK(14): undiagnosed floating-point exception";
            case 13 -> "FPE_INVDEC(13): invalid decimal digit";
            case 12 -> "FPE_INVASC(12): invalid ASCII digit";
            case 11 -> "FPE_DECERR(11): packed decimal error";
            case 10 -> "FPE_DECDIV(10): decimal division by zero";
            case 9 ->  "FPE_DECOVF(9): decimal overflow";
            case 8 ->  "FPE_FLTSUB(8): subscript out of range";
            case 7 ->  "FPE_FLTINV(7): floating point invalid operation";
            case 6 ->  "FPE_FLTRES(6): floating point inexact result";
            case 5 ->  "FPE_FLTUND(5): floating point underflow";
            case 4 ->  "FPE_FLTOVF(4): floating point overflow";
            case 3 ->  "FPE_FLTDIV(3): floating point divide by zero";
            case 2 ->  "FPE_INTOVF(2): integer overflow";
            case 1 ->  "FPE_INTDIV(1): integer divide by zero";
            default -> "UnSupported sigFPE code";

        };
    }
    private String analysis_SigCHLD(int code){
        return switch(code){
            case 6 -> "CLD_CONTINUED(6): stopped child has continued";
            case 5 -> "CLD_STOPPED(5): child has stopped";
            case 4 -> "CLD_TRAPPED(4): traced child has trapped";
            case 3 -> "CLD_DUMPED(3): child terminated abnormally";
            case 2 -> "CLD_KILLED(2): child was killed";
            case 1 -> "CLD_EXITED(1): child has exited";
            default -> "UnSupported SigCHLD code";
        };
    }
    private String analysis_SigSYS(int code){
        return switch(code){
            case 2 -> "SYS_USER_DISPATCH(2): syscall user dispatch triggered";
            case 1 -> "SYS_SECCOMP(1): seccomp triggered";
            default -> "UnSupported SigSYS code";
        };
    }
    private String analysis_SigPOLL(int code){
        return switch(code){
            case 6 -> "POLL_HUP(6): device disconnected";
            case 5 -> "POLL_PRI(5): high priority input available";
            case 4 -> "POLL_ERR(4): i/o error";
            case 3 -> "POLL_MSG(3): input message available";
            case 2 -> "POLL_OUT(2): output buffers available";
            case 1 -> "POLL_IN(1): data input available";
            default -> "UnSupported SigPOLL code";
        };
    }
    private String analysis_SigSEGV(int code){
        return switch(code){
            case 10 -> "SEGV_CPERR(10): Control protection fault";
            case 9 -> "SEGV_MTESERR(9): Synchronous ARM MTE exception";
            case 8 -> "SEGV_MTEAERR(8): Asynchronous ARM MTE error";
            case 7 -> "SEGV_ADIPERR(7): Precise MCD exception";
            case 6 -> "SEGV_ADIDERR(6): Disrupting MCD error";
            case 5 -> "SEGV_ACCADI(5): ADI not enabled for mapped object";
            case 4 -> "SEGV_PKUERR(4): failed protection key checks";
            case 3 -> "SEGV_BNDERR(3): failed address bound checks";
            case 2 -> "SEGV_ACCERR(2): invalid permissions for mapped object";
            case 1 -> "SEGV_MAPERR(1): address not mapped to object";
            default -> "UnSupported SIGSEGV code";
        };
    }
    public String get_name_signal(){
        return switch(sigInfo.signo){
            case 31 -> "SigSYS";
            case 29 -> "SigPOLL";
            case 17 -> "SigCHLD";
            case 11 -> "SigSEGV";
            case 8 -> "SigFPE";
            case 7 -> "SigBUS";
            case 5 -> "SigTRAP";
            case 4 -> "SigILL";
            default -> "UnSupported Signal";
        };
    }
}
