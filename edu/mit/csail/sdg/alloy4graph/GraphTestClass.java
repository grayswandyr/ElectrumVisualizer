/**
 * Alloy Analyzer 4 -- Copyright (c) 2006-2009, Felix Chang
 * .
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
 * 
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
