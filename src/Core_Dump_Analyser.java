import java.io.*;

public class Core_Dump_Analyser {
    public static void main(String[] args) throws IOException {
        RandomAccessFile core = new RandomAccessFile(args[0],"r");

        byte[] test_read = new byte[64];
        core.readFully(test_read);
        for(byte b : test_read){
            System.out.printf("%02x ", b & 0xff);
        
        }
    }
}
