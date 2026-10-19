import java.io.IOException;

public class CDA{
    public static void main(String[] args){
        javax.swing.SwingUtilities.invokeLater(() -> {
        GUI gui = new GUI();
        gui.setVisible(true);
        });
    }
}

