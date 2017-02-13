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

import static edu.mit.csail.sdg.alloy4graph.Artist.getBounds;
import static edu.mit.csail.sdg.alloy4graph.Graph.esc;
import static edu.mit.csail.sdg.alloy4graph.Graph.selfLoopA;
import static edu.mit.csail.sdg.alloy4graph.Graph.selfLoopGL;
import static edu.mit.csail.sdg.alloy4graph.Graph.selfLoopGR;
import static java.lang.StrictMath.round;
import static java.lang.StrictMath.sqrt;

import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;

/**
 * Mutable; represents a graphical node.
 *
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread.
 */
// [N7-G.Dupont] Added superclass AbstractGraphNode
public strictfp class GraphNode extends AbstractGraphNode {

    // =============================== adjustable options ==================================================
    /**
     * This determines the minimum width of a dummy node.
     */
    private static final int dummyWidth = 30;

    /**
     * This determines the minimum height of a dummy node.
     */
    private static final int dummyHeight = 10;

    /**
     * This determines the minimum amount of padding added above, left, right,
     * and below the text label.
     */
    private static final int labelPadding = 5;

    /**
     * Color to use to show a highlighted node.
     */
    private static final Color COLOR_CHOSENNODE = Color.LIGHT_GRAY;

    /**
     * Minimum horizontal distance between adjacent nodes of the subGraph.
     */
    static final int xJumpNode = 20;

    /**
     * Minimum horizontal distance between adjacent layers of the subGraph.
     */
    static final int yJumpNode = 20;

    // =============================== cached for performance ===================================
    /**
     * The maximum ascent and descent. We deliberately do NOT make this field
     * "static" because only AWT thread can call Artist.
     */
    private final int ad = Artist.getMaxAscentAndDescent();

    /**
     * Caches the value of sqrt(3.0). The extra digits in the definition will be
     * truncated by the Java compiler.
     */
    protected static final double sqrt3 = 1.7320508075688772935274463415058723669428052538103806280558D;

    /**
     * Caches the value of sin(36 degree). The extra digits in the definition
     * will be truncated by the Java compiler.
     */
    protected static final double sin36 = 0.5877852522924731291687059546390727685976524376431459910723D;

    /**
     * Caches the value of cos(36 degree). The extra digits in the definition
     * will be truncated by the Java compiler.
     */
    protected static final double cos36 = 0.8090169943749474241022934171828190588601545899028814310677D;

    /**
     * Caches the value of cos(18 degree). The extra digits in the definition
     * will be truncated by the Java compiler.
     */
    protected static final double cos18 = 0.9510565162951535721164393333793821434056986341257502224473D;

    /**
     * Caches the value of tan(18 degree). The extra digits in the definition
     * will be truncated by the Java compiler.
     */
    protected static final double tan18 = 0.3249196962329063261558714122151344649549034715214751003078D;

    // =============================== these fields do not affect the computed bounds ===============================================
    /**
     * The layer that this node is in; must stay in sync with Graph.layerlist
     */
    private int layer = 0;

    /**
     * The current position of this node in the graph's node list; must stay in
     * sync with Graph.nodelist
     */
    int pos;

    /**
     * A list of ports on the node. [N7-G. Dupont]
     */
    /*package*/
    LinkedList<GraphPort> ports = new LinkedList<GraphPort>();

    /**
     * Store the current number of ports on each side. This is used mainly for
     * establishing port order. [N7-G. Dupont]
     */
    /*package*/
    Map<GraphPort.Orientation, Integer> numPorts;

    /**
     * Increment the number of ports on target side
     *
     * @param or the side on which to increment the number of ports
     * @return the previous number of ports [N7-G. Dupont]
     */
    /*package*/ int incNumPorts(GraphPort.Orientation or) {
        int r = this.numPorts.get(or);
        this.numPorts.put(or, r + 1);
        return r;
    }

    /**
     * Decrement the number of ports on target side
     *
     * @param or the side on which to decrement the number of ports
     * @return the previous number of ports [N7-G. Dupont]
     */
    /*package*/ int decNumPorts(GraphPort.Orientation or) {
        int r = this.numPorts.get(or);
        this.numPorts.put(or, r - 1);
        return r;
    }

    // =============================== these fields affect the computed bounds ===================================================
    /**
     * The font boldness.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private boolean fontBold = false;

    /**
     * The node labels; if null or empty, then the node has no labels.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private List<String> labels = null;

    /**
     * The node color; never null.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private Color color = Color.WHITE;

    /**
     * The line style; never null.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private DotStyle style = DotStyle.SOLID;

    // ============================ these fields are computed by calcBounds() =========================================
    /**
     * If (updown>=0), this is the distance from the center to the top edge.
     */
    private int updown = (-1);

    /**
     * If (updown>=0), this is the distance from the center to the left edge.
     */
    private int side = 0;

    /**
     * If (updown>=0), this is the vertical distance between the center of the
     * text label and the center of the node.
     */
    private int yShift = 0;

    /**
     * If (updown>=0), this is the width of the text label.
     */
    private int width = 0;

    /**
     * If (updown>=0), this is the height of the text label.
     */
    private int height = 0;

    /**
     * If (updown>=0), this is the amount of space on the right set-aside for
     * self-loops (which is 0 if node has no self loops)
     */
    private int reserved = 0;

    /**
     * If (updown>=0 and shape!=null), this is the bounding polygon. Note: if
     * not null, it must be either a GeneralPath or a Polygon.
     */
    private Shape poly = null;

    /**
     * If (updown>=0 and shape!=null and poly2!=null), then poly2 will also be
     * drawn during the draw() method. Note: if not null, it must be either a
     * GeneralPath or a Polygon.
     */
    private Shape poly2 = null;

    /**
     * If (updown>=0 and shape!=null and poly3!=null), then poly3 will also be
     * drawn during the draw() method. Note: if not null, it must be either a
     * GeneralPath or a Polygon.
     */
    private Shape poly3 = null;

    //===================================================================================================  
    //
    /**
     * This field contains the children of this node. [N7-R. Bossut, M. Quentin]
     */
    private HashSet<GraphNode> children;

    /**
     * This field contains the subGraph of the node if it exists. [N7-R.Bossut,
     * M. Quentin]
     */
    private Graph subGraph;

    /**
     * This field contains the Height of the node's subGraph if it exists.
     */
    private int subGraphHeight;

    /**
     * This field contains the Width of the node's subGraph if it exists.
     */
    private int subGraphWidth;

    //====================================================================================================
    /**
     * Create a new node with the given list of labels, then add it to the given
     * graph.
     */
    public GraphNode(Graph graph, Object uuid, String... labels) {
        super(graph, uuid);
        this.pos = graph.nodelist.size();
        this.children = new HashSet<>(); //[N7-R. Bossut, M. Quentin]	
        graph.nodelist.add(this);
        if (graph.layerlist.size() == 0) {
            graph.layerlist.add(new ArrayList<GraphNode>());
        }
        graph.layerlist.get(0).add(this);
        if (labels != null && labels.length > 0) {
            this.labels = new ArrayList<String>(labels.length);
            for (int i = 0; i < labels.length; i++) {
                this.labels.add(labels[i]);
            }
        }

        // [N7-G. Dupont] Instanciate map port
        this.numPorts = new HashMap<GraphPort.Orientation, Integer>();
        this.numPorts.put(GraphPort.Orientation.North, 0);
        this.numPorts.put(GraphPort.Orientation.South, 0);
        this.numPorts.put(GraphPort.Orientation.East, 0);
        this.numPorts.put(GraphPort.Orientation.West, 0);
    }

    /**
     * Get the children of the Node. [N7-R. Bossut, M. Quentin]
     */
    public HashSet<GraphNode> getChildren() {
        return this.children;
    }

    /**
     * Get the SubGraph of the Node. [N7-R. Bossut, M. Quentin]
     */
    public Graph getSubGraph() {
        subGraph = (subGraph == null) ? new Graph(1.0) : this.subGraph;
        return (subGraph);
    }

    /**
     * Set the children of the Node. [N7-R. Bossut, M. Quentin]
     */
    public void setChildren(HashSet<GraphNode> children) {
        this.children = children;
    }

    /**
     * Add a child to the family of the Node. [N7-R. Bossut, M. Quentin]
     */
    public void addChild(GraphNode gn) {
        this.children.add(gn);
        subGraph.nodelist.add(gn);
    }

    public void setSubGraphWidth(int width) {
        this.subGraphWidth = width;
    }

    public void setSubGraphHeight(int height) {
        this.subGraphHeight = height;
    }

    /**
     * Changes the layer that this node is in; the new layer must be 0 or
     * greater.
     * <p>
     * If a node is removed from a layer, the order of the other nodes in that
     * layer remain unchanged.
     * <p>
     * If a node is added to a new layer, then it is added to the right of the
     * original rightmost node in that layer.
     */
    void setLayer(int newLayer) {
        if (newLayer < 0) {
            throw new IllegalArgumentException("The layer cannot be negative!");
        }
        if (layer == newLayer) {
            return;
        }
        graph.layerlist.get(layer).remove(this);
        layer = newLayer;
        while (layer >= graph.layerlist.size()) {
            graph.layerlist.add(new ArrayList<GraphNode>());
        }
        graph.layerlist.get(layer).add(this);
    }

    /**
     * Returns an unmodifiable view of the list of "in" edges.
     */
    public List<GraphEdge> inEdges() {
        return Collections.unmodifiableList(ins);
    }

    /**
     * Returns an unmodifiable view of the list of "out" edges.
     */
    public List<GraphEdge> outEdges() {
        return Collections.unmodifiableList(outs);
    }

    /**
     * Returns an unmodifiable view of the list of "self" edges.
     */
    public List<GraphEdge> selfEdges() {
        return Collections.unmodifiableList(selfs);
    }

    /**
     * Returns the node's current position in the node list, which is always
     * between 0 and node.size()-1
     */
    int pos() {
        return pos;
    }

    /**
     * Returns the layer that this node is in.
     */
    int layer() {
        return layer;
    }

    /**
     * Changes the node shape (where null means change the node into a dummy
     * node), then invalidate the computed bounds.
     */
    public GraphNode set(DotShape shape) {
        if (shape() != shape) {
            setShape(shape);
            updown = (-1);
        }
        return this;
    }

    /**
     * Changes the node color, then invalidate the computed bounds.
     */
    public GraphNode set(Color color) {
        if (this.color != color && color != null) {
            this.color = color;
            updown = (-1);
        }
        return this;
    }

    /**
     * Changes the line style, then invalidate the computed bounds.
     */
    public GraphNode set(DotStyle style) {
        if (this.style != style && style != null) {
            this.style = style;
            updown = (-1);
        }
        return this;
    }

    /**
     * Changes the font boldness, then invalidate the computed bounds.
     */
    public GraphNode setFontBoldness(boolean bold) {
        if (this.fontBold != bold) {
            this.fontBold = bold;
            updown = (-1);
        }
        return this;
    }

    /**
     * Add the given label after the existing labels, then invalidate the
     * computed bounds.
     */
    public GraphNode addLabel(String label) {
        if (label == null || label.length() == 0) {
            return this;
        }
        if (labels == null) {
            labels = new ArrayList<String>();
        }
        labels.add(label);
        updown = (-1);
        return this;
    }

    /**
     * Returns the node height.
     */
    @Override
    public int getHeight() {
        if (updown < 0) {
            calcBounds();
        }
        return updown + updown;
    }

    /**
     * Returns the node width.
     */
    @Override
    public int getWidth() {
        if (updown < 0) {
            calcBounds();
        }
        return side + side;
    }

    /**
     * Returns the bounding rectangle (with 2*xfluff added to the width, and
     * 2*yfluff added to the height)
     */
    Rectangle2D getBoundingBox(int xfluff, int yfluff) {
        if (updown < 0) {
            calcBounds();
        }
        return new Rectangle2D.Double(x() - side - xfluff, y() - updown - yfluff, side + side + xfluff + xfluff, updown + updown + yfluff + yfluff);
    }

    /**
     * Returns the amount of space we need to reserve on the right hand side for
     * the self edges (0 if this has no self edges now)
     */
    int getReserved() {
        if (selfs.isEmpty()) {
            return 0;
        } else if (updown < 0) {
            calcBounds();
        }
        return reserved;
    }

    /**
     * Returns true if the node contains the given point or not.
     */
    @Override
    boolean contains(double x, double y) {
        if (shape() == null) {
            return false;
        } else if (updown < 0) {
            calcBounds();
        }
        return poly.contains(x - x(), y - y());
    }

    /**
     * Draws this node at its current (x, y) location; this method will call
     * calcBounds() if necessary.
     */
    @Override
    void draw(Artist gr, double scale, boolean highlight) {
        draw(gr, scale, highlight, 1);
    }

    void draw(Artist gr, double scale, boolean highlight, int maxDepth) {

        if (shape() == null) {
            return;
        } else if (updown < 0) {
            calcBounds();
        }
        final int top = graph.getTop(), left = graph.getLeft();
        gr.set(style, scale);
        gr.translate(x() - left, y() - top);
        gr.setFont(fontBold);
        if (highlight) {
            gr.setColor(COLOR_CHOSENNODE);
        } else {
            gr.setColor(color);
        }

        if (children.isEmpty()) {
            drawRegular(gr, scale, highlight, top, left);
        } else {
            // [N7-Bossut, Quentin] Draw the subGraph
            if (maxDepth > 0) {
                drawSubgraph(gr, scale, maxDepth, top, left);
            } else {
                //We cannot draw the subgraph (it is too deep), we have to paint the node and a button the user have to click to see the subgraph.
                drawContainer(gr, scale, highlight, maxDepth, top, left);
            }
        }
    }

    /**
     * Draws a regular node (not a containig one). Draws this node at its
     * current (x, y) location; this method will call calcBounds() if necessary.
     */
    private void drawRegular(Artist gr, double scale, boolean highlight, int top, int left) {

        if (shape() == DotShape.CIRCLE || shape() == DotShape.M_CIRCLE || shape() == DotShape.DOUBLE_CIRCLE) {
            int hw = width / 2, hh = height / 2;
            int radius = ((int) (sqrt(hw * ((double) hw) + ((double) hh) * hh))) + 2;
            if (shape() == DotShape.DOUBLE_CIRCLE) {
                radius = radius + 5;
            }
            gr.fillCircle(radius);
            gr.setColor(Color.BLACK);
            gr.drawCircle(radius);
            if (style == DotStyle.DOTTED || style == DotStyle.DASHED) {
                gr.set(DotStyle.SOLID, scale);
            }
            if (shape() == DotShape.M_CIRCLE && 10 * radius >= 25 && radius > 5) {
                int d = (int) sqrt(10 * radius - 25.0D);
                if (d > 0) {
                    gr.drawLine(-d, -radius + 5, d, -radius + 5);
                    gr.drawLine(-d, radius - 5, d, radius - 5);
                }
            }
            if (shape() == DotShape.DOUBLE_CIRCLE) {
                gr.drawCircle(radius - 5);
            }
        } else {
            gr.draw(poly, true); // Draw the full shape in the current color
            gr.setColor(Color.BLACK); // Set the current color to black
            gr.draw(poly, false); // Draw the boders of the shape
            if (poly2 != null) {
                gr.draw(poly2, false);
            }
            if (poly3 != null) {
                gr.draw(poly3, false);
            }
            if (style == DotStyle.DOTTED || style == DotStyle.DASHED) {
                gr.set(DotStyle.SOLID, scale);
            }
            if (shape() == DotShape.M_DIAMOND) {
                gr.drawLine(-side + 8, -8, -side + 8, 8);
                gr.drawLine(-8, -side + 8, 8, -side + 8);
                gr.drawLine(side - 8, -8, side - 8, 8);
                gr.drawLine(-8, side - 8, 8, side - 8);
            }
            if (shape() == DotShape.M_SQUARE) {
                gr.drawLine(-side, -side + 8, -side + 8, -side);
                gr.drawLine(side, -side + 8, side - 8, -side);
                gr.drawLine(-side, side - 8, -side + 8, side);
                gr.drawLine(side, side - 8, side - 8, side);
            }
        }
        gr.set(DotStyle.SOLID, scale);
        int clr = color.getRGB() & 0xFFFFFF;
        gr.setColor((clr == 0x000000 || clr == 0xff0000 || clr == 0x0000ff) ? Color.WHITE : Color.BLACK);
        if (labels != null && labels.size() > 0) {
            int x = (-width / 2), y = yShift + (-labels.size() * ad / 2);
            for (int i = 0; i < labels.size(); i++) {
                String t = labels.get(i);
                int w = ((int) (getBounds(fontBold, t).getWidth())) + 1; // Round it up
                if (width > w) {
                    w = (width - w) / 2;
                } else {
                    w = 0;
                }
                gr.drawString(t, x + w, y + Artist.getMaxAscent());
                y = y + ad;
            }
        }

        // [N7-G. Dupont] Draw each ports
        for (GraphPort p : this.ports) {
            p.draw(gr, scale, false);
        }

        gr.translate(left - x(), top - y());
    }

    /**
     * Draws a node that has children that can also be drawn (in the view of the
     * maxDepth parameter). Layouts the subgraph and then draws (it will calls
     * the draw method of the sub-nodes, so it works recursively). Then draws
     * the node arround the subgraph.
     */
    private void drawSubgraph(Artist gr, double scale, int top, int maxDepth, int left) {
        // [N7-Bossut, Quentin] Draw the subGraph         
        //We have'nt reach the depth max yet, we can draw the subgraph.

        gr.setColor(Color.YELLOW);
        gr.draw(poly, true);
        gr.setColor(Color.BLACK);
        gr.draw(poly, false);
        
        subGraph.layoutSubGraph(this);
        subGraph.draw(gr, scale, uuid, true, (maxDepth - 1));

        /**
         * Inutile pour le moment. if (poly2 != null) { gr.draw(poly2, false); }
         * if (poly3 != null) { gr.draw(poly3, false); } if (style ==
         * DotStyle.DOTTED || style == DotStyle.DASHED) { gr.set(DotStyle.SOLID,
         * scale); } if (shape() == DotShape.M_DIAMOND) { gr.drawLine(-side + 8,
         * -8, -side + 8, 8); gr.drawLine(-8, -side + 8, 8, -side + 8);
         * gr.drawLine(side - 8, -8, side - 8, 8); gr.drawLine(-8, side - 8, 8,
         * side - 8); } if (shape() == DotShape.M_SQUARE) { gr.drawLine(-side,
         * -side + 8, -side + 8, -side); gr.drawLine(side, -side + 8, side - 8,
         * -side); gr.drawLine(-side, side - 8, -side + 8, side);
         * gr.drawLine(side, side - 8, side - 8, side); }
         */
        // Draw the label into the GraphNode
        gr.set(DotStyle.SOLID, scale);
        int clr = color.getRGB() & 0xFFFFFF;
        gr.setColor((clr == 0x000000 || clr == 0xff0000 || clr == 0x0000ff) ? Color.WHITE : Color.BLACK);
        if (labels != null && labels.size() > 0) {
            int x = (-width / 2), y = yShift + (-labels.size() * ad / 2);
            for (int i = 0; i < labels.size(); i++) {
                String t = labels.get(i);
                int w = ((int) (getBounds(fontBold, t).getWidth())) + 1; // Round it up
                if (width > w) {
                    w = (width - w) / 2;
                } else {
                    w = 0;
                }
                gr.drawString(t, x + w, y + Artist.getMaxAscent());
                y = y + ad;
            }
        }

        gr.translate(left - x(), top - y());
    }

    /**
     * Draws a container when it subgraph cannot be dran because of the maximum
     * depth level. Draws the node as a regular one and an indicator meaning
     * that the sugraph is hidden.
     */
    private void drawContainer(Artist gr, double scale, boolean highlight, int maxDepth, int top, int left) {
        drawRegular(gr, scale, highlight, top, left);
        //TODO Button.
    }

    /**
     * Helper method that sets the Y coordinate of every node in a given layer.
     */
    private void setY(int layer, int y) {
        for (GraphNode n : graph.layer(layer)) {
            n.setY(y);
        }
    }

    /**
     * Helper method that shifts a node up.
     */
    private void shiftUp(int y) {
        final int[] ph = graph.layerPH;
        final int yJump = Graph.yJump / 6;
        int i = layer();
        setY(i, y);
        y = y - ph[i] / 2; // y is now the top-most edge of this layer
        for (i++; i < graph.layers(); i++) {
            List<GraphNode> list = graph.layer(i);
            GraphNode first = list.get(0);
            if (first.y() + ph[i] / 2 + yJump > y) {
                setY(i, y - ph[i] / 2 - yJump);
            }
            y = first.y() - ph[i] / 2;
        }
        graph.relayout_edges(false);
    }

    /**
     * Helper method that shifts a node down.
     */
    private void shiftDown(int y) {
        final int[] ph = graph.layerPH;
        final int yJump = Graph.yJump / 6;
        int i = layer();
        setY(i, y);
        y = y + ph[i] / 2; // y is now the bottom-most edge of this layer
        for (i--; i >= 0; i--) {
            List<GraphNode> list = graph.layer(i);
            GraphNode first = list.get(0);
            if (first.y() - ph[i] / 2 - yJump < y) {
                setY(i, y + ph[i] / 2 + yJump);
            }
            y = first.y() + ph[i] / 2;
        }
        graph.relayout_edges(false);
    }

    /**
     * Helper method that shifts a node left.
     */
    private void shiftLeft(List<GraphNode> peers, int i, int x) {
        final int xJump = Graph.xJump / 3;
        this.setX(x);
        x = x - (shape() == null ? 0 : side); // x is now the left-most edge of this node
        for (i--; i >= 0; i--) {
            GraphNode node = peers.get(i);
            int side = (node.shape() == null ? 0 : node.side);
            if (node.x() + side + node.getReserved() + xJump > x) {
                node.setX(x - side - node.getReserved() - xJump);
            }
            x = node.x() - side;
        }
    }

    /**
     * Helper method that shifts a node right.
     */
    private void shiftRight(List<GraphNode> peers, int i, int x) {
        final int xJump = Graph.xJump / 3;
        this.setX(x);
        x = x + (shape() == null ? 0 : side) + getReserved(); // x is now the right most edge of this node
        for (i++; i < peers.size(); i++) {
            GraphNode node = peers.get(i);
            int side = (node.shape() == null ? 0 : node.side);
            if (node.x() - side - xJump < x) {
                node.setX(x + side + xJump);
            }
            x = node.x() + side + node.getReserved();
        }
    }

    /**
     * Helper method that swaps a node towards the left.
     */
    private void swapLeft(List<GraphNode> peers, int i, int x) {
        int side = (shape() == null ? 2 : this.side);
        int left = x - side;
        while (true) {
            if (i == 0) {
                this.setX(x);
                return;
            } // no clash possible
            GraphNode other = peers.get(i - 1);
            int otherSide = (other.shape() == null ? 0 : other.side);
            int otherRight = other.x() + otherSide + other.getReserved();
            if (otherRight < left) {
                this.setX(x);
                return;
            } // no clash
            graph.swapNodes(layer(), i, i - 1);
            i--;
            if (other.shape() != null) {
                other.shiftRight(peers, i + 1, x + side + getReserved() + otherSide);
            }
        }
    }

    /**
     * Helper method that swaps a node towards the right.
     */
    private void swapRight(List<GraphNode> peers, int i, int x) {
        int side = (shape() == null ? 2 : this.side);
        int right = x + side + getReserved();
        while (true) {
            if (i == peers.size() - 1) {
                this.setX(x);
                return;
            } // no clash possible
            GraphNode other = peers.get(i + 1);
            int otherSide = (other.shape() == null ? 0 : other.side);
            int otherLeft = other.x() - otherSide;
            if (otherLeft > right) {
                this.setX(x);
                return;
            } // no clash
            graph.swapNodes(layer(), i, i + 1);
            i++;
            if (other.shape() != null) {
                other.shiftLeft(peers, i - 1, x - side - other.getReserved() - otherSide);
            }
        }
    }

    /**
     * Assuming the graph is already laid out, this shifts this node (and
     * re-layouts nearby nodes/edges as necessary)
     */
    void tweak(int x, int y) {
        if (x() == x && y() == y) {
            return; // If no change, then return right away
        }
        List<GraphNode> layer = graph.layer(layer());
        final int n = layer.size();
        int i;
        for (i = 0; i < n; i++) {
            if (layer.get(i) == this) {
                break; // Figure out this node's position in its layer
            }
        }
        if (x() > x) {
            swapLeft(layer, i, x);
        } else if (x() < x) {
            swapRight(layer, i, x);
        }
        if (y() > y) {
            shiftUp(y);
        } else if (y() < y) {
            shiftDown(y);
        } else {
            graph.relayout_edges(layer());
        }
        graph.recalcBound(false);
    }

    //===================================================================================================
    /**
     * (Re-)calculate this node's bounds.
     */
    void calcBounds() {
        reserved = (yShift = 0);
        width = 2 * labelPadding;
        if (width < dummyWidth) {
            side = dummyWidth / 2;
        }
        height = width;
        if (height < dummyHeight) {
            updown = dummyHeight / 2;
        }
        poly = (poly2 = (poly3 = null));
        if (shape() == null) {
            return;
        }
        Polygon poly = new Polygon();
        if (labels != null) {
            for (int i = 0; i < labels.size(); i++) {
                String t = labels.get(i);
                Rectangle2D rect = getBounds(fontBold, t);
                int ww = ((int) (rect.getWidth())) + 1; // Round it up
                if (width < ww) {
                    width = ww;
                }
                height = height + ad;
            }
        }
        int hw = ((width + 1) / 2) + labelPadding;
        if (hw < ad / 2) {
            hw = ad / 2;
        }
        width = hw * 2;
        side = hw;
        int hh = ((height + 1) / 2) + labelPadding;
        if (hh < ad / 2) {
            hh = ad / 2;
        }
        height = hh * 2;
        updown = hh;

        portBounds(); // [N7-G.Dupont]

        imbricatedNodeBounds(); // [N7-M.Quentin]

        switch (shape()) {
            case HOUSE: {
                yShift = ad / 2;
                updown = updown + yShift;
                poly.addPoint(-hw, yShift - hh);
                poly.addPoint(0, -updown);
                poly.addPoint(hw, yShift - hh);
                poly.addPoint(hw, yShift + hh);
                poly.addPoint(-hw, yShift + hh);
                break;
            }
            case INV_HOUSE: {
                yShift = -ad / 2;
                updown = updown - yShift;
                poly.addPoint(-hw, yShift - hh);
                poly.addPoint(hw, yShift - hh);
                poly.addPoint(hw, yShift + hh);
                poly.addPoint(0, updown);
                poly.addPoint(-hw, yShift + hh);
                break;
            }
            case TRIANGLE:
            case INV_TRIANGLE: {
                int dx = (int) (height / sqrt3);
                dx = dx + 1;
                if (dx < 6) {
                    dx = 6;
                }
                int dy = (int) (hw * sqrt3);
                dy = dy + 1;
                if (dy < 6) {
                    dy = 6;
                }
                dy = (dy / 2) * 2;
                side += dx;
                updown += dy / 2;
                if (shape() == DotShape.TRIANGLE) {
                    yShift = dy / 2;
                    poly.addPoint(0, -updown);
                    poly.addPoint(hw + dx, updown);
                    poly.addPoint(-hw - dx, updown);
                } else {
                    yShift = -dy / 2;
                    poly.addPoint(0, updown);
                    poly.addPoint(hw + dx, -updown);
                    poly.addPoint(-hw - dx, -updown);
                }
                break;
            }
            case HEXAGON: {
                side += ad;
                poly.addPoint(-hw - ad, 0);
                poly.addPoint(-hw, -hh);
                poly.addPoint(hw, -hh);
                poly.addPoint(hw + ad, 0);
                poly.addPoint(hw, hh);
                poly.addPoint(-hw, hh);
                break;
            }
            case TRAPEZOID: {
                side += ad;
                poly.addPoint(-hw, -hh);
                poly.addPoint(hw, -hh);
                poly.addPoint(hw + ad, hh);
                poly.addPoint(-hw - ad, hh);
                break;
            }
            case INV_TRAPEZOID: {
                side += ad;
                poly.addPoint(-hw - ad, -hh);
                poly.addPoint(hw + ad, -hh);
                poly.addPoint(hw, hh);
                poly.addPoint(-hw, hh);
                break;
            }
            case PARALLELOGRAM: {
                side += ad;
                poly.addPoint(-hw, -hh);
                poly.addPoint(hw + ad, -hh);
                poly.addPoint(hw, hh);
                poly.addPoint(-hw - ad, hh);
                break;
            }
            case M_DIAMOND:
            case DIAMOND: {
                if (shape() == DotShape.M_DIAMOND) {
                    if (hw < 10) {
                        hw = 10;
                        side = 10;
                        width = 20;
                    }
                    if (hh < 10) {
                        hh = 10;
                        updown = 10;
                        height = 20;
                    }
                }
                updown += hw;
                side += hh;
                poly.addPoint(-hw - hh, 0);
                poly.addPoint(0, -hh - hw);
                poly.addPoint(hw + hh, 0);
                poly.addPoint(0, hh + hw);
                break;
            }
            case M_SQUARE: {
                if (hh < hw) {
                    hh = hw;
                } else {
                    hw = hh;
                }
                if (hh < 6) {
                    hh = 6;
                    hw = 6;
                }
                this.width = hw * 2;
                this.side = hw;
                this.height = hh * 2;
                this.updown = hh;
                side += 4;
                updown += 4;
                poly.addPoint(-hw - 4, -hh - 4);
                poly.addPoint(hw + 4, -hh - 4);
                poly.addPoint(hw + 4, hh + 4);
                poly.addPoint(-hw - 4, hh + 4);
                break;
            }
            case OCTAGON:
            case DOUBLE_OCTAGON:
            case TRIPLE_OCTAGON: {
                int dx = (width) / 3, dy = ad;
                updown += dy;
                poly.addPoint(-hw, -hh);
                poly.addPoint(-hw + dx, -hh - dy);
                poly.addPoint(hw - dx, -hh - dy);
                poly.addPoint(hw, -hh);
                poly.addPoint(hw, hh);
                poly.addPoint(hw - dx, hh + dy);
                poly.addPoint(-hw + dx, hh + dy);
                poly.addPoint(-hw, hh);
                if (shape() == DotShape.OCTAGON) {
                    break;
                }
                double c = sqrt(dx * dx + dy * dy), a = (dx * dy) / c, k = ((a + 5) * dy) / dx, r = sqrt((a + 5) * (a + 5) + k * k) - dy;
                double dx1 = ((r - 5) * dx) / dy, dy1 = -(((dx + 5D) * dy) / dx - dy - r);
                int x1 = (int) (round(dx1)), y1 = (int) (round(dy1));
                updown += 5;
                side += 5;
                poly2 = poly;
                poly = new Polygon();
                poly.addPoint(-hw - 5, -hh - y1);
                poly.addPoint(-hw + dx - x1, -hh - dy - 5);
                poly.addPoint(hw - dx + x1, -hh - dy - 5);
                poly.addPoint(hw + 5, -hh - y1);
                poly.addPoint(hw + 5, hh + y1);
                poly.addPoint(hw - dx + x1, hh + dy + 5);
                poly.addPoint(-hw + dx - x1, hh + dy + 5);
                poly.addPoint(-hw - 5, hh + y1);
                if (shape() == DotShape.DOUBLE_OCTAGON) {
                    break;
                }
                updown += 5;
                side += 5;
                poly3 = poly;
                poly = new Polygon();
                x1 = (int) (round(dx1 * 2));
                y1 = (int) (round(dy1 * 2));
                poly.addPoint(-hw - 10, -hh - y1);
                poly.addPoint(-hw + dx - x1, -hh - dy - 10);
                poly.addPoint(hw - dx + x1, -hh - dy - 10);
                poly.addPoint(hw + 10, -hh - y1);
                poly.addPoint(hw + 10, hh + y1);
                poly.addPoint(hw - dx + x1, hh + dy + 10);
                poly.addPoint(-hw + dx - x1, hh + dy + 10);
                poly.addPoint(-hw - 10, hh + y1);
                break;
            }
            case M_CIRCLE:
            case CIRCLE:
            case DOUBLE_CIRCLE: {
                int radius = ((int) (sqrt(hw * ((double) hw) + ((double) hh) * hh))) + 2;
                if (shape() == DotShape.DOUBLE_CIRCLE) {
                    radius = radius + 5;
                }
                int L = ((int) (radius / cos18)) + 2, a = (int) (L * sin36), b = (int) (L * cos36), c = (int) (radius * tan18);
                poly.addPoint(-L, 0);
                poly.addPoint(-b, a);
                poly.addPoint(-c, L);
                poly.addPoint(c, L);
                poly.addPoint(b, a);
                poly.addPoint(L, 0);
                poly.addPoint(b, -a);
                poly.addPoint(c, -L);
                poly.addPoint(-c, -L);
                poly.addPoint(-b, -a);
                updown = L;
                side = L;
                break;
            }
            case EGG:
            case ELLIPSE: {
                int pad = ad / 2;
                side += pad;
                updown += pad;
                int d = (shape() == DotShape.ELLIPSE) ? 0 : (ad / 2);
                GeneralPath path = new GeneralPath();
                path.moveTo(-side, d);
                path.quadTo(-side, -updown, 0, -updown);
                path.quadTo(side, -updown, side, d);
                path.quadTo(side, updown, 0, updown);
                path.quadTo(-side, updown, -side, d);
                path.closePath();
                this.poly = path;
            }
            default: { // BOX
                if (shape() != DotShape.BOX) {
                    int d = ad / 2;
                    hw = hw + d;
                    side = hw;
                    hh = hh + d;
                    updown = hh;
                }
                // [N7-G.Dupont] Using side and updown to get the job
                poly.addPoint(-this.side, -this.updown);
                poly.addPoint(this.side, -this.updown);
                poly.addPoint(this.side, this.updown);
                poly.addPoint(-this.side, this.updown);
            }
        }
        if (shape() != DotShape.EGG && shape() != DotShape.ELLIPSE) {
            this.poly = poly;
        }
        for (int i = 0; i < selfs.size(); i++) {
            if (i == 0) {
                reserved = side + selfLoopA;
                continue;
            }
            String label = selfs.get(i - 1).label();
            reserved = reserved + (int) (getBounds(false, label).getWidth()) + selfLoopGL + selfLoopGR;
        }
        if (reserved > 0) {
            String label = selfs.get(selfs.size() - 1).label();
            reserved = reserved + (int) (getBounds(false, label).getWidth()) + selfLoopGL + selfLoopGR;
        }
    }

    /**
     * [N7- R Bossut, M Quentin] Recalculate the boundaries of the imbricated
     * nodes given the current boundaries and the ones of its children.
     */
    void imbricatedNodeBounds() {

        if (!children.isEmpty()) {

            int nbChildren = children.size();
            int maxUpdown = 0;
            int maxSide = 0;
            for (GraphNode gn : children) {
                if (gn.updown < 0) {
                    gn.calcBounds();
                }
                maxUpdown = (gn.updown > maxUpdown) ? gn.updown : maxUpdown;
                maxSide = (gn.side > maxSide) ? gn.side : maxSide;
            }

             //this.updown = nbChildren * maxUpdown + yJumpNode * (nbChildren - 1);
            //this.side = nbChildren * maxSide * xJumpNode * (nbChildren - 1); 
            this.updown = nbChildren * maxUpdown + yJumpNode * (nbChildren - 1);
            this.side = nbChildren * maxSide;

            //TODO 
            //Find the dimension of the figure
            
            //TODO
            //Change the form of the polygon
            poly = new Polygon();
            ((Polygon) poly).addPoint(-side, -updown);
            ((Polygon) poly).addPoint(side, -updown);
            ((Polygon) poly).addPoint(side, updown);
            ((Polygon) poly).addPoint(-side, updown);

        }

    }

    /**
     * [N7-G Dupont] Recalculate the boundaries of the node given the current
     * boundaries and the ports.
     */
    private void portBounds() {
        // 1) Get the number of port for each side
        // 2) Compute, for each side pair (N/S and E/W) the biggest port size
        int northPorts = 0, southPorts = 0, eastPorts = 0, westPorts = 0;
        int maxVPortSize = -1, maxHPortSize = -1;
        for (GraphPort port : this.ports) {
            int portSize = port.getSize();
            switch (port.getOrientation()) {
                case East:
                    eastPorts++;
                    if (portSize > maxHPortSize) {
                        maxHPortSize = portSize;
                    }
                    break;
                case West:
                    westPorts++;
                    if (portSize > maxHPortSize) {
                        maxHPortSize = portSize;
                    }
                    break;
                case South:
                    southPorts++;
                    if (portSize > maxVPortSize) {
                        maxVPortSize = portSize;
                    }
                    break;
                case North:
                    northPorts++;
                    if (portSize > maxVPortSize) {
                        maxVPortSize = portSize;
                    }
                    break;
            }
        }

        if (northPorts == 0 && southPorts == 0 && eastPorts == 0 && westPorts == 0) {
            return;
        }

        // Compute the minimal side and updown for a "correct" display of the
        // ports (according to minimal ports distance)
        int minside = (Math.max(eastPorts, westPorts) + 1) * GraphPort.PortDistance / 2,
                minupdown = (Math.max(northPorts, southPorts) + 1) * GraphPort.PortDistance / 2;

        // Compute the total padded size including ports (and eventually labels)
        int paddedside = 0, paddedupdown = 0;

        if (maxVPortSize > -1) {
            paddedupdown = maxVPortSize + GraphPort.PortPadding;
        }

        if (maxHPortSize > -1) {
            paddedside = maxHPortSize + GraphPort.PortPadding;
        }

        // Decide between the two values (the bigger, the easier to read)
        this.side += Math.max(minside, paddedside);
        this.updown += Math.max(minupdown, paddedupdown);
    }

    //===================================================================================================
    /**
     * Returns a DOT representation of this node (or "" if this is a dummy node)
     */
    @Override
    public String toString() {
        if (shape() == null) {
            return ""; // This means it's a virtual node
        }
        int rgb = color.getRGB() & 0xFFFFFF;
        String text = (rgb == 0xFF0000 || rgb == 0x0000FF || rgb == 0) ? "FFFFFF" : "000000";
        String main = Integer.toHexString(rgb);
        while (main.length() < 6) {
            main = "0" + main;
        }
        StringBuilder out = new StringBuilder();
        out.append("\"N" + pos + "\"");
        out.append(" [");
        out.append("uuid=\"");
        if (uuid != null) {
            out.append(esc(uuid.toString()));
        }
        out.append("\", label=\"");
        boolean first = true;
        if (labels != null) {
            for (String label : labels) {
                if (label.length() > 0) {
                    out.append((first ? "" : "\\n") + esc(label));
                    first = false;
                }
            }
        }
        out.append("\", color=\"#" + main + "\"");
        out.append(", fontcolor = \"#" + text + "\"");
        out.append(", shape = \"" + shape().getDotText() + "\"");
        out.append(", style = \"filled, " + style.getDotText() + "\"");
        out.append("]\n");
        return out.toString();
    }
}
