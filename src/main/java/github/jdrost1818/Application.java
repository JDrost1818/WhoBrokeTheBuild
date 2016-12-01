package github.jdrost1818;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Regularly checks the build for a variety of things
 *      1 - Are there any new builds?
 *          - If there are, we now should track them
 *      2 - Are any builds newly broken?
 *          - If there are, alert something
 *      3 - Are any builds still broken?
 *          - If a threshold of time is reached, do something
 *
 * And yes - this is written very poorly. No need to be mean about it,
 * the author, me, is well aware.
 *
 * Created by JAD0911 on 4/13/2016.
 */
public class Application {

    private static final String PASS_CLASS = "job-status-blue";
    private static final String WARNING_CLASS = "job-status-yellow";
    private static final String FAIL_CLASS = "job-status-red";

    private enum Status {
        PASSING, ERRORING, FAILING;

        public static Status getStatus(String key) {
            if (key.equals(PASS_CLASS)) {
                return PASSING;
            } else if (key.equals(WARNING_CLASS)) {
                return ERRORING;
            } else if (key.equals(FAIL_CLASS)) {
                return FAILING;
            } else {
                return null;
            }
        }

        public static Status getNotThisStatus(Status curBuildStatus) {
            for (Status status : Status.values()) {
                if (curBuildStatus != status) {
                    return status;
                }
            }
            return null;
        }
    }

    private void run(String jenkinsUrl) throws IOException {
        Document doc;
        Map<String, Status> buildStatuses = new HashMap<String, Status>();
        boolean shouldAlert = true;

        // So much nesting... Haha whoops
        while ((doc = connect(jenkinsUrl)) != null) {
            System.out.println("Checking if someone broke the build");
            Elements buildTable = doc.select("tr");
            for (Element curBuild : buildTable) {
                if (!curBuild.hasClass("header")) {
                    String curBuildName = curBuild.id();
                    Status curBuildStatus = Status.getStatus(curBuild.className());

                    if (!buildStatuses.containsKey(curBuildName)) {
                        buildStatuses.put(curBuildName, Status.getNotThisStatus(curBuildStatus));
                    }

                    // This means someone broke the build since the last time we checked
                    if (curBuildStatus != buildStatuses.get(curBuildName) && curBuildStatus == Status.PASSING) {
                        shouldAlert = true;
                    }
                }
            }
            if (shouldAlert) {
                alert("BROKEN BUILD");
            }
            shouldAlert = false;

            // Sleep 30 seconds
            System.out.println("All builds are fine. For now...");
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void alert(String curBuildName) throws IOException {
        final ClassLoader cl = this.getClass().getClassLoader();
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    InputStream buildBrokenSound = cl.getResourceAsStream("alarm.wav");
                    final AudioStream bb;
                    try {
                        bb = new AudioStream(buildBrokenSound);
                        AudioPlayer.player.start(bb);
                        Thread.sleep(42000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();
        new Thread(new Runnable() {
            public void run() {
                while (true) {
                    InputStream buildBrokenSound = cl.getResourceAsStream("buildBroken.wav");
                    final AudioStream bb;
                    try {
                        bb = new AudioStream(buildBrokenSound);
                        AudioPlayer.player.start(bb);
                        Thread.sleep(1800);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }
        }).start();

        JFrame alertFrame = new JFrame("BUILD IS BROKEN!!!");
        alertFrame.setAlwaysOnTop(true);
        alertFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel alertPanel = new JPanel(null);
        alertPanel.setSize(new Dimension(1280, 1024));
        alertFrame.setSize(new Dimension(1280, 1024));
        alertFrame.setContentPane(alertPanel);
        alertFrame.setVisible(true);
        alertFrame.setLocationRelativeTo(null);

        JLabel buildIsBroken = new JLabel("BUILD IS BROKEN");
        buildIsBroken.setBounds(140, 430, 1000, 124);
        alertPanel.add(buildIsBroken);
        buildIsBroken.setFont(buildIsBroken.getFont().deriveFont(100f));
        buildIsBroken.setVerticalAlignment(SwingConstants.CENTER);
        buildIsBroken.setHorizontalAlignment(SwingConstants.CENTER);
        buildIsBroken.setForeground(Color.BLACK);

        while(true) {
            alertPanel.setBackground(Color.RED);
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            alertPanel.setBackground(Color.WHITE);
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }

    private static Document connect(String url) {
        Document doc = null;
        int numTries = 0;
        while (numTries < 5 && doc == null) {
            try {
                doc = Jsoup.connect(url).get();
            } catch (IOException e) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                }
                numTries++;
            }
        }
        return doc;
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Missing Required Arg - USAGE: run <jenkins-url>");
        }
        new Application().run(args[0]);
    }

}
