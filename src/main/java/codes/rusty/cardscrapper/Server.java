package codes.rusty.cardscrapper;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.regex.Pattern;

public class Server {

    private static final File cards = new File("cards/");
    private static final byte[] staticImage;

    static {
        staticImage = readAsByteArray(new File("./static.png"));
    }
    private HttpServer server;
    private int port;

    public Server() {
        System.out.print("Creating server... ");
        this.port = 8080;

        while (this.port < 8180) {
            try {
                this.server = HttpServer.create(new InetSocketAddress(this.port), 0);
                this.server.createContext("/", this::handle);
                this.server.setExecutor(null);
                break;
            } catch (BindException e) {
                if (e.getMessage().equals("Address already in use: bind")) {
                    this.port++;
                } else {
                    if (Main.flags.isDebug()) {
                        e.printStackTrace();
                    }
                    System.out.println("Unable to start webserver! Exiting...");
                    System.exit(0);
                }
            } catch (Exception e) {
                if (Main.flags.isDebug()) {
                    e.printStackTrace();
                }
                System.out.println("Unable to start webserver! Exiting...");
                System.exit(0);
            }
        }

        System.out.println("Done");
    }

    public void handle(HttpExchange ex) {
        try {
            String card = ex.getRequestURI().getPath().substring(1);

            byte[] image;
            if (isInt(card)) {
                File cardFile = new File(cards, card + ".png");
                if (!cardFile.exists()) {
                    image = staticImage;
                } else {
                    image = readAsByteArray(cardFile);
                }
            } else {
                image = staticImage;
            }

            ex.sendResponseHeaders(200, image.length);
            OutputStream stream = ex.getResponseBody();
            stream.write(image);
            stream.close();
        } catch (Exception e) {
            if (Main.flags.isDebug()) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        System.out.print("Starting server... ");
        this.server.start();
        System.out.println("Done");
        try {
            java.awt.Desktop.getDesktop().browse(new URI("http://duelingnetwork.com/?card_image_base=http://localhost:" + port + "/"));
        } catch (Exception e) {
            if (Main.flags.isDebug()) {
                e.printStackTrace();
            }
            System.out.println("To view card art, please visit:");
            System.out.println("http://duelingnetwork.com/?card_image_base=http://localhost:" + port + "/");
        }
    }

    private static final Pattern pattern = Pattern.compile("[0-9]*");

    public boolean isInt(String text) {
        return pattern.matcher(text).matches();
    }

    public static byte[] readAsByteArray(File file) {
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(file));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int read = 0;
            int total = 0;

            while ((read = dis.read(buf)) != -1) {
                out.write(buf);
                total += read;
            }

            return Arrays.copyOf(out.toByteArray(), total);
        } catch (Exception e) {
            if (Main.flags.isDebug()) {
                e.printStackTrace();
            }
            System.out.println("Unable to load static cardart! Exiting...");
            System.exit(0);
        }

        return null;
    }
}
