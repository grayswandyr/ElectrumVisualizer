/* Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF
 * OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.mit.csail.sdg.alloy4viz;

import edu.mit.csail.sdg.alloy4.MailBug;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4.OurUtil;
import edu.mit.csail.sdg.alloy4.OurDialog;
import edu.mit.csail.sdg.alloy4.OurAntiAlias;
import edu.mit.csail.sdg.alloy4.Util.IntPref;
import edu.mit.csail.sdg.alloy4.Util.BooleanPref;
import edu.mit.csail.sdg.alloy4.Util.StringPref;
import edu.mit.csail.sdg.alloy4.Version;
import edu.mit.csail.sdg.alloy4.Err;

import java.util.prefs.Preferences;
import java.util.Locale;

import java.io.File;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import java.awt.GraphicsEnvironment;

public class VizMain {

    /**
     * The latest welcome screen; each time we update the welcome screen, we
     * increment this number.
     */
    private static final int welcomeLevel = 2;

    // Verify that the graphics environment is set up
    static {
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        } catch (Throwable ex) {
            System.err.println("Unable to start the graphical environment.");
            System.err.println("If you're on Mac OS X:");
            System.err.println("   Please make sure you are running as the current local user.");
            System.err.println("If you're on Linux or FreeBSD:");
            System.err.println("   Please make sure your X Windows is configured.");
            System.err.println("   You can verify this by typing \"xhost\"; it should not give an error message.");
            System.err.flush();
            System.exit(1);
        }
    }

    /**
     * The most recent Alloy version (as queried from alloy.mit.edu); -1 if
     * alloy.mit.edu has not replied yet.
     */
    private int latestAlloyVersion = (-1);

    /**
     * The most recent Alloy version name (as queried from alloy.mit.edu);
     * "unknown" if alloy.mit.edu has not replied yet.
     */
    private String latestAlloyVersionName = "unknown";

    /**
     * The system-specific file separator (forward-slash on UNIX, back-slash on
     * Windows, etc.)
     */
    private static final String fs = System.getProperty("file.separator");

    /**
     * This variable caches the result of alloyHome() function call.
     */
    private static String alloyHome = null;

    /**
     * Find a temporary directory to store Alloy files; it's guaranteed to be a
     * canonical absolute path.
     */
    private static synchronized String alloyHome() {
        if (alloyHome != null) {
            return alloyHome;
        }

        // Retrieve temp directory path
        String temp = System.getProperty("java.io.tmpdir");
        if (temp == null || temp.length() == 0) {
            OurDialog.fatal("Error. JVM need to specify a temporary directory using java.io.tmpdir property.");
        }

        // Retrieve user name
        String username = System.getProperty("user.name");

        // Create a temporary file with the retrieved tmp path and username
        File tempfile = new File(temp + File.separatorChar + "alloy4tmp40-" + (username == null ? "" : username));
        tempfile.mkdirs();

        // Canonize file path
        String ans = Util.canon(tempfile.getPath());
        if (!tempfile.isDirectory()) // If the created file does not exists, it means that we did not create it => fail
        {
            OurDialog.fatal("Error. Cannot create the temporary directory " + ans);
        }

        if (!Util.onWindows()) {
            // In some case it can be because of bad acces right => give all accesses to current user (chmod 700)
            String[] args = {"chmod", "700", ans};
            try {
                Runtime.getRuntime().exec(args).waitFor();
            } catch (Throwable ex) {
            } // We only intend to make a best effort.
        }

        alloyHome = ans;
        return alloyHome;
    }

    /**
     * True if Alloy Analyzer should let warning be nonfatal.
     */
    private static final BooleanPref WarningNonfatal = new BooleanPref("WarningNonfatal");

    /**
     * True if Alloy Analyzer should automatically visualize the latest
     * instance.
     */
    private static final BooleanPref AutoVisualize = new BooleanPref("AutoVisualize");

    /**
     * True if Alloy Analyzer should insist on antialias.
     */
    private static final BooleanPref AntiAlias = new BooleanPref("AntiAlias");

    /**
     * True if Alloy Analyzer should record the raw Kodkod input and output.
     */
    private static final BooleanPref RecordKodkod = new BooleanPref("RecordKodkod");

    /**
     * True if Alloy Analyzer should enable the new Implicit This name
     * resolution.
     */
    private static final BooleanPref ImplicitThis = new BooleanPref("ImplicitThis");

    /**
     * True if Alloy Analyzer should not report models that overflow.
     */
    private static final BooleanPref NoOverflow = new BooleanPref("NoOverflow");

    /**
     * The latest X corrdinate of the Alloy Analyzer's main window.
     */
    private static final IntPref AnalyzerX = new IntPref("AnalyzerX", 0, -1, 65535);

    /**
     * The latest Y corrdinate of the Alloy Analyzer's main window.
     */
    private static final IntPref AnalyzerY = new IntPref("AnalyzerY", 0, -1, 65535);

    /**
     * The latest width of the Alloy Analyzer's main window.
     */
    private static final IntPref AnalyzerWidth = new IntPref("AnalyzerWidth", 0, -1, 65535);

    /**
     * The latest height of the Alloy Analyzer's main window.
     */
    private static final IntPref AnalyzerHeight = new IntPref("AnalyzerHeight", 0, -1, 65535);

    /**
     * The latest font size of the Alloy Analyzer.
     */
    private static final IntPref FontSize = new IntPref("FontSize", 9, 12, 72);

    /**
     * The latest font name of the Alloy Analyzer.
     */
    private static final StringPref FontName = new StringPref("FontName", "Lucida Grande");

    /**
     * The latest tab distance of the Alloy Analyzer.
     */
    private static final IntPref TabSize = new IntPref("TabSize", 1, 2, 16);

    /**
     * The latest welcome screen that the user has seen.
     */
    private static final IntPref Welcome = new IntPref("Welcome", 0, 0, 1000);

    /**
     * Whether syntax highlighting should be disabled or not.
     */
    private static final BooleanPref SyntaxDisabled = new BooleanPref("SyntaxHighlightingDisabled");

    /**
     * The number of recursion unrolls.
     */
    private static final IntPref Unrolls = new IntPref("Unrolls", -1, -1, 3);

    /**
     * The skolem depth.
     */
    private static final IntPref SkolemDepth = new IntPref("SkolemDepth3", 0, 1, 4);

    /**
     * The unsat core minimization strategy.
     */
    private static final IntPref CoreMinimization = new IntPref("CoreMinimization", 0, 2, 2);

    /**
     * The unsat core granularity.
     */
    private static final IntPref CoreGranularity = new IntPref("CoreGranularity", 0, 0, 3);

    /**
     * The temporal trace length. pt.uminho.haslab
     */
    private static final IntPref MaxTraceLength = new IntPref("MaxTraceLength", 1, 20, 100);

    /**
     * The amount of memory (in M) to allocate for Kodkod and the SAT solvers.
     */
    private static final IntPref SubMemory = new IntPref("SubMemory", 16, 768, 65535);

    /**
     * The amount of stack (in K) to allocate for Kodkod and the SAT solvers.
     */
    private static final IntPref SubStack = new IntPref("SubStack", 16, 8192, 65536);

    /**
     * The first file in Alloy Analyzer's "open recent" list.
     */
    private static final StringPref Model0 = new StringPref("Model0");

    /**
     * The second file in Alloy Analyzer's "open recent" list.
     */
    private static final StringPref Model1 = new StringPref("Model1");

    /**
     * The third file in Alloy Analyzer's "open recent" list.
     */
    private static final StringPref Model2 = new StringPref("Model2");

    /**
     * The fourth file in Alloy Analyzer's "open recent" list.
     */
    private static final StringPref Model3 = new StringPref("Model3");

    /**
     * This enum defines the set of possible message verbosity levels.
     */
    private enum Verbosity {

        /**
         * Level 0.
         */
        DEFAULT("0", "low"),
        /**
         * Level 1.
         */
        VERBOSE("1", "medium"),
        /**
         * Level 2.
         */
        DEBUG("2", "high"),
        /**
         * Level 3.
         */
        FULLDEBUG("3", "debug only");

        /**
         * Returns true if it is greater than or equal to "other".
         */
        public boolean geq(Verbosity other) {
            return ordinal() >= other.ordinal();
        }
        /**
         * This is a unique String for this value; it should be kept consistent
         * in future versions.
         */
        private final String id;
        /**
         * This is the label that the toString() method will return.
         */
        private final String label;

        /**
         * Constructs a new Verbosity value with the given id and label.
         */
        private Verbosity(String id, String label) {
            this.id = id;
            this.label = label;
        }

        /**
         * Given an id, return the enum value corresponding to it (if there's no
         * match, then return DEFAULT).
         */
        private static Verbosity parse(String id) {
            for (Verbosity vb : values()) {
                if (vb.id.equals(id)) {
                    return vb;
                }
            }
            return DEFAULT;
        }

        /**
         * Returns the human-readable label for this enum value.
         */
        @Override
        public final String toString() {
            return label;
        }

        /**
         * Saves this value into the Java preference object.
         */
        private void set() {
            Preferences.userNodeForPackage(Util.class).put("Verbosity", id);
        }

        /**
         * Reads the current value of the Java preference object (if it's not
         * set, then return DEFAULT).
         */
        private static Verbosity get() {
            return parse(Preferences.userNodeForPackage(Util.class).get("Verbosity", ""));
        }
    };

    /**
     * Copy the required files from the JAR into a temporary directory.
     */
    private void copyFromJAR() {
        // Compute the appropriate platform
        String os = System.getProperty("os.name").toLowerCase(Locale.US).replace(' ', '-');
        if (os.startsWith("mac-")) {
            os = "mac";
        } else if (os.startsWith("windows-")) {
            os = "windows";
        }
        String arch = System.getProperty("os.arch").toLowerCase(Locale.US).replace(' ', '-');
        if (arch.equals("powerpc")) {
            arch = "ppc-" + os;
        } else {
            arch = arch.replaceAll("\\Ai[3456]86\\z", "x86") + "-" + os;
        }
        if (os.equals("mac")) {
            arch = "x86-mac"; // our pre-compiled binaries are all universal binaries
        }        // Find out the appropriate Alloy directory
        final String platformBinary = alloyHome() + fs + "binary";
        // Write a few test files
        try {
            (new File(platformBinary)).mkdirs();
            Util.writeAll(platformBinary + fs + "tmp.cnf", "p cnf 3 1\n1 0\n");
        } catch (Err er) {
            // The error will be caught later by the "berkmin" or "spear" test
        }
        // Copy the platform-dependent binaries
        Util.copy(true, false, platformBinary,
                arch + "/libminisat.so", arch + "/libminisatx1.so", arch + "/libminisat.jnilib",
                arch + "/libminisatprover.so", arch + "/libminisatproverx1.so", arch + "/libminisatprover.jnilib",
                arch + "/libzchaff.so", arch + "/libzchaffx1.so", arch + "/libzchaff.jnilib",
                arch + "/berkmin", arch + "/spear");
        Util.copy(false, false, platformBinary,
                arch + "/minisat.dll", arch + "/minisatprover.dll", arch + "/zchaff.dll",
                arch + "/berkmin.exe", arch + "/spear.exe");
        // Record the locations
        System.setProperty("alloy.theme0", alloyHome() + fs + "models");
        System.setProperty("alloy.home", alloyHome());
        System.setProperty("debug", "no"); // [HASLab]
    }

    private VizGUI viz;
    private VizLogger log;

    // CSTR
    private VizMain(String[] args) {
        log = new VizLogger("Visualizer");
        log.log("Setting up mails");
        MailBug.setup();

        // Enable better look-and-feel
        if (Util.onMac() || Util.onWindows()) {
            log.log("Enabling better look and feel");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Alloy Analyzer (Electrum) " + Version.version());
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "true");
            System.setProperty("com.apple.mrj.application.live-resize", "true");
            System.setProperty("com.apple.macos.useScreenMenuBar", "true");
            System.setProperty("apple.laf.useScreenMenuBar", "true");
//            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable e) { }
        }

        log.log("Creating VizGUI interface...");

        String fileName;

        if (args.length == 0) {
            File file = OurDialog.askFile(true, null, ".xml", ".xml instance files");
            if (file == null) {
                log.log("You have to select an xml file.");
                return;
            }
            Util.setCurrentDirectory(file.getParentFile());
            fileName = file.getPath();
        } else {
            fileName = args[0];
        }

        this.viz = new VizGUI(true, fileName, null);
        this.viz.doSetFontSize(FontSize.get());

        log.log("Showing VizGUI...");
        this.viz.doShowViz();

        //////////////////// FINISH INIT ////////////////////////
        // Choose the appropriate font
        int fontSize = FontSize.get();
        String fontName = FontName.get();
        while (true) {
            if (!OurDialog.hasFont(fontName)) {
                fontName = "Lucida Grande";
            } else {
                break;
            }
            if (!OurDialog.hasFont(fontName)) {
                fontName = "Verdana";
            } else {
                break;
            }
            if (!OurDialog.hasFont(fontName)) {
                fontName = "Courier New";
            } else {
                break;
            }
            if (!OurDialog.hasFont(fontName)) {
                fontName = "Lucida Grande";
            }
            break;
        }
        FontName.set(fontName);

        // Choose the antiAlias setting
        OurAntiAlias.enableAntiAlias(AntiAlias.get());

        // Add the new JNI location to the java.library.path
        copyFromJAR();
        final String binary = alloyHome() + fs + "binary";
        try {
            System.setProperty("java.library.path", binary);
            // The above line is actually useless on Sun JDK/JRE (see Sun's bug ID 4280189)
            // The following 4 lines should work for Sun's JDK/JRE (though they probably won't work for others)
            String[] newarray = new String[]{binary};
            java.lang.reflect.Field old = ClassLoader.class.getDeclaredField("usr_paths");
            old.setAccessible(true);
            old.set(null, newarray);
        } catch (Throwable ex) {
        }

        // If the temporary directory has become too big, then tell the user they can "clear temporary directory".
        /*long space = computeTemporarySpaceUsed();
         if (space<0 || space>=20*1024768) {
         if (space<0) log.logBold("Warning: Alloy4's temporary directory has exceeded 1024M.\n");
         else log.logBold("Warning: Alloy4's temporary directory now uses "+(space/1024768)+"M.\n");
         log.log("To clear the temporary directory,\n"
         +"go to the File menu and click \"Clear Temporary Directory\"\n");
         log.logDivider();
         log.flush();
         }*/
        // Launch the welcome screen if needed
        if (!"yes".equals(System.getProperty("debug")) && Welcome.get() < welcomeLevel) {
            JCheckBox again = new JCheckBox("Show this message every time you start the Alloy Analyzer");
            again.setSelected(true);
            OurDialog.showmsg("Welcome",
                    "Thank you for using the Alloy Analyzer " + Version.version(),
                    " ",
                    "Version 4 of the Alloy Analyzer is a complete rewrite,",
                    "offering improvements in robustness, performance and usability.",
                    "Models written in Alloy 3 will require some small alterations to run in Alloy 4.",
                    " ",
                    "Here are some quick tips:",
                    " ",
                    "* Function calls now use [ ] instead of ( )",
                    "  For more details, please see http://alloy.mit.edu/alloy4/quickguide/",
                    " ",
                    "* The Execute button always executes the latest command.",
                    "  To choose which command to execute, go to the Execute menu.",
                    " ",
                    "* The Alloy Analyzer comes with a variety of sample models.",
                    "  To see them, go to the File menu and click Open Sample Models.",
                    " ",
                    again
            );
            if (!again.isSelected()) {
                Welcome.set(welcomeLevel);
            }
        }

        // Periodically ask the MailBug thread to see if there is a newer version or not
        final long now = System.currentTimeMillis();
        final Timer t = new Timer(800, null);
        t.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int n = MailBug.latestBuildNumber();
                // If beyond 3 seconds, then we should stop because the log message may run into other user messages
                if (System.currentTimeMillis() - now >= 3000 || n <= Version.buildNumber()) {
                    t.stop();
                    return;
                }
                latestAlloyVersion = n;
                latestAlloyVersionName = MailBug.latestBuildName();
                log.logBold("An updated version of the Alloy Analyzer has been released.\n");
                log.log("Please visit alloy.mit.edu to download the latest version:\nVersion " + latestAlloyVersionName + "\n");
                t.stop();
            }
        });
        t.start();
    }

    public static void main(final String[] args) throws Exception {
        /*if (args.length == 0) {
         System.err.println("Error: you must provide an input file to run ElectrumVisualizer");
         System.exit(-1);
         }*/
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new VizMain(args);
            }
        });
    }
}
