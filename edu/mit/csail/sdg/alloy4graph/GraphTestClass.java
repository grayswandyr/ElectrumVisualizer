/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.csail.sdg.alloy4graph;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 *
 * @author gdupont
 */
public class GraphTestClass extends JFrame {
    public GraphTestClass(String[] args) {
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        
        // Create graph
        Graph graph = new Graph(1.0);
        
        GraphNode nodeA = new GraphNode(graph, new Integer(1), "Test");
        GraphPort portA = new GraphPort(nodeA, new Integer(2), "Port", 0, GraphPort.Orientation.South);
        
        // Show graph on a graphviewer component
        JPanel graphpan = new GraphViewer(graph);
        this.add(graphpan);
        this.setSize(200, 200);
        this.setVisible(true);
        graphpan.updateUI();
    }
    
    public static void main(final String[] args) throws Exception {
        /*if (args.length == 0) {
            System.err.println("Error: you must provide an input file to run ElectrumVisualizer");
            System.exit(-1);
        }*/
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new GraphTestClass(args); }
        });
    }
}
