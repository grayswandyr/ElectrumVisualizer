/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.csail.sdg.alloy4graph;

import java.awt.BorderLayout;
import java.awt.Color;
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
        
        GraphPort portA = new GraphPort(nodeA, new Integer(2), "Port A", 0, GraphPort.Orientation.North);
        portA.setColor(Color.RED);
        portA.setShape(DotShape.CIRCLE);
        
        GraphPort portA2 = new GraphPort(nodeA, new Integer(6), "Port A2", 1, GraphPort.Orientation.North);
        portA2.setColor(Color.PINK);
        portA2.setShape(DotShape.CIRCLE);
        
        GraphPort portB = new GraphPort(nodeA, new Integer(3), "Port B", 0, GraphPort.Orientation.South);
        portB.setColor(Color.GREEN);
        
        GraphPort portC = new GraphPort(nodeA, new Integer(4), "Port C", 0, GraphPort.Orientation.East);
        portC.setColor(Color.BLUE);
        
        GraphPort portD = new GraphPort(nodeA, new Integer(5), "Port D", 0, GraphPort.Orientation.West);
        portD.setColor(Color.ORANGE);
        
        // Show graph on a graphviewer component
        JPanel graphpan = new GraphViewer(graph);
        this.setLayout(new BorderLayout());
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
