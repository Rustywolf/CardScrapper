package codes.rusty.cardscrapper;

import java.io.File;

public class Flags {

    private final String[] args;
    
    private RunType runType = RunType.NONE;
    private File ygoProFile = null;
    private File hqFile = null;
    private boolean debug = false;
    
    public Flags(String[] args) {
        this.args = args;
    }
    
    public void parse() {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--debug":
                    debug = true;
                    break;
                    
                case "--run":
                    runType = RunType.SERVER;
                    break;
                    
                case "--dump":
                    if (args.length <= i++) {
                        System.out.println("Please link to the DEVPro/YGOPro folder as an argument!");
                        System.exit(0);
                    }
                    
                    runType = RunType.DUMP;
                    ygoProFile = new File(args[i]);
                    break;
                
                case "--hq":
                    if (args.length <= i + 2) {
                        System.out.println("Please link to the DEVPro/YGOPro folder and the HQ Image folder as arguments!");
                        System.exit(0);
                    }
                    
                    runType = RunType.HQ;
                    ygoProFile = new File(args[++i]);
                    hqFile = new File(args[++i]);
                    break;
            }
        }
        
        if (runType == RunType.NONE) {
            System.out.println("Please provide a flag to run this program!");
            System.exit(0);
        }
    }

    public RunType getRunType() {
        return runType;
    }

    public File getYgoProFile() {
        return ygoProFile;
    }

    public File getHqFile() {
        return hqFile;
    }

    public boolean isDebug() {
        return debug;
    }
    
    public static enum RunType {
        NONE, SERVER, DUMP, HQ
    }
    
}
