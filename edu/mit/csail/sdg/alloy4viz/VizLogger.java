package edu.mit.csail.sdg.alloy4viz;

public class VizLogger {
    private String tag = "";

    public VizLogger(String tag) {
        this.tag = tag;
    }

    public void log(String msg) {
        System.out.println(
            (tag.length() == 0 ? "" : "[" + tag + "] ") +
            msg
        );
    }

    public void logBold(String msg) {
        this.log(msg);
    }
}


