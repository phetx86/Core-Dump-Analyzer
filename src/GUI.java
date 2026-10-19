import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GUI extends JFrame{
    private final DefaultListModel<String> fileListModel = new DefaultListModel<>();
    private final JList<String> fileList = new JList<>(fileListModel);
    private final Map<String, File> filePathMap = new HashMap<>();

    private final JTextArea mainReportArea = createTextArea(20);
    private final JTextArea ntFileArea = createTextArea(16);

    private final JTextArea gprArea = createTextArea(16);
    private final JTextArea rflagsArea = createTextArea(16);
    private final JTextArea simdArea = createTextArea(16);
    private final JTextArea fpuArea = createTextArea(16);

    public GUI(){
        setTitle("Core Dump Analyzer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 600));

        buildGUI();

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
    }

    private JTextArea createTextArea(int fontSize){
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        area.setLineWrap(false);
        area.setMargin(new Insets(8, 8, 8, 8));
        return area;
    }

    private JScrollPane createScrollPane(JTextArea area){
        return new JScrollPane(area,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private JPanel createReportPanel(String title, JTextArea area){
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.LEADING,
                TitledBorder.TOP
        ));
        panel.add(createScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private void buildGUI(){
        JButton importButton = new JButton("IMPORT");
        importButton.setFont(new Font("Tahoma", Font.PLAIN, 16));
        importButton.addActionListener(this::importFile);

        fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileList.setFont(new Font("Tahoma", Font.PLAIN, 18));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        leftPanel.add(importButton, BorderLayout.NORTH);
        leftPanel.add(createScrollPaneForList(), BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(240, 600));

        JPanel reportPanel = new JPanel(new GridLayout(1, 2, 5, 5));

        reportPanel.add(createReportPanel("Crash Report", mainReportArea));

        reportPanel.add(createReportPanel("NT_FILE Mapping", ntFileArea));

        JPanel registerPanel = new JPanel(new GridLayout(1, 4, 5, 5));

        registerPanel.add(createReportPanel("General Purpose Registers", gprArea));

        registerPanel.add(createReportPanel("RFLAGS", rflagsArea));

        registerPanel.add(createReportPanel("SIMD", simdArea));

        registerPanel.add(createReportPanel("FPU", fpuArea));

        JSplitPane mainSplit = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                reportPanel,
                registerPanel
        );

        mainSplit.setResizeWeight(0.50);
        mainSplit.setContinuousLayout(true);
        mainSplit.setOneTouchExpandable(true);

        JSplitPane rootSplit = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                leftPanel,
                mainSplit
        );

        rootSplit.setResizeWeight(0.16);
        rootSplit.setContinuousLayout(true);
        rootSplit.setOneTouchExpandable(true);

        setLayout(new BorderLayout());
        add(rootSplit, BorderLayout.CENTER);

        fileList.addListSelectionListener(event -> {
            if(event.getValueIsAdjusting()) {
                return;
            }

            String selectedName = fileList.getSelectedValue();
            File selectedFile = filePathMap.get(selectedName);

            if(selectedFile != null){
                loadAndDisplayCoreDump(selectedFile.getAbsolutePath());
            }
        });
    }

    private JScrollPane createScrollPaneForList(){
        return new JScrollPane(
                fileList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );
    }

    private void importFile(java.awt.event.ActionEvent event){
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        int result = chooser.showOpenDialog(this);

        if(result != JFileChooser.APPROVE_OPTION){
            return;
        }
        File selectedFile = chooser.getSelectedFile().getAbsoluteFile();

        String fullPath = selectedFile.getAbsolutePath();
        for(Map.Entry<String, File> entry : filePathMap.entrySet()){
            if(entry.getValue().getAbsolutePath().equals(fullPath)){
                fileList.setSelectedValue(entry.getKey(),true);
                return;
            }
        }

        String displayName = selectedFile.getName();

        boolean duplicateName = false;

        for(String existingName : filePathMap.keySet()){
            if(existingName.equals(displayName) || existingName.startsWith(
               displayName + " — ")) {
                duplicateName = true;
                break;
            }
        }

        if(duplicateName){
            displayName += " — "+ selectedFile.getParent();
        }

        filePathMap.put(displayName, selectedFile);
        fileListModel.addElement(displayName);
        fileList.setSelectedValue(displayName,true);
    }

    private void loadAndDisplayCoreDump(String filePath){
        try{
            ELFParser parser = new ELFParser(filePath);
            CoreDumpData data = parser.parse();
            StringBuilder report = new StringBuilder();

            report.append("Signal: ")
                    .append(data.get_name_signal())
                    .append(" (Signo: ")
                    .append(data.sigInfo.signo)
                    .append(")\n");

            report.append("Code: ")
                    .append(data.sigInfo.code)
                    .append("\n");

            report.append("Errno: ")
                    .append(data.sigInfo.errno)
                    .append("\n");

            report.append("Detail: ")
                    .append(data.code_detail())
                    .append("\n");

            report.append("PID: ")
                    .append(data.prStatus.pid)
                    .append("\n");

            report.append("PPID: ")
                    .append(data.prStatus.ppid)
                    .append("\n");

            report.append(String.format(
                    "Fault address: 0x%016x%n",
                    data.sigInfo.faultAddress))
                   .append("\n");

            long rip = data.prStatus.registers[16];
            boolean found = false;

            report.append(String.format(
                    "RIP: 0x%016x%n",rip)).append("\n");

            for(int i = 0; i < data.ntFileInfo.count; i++){
                if(rip >= data.ntFileInfo.start[i] && rip < data.ntFileInfo.end[i]){
                    report.append("RIP mapping: ").append(data.ntFileInfo.fileName[i])
                    .append("\n");
                    found = true;
                    break;
                }
            }
            if(!found){
                report.append("RIP mapping: Not found in NT_FILE\n");
            }

            mainReportArea.setText(report.toString());
            mainReportArea.setCaretPosition(0);

            StringBuilder mapping = new StringBuilder();

            mapping.append("Page size: ")
                    .append(data.ntFileInfo.pageSize)
                    .append("\n\n");

            for(int i = 0;i < data.ntFileInfo.count; i++){
                mapping.append(String.format(
                        "Start: 0x%016x%n",
                        data.ntFileInfo.start[i]));

                mapping.append(String.format(
                        "End: 0x%016x%n",
                        data.ntFileInfo.end[i]));

                mapping.append("File offset (pages): ")
                        .append(data.ntFileInfo.fileOffset[i])
                        .append("\n");

                mapping.append("File: ")
                        .append(data.ntFileInfo.fileName[i])
                        .append("\n\n");
            }

            ntFileArea.setText(mapping.toString());
            ntFileArea.setCaretPosition(0);
            setGprText(data.getGprReport());
            setRflagsText(data.getRflagsReport());
            setSimdText(data.getSimdReport());
            setFpuText(data.getFPUreport());

        }catch(IOException | IllegalArgumentException e){
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot analyse core dump:\n"
                            + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void clearRegisterAreas(){
        gprArea.setText("");
        rflagsArea.setText("");
        simdArea.setText("");
        fpuArea.setText("");
    }

    public void setGprText(String text){
        gprArea.setText(text);
        gprArea.setCaretPosition(0);
    }

    public void setRflagsText(String text){
        rflagsArea.setText(text);
        rflagsArea.setCaretPosition(0);
    }

    public void setSimdText(String text){
        simdArea.setText(text);
        simdArea.setCaretPosition(0);
    }

    public void setFpuText(String text){
        fpuArea.setText(text);
        fpuArea.setCaretPosition(0);
    }
}

