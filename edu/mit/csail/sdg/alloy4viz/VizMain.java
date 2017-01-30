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

import java.awt.GraphicsEnvironment;

public class VizMain {
    // Verify that the graphics environment is set up
    static {
        try {
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        } catch(Throwable ex) {
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
     
    /** The most recent Alloy version (as queried from alloy.mit.edu); -1 if alloy.mit.edu has not replied yet. */
    private int latestAlloyVersion = (-1);

    /** The most recent Alloy version name (as queried from alloy.mit.edu); "unknown" if alloy.mit.edu has not replied yet. */
    private String latestAlloyVersionName = "unknown";
    
    /** The system-specific file separator (forward-slash on UNIX, back-slash on Windows, etc.) */
    private static final String fs = System.getProperty("file.separator");
    
    /** This variable caches the result of alloyHome() function call. */
    private static String alloyHome = null;

    /** Find a temporary directory to store Alloy files; it's guaranteed to be a canonical absolute path. */
    private static synchronized String alloyHome() {
        if (alloyHome!=null) return alloyHome;
        String temp=System.getProperty("java.io.tmpdir");
        if (temp==null || temp.length()==0)
            OurDialog.fatal("Error. JVM need to specify a temporary directory using java.io.tmpdir property.");
        String username=System.getProperty("user.name");
        File tempfile=new File(temp+File.separatorChar+"alloy4tmp40-"+(username==null?"":username));
        tempfile.mkdirs();
        String ans=Util.canon(tempfile.getPath());
        if (!tempfile.isDirectory()) {
            OurDialog.fatal("Error. Cannot create the temporary directory "+ans);
        }
        if (!Util.onWindows()) {
            String[] args={"chmod", "700", ans};
            try {Runtime.getRuntime().exec(args).waitFor();}
            catch (Throwable ex) {} // We only intend to make a best effort.
        }
        return alloyHome=ans;
    }

    /** True if Alloy Analyzer should let warning be nonfatal. */
    private static final BooleanPref WarningNonfatal = new BooleanPref("WarningNonfatal");

    /** True if Alloy Analyzer should automatically visualize the latest instance. */
    private static final BooleanPref AutoVisualize = new BooleanPref("AutoVisualize");

    /** True if Alloy Analyzer should insist on antialias. */
    private static final BooleanPref AntiAlias = new BooleanPref("AntiAlias");

    /** True if Alloy Analyzer should record the raw Kodkod input and output. */
    private static final BooleanPref RecordKodkod = new BooleanPref("RecordKodkod");

    /** True if Alloy Analyzer should enable the new Implicit This name resolution. */
    private static final BooleanPref ImplicitThis = new BooleanPref("ImplicitThis");
    
    /** True if Alloy Analyzer should not report models that overflow. */
    private static final BooleanPref NoOverflow = new BooleanPref("NoOverflow");

    /** The latest X corrdinate of the Alloy Analyzer's main window. */
    private static final IntPref AnalyzerX = new IntPref("AnalyzerX",0,-1,65535);

    /** The latest Y corrdinate of the Alloy Analyzer's main window. */
    private static final IntPref AnalyzerY = new IntPref("AnalyzerY",0,-1,65535);

    /** The latest width of the Alloy Analyzer's main window. */
    private static final IntPref AnalyzerWidth = new IntPref("AnalyzerWidth",0,-1,65535);

    /** The latest height of the Alloy Analyzer's main window. */
    private static final IntPref AnalyzerHeight = new IntPref("AnalyzerHeight",0,-1,65535);

    /** The latest font size of the Alloy Analyzer. */
    private static final IntPref FontSize = new IntPref("FontSize",9,12,72);

    /** The latest font name of the Alloy Analyzer. */
    private static final StringPref FontName = new StringPref("FontName","Lucida Grande");

    /** The latest tab distance of the Alloy Analyzer. */
    private static final IntPref TabSize = new IntPref("TabSize",1,2,16);

    /** The latest welcome screen that the user has seen. */
    private static final IntPref Welcome = new IntPref("Welcome",0,0,1000);

    /** Whether syntax highlighting should be disabled or not. */
    private static final BooleanPref SyntaxDisabled = new BooleanPref("SyntaxHighlightingDisabled");

    /** The number of recursion unrolls. */
    private static final IntPref Unrolls = new IntPref("Unrolls", -1, -1, 3);

    /** The skolem depth. */
    private static final IntPref SkolemDepth = new IntPref("SkolemDepth3", 0, 1, 4);

    /** The unsat core minimization strategy. */
    private static final IntPref CoreMinimization = new IntPref("CoreMinimization",0,2,2);
    
    /** The unsat core granularity. */
    private static final IntPref CoreGranularity = new IntPref("CoreGranularity",0,0,3);

    /** The temporal trace length.
     * pt.uminho.haslab */
    private static final IntPref MaxTraceLength = new IntPref("MaxTraceLength",1,20,100);

    /** The amount of memory (in M) to allocate for Kodkod and the SAT solvers. */
    private static final IntPref SubMemory = new IntPref("SubMemory",16,768,65535);

    /** The amount of stack (in K) to allocate for Kodkod and the SAT solvers. */
    private static final IntPref SubStack = new IntPref("SubStack",16,8192,65536);

    /** The first file in Alloy Analyzer's "open recent" list. */
    private static final StringPref Model0 = new StringPref("Model0");

    /** The second file in Alloy Analyzer's "open recent" list. */
    private static final StringPref Model1 = new StringPref("Model1");

    /** The third file in Alloy Analyzer's "open recent" list. */
    private static final StringPref Model2 = new StringPref("Model2");

    /** The fourth file in Alloy Analyzer's "open recent" list. */
    private static final StringPref Model3 = new StringPref("Model3");

    /** This enum defines the set of possible message verbosity levels. */
    private enum Verbosity {
        /** Level 0. */  DEFAULT("0", "low"),
        /** Level 1. */  VERBOSE("1", "medium"),
        /** Level 2. */  DEBUG("2", "high"),
        /** Level 3. */  FULLDEBUG("3", "debug only");
        /** Returns true if it is greater than or equal to "other". */
        public boolean geq(Verbosity other) { return ordinal() >= other.ordinal(); }
        /** This is a unique String for this value; it should be kept consistent in future versions. */
        private final String id;
        /** This is the label that the toString() method will return. */
        private final String label;
        /** Constructs a new Verbosity value with the given id and label. */
        private Verbosity(String id, String label) { this.id=id; this.label=label; }
        /** Given an id, return the enum value corresponding to it (if there's no match, then return DEFAULT). */
        private static Verbosity parse(String id) {
            for(Verbosity vb: values()) if (vb.id.equals(id)) return vb;
            return DEFAULT;
        }
        /** Returns the human-readable label for this enum value. */
        @Override public final String toString() { return label; }
        /** Saves this value into the Java preference object. */
        private void set() { Preferences.userNodeForPackage(Util.class).put("Verbosity",id); }
        /** Reads the current value of the Java preference object (if it's not set, then return DEFAULT). */
        private static Verbosity get() { return parse(Preferences.userNodeForPackage(Util.class).get("Verbosity","")); }
    };

    /** Copy the required files from the JAR into a temporary directory. */
    private void copyFromJAR() {
        // Compute the appropriate platform
        String os = System.getProperty("os.name").toLowerCase(Locale.US).replace(' ','-');
        if (os.startsWith("mac-")) os="mac"; else if (os.startsWith("windows-")) os="windows";
        String arch = System.getProperty("os.arch").toLowerCase(Locale.US).replace(' ','-');
        if (arch.equals("powerpc")) arch="ppc-"+os; else arch=arch.replaceAll("\\Ai[3456]86\\z","x86")+"-"+os;
        if (os.equals("mac")) arch="x86-mac"; // our pre-compiled binaries are all universal binaries
        // Find out the appropriate Alloy directory
        final String platformBinary = alloyHome() + fs + "binary";
        // Write a few test files
        try {
            (new File(platformBinary)).mkdirs();
            Util.writeAll(platformBinary + fs + "tmp.cnf", "p cnf 3 1\n1 0\n");
        } catch(Err er) {
            // The error will be caught later by the "berkmin" or "spear" test
        }
        // Copy the platform-dependent binaries
        Util.copy(true, false, platformBinary,
           arch+"/libminisat.so", arch+"/libminisatx1.so", arch+"/libminisat.jnilib",
           arch+"/libminisatprover.so", arch+"/libminisatproverx1.so", arch+"/libminisatprover.jnilib",
           arch+"/libzchaff.so", arch+"/libzchaffx1.so", arch+"/libzchaff.jnilib",
           arch+"/berkmin", arch+"/spear");
        Util.copy(false, false, platformBinary,
           arch+"/minisat.dll", arch+"/minisatprover.dll", arch+"/zchaff.dll",
           arch+"/berkmin.exe", arch+"/spear.exe");
        // Copy the model files
        /*Util.copy(false, true, alloyHome(),
           "models/book/appendixA/addressBook1.als", "models/book/appendixA/addressBook2.als", "models/book/appendixA/barbers.als",
           "models/book/appendixA/closure.als", "models/book/appendixA/distribution.als", "models/book/appendixA/phones.als",
           "models/book/appendixA/prison.als", "models/book/appendixA/properties.als", "models/book/appendixA/ring.als",
           "models/book/appendixA/spanning.als", "models/book/appendixA/tree.als", "models/book/appendixA/tube.als", "models/book/appendixA/undirected.als",
           "models/book/appendixE/hotel.thm", "models/book/appendixE/p300-hotel.als", "models/book/appendixE/p303-hotel.als", "models/book/appendixE/p306-hotel.als",
           "models/book/chapter2/addressBook1a.als", "models/book/chapter2/addressBook1b.als", "models/book/chapter2/addressBook1c.als",
           "models/book/chapter2/addressBook1d.als", "models/book/chapter2/addressBook1e.als", "models/book/chapter2/addressBook1f.als",
           "models/book/chapter2/addressBook1g.als", "models/book/chapter2/addressBook1h.als", "models/book/chapter2/addressBook2a.als",
           "models/book/chapter2/addressBook2b.als", "models/book/chapter2/addressBook2c.als", "models/book/chapter2/addressBook2d.als",
           "models/book/chapter2/addressBook2e.als", "models/book/chapter2/addressBook3a.als", "models/book/chapter2/addressBook3b.als",
           "models/book/chapter2/addressBook3c.als", "models/book/chapter2/addressBook3d.als", "models/book/chapter2/theme.thm",
           "models/book/chapter4/filesystem.als", "models/book/chapter4/grandpa1.als",
           "models/book/chapter4/grandpa2.als", "models/book/chapter4/grandpa3.als", "models/book/chapter4/lights.als",
           "models/book/chapter5/addressBook.als", "models/book/chapter5/lists.als", "models/book/chapter5/sets1.als", "models/book/chapter5/sets2.als",
           "models/book/chapter6/hotel.thm", "models/book/chapter6/hotel1.als", "models/book/chapter6/hotel2.als",
           "models/book/chapter6/hotel3.als", "models/book/chapter6/hotel4.als", "models/book/chapter6/mediaAssets.als",
           "models/book/chapter6/memory/abstractMemory.als", "models/book/chapter6/memory/cacheMemory.als",
           "models/book/chapter6/memory/checkCache.als", "models/book/chapter6/memory/checkFixedSize.als",
           "models/book/chapter6/memory/fixedSizeMemory.als", "models/book/chapter6/memory/fixedSizeMemory_H.als",
           "models/book/chapter6/ringElection.thm", "models/book/chapter6/ringElection1.als", "models/book/chapter6/ringElection2.als",
           "models/examples/algorithms/dijkstra.als", "models/examples/algorithms/dijkstra.thm",
           "models/examples/algorithms/messaging.als", "models/examples/algorithms/messaging.thm",
           "models/examples/algorithms/opt_spantree.als", "models/examples/algorithms/opt_spantree.thm",
           "models/examples/algorithms/peterson.als",
           "models/examples/algorithms/ringlead.als", "models/examples/algorithms/ringlead.thm",
           "models/examples/algorithms/s_ringlead.als",
           "models/examples/algorithms/stable_mutex_ring.als", "models/examples/algorithms/stable_mutex_ring.thm",
           "models/examples/algorithms/stable_orient_ring.als", "models/examples/algorithms/stable_orient_ring.thm",
           "models/examples/algorithms/stable_ringlead.als", "models/examples/algorithms/stable_ringlead.thm",
           "models/examples/case_studies/INSLabel.als", "models/examples/case_studies/chord.als",
           "models/examples/case_studies/chord2.als", "models/examples/case_studies/chordbugmodel.als",
           "models/examples/case_studies/com.als", "models/examples/case_studies/firewire.als", "models/examples/case_studies/firewire.thm",
           "models/examples/case_studies/ins.als", "models/examples/case_studies/iolus.als",
           "models/examples/case_studies/sync.als", "models/examples/case_studies/syncimpl.als",
           "models/examples/puzzles/farmer.als", "models/examples/puzzles/farmer.thm",
           "models/examples/puzzles/handshake.als", "models/examples/puzzles/handshake.thm",
           "models/examples/puzzles/hanoi.als", "models/examples/puzzles/hanoi.thm",
           "models/examples/systems/file_system.als", "models/examples/systems/file_system.thm",
           "models/examples/systems/javatypes_soundness.als",
           "models/examples/systems/lists.als", "models/examples/systems/lists.thm",
           "models/examples/systems/marksweepgc.als", "models/examples/systems/views.als",
           "models/examples/toys/birthday.als", "models/examples/toys/birthday.thm",
           "models/examples/toys/ceilingsAndFloors.als", "models/examples/toys/ceilingsAndFloors.thm",
           "models/examples/toys/genealogy.als", "models/examples/toys/genealogy.thm",
           "models/examples/toys/grandpa.als", "models/examples/toys/grandpa.thm",
           "models/examples/toys/javatypes.als", "models/examples/toys/life.als", "models/examples/toys/life.thm",
           "models/examples/toys/numbering.als", "models/examples/toys/railway.als", "models/examples/toys/railway.thm",
           "models/examples/toys/trivial.als",
           "models/examples/tutorial/farmer.als",
           "models/util/boolean.als", "models/util/graph.als", "models/util/integer.als", "models/util/natural.als",
           "models/util/ordering.als", "models/util/relation.als", "models/util/seqrel.als", "models/util/sequence.als",
           "models/util/sequniv.als", "models/util/ternary.als", "models/util/time.als", "models/Temporal_Examples/firewire.ele", // pt.uminho.haslab
                "models/Temporal_Examples/hotel.ele","models/Temporal_Examples/lift_spl.ele","models/Temporal_Examples/ring.ele", // pt.uminho.haslab
                "models/Temporal_Examples/span_tree.ele", "models/Temporal_Examples/ex1.ele" // pt.uminho.haslab
           );*/
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
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Alloy Analyzer (Electrum) "+Version.version());
            System.setProperty("com.apple.mrj.application.growbox.intrudes","true");
            System.setProperty("com.apple.mrj.application.live-resize","true");
            System.setProperty("com.apple.macos.useScreenMenuBar","true");
            System.setProperty("apple.laf.useScreenMenuBar","true");
//            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Throwable e) { }
        }

        // Figure out the desired x, y, width, and height
        /*int screenWidth=OurUtil.getScreenWidth(), screenHeight=OurUtil.getScreenHeight();
        int width=AnalyzerWidth.get();
        if (width<=0) width=screenWidth/10*8; else if (width<100) width=100;
        if (width>screenWidth) width=screenWidth;
        int height=AnalyzerHeight.get();
        if (height<=0) height=screenHeight/10*8; else if (height<100) height=100;
        if (height>screenHeight) height=screenHeight;
        int x=AnalyzerX.get(); if (x<0) x=screenWidth/10; if (x>screenWidth-100) x=screenWidth-100;
        int y=AnalyzerY.get(); if (y<0) y=screenHeight/10; if (y>screenHeight-100) y=screenHeight-100;*/

        // Put up a slash screen
        /*final JFrame frame = new JFrame("Alloy Analyzer");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        //        frame.pack(); // [HASLab]
        if (!Util.onMac() && !Util.onWindows()) {
            String gravity = System.getenv("_JAVA_AWT_WM_STATIC_GRAVITY");
            if (gravity==null || gravity.length()==0) {
                // many Window managers do not respect ICCCM2; this should help avoid the Title Bar being shifted "off screen"
                if (x<30) { if (x<0) x=0; width=width-(30-x);   x=30; }
                if (y<30) { if (y<0) y=0; height=height-(30-y); y=30; }
            }
            if (width<100) width=100;
            if (height<100) height=100;
        }
        frame.setSize(width,height);
        frame.setLocation(x,y);
        frame.setVisible(true);
        frame.setTitle("Alloy Analyzer (Electrum) "+Version.version()+" loading... please wait...");
        final int windowWidth = width;*/

        log.log("Creating VizGUI interface...");
        this.viz = new VizGUI(true, args[0], null);
        log.log("Showing VizGUI...");
        this.viz.doShowViz();
        // We intentionally call setVisible(true) first before settings the "please wait" title,
        // since we want the minimized window title on Linux/FreeBSD to just say Alloy Analyzer

        // Test the allowed memory sizes
        /*final WorkerEngine.WorkerCallback c = new WorkerEngine.WorkerCallback() {
            private final List<Integer> allowed = new ArrayList<Integer>();
            private final List<Integer> toTry = new ArrayList<Integer>(Arrays.asList(256,512,768,1024,1536,2048,2560,3072,3584,4096));
            private int mem;
            public synchronized void callback(Object msg) {
                if (toTry.size()==0) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() { SimpleGUI.this.frame=frame; SimpleGUI.this.finishInit(args, allowed, windowWidth); }
                    });
                    return;
                }
                try { mem=toTry.remove(0); WorkerEngine.stop(); WorkerEngine.run(dummyTask, mem, 128, "", "", this); return; } catch(IOException ex) { fail(); }
            }
            public synchronized void done() {
                //System.out.println("Alloy4 can use "+mem+"M"); System.out.flush();
                allowed.add(mem);
                callback(null);
            }
            public synchronized void fail() {
                //System.out.println("Alloy4 cannot use "+mem+"M"); System.out.flush();
                callback(null);
            }
        };
        c.callback(null);*/

        //////////////////// FINISH INIT ////////////////////////
        // Add the listeners
        /*try {
            wrap = true;
            //frame.addWindowListener(doQuit());
        } finally {
            wrap = false;
        }*/
        //frame.addComponentListener(this);

        // initialize the "allowed memory sizes" array
        /*allowedMemorySizes = new ArrayList<Integer>(initialAllowedMemorySizes);
        int newmem = SubMemory.get();
        if (!allowedMemorySizes.contains(newmem)) {
           int newmemlen = allowedMemorySizes.size();
           if (allowedMemorySizes.contains(768) || newmemlen==0)
              SubMemory.set(768); // a nice default value
           else
              SubMemory.set(allowedMemorySizes.get(newmemlen-1));
        }*/

        // Choose the appropriate font
        int fontSize=FontSize.get();
        String fontName=FontName.get();
        while(true) {
            if (!OurDialog.hasFont(fontName)) fontName="Lucida Grande"; else break;
            if (!OurDialog.hasFont(fontName)) fontName="Verdana"; else break;
            if (!OurDialog.hasFont(fontName)) fontName="Courier New"; else break;
            if (!OurDialog.hasFont(fontName)) fontName="Lucida Grande";
            break;
        }
        FontName.set(fontName);

        // Copy required files from the JAR
        //copyFromJAR();
        //final String binary = alloyHome() + fs + "binary";

        // Create the menu bar
        /*JMenuBar bar = new JMenuBar();
        try {
            wrap = true;
            filemenu    = menu(bar,  "&File",    doRefreshFile());
            editmenu    = menu(bar,  "&Edit",    doRefreshEdit());
            runmenu     = menu(bar,  "E&xecute", doRefreshRun());
            optmenu     = menu(bar,  "&Options", doRefreshOption());
            windowmenu  = menu(bar,  "&Window",  doRefreshWindow(false));
            windowmenu2 = menu(null, "&Window",  doRefreshWindow(true));
            helpmenu    = menu(bar,  "&Help",    null);
            if (!Util.onMac()) menuItem(helpmenu, "About Alloy...", 'A', doAbout());
            menuItem(helpmenu, "Quick Guide",                       'Q', doHelp());
            menuItem(helpmenu, "See the Copyright Notices...",      'L', doLicense());
        } finally {
            wrap = false;
        }*/

        // Pre-load the visualizer
        //viz = new VizGUI(false, "", windowmenu2, enumerator, evaluator);
        viz.doSetFontSize(FontSize.get());

        // Create the toolbar
        /*try {
            wrap = true;
            toolbar = new JToolBar();
            toolbar.setFloatable(false);
            if (!Util.onMac()) toolbar.setBackground(background);
            toolbar.add(OurUtil.button("New", "Starts a new blank model", "images/24_new.gif", doNew()));
            toolbar.add(OurUtil.button("Open", "Opens an existing model", "images/24_open.gif", doOpen()));
            toolbar.add(OurUtil.button("Reload", "Reload all the models from disk", "images/24_reload.gif", doReloadAll()));
            toolbar.add(OurUtil.button("Save", "Saves the current model", "images/24_save.gif", doSave()));
            toolbar.add(runbutton=OurUtil.button("Execute", "Executes the latest command", "images/24_execute.gif", doExecuteLatest()));
            toolbar.add(stopbutton=OurUtil.button("Stop", "Stops the current analysis", "images/24_execute_abort2.gif", doStop(2)));
            stopbutton.setVisible(false);
            toolbar.add(showbutton=OurUtil.button("Show", "Shows the latest instance", "images/24_graph.gif", doShowLatest()));
//            toolbar.add(untempbutton=OurUtil.button("Untemp", "Shows the latest instance", "images/24_graph.gif", doUntempLatest()));
                toolbar.add(Box.createHorizontalGlue());
            toolbar.setBorder(new OurBorder(false,false,false,false));
        } finally {
            wrap = false;
        }*/

        // Choose the antiAlias setting
        OurAntiAlias.enableAntiAlias(AntiAlias.get());

        // Create the message area
        /*logpane = OurUtil.scrollpane(null);
        log = new SwingLogPanel(logpane, fontName, fontSize, background, Color.BLACK, new Color(.7f,.2f,.2f), this);*/

        // Create the text area
        /*text = new OurTabbedSyntaxWidget(fontName, fontSize, TabSize.get());
        text.listeners.add(this);
        text.enableSyntax(! SyntaxDisabled.get());*/

        // Add everything to the frame, then display the frame
        /*Container all=frame.getContentPane();
        all.setLayout(new BorderLayout());
        all.removeAll();
        JPanel lefthalf=new JPanel();
        lefthalf.setLayout(new BorderLayout());
        lefthalf.add(toolbar, BorderLayout.NORTH);
        text.addTo(lefthalf, BorderLayout.CENTER);
        splitpane = OurUtil.splitpane(JSplitPane.HORIZONTAL_SPLIT, lefthalf, logpane, width/2);
        splitpane.setResizeWeight(0.5D);
        status = OurUtil.make(OurAntiAlias.label(" "), new Font(fontName, Font.PLAIN, fontSize), Color.BLACK, background);
        status.setBorder(new OurBorder(true,false,false,false));
        all.add(splitpane, BorderLayout.CENTER);
        all.add(status, BorderLayout.SOUTH);*/

        // Generate some informative log messages
        //log.logBold("Alloy Analyzer (Electrum) "+Version.version()+" (build date: "+Version.buildDate()+")\n\n");

        // If on Mac, then register an application listener
        /*try {
            wrap = true;
            if (Util.onMac()) MacUtil.registerApplicationListener(doShow(), doAbout(), doOpenFile(""), doQuit());
        } finally {
            wrap = false;
        }*/

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
            old.set(null,newarray);
        } catch (Throwable ex) { }

        // Testing the SAT solvers
        /*if (1==1) {
            satChoices = SatSolver.values().makeCopy();
//            String test1 = Subprocess.exec(20000, new String[]{binary+fs+"berkmin", binary+fs+"tmp.cnf"});
//            if (!isSat(test1)) satChoices.remove(SatSolver.BerkMinPIPE);
            satChoices.remove(SatSolver.BerkMinPIPE);
            String test2 = Subprocess.exec(20000, new String[]{binary+fs+"spear", "--model", "--dimacs", binary+fs+"tmp.cnf"});
            if (!isSat(test2)) satChoices.remove(SatSolver.SpearPIPE);
            if (!loadLibrary("minisat")) {
                log.logBold("Warning: JNI-based SAT solver does not work on this platform.\n");
                log.log("This is okay, since you can still use SAT4J as the solver.\n"+
                "For more information, please visit http://alloy.mit.edu/alloy4/\n");
                log.logDivider();
                log.flush();
                satChoices.remove(SatSolver.MiniSatJNI);
            }
            if (!loadLibrary("minisatprover")) satChoices.remove(SatSolver.MiniSatProverJNI);
            if (!loadLibrary("zchaff"))        satChoices.remove(SatSolver.ZChaffJNI);
            SatSolver now = SatSolver.get();
            if (!satChoices.contains(now)) {
                now=SatSolver.ZChaffJNI;
                if (!satChoices.contains(now)) now=SatSolver.SAT4J;
                now.set();
            }
            if (now==SatSolver.SAT4J && satChoices.size()>3 && satChoices.contains(SatSolver.CNF) && satChoices.contains(SatSolver.KK)) {
                log.logBold("Warning: Alloy4 defaults to SAT4J since it is pure Java and very reliable.\n");
                log.log("For faster performance, go to Options menu and try another solver like MiniSat.\n");
                log.log("If these native solvers fail on your computer, remember to change back to SAT4J.\n");
                log.logDivider();
                log.flush();
            }
        }*/

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

        // Refreshes all the menu items
        /*doRefreshFile(); OurUtil.enableAll(filemenu);
        doRefreshEdit(); OurUtil.enableAll(editmenu);
        doRefreshRun(); OurUtil.enableAll(runmenu);
        doRefreshOption();
        doRefreshWindow(false); OurUtil.enableAll(windowmenu);
        frame.setJMenuBar(bar);*/

        // Open the given file, if a filename is given in the command line
        /*for(String f:args) if (f.toLowerCase(Locale.US).endsWith(".als")) {
            File file = new File(f);
            if (file.exists() && file.isFile()) doOpenFile(file.getPath());
        }*/

        // Update the title and status bar
        //notifyChange();
        //text.get().requestFocusInWindow();

        // Launch the welcome screen if needed
        /*if (!"yes".equals(System.getProperty("debug")) && Welcome.get() < welcomeLevel) {
           JCheckBox again = new JCheckBox("Show this message every time you start the Alloy Analyzer");
           again.setSelected(true);
           OurDialog.showmsg("Welcome",
                 "Thank you for using the Alloy Analyzer "+Version.version(),
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
           doShow();
           if (!again.isSelected()) Welcome.set(welcomeLevel);
        }*/

        // Periodically ask the MailBug thread to see if there is a newer version or not
        final long now = System.currentTimeMillis();
        final Timer t = new Timer(800, null);
        t.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              int n = MailBug.latestBuildNumber();
              // If beyond 3 seconds, then we should stop because the log message may run into other user messages
              if (System.currentTimeMillis() - now >= 3000 || n <= Version.buildNumber()) { t.stop(); return; }
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
        if (args.length == 0) {
            System.err.println("Error: you must provide an input file to run ElectrumVisualizer");
            System.exit(-1);
        }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new VizMain(args); }
        });
    }
}



