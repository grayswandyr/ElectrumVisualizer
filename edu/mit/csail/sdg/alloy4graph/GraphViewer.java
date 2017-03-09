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
package edu.mit.csail.sdg.alloy4graph;

import de.erichseifert.vectorgraphics2d.EPSGraphics2D;
import static java.awt.Color.BLACK;
import static java.awt.Color.WHITE;
import static java.awt.event.InputEvent.BUTTON1_MASK;
import static java.awt.event.InputEvent.BUTTON3_MASK;
import static java.awt.event.InputEvent.CTRL_MASK;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JFrame;

import edu.mit.csail.sdg.alloy4.OurDialog;
import edu.mit.csail.sdg.alloy4.OurPDFWriter;
import edu.mit.csail.sdg.alloy4.OurPNGWriter;
import edu.mit.csail.sdg.alloy4.OurUtil;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4.OurBorder;

import java.util.HashMap;

//[N7-G.Dupont] Use of VectorGraphics2D for PDF export
import de.erichseifert.vectorgraphics2d.PDFGraphics2D;
import de.erichseifert.vectorgraphics2d.ProcessingPipeline;
import de.erichseifert.vectorgraphics2d.SVGGraphics2D;
import edu.mit.csail.sdg.alloy4.VectorialExporter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import edu.mit.csail.sdg.alloy4viz.AlloyAtom;
import edu.mit.csail.sdg.alloy4viz.AlloyInstance;
import edu.mit.csail.sdg.alloy4viz.AlloyRelation;
import edu.mit.csail.sdg.alloy4viz.AlloyTuple;
import edu.mit.csail.sdg.alloy4viz.StaticGraphMaker;
import edu.mit.csail.sdg.alloy4viz.VizState;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JCheckBoxMenuItem;

/**
 * This class displays the graph.
 *
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread.
 */
public final strictfp class GraphViewer extends JPanel {

    /**
     * This ensures the class can be serialized reliably.
     */
    private static final long serialVersionUID = 0;

    /**
     * The graph that we are displaying.
     */
    private final Graph graph;

    /**
     * The current amount of zoom.
     */
    private double scale = 1d;

    /**
     * The currently hovered GraphNode or GraphEdge or group, or null if there
     * is none.
     */
    private Object highlight = null;

    /**
     * The currently selected GraphNode or GraphEdge or group, or null if there
     * is none.
     */
    private Object selected = null;
    
    /**
     * [N7-G.Dupont] Hovered port.
     */
    private GraphPort hoveredPort = null;

    /**
     * The button that initialized the drag-and-drop; this value is undefined
     * when we're not currently doing drag-and-drop.
     */
    private int dragButton = 0;

    /**
     * The right-click context menu associated with this JPanel.
     */
    public final JPopupMenu pop = new JPopupMenu();

    /**
     * The VizState associated to the GraphViewer.
     */
    private VizState state;
    
    /**
     * Locates the node or edge at the given (X,Y) location.
     */
    private Object alloyFind(int mouseX, int mouseY) {
        return graph.find(scale, mouseX, mouseY);
    }

    /**
     * Returns the annotation for the node or edge at location x,y (or null if
     * none)
     */
    public Object alloyGetAnnotationAtXY(int mouseX, int mouseY) {
        Object obj = alloyFind(mouseX, mouseY);
        if (obj instanceof GraphNode) {
            return ((GraphNode) obj).uuid;
        }
        if (obj instanceof GraphEdge) {
            return ((GraphEdge) obj).uuid;
        }
        return null;
    }

    /**
     * Returns the annotation for the currently selected node/edge (or null if
     * none)
     */
    public Object alloyGetSelectedAnnotation() {
        if (selected instanceof GraphNode) {
            return ((GraphNode) selected).uuid;
        }
        if (selected instanceof GraphEdge) {
            return ((GraphEdge) selected).uuid;
        }
        // [N7-G.Dupont] Added port support
        if (highlight instanceof GraphPort) {
            return ((GraphPort) highlight).uuid;
        }
        return null;
    }

    /**
     * Returns the annotation for the currently highlighted node/edge (or null
     * if none)
     */
    public Object alloyGetHighlightedAnnotation() {
        if (highlight instanceof GraphNode) {
            return ((GraphNode) highlight).uuid;
        }
        if (highlight instanceof GraphEdge) {
            return ((GraphEdge) highlight).uuid;
        }
        // [N7-G.Dupont] Added port support
        if (highlight instanceof GraphPort) {
            return ((GraphPort) highlight).uuid;
        }
        return null;
    }

    /**
     * Stores the mouse positions needed to calculate drag-and-drop.
     */
    private int oldMouseX = 0, oldMouseY = 0, oldX = 0, oldY = 0;

    /**
     * Repaint this component.
     */
    public void alloyRepaint() {
        Container c = getParent();
        while (c != null) {
            if (c instanceof JViewport) {
                break;
            } else {
                c = c.getParent();
            }
        }
        setSize((int) (graph.getTotalWidth() * scale), (int) (graph.getTotalHeight() * scale));
        if (c != null) {
            c.invalidate();
            c.repaint();
            c.validate();
        } else {
            invalidate();
            repaint();
            validate();
        }
    }

    /**
     * Construct a GraphViewer that displays the given graph.
     * @param graph : The graph to display
     * @param instance : The instance of the model
     * @param view : The State of the model
     */
    public GraphViewer(final Graph graph, AlloyInstance instance, VizState view) {
        OurUtil.make(this, BLACK, WHITE, new EmptyBorder(0, 0, 0, 0));
        setBorder(null);
        this.scale = graph.defaultScale;
        this.graph = graph;
        this.state = view;
        
        StaticGraphMaker sgm = this.graph.sgm;
        
        graph.layout();
              
        /**
         * [N7] @Julien Richer @Louis Fauvarque
         * Add the edges linked to ports
         * 
         * WARNING : ArrayList becomes a reserved group type for Edges linked with ports
         */
        
        // Create the port edges
        ArrayList<AlloyRelation> portRelations = view.isPort.getKeysFromValue(true);
        Set<AlloyRelation> relations = instance.model.getRelations();
        
        Set<AlloyTuple> tupleSet = null;

        for (AlloyRelation rel : relations) {
            if (portRelations.contains(rel))
                continue;
            tupleSet = instance.relation2tuples(rel);

            for (AlloyTuple tuple : tupleSet) {
                AlloyAtom start = tuple.getStart();
                AlloyAtom end = tuple.getEnd();
                String uuid = "Port[" + tuple + "]";

                // [N7-G.Dupont] Add a proper label to the arcs
                String label = view.label.get(rel);
                if (tuple.getArity() > 2) {
                    StringBuilder moreLabel = new StringBuilder();
                    List<AlloyAtom> atoms = tuple.getAtoms();
                    for (int i = 1; i < atoms.size() - 1; i++) {
                        if (i > 1) {
                            moreLabel.append(", ");
                        }
                        moreLabel.append(sgm.atomname(atoms.get(i), false));
                    }
                    if (label.length() == 0) { 
                        //label=moreLabel.toString();
                    } else {
                        label = label + (" [" + moreLabel + "]");
                    }
                }

                // Build edges between ports
                List<AbstractGraphNode> startgn = null, endgn = null;
                // From port to port
                if (sgm.isPort(portRelations, start) && sgm.isPort(portRelations, end)) {
                    startgn = sgm.getPortsFromAtom(start);
                    endgn = sgm.getPortsFromAtom(end);
                }
                // From node to port
                else if (!sgm.isPort(portRelations, start) && sgm.isPort(portRelations, end)){
                    startgn = sgm.getNodesFromAtom(start);
                    endgn = sgm.getPortsFromAtom(end);
                }
                // From port to node
                else if (sgm.isPort(portRelations, start) && !sgm.isPort(portRelations, end)){
                    startgn = sgm.getPortsFromAtom(start);
                    endgn = sgm.getNodesFromAtom(end);
                }

                boolean sameGraph = false;
                //If there is one start and one end in a same graph, we shall not draw edges between different graphs.
                if (startgn == null || endgn == null)
                    continue;
                for (AbstractGraphNode sgn : startgn) {
                    for (AbstractGraphNode egn : endgn) {
                        if (egn.graph == sgn.graph){
                            sameGraph = true;
                            break;
                        }
                    }
                }

                for (AbstractGraphNode sgn : startgn){
                    for (AbstractGraphNode egn : endgn){
                        if (sgn == null || egn == null || (sameGraph && sgn.graph != egn.graph))
                            continue;
                        
                        if(Math.abs(sgn.layer() - egn.layer()) <= 1){
                            GraphEdge e = new GraphEdge(sgn, egn, graph, uuid, label, rel);                        
                            e.setStyle(view.edgeStyle.resolve(rel));

                            DotColor color = view.edgeColor.resolve(rel);
                            e.setColor(color.getColor(view.getEdgePalette()));

                            e.resetPath();
                            e.layout_arrowHead();
                        } else {
                            for (AbstractGraphNode n : graph.nodes) {
                                // We go through all the dummy nodes
                                if (n.shape() != null){
                                    continue;
                                }
                                Object group = n.ins.get(0).group;
                                // If the group is an arraylist, then it is a dummy node linked to a port
                                if (!(group instanceof ArrayList)){
                                    continue;
                                }
                                ArrayList<AbstractGraphNode> groupN = (ArrayList<AbstractGraphNode>) group;
                                if (groupN.get(0) == sgn && groupN.get(1) == egn) {
                                    GraphEdge e;
                                    if (n.ins.get(0).a().shape() == null) {
                                        e = new GraphEdge(n.ins.get(0).a(), n, graph, uuid, label, rel);
                                    } else {
                                        e = new GraphEdge(sgn, n, graph, uuid, label, rel);
                                    }
                                    e.setStyle(view.edgeStyle.resolve(rel));

                                    DotColor color = view.edgeColor.resolve(rel);
                                    e.setColor(color.getColor(view.getEdgePalette()));

                                    e.resetPath();
                                    e.layout_arrowHead();
                                    n.ins.get(0).a().outs.remove(n.ins.get(0));
                                    n.ins.remove(n.ins.get(0));

                                    if (n.outs.get(0).b().shape() != null) {
                                        GraphEdge elast = new GraphEdge(n, egn, graph, uuid, label, rel);
                                        elast.setStyle(view.edgeStyle.resolve(rel));

                                        DotColor colorlast = view.edgeColor.resolve(rel);
                                        elast.setColor(colorlast.getColor(view.getEdgePalette()));

                                        elast.resetPath();
                                        elast.layout_arrowHead();
                                        n.outs.get(0).b().ins.remove(n.outs.get(0));
                                        n.outs.remove(n.outs.get(0));
                                    }
                                }
                            } //<for(n : graph.nodes)>
                        } // [else]<Math.abs(sgn.layer() - egn.layer()) <= 1>
                    } //<for(egn : endgn)>
                } //<for(sgn : startgn)>
            } //<for(tuple : tupleSet)>
        }
        
        // GUI related
        final JMenuItem zoomIn = new JMenuItem("Zoom In");
        final JMenuItem zoomOut = new JMenuItem("Zoom Out");
        final JMenuItem zoomToFit = new JMenuItem("Zoom to Fit");
        final JMenuItem print = new JMenuItem("Export to PNG");
        final JMenuItem printVect = new JMenuItem("Export as vectorial image..."); // [N7-G. Dupont]

        pop.add(zoomIn);
        pop.add(zoomOut);
        pop.add(zoomToFit);
        pop.addSeparator();
        pop.add(print);
        pop.add(printVect);
        
        ActionListener act = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Container c = getParent();
                while (c != null) {
                    if (c instanceof JViewport) {
                        break;
                    } else {
                        c = c.getParent();
                    }
                }
                if (e.getSource() == print) {
                    alloySaveAs();
                }
                if (e.getSource() == zoomIn) {
                    scale = scale * 1.33d;
                    if (!(scale < 500d)) {
                        scale = 500d;
                    }
                }
                if (e.getSource() == zoomOut) {
                    scale = scale / 1.33d;
                    if (!(scale > 0.1d)) {
                        scale = 0.1d;
                    }
                }
                if (e.getSource() == zoomToFit) {
                    if (c == null) {
                        return;
                    }
                    int w = c.getWidth() - 15, h = c.getHeight() - 15; // 15 gives a comfortable round-off margin
                    if (w <= 0 || h <= 0) {
                        return;
                    }
                    double scale1 = ((double) w) / graph.getTotalWidth(), scale2 = ((double) h) / graph.getTotalHeight();
                    if (scale1 < scale2) {
                        scale = scale1;
                    } else {
                        scale = scale2;
                    }
                }
                // [N7-G.Dupont]
                if (e.getSource() == printVect) {
                    alloySaveVectorWindow();
                }
                alloyRepaint();
            }
        };
        zoomIn.addActionListener(act);
        zoomOut.addActionListener(act);
        zoomToFit.addActionListener(act);
        print.addActionListener(act);
        printVect.addActionListener(act); //[N7-G.Dupont]
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent ev) {
                if (pop.isVisible()) {
                    return;
                }
                
                // [N7-G.Dupont]
                Object obj = alloyFind(ev.getX(), ev.getY());
                boolean needRepaint = false;
                
                if (hoveredPort != null && obj != hoveredPort) {
                    hoveredPort.setHovered(false);
                    hoveredPort = null;
                    needRepaint = true;
                }
                
                if (highlight != null && obj != highlight) {
                    if (highlight instanceof AbstractGraphElement)
                        ((AbstractGraphElement)highlight).setHighlight(false);
                    highlight = null;
                    needRepaint = true;
                }
                
                if (obj instanceof GraphPort) {
                    hoveredPort = (GraphPort)obj;
                    hoveredPort.setHovered(true);
                    needRepaint = true;
                }
                
                if (obj != null) {
                    if (highlight != null && highlight instanceof AbstractGraphElement)
                        ((AbstractGraphElement)highlight).setHighlight(false); //[N7-G.Dupont]
                    highlight = obj;
                    if (highlight instanceof AbstractGraphElement)
                        ((AbstractGraphElement)highlight).setHighlight(true); //[N7-G.Dupont]
                    
                    needRepaint = true;
                }
                 
                if (needRepaint)
                    alloyRepaint();
            }

            @Override
            public void mouseDragged(MouseEvent ev) {
                if (selected instanceof GraphNode && dragButton == 1) {
                    int newX = (int) (oldX + (ev.getX() - oldMouseX) / scale);
                    int newY = (int) (oldY + (ev.getY() - oldMouseY) / scale);
                    GraphNode n = (GraphNode) selected;

                    n.setHighlight(true);
                    if (n.getFather() != null){ //If the selected node is in a one node subgraph, we do not allow moving it.
                      if (n.getFather().getChildren().size() <= 1)
                        return;
                    }

                    if (n.x() != newX || n.y() != newY) {
                        n.tweak(newX, newY);
                        alloyRepaint();
                        scrollRectToVisible(new Rectangle(
                                (int) ((newX - graph.getLeft()) * scale) - n.getWidth() / 2 - 5,
                                (int) ((newY - graph.getTop()) * scale) - n.getHeight() / 2 - 5,
                                n.getWidth() + n.getReserved() + 10, n.getHeight() + 10
                        ));
                    }
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent ev) {
                Object obj = alloyFind(ev.getX(), ev.getY());
                graph.recalcBound(true);
                selected = null;
                highlight = obj;
                if (highlight instanceof AbstractGraphElement)
                    ((AbstractGraphElement)highlight).setHighlight(true); //[N7-G.Dupont]
                alloyRepaint();
            }

            long timeLastClick = 0;

            @Override
            public void mousePressed(MouseEvent ev) {
                dragButton = 0;
                int mod = ev.getModifiers();
                if ((mod & BUTTON3_MASK) != 0) {
                    // Right button clicked
                    selected = alloyFind(ev.getX(), ev.getY());
                    if (highlight instanceof AbstractGraphElement) ((AbstractGraphElement)highlight).setHighlight(false); //[N7-G.Dupont]
                    highlight = null;
                    alloyRepaint();
                    pop.show(GraphViewer.this, ev.getX(), ev.getY());
                } else if ((mod & BUTTON1_MASK) != 0 && (mod & CTRL_MASK) != 0) {
                    // Left button clicked + Ctrl key modifier
                    // This lets Ctrl+LeftClick bring up the popup menu, just like RightClick,
                    // since many Mac mouses do not have a right button.
                    selected = alloyFind(ev.getX(), ev.getY());
                    if (highlight instanceof AbstractGraphElement)  ((AbstractGraphElement)highlight).setHighlight(false); //[N7-G.Dupont]
                    highlight = null;
                    alloyRepaint();
                    pop.show(GraphViewer.this, ev.getX(), ev.getY());
                } else if ((mod & BUTTON1_MASK) != 0) {
                    dragButton = 1;
                    selected = alloyFind(oldMouseX = ev.getX(), oldMouseY = ev.getY());
                    highlight = null;
                    alloyRepaint();
                    if (selected instanceof GraphNode) {
                        GraphNode sel = (GraphNode) selected;
                        oldX = sel.x();
                        oldY = sel.y();
                        sel.setHighlight(true);
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - timeLastClick < 800) {
                            //Double click on a node, we have to show the subgraph if there is one.
                            if (sel.hasChild()) {
                                //The node double-clicked has children we have to print the subgraph in a new window.
                                showSubgraph(sel);
                            }
                        }
                        timeLastClick = System.currentTimeMillis();
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent ev) {
                if (highlight != null) {
                    if (highlight instanceof AbstractGraphElement)
                        ((AbstractGraphElement)highlight).setHighlight(false); //[N7-G.Dupont]
                    highlight = null;
                    alloyRepaint();
                }
            }
        });
    }

    /**
     * Displays the subgraph of the given GraphNode in a new window.
     */
    private void showSubgraph(GraphNode node) {
        JFrame windowSubgraph = new JFrame(node.uuid.toString());
        int x = 200;
        int y = 200;
        //We have to duplicate the subgraph (each node and edge) so moving nodes in the window won't move those of the main graph.
        Graph toBeShownGraph = new Graph(node.getSubGraph().defaultScale, graph.sgm);
        //A mapping between original nodes and copies.
        HashMap<GraphNode, GraphNode> dupl = duplicateSubnodes(toBeShownGraph, node);
        //We also have to 'duplicate' the edges of every subnodes.
        for (GraphNode n : dupl.keySet()){
            // For each child-node, we check every edge from this node.
            for (GraphEdge e : n.outs) {
                //If the 'to' node of the edge is also in the subgraph, we have to duplicate the edge.
                if (dupl.containsKey(e.getB())) {
                    GraphNode copyN = dupl.get(n);
                    GraphNode copyB = dupl.get(e.getB());
                    if (!(copyN == null || copyB == null)){ //This should always be true.
                        e = new GraphEdge(e, copyN, copyB);
                    }
                }
            }
            // We don't need to check the edges of ins since checking every out of every node already covers every possible edge of the subgraph.
        }
        toBeShownGraph.layout();
        int width = toBeShownGraph.getTotalWidth() + 25;
        int height = toBeShownGraph.getTotalHeight() + 100;
        //Create the graphviewer in a scroll panel. 
        JScrollPane diagramScrollPanel;
        GraphViewer view = new GraphViewer(toBeShownGraph, getGraph().instance, state);
        diagramScrollPanel = OurUtil.scrollpane(view, new OurBorder(true, true, true, false));
        diagramScrollPanel.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                diagramScrollPanel.invalidate();
                diagramScrollPanel.repaint();
                diagramScrollPanel.validate();
            }
        });
        diagramScrollPanel.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            public void adjustmentValueChanged(AdjustmentEvent e) {
                diagramScrollPanel.invalidate();
                diagramScrollPanel.repaint();
                diagramScrollPanel.validate();
            }
        });
        //We show this scroll panel in the window.
        windowSubgraph.setContentPane(diagramScrollPanel);
        windowSubgraph.setBackground(Color.WHITE);
        windowSubgraph.setSize(width, height);
        windowSubgraph.setLocation(x, y);
        windowSubgraph.setVisible(true);
    }

    /**
     * This method duplicates all subnodes of the given node, recursively to get greatchild.
     * @return a map mapping old nodes to new ones. 
     */
    private HashMap<GraphNode, GraphNode> duplicateSubnodes(Graph toBeShownGraph, GraphNode node){
      HashMap<GraphNode, GraphNode> map = new HashMap<GraphNode, GraphNode>();
      if (node.getChildren().isEmpty()) return map; //If the given node has no children, we return an empty list (this should never happen).
      for (AbstractGraphNode an : node.getChildren()){ //We duplicate each node and add them to the map.
        if (an instanceof GraphNode){
            GraphNode n = (GraphNode)an;
            GraphNode d = new GraphNode(n, toBeShownGraph);
            map.put(n, d);
            if (!(n.getChildren().isEmpty())){ //If the node we are duplicating have children, we dupliacte them too and add them to map.
              HashMap<GraphNode, GraphNode> m = duplicateSubnodes(d.getSubGraph(), n);
              for (AbstractGraphNode child : n.getChildren()){ //We have to have to precise the father of these duplicated child.
                GraphNode duplicatedChild = m.get(child);
                if (!(duplicatedChild == null)) {
                  duplicatedChild.setFather(d);
                  d.addChild(duplicatedChild);
                }
              }
              map.putAll(m);
            }
        }
      }
      return map;
    }

    /**
     * This color is used as the background for a JTextField that contains bad
     * data.
     * <p>
     * Note: we intentionally choose to make it an instance field rather than a
     * static field, since we want to make sure we only instantiate it from the
     * AWT Event Dispatching thread.
     */
    private final Color badColor = new Color(255, 200, 200);

    /**
     * This synchronized field stores the most recent DPI value.
     */
    private static volatile double oldDPI = 72;

    /**
     * True if we are currently in the middle of a DocumentListener already.
     */
    private boolean recursive = false;

    /**
     * This updates the three input boxes and the three accompanying text
     * labels, then return the width in pixels.
     */
    private int alloyRefresh(int who, double ratio, JTextField w1, JLabel w2, JTextField h1, JLabel h2, JTextField d1, JLabel d2, JLabel msg) {
        if (recursive) {
            return 0;
        }
        try {
            recursive = true;
            w1.setBackground(WHITE);
            h1.setBackground(WHITE);
            d1.setBackground(WHITE);
            boolean bad = false;
            double w;
            try {
                w = Double.parseDouble(w1.getText());
            } catch (NumberFormatException ex) {
                w = 0;
            }
            double h;
            try {
                h = Double.parseDouble(h1.getText());
            } catch (NumberFormatException ex) {
                h = 0;
            }
            double d;
            try {
                d = Double.parseDouble(d1.getText());
            } catch (NumberFormatException ex) {
                d = 0;
            }
            if (who == 1) {
                h = ((int) (w * 100 / ratio)) / 100D;
                h1.setText("" + h);
            } // Maintains aspect ratio
            if (who == 2) {
                w = ((int) (h * 100 * ratio)) / 100D;
                w1.setText("" + w);
            } // Maintains aspect ratio
            if (!(d >= 0.01) || !(d <= 10000)) {
                bad = true;
                d1.setBackground(badColor);
                msg.setText("DPI must be between 0.01 and 10000");
            }
            if (!(h >= 0.01) || !(h <= 10000)) {
                bad = true;
                h1.setBackground(badColor);
                msg.setText("Height must be between 0.01 and 10000");
                if (who == 1) {
                    h1.setText("");
                }
            }
            if (!(w >= 0.01) || !(w <= 10000)) {
                bad = true;
                w1.setBackground(badColor);
                msg.setText("Width must be between 0.01 and 10000");
                if (who == 2) {
                    w1.setText("");
                }
            }
            if (bad) {
                w2.setText(" inches");
                h2.setText(" inches");
                return 0;
            } else {
                msg.setText(" ");
            }
            w2.setText(" inches (" + (int) (w * d) + " pixels)");
            h2.setText(" inches (" + (int) (h * d) + " pixels)");
            return (int) (w * d);
        } finally {
            recursive = false;
        }
    }

    /**
     * Export the current drawing as a PNG or PDF file by asking the user for
     * the filename and the image resolution.
     */
    public void alloySaveAs() {
        // Figure out the initial width, height, and DPI that we might want to suggest to the user
        final double ratio = ((double) (graph.getTotalWidth())) / graph.getTotalHeight();
        double dpi, iw = 8.5D, ih = ((int) (iw * 100 / ratio)) / 100D;    // First set the width to be 8.5inch and compute height accordingly
        if (ih > 11D) {
            ih = 11D;
            iw = ((int) (ih * 100 * ratio)) / 100D;
        } // If too tall, then set height=11inch, and compute width accordingly
        synchronized (GraphViewer.class) {
            dpi = oldDPI;
        }
        // Prepare the dialog box
        final JLabel msg = OurUtil.label(" ", Color.RED);
        final JLabel w = OurUtil.label("Width: " + ((int) (graph.getTotalWidth() * scale)) + " pixels");
        final JLabel h = OurUtil.label("Height: " + ((int) (graph.getTotalHeight() * scale)) + " pixels");
        final JTextField w1 = new JTextField("" + iw);
        final JLabel w0 = OurUtil.label("Width: "), w2 = OurUtil.label("");
        final JTextField h1 = new JTextField("" + ih);
        final JLabel h0 = OurUtil.label("Height: "), h2 = OurUtil.label("");
        final JTextField d1 = new JTextField("" + (int) dpi);
        final JLabel d0 = OurUtil.label("Resolution: "), d2 = OurUtil.label(" dots per inch");
        final JTextField dp1 = new JTextField("" + (int) dpi);
        final JLabel dp0 = OurUtil.label("Resolution: "), dp2 = OurUtil.label(" dots per inch");
        alloyRefresh(0, ratio, w1, w2, h1, h2, d1, d2, msg);
        Dimension dim = new Dimension(100, 20);
        w1.setMaximumSize(dim);
        w1.setPreferredSize(dim);
        w1.setEnabled(false);
        h1.setMaximumSize(dim);
        h1.setPreferredSize(dim);
        h1.setEnabled(false);
        d1.setMaximumSize(dim);
        d1.setPreferredSize(dim);
        d1.setEnabled(false);
        dp1.setMaximumSize(dim);
        dp1.setPreferredSize(dim);
        dp1.setEnabled(false);
        w1.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                alloyRefresh(1, ratio, w1, w2, h1, h2, d1, d2, msg);
            }

            public void insertUpdate(DocumentEvent e) {
                changedUpdate(null);
            }

            public void removeUpdate(DocumentEvent e) {
                changedUpdate(null);
            }
        });
        h1.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                alloyRefresh(2, ratio, w1, w2, h1, h2, d1, d2, msg);
            }

            public void insertUpdate(DocumentEvent e) {
                changedUpdate(null);
            }

            public void removeUpdate(DocumentEvent e) {
                changedUpdate(null);
            }
        });
        d1.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                alloyRefresh(3, ratio, w1, w2, h1, h2, d1, d2, msg);
            }

            public void insertUpdate(DocumentEvent e) {
                changedUpdate(null);
            }

            public void removeUpdate(DocumentEvent e) {
                changedUpdate(null);
            }
        });
        final JRadioButton b1 = new JRadioButton("As a PNG with the window's current magnification:", true);
        final JRadioButton b2 = new JRadioButton("As a PNG with a specific width, height, and resolution:", false);
        //final JRadioButton b3 = new JRadioButton("As a PDF with the given resolution:", false);
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b2.setSelected(false); //b3.setSelected(false);
                if (!b1.isSelected()) {
                    b1.setSelected(true);
                }
                w1.setEnabled(false);
                h1.setEnabled(false);
                d1.setEnabled(false);
                dp1.setEnabled(false);
                msg.setText(" ");
                w1.setBackground(WHITE);
                h1.setBackground(WHITE);
                d1.setBackground(WHITE);
            }
        });
        b2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                b1.setSelected(false); //b3.setSelected(false);
                if (!b2.isSelected()) {
                    b2.setSelected(true);
                }
                w1.setEnabled(true);
                h1.setEnabled(true);
                d1.setEnabled(true);
                dp1.setEnabled(false);
                msg.setText(" ");
                alloyRefresh(1, ratio, w1, w2, h1, h2, d1, d2, msg);
            }
        });
        // Ask whether the user wants to change the width, height, and DPI
        double myScale;
        while (true) {
            if (!OurDialog.getInput("Export as PNG", new Object[]{
                b1, OurUtil.makeH(20, w, null), OurUtil.makeH(20, h, null), " ",
                b2, OurUtil.makeH(20, w0, w1, w2, null),
                OurUtil.makeH(20, h0, h1, h2, null),
                OurUtil.makeH(20, d0, d1, d2, null),
                OurUtil.makeH(20, msg, null), //b3, OurUtil.makeH(20, dp0, dp1, dp2, null)
            })) {
                return;
            }
            // Let's validate the values
            if (b2.isSelected()) {
                int widthInPixel = alloyRefresh(3, ratio, w1, w2, h1, h2, d1, d2, msg);
                String err = msg.getText().trim();
                if (err.length() > 0) {
                    continue;
                }
                dpi = Double.parseDouble(d1.getText());
                myScale = ((double) widthInPixel) / graph.getTotalWidth();
                int heightInPixel = (int) (graph.getTotalHeight() * myScale);
                if (widthInPixel > 4000 || heightInPixel > 4000) {
                    if (!OurDialog.yesno("The image dimension (" + widthInPixel + "x" + heightInPixel + ") is very large. Are you sure?")) {
                        continue;
                    }
                }
            } else {
                dpi = 72;
                myScale = scale;
            }
            break;
        }
        // Ask the user for a filename
        File filename;
        filename = OurDialog.askFile(false, null, ".png", "PNG file");
        if (filename == null) {
            return;
        }
        if (filename.exists() && !OurDialog.askOverwrite(filename.getAbsolutePath())) {
            return;
        }
        // Attempt to write the PNG or PDF file
        try {
            System.gc(); // Try to avoid possible premature out-of-memory exceptions
            alloySaveAsPNG(filename.getAbsolutePath(), myScale, dpi, dpi);
            synchronized (GraphViewer.class) {
                oldDPI = dpi;
            }
            Util.setCurrentDirectory(filename.getParentFile());
        } catch (Throwable ex) {
            OurDialog.alert("An error has occured in writing the output file:\n" + ex);
        }
    }

    private void alloySaveVectorWindow() {
        VectorialExporter.asModalDialog(SwingUtilities.getWindowAncestor(this), "Vectorial export", new VectorialExporter.ExportCallback() {
            @Override
            public void exportAction(
                    String filename,
                    VectorialExporter.OutputFormat format,
                    Integer width, Integer height,
                    Integer marginleft, Integer marginright,
                    Integer margintop, Integer marginbottom) {
                //
                alloySaveAsVector(filename, format, width, height, marginleft, marginright, margintop, marginbottom);
            }
        });
    }

    /**
     * Export the current drawing as a PDF file with the given image resolution.
     * [N7-G.Dupont] Export with an external library, more flexible and easier
     * to use.
     */
    public void alloySaveAsVector(String filename, VectorialExporter.OutputFormat format, int paperW, int paperH, int marginLeft, int marginRight, int marginTop, int marginBottom) {
        // Parameters of the graphics
        double graphW = graph.getTotalWidth(), graphH = graph.getTotalHeight();
        double contentW = (double) (paperW - marginLeft - marginRight),
                contentH = (double) (paperH - marginTop - marginBottom);
        final double nscale;

        if (graphW > graphH) // We want graphW = contentW
        {
            nscale = contentW / graphW;
        } else {
            nscale = contentH / graphH;
        }

        OurDialog.waitFor(
                SwingUtilities.getWindowAncestor(this),
                "Exporting to " + format.name(), "Exporting graph to " + format.name() + "...", "Export finished !",
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() {
                        ProcessingPipeline g = null;

                        switch (format) {
                            case PDF:
                                g = new PDFGraphics2D(0.0, 0.0, paperW, paperH);
                                break;
                            case SVG:
                                g = new SVGGraphics2D(0.0, 0.0, paperW, paperH);
                                break;
                            case EPS:
                                g = new EPSGraphics2D(0.0, 0.0, paperW, paperH);
                                break;
                            default:
                            // nop
                        }

                        g.translate(marginLeft, marginRight);
                        g.scale(nscale, nscale);
                        graph.draw(new Artist(g), nscale, null, false);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(filename);
                            fos.write(g.getBytes());
                        } catch (FileNotFoundException ex) {
                            OurDialog.alert("Cannot write to file " + filename + ": file not found (" + ex.getMessage() + ")");
                        } catch (SecurityException ex) {
                            OurDialog.alert("Cannot write to file " + filename + ": security issue (" + ex.getMessage() + ")");
                        } catch (IOException ex) {
                            OurDialog.alert("Cannot write to file " + filename + ": " + ex.getMessage());
                        } finally {
                            if (fos != null) {
                                try {
                                    fos.close();
                                } catch (IOException ex) {
                                    OurDialog.alert("Error while closing file " + filename + ": " + ex.getMessage());
                                }
                            }
                        }
                        return null;
                    }
                }
        );
    }

    /**
     * Export the current drawing as a PNG file with the given file name and
     * image resolution.
     */
    public void alloySaveAsPNG(String filename, double scale, double dpiX, double dpiY) throws IOException {
        try {
            int width = (int) (graph.getTotalWidth() * scale);
            if (width < 10) {
                width = 10;
            }
            int height = (int) (graph.getTotalHeight() * scale);
            if (height < 10) {
                height = 10;
            }
            BufferedImage bf = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D gr = (Graphics2D) (bf.getGraphics());
            gr.setColor(WHITE);
            gr.fillRect(0, 0, width, height);
            gr.setColor(BLACK);
            gr.scale(scale, scale);
            gr.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graph.draw(new Artist(gr), scale, null, false);
            OurPNGWriter.writePNG(bf, filename, dpiX, dpiY);
        } catch (Throwable ex) {
            if (ex instanceof IOException) {
                throw (IOException) ex;
            }
            throw new IOException("Failure writing the PNG file to " + filename + " (" + ex + ")");
        }
    }

    /**
     * Show the popup menu at location (x,y)
     */
    public void alloyPopup(Component c, int x, int y) {
        pop.show(c, x, y);
    }

    /**
     * Returns a DOT representation of the current graph.
     */
    @Override
    public String toString() {
        return graph.toString();
    }

    /**
     * Returns the preferred size of this component.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) (graph.getTotalWidth() * scale), (int) (graph.getTotalHeight() * scale));
    }

    /**
     * This method is called by Swing to draw this component.
     */
    @Override
    public void paintComponent(final Graphics gr) {
        super.paintComponent(gr);
        Graphics2D g2 = (Graphics2D) gr;
        AffineTransform oldAF = (AffineTransform) (g2.getTransform().clone());
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.scale(scale, scale);
        Object sel = (selected != null ? selected : highlight);
        AbstractGraphNode c = null;
        if (sel instanceof AbstractGraphNode && ((AbstractGraphNode) sel).shape() == null) {
            c = (AbstractGraphNode) sel;
            sel = c.ins.get(0);
        }
        graph.draw(new Artist(g2), scale, sel, true);
        if (c != null) {
            gr.setColor(((GraphEdge) sel).getColor());
            gr.fillArc(c.x() - 5 - graph.getLeft(), c.y() - 5 - graph.getTop(), 10, 10, 0, 360);
        }
        g2.setTransform(oldAF);
    }
    
    /**
     * [N7-G.Dupont] Returns the graph.
     */
    public Graph getGraph() {
        return this.graph;
    }
}
