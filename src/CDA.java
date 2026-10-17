import java.io.IOException;

public class CDA{
    public static void main(String[] args){
        if(args.length != 1){
            System.out.println("Usage: java Core_Dump_Analyser <core-file>");
            return;
        }

        try{
            ELFParser parser = new ELFParser(args[0]);
            CoreDumpData data = parser.parse();

            System.out.println("PID: " + data.prStatus.pid);
            System.out.println("PPID: " + data.prStatus.ppid);

            System.out.printf("RIP: 0x%016x%n",
                    data.prStatus.registers[16]);

            System.out.println("Signal: " + data.sigInfo.signo);
            System.out.printf("Fault address: 0x%016x%n",
                    data.sigInfo.faultAddress);

            System.out.println("Page size: " + data.ntFileInfo.pageSize);

            for(int i = 0; i < data.ntFileInfo.count; i++){
                System.out.printf("Start: 0x%016x%n",
                        data.ntFileInfo.start[i]);

                System.out.printf("End: 0x%016x%n",
                        data.ntFileInfo.end[i]);

                System.out.println("File offset (pages): "
                        + data.ntFileInfo.fileOffset[i]);

                System.out.println("File: "
                        + data.ntFileInfo.fileName[i]);
            }

        }catch(IOException | IllegalArgumentException e){
            System.err.println("Cannot analyse core dump: "
                    + e.getMessage());
        }
    }
}

