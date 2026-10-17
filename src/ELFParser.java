import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class ELFParser{
    private static final int PT_NOTE = 4;
    private static final int NT_PRSTATUS = 1;
    private static final int NT_PRFPREG = 2;
    private static final int NT_SIGINFO = 0x53494749;
    private static final int NT_FILE = 0x46494c45;

    private static final String[] REG_NAMES = {
        "r15", "r14", "r13", "r12", "rbp", "rbx",
        "r11", "r10", "r9", "r8", "rax", "rcx",
        "rdx", "rsi", "rdi", "orig_rax", "rip", "cs",
        "rflags", "rsp", "ss", "fs_base", "gs_base",
        "ds", "es", "fs", "gs"
    };

    private final RandomAccessFile core;

    public ELFParser(String path) throws IOException{
        core = new RandomAccessFile(path, "r");
    }

    CoreDumpData parse() throws IOException{
        try {
            byte[] header = new byte[64];
            core.seek(0);
            core.readFully(header);

            if(header[0] != 0x7f
                    || header[1] != 'E'
                    || header[2] != 'L'
                    || header[3] != 'F'){
                throw new IllegalArgumentException("Not an ELF file");
            }

            if((header[4] & 0xff) != 2
                    || (header[5] & 0xff) != 1){
                throw new IllegalArgumentException(
                        "Only ELF64 little-endian is supported");
            }

            long phoff = readOffAddr(header, 32);
            int phentsize = readHalf(header, 54);
            int phnum = readHalf(header, 56);

            byte[] prstatusDesc = null;
            byte[] siginfoDesc = null;
            byte[] fileDesc = null;
            byte[] prfpregDesc = null;

            for(int i = 0; i < phnum; i++){
                long phdr = phoff + (long) i * phentsize;
                core.seek(phdr);

                int pType = readWord();

                if(pType != PT_NOTE){
                    continue;
                }

                core.seek(phdr + 8);
                long pOffset = readOffAddr();

                core.seek(phdr + 32);
                long pFileSize = readOffAddr();

                long end = pOffset + pFileSize;
                core.seek(pOffset);

                while(core.getFilePointer() + 12 <= end){
                    int nameSize = readWord();
                    int descSize = readWord();
                    int type = readWord();

                    if(nameSize < 0 || descSize < 0){
                        throw new IOException("Invalid ELF note size");
                    }

                    long nextName = core.getFilePointer()
                            + align4(nameSize);

                    if(nextName > end){
                        throw new IOException("Invalid ELF note name");
                    }

                    core.seek(nextName);

                    long nextDesc = core.getFilePointer()
                            + (long) descSize;

                    if(nextDesc > end){
                        throw new IOException("Invalid ELF note descriptor");
                    }

                    byte[] desc = new byte[descSize];
                    core.readFully(desc);

                    if(type == NT_PRSTATUS && prstatusDesc == null){
                        prstatusDesc = desc;
                    }else if(type == NT_PRFPREG && prfpregDesc == null){
                        prfpregDesc = desc;
                    }else if(type == NT_SIGINFO){
                        siginfoDesc = desc;
                    }else if(type == NT_FILE){
                        fileDesc = desc;
                    }

                    core.seek(nextDesc + align4(descSize) - descSize);
                }
            }

            if(prstatusDesc == null
               || siginfoDesc == null
               || fileDesc == null
               || prfpregDesc == null){
                throw new IOException("Required ELF notes are missing");
            }

            PrStatus prStatus = parsePrStatus(prstatusDesc);
            SigInfo sigInfo = parseSigInfo(siginfoDesc);
            NTFileInfo ntFileInfo = parseNTFile(fileDesc);
            
            PrFPreg prFPreg = null;
            if(prfpregDesc != null){
                prFPreg = parsePrFPreg(prfpregDesc);
            }

            return new CoreDumpData(prStatus,sigInfo,ntFileInfo,prFPreg);
        }finally{
            core.close();
        }
    }

    private PrStatus parsePrStatus(byte[] desc) throws IOException{
        if(desc.length < 112 + REG_NAMES.length * 8){
            throw new IOException("PRSTATUS descriptor is too short");
        }

        int pid = readWord(desc, 32);
        int ppid = readWord(desc, 36);

        long[] registers = new long[REG_NAMES.length];

        for(int i = 0; i < registers.length; i++){
            registers[i] = readOffAddr(desc, 112 + i * 8);
        }

        return new PrStatus(pid, ppid, registers);
    }
    private PrFPreg parsePrFPreg(byte[] desc) throws IOException{
        if(desc.length < 512){
            throw new IOException("PrfPreg descriptor is too short");
        }
        
        int cwd = readHalf(desc,0);
        int swd =readHalf(desc,2);
        int twd = desc[4] & 0xFF;
        int mxcsr = readWord(desc,24);
        byte[][] st_reg = new byte[8][];
        byte[][] xmm_reg = new byte[16][];

        for(int i = 0;i < st_reg.length;i++){
            st_reg[i] = Arrays.copyOfRange(desc,32 + i * 16,32 + i * 16 + 10);
        }

        for(int i = 0;i < xmm_reg.length;i++){
            xmm_reg[i] = Arrays.copyOfRange(desc,160 + i * 16,160 + i * 16 + 16);
        }
        

        return new PrFPreg(cwd,swd,twd,mxcsr,st_reg,xmm_reg);
    }

    private SigInfo parseSigInfo(byte[] desc) throws IOException{
        if(desc.length < 24){
            throw new IOException("SIGINFO descriptor is too short");
        }

        return new SigInfo(
                readWord(desc, 0),
                readWord(desc, 4),
                readWord(desc, 8),
                readOffAddr(desc, 16)
        );
    }

    private NTFileInfo parseNTFile(byte[] desc) throws IOException{
        if(desc.length < 16){
            throw new IOException("NT_FILE descriptor is too short");
        }

        long countLong = readOffAddr(desc, 0);
        long pageSize = readOffAddr(desc, 8);

        if(countLong < 0 || countLong > Integer.MAX_VALUE){
            throw new IOException("Invalid NT_FILE mapping count");
        }

        int count = (int) countLong;
        long tableEnd = 16L + (long) count * 24;

        if(tableEnd > desc.length){
            throw new IOException("NT_FILE mapping table is truncated");
        }

        long[] start = new long[count];
        long[] end = new long[count];
        long[] fileOffset = new long[count];
        String[] fileName = new String[count];

        for(int i = 0; i < count; i++){
            int offset = 16 + i * 24;

            start[i] = readOffAddr(desc, offset);
            end[i] = readOffAddr(desc, offset + 8);
            fileOffset[i] = readOffAddr(desc, offset + 16);
        }

        int position = (int) tableEnd;

        for(int i = 0; i < count; i++){
            int nameEnd = position;

            while(nameEnd < desc.length && desc[nameEnd] != 0){
                nameEnd++;
            }

            if(nameEnd == desc.length){
                throw new IOException("NT_FILE filename is unterminated");
            }

            fileName[i] = new String(
                    desc, position, nameEnd - position,
                    StandardCharsets.US_ASCII
            );

            position = nameEnd + 1;
        }

        return new NTFileInfo(
                count, pageSize, start, end, fileOffset, fileName
        );
    }

    private int readWord() throws IOException{
        byte[] b = new byte[4];
        core.readFully(b);
        return readWord(b, 0);
    }

    private long readOffAddr() throws IOException{
        byte[] b = new byte[8];
        core.readFully(b);
        return readOffAddr(b, 0);
    }

    private static int readWord(byte[] b, int offset){
        int value = 0;

        for(int i = 0; i < 4; i++){
            value |= (b[offset + i] & 0xff) << (i * 8);
        }

        return value;
    }

    private static int readHalf(byte[] b, int offset){
        return(b[offset] & 0xff) | ((b[offset + 1] & 0xff) << 8);
    }

    private static long readOffAddr(byte[] b, int offset){
        long value = 0;

        for(int i = 0; i < 8; i++){
            value |= (b[offset + i] & 0xffL) << (i * 8);
        }

        return value;
    }

    private static int align4(int size){
        int align = size % 4;
        if(align != 0){
           size += 4 - align; 
       }
       return size;
    }
}

