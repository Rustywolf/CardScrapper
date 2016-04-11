package codes.rusty.cardscrapper;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.commons.lang.StringEscapeUtils;

public class Main {

    public static final boolean DEBUG = false;

    private static File ygoPro;
    private static Connection dbCards;
    private static Map<Integer, String> dnIdMap;

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Please provide an argument to execute the program!");
            System.exit(0);
        } else {
            if (args[0].equalsIgnoreCase("--dump")) {
                if (args.length < 2) {
                    System.out.println("Please link to the DEVPro/YGOPro folder as an argument!");
                    System.exit(0);
                }

                ygoPro = new File(args[1]);
                if (!ygoPro.isDirectory()) {
                    System.out.println("Please link to the DEVPro/YGOPro folder as an argument!");
                    System.exit(0);
                }

                dumpCardList(); // Load DN ids and names into map
                loadCardDB(); // Load DB from YGOPro
                processCards(); // Crop and save the images YGOPro provides
            } else if (args[0].equalsIgnoreCase("--run")) {
                Server server = new Server();
                server.run();
            }
        }
    }

    public static void dumpCardList() {
        try {
            URLConnection conn = new URL("http://duelingnetwork.com/cardlist/").openConnection();
            System.out.print("Downloading DN card list... ");

            StringBuilder builder = new StringBuilder();
            byte[] buf = new byte[4096];
            int read = 0;
            DataInputStream dis = new DataInputStream(conn.getInputStream());

            while ((read = dis.read(buf)) != -1) {
                builder.append(new String(read == 4096 ? buf : Arrays.copyOf(buf, read)));
            }

            System.out.println("Done");
            System.out.print("Parsing DN card list... ");

            String list = builder.toString();
            list = StringEscapeUtils.unescapeHtml(list.replaceAll("<br />", "\n"));
            list = list.substring(57, list.length() - 18); //Strip beginning/end of webpage - hardcoding this because it should never change .. hopefully

            String[] lines = list.split("\n");
            dnIdMap = new HashMap<>(lines.length);

            for (String line : lines) {
                String[] split = line.split(" ", 2);
                int id;
                try {
                    id = Integer.valueOf(split[0]);
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected String, expected number in list!");
                }

                dnIdMap.put(id, split[1]);
            }

            System.out.println("Done");
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            System.out.println("Unable to load DN card list!");
            System.exit(0);
        }
    }

    public static void loadCardDB() {
        try {
            System.out.print("Loading PRO card list... ");

            Class.forName("org.sqlite.JDBC");
            dbCards = DriverManager.getConnection("jdbc:sqlite:" + ygoPro.getAbsolutePath() + "/cards.cdb");

            System.out.println("Done");
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            System.out.println("Unable to open database! Exiting...");
            System.exit(0);
        }

    }

    public static void processCards() {
        System.out.println("Creating card art... ");

        File logFile;
        PrintStream logStream;
        try {
            logFile = new File("./cards.log");
            logFile.createNewFile();
            logStream = new PrintStream(new FileOutputStream(logFile));
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            logFile = null;
            logStream = null;
        }

        final PrintStream log = logStream;
        dnIdMap.forEach((Integer id, String name) -> {
            try {
                String escapedName = escapeName(name).trim();
                String weirdName = escapedName.replaceAll("(#|\u00A0)", "");

                ResultSet set = dbCards.createStatement().executeQuery("SELECT * from texts WHERE name LIKE '" + escapedName + "' OR name LIKE '" + weirdName + "'");
                if (!set.next()) {
                    if (log != null) {
                        log.println("Unable to find card '" + name + "'");
                    }
                    
                    return;
                }

                processImage(id, set.getInt("id"), set.getString("desc").startsWith("Pendulum"), log);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                System.out.println("Unable to execute query! Exiting...");
                System.exit(0);
            }
        });
    }

    public static void processImage(int dnId, int ypId, boolean pend, PrintStream log) {
        try {
            File file = new File(ygoPro.getAbsoluteFile() + "/pics/" + ypId + ".jpg");
            BufferedImage image = ImageIO.read(file);

            int x1, x2, y1, y2;

            if (pend) {
                x1 = 11;
                x2 = 165;
                y1 = 45;
                y2 = 159;
            } else if (image.getWidth() == 177) {
                x1 = 24;
                x2 = 152;
                y1 = 55;
                y2 = 181;
            } else {
                x1 = 22;
                x2 = 151;
                y1 = 48;
                y2 = 177;
            }

            BufferedImage cardArt = new BufferedImage(x2 - x1, y2 - y1, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics g = cardArt.getGraphics();
            g.drawImage(image, -x1, -y1, null);
            g.dispose();

            File out = new File("./cards/" + dnId + ".png");
            out.getParentFile().mkdirs();
            ImageIO.write(cardArt, "PNG", out);
        } catch (Exception e) {
            if (DEBUG) {
                e.printStackTrace();
            }
            log.println("Unable to load image #" + ypId);
        }

    }

    public static String escapeName(String name) {
        return name.replaceAll("'", "''");
    }

}
