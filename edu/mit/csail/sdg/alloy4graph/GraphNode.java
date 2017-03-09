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
import java.awt.Rectangle;
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
    
    private static final Color COLOR_DIFFNODE = new Color(230,230,230);

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
     * A list of ports on the node.
     * [N7-G. Dupont]
     */
    /*package*/LinkedList<GraphPort> ports = new LinkedList<GraphPort>();
    
    /**
     * Store the current number of ports on each side.
     * This is used mainly for establishing port order.
     * [N7-G. Dupont]
     */
    /*package*/Map<GraphPort.Orientation,Integer> numPorts;
    
    /**
     * Increment the number of ports on target side
     * @param or the side on which to increment the number of ports
     * @return the previous number of ports
     * [N7-G. Dupont]
     */
    /*package*/ int incNumPorts(GraphPort.Orientation or) {
        int r = this.numPorts.get(or);
        this.numPorts.put(or, r + 1);
        return r;
    }
    
    /**
     * Decrement the number of ports on target side
     * @param or the side on which to decrement the number of ports
     * @return the previous number of ports
     * [N7-G. Dupont]
     */
    /*package*/ int decNumPorts(GraphPort.Orientation or) {
        int r = this.numPorts.get(or);
        this.numPorts.put(or, r - 1);
        return r;
    }

    // =============================== these fields affect the computed bounds ===================================================
    /**
     * The node labels; if null or empty, then the node has no labels.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private List<String> labels = null;

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
    Shape poly() { return poly; }

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
    
    /**
     * [N7] @Louis Fauvarque
     * Indicates if this node needs to be highlighted (for comparative views 
     * only)
     */
    private boolean needHighlight;
    
    /**
     * [N7-M.Quentin] This field is here to ensure we only layout the subgraph one time.
     */
    private boolean layout = false;

    //====================================================================================================
    /**
     * Create a new node with the given list of labels, then add it to the given
     * graph.
     */
    public GraphNode(Graph graph, Object uuid, String... labels) {
        super(graph, uuid, null);
        this.pos = graph.nodelist.size();
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
        this.numPorts.put(GraphPort.Orientation.NorthEast, 0);
        this.numPorts.put(GraphPort.Orientation.NorthWest, 0);
        this.numPorts.put(GraphPort.Orientation.South, 0);
        this.numPorts.put(GraphPort.Orientation.SouthEast, 0);
        this.numPorts.put(GraphPort.Orientation.SouthWest, 0);
        this.numPorts.put(GraphPort.Orientation.West, 0);
        this.numPorts.put(GraphPort.Orientation.East, 0);
    }

    //[N7-R.Bossut, M.Quentin]
    // Adds an integer parameter the maximum depth level in this node.
    public GraphNode(Graph graph, Object uuid, int maxDepth, String... labels) {
        this(graph, uuid, labels);
        this.maxDepth = maxDepth;
    }

    /**
     * Duplicate the given GraphNode, changing the graph in which it is, setting subGraph at null and letting empty the child list.
     * We let the calling method duplicate the subgraph and add duplicate nodes to children.
     */
    public GraphNode(GraphNode toBeCopied, Graph graph) {
        super(graph, toBeCopied.uuid, toBeCopied.getFather());
        this.pos = graph.nodelist.size();
        this.setColor(toBeCopied.getColor());
        this.setFontBoldness(toBeCopied.getFontBoldness());
        this.setStyle(toBeCopied.getStyle());
        this.setShape(toBeCopied.shape());	
        this.maxDepth = toBeCopied.getMaxDepth() + 1; //We do not show the "upper depth" of the graph, we can show one level deeper.
        graph.nodelist.add(this);
        if (graph.layerlist.size() == 0) {
            graph.layerlist.add(new ArrayList<GraphNode>());
        }
        graph.layerlist.get(0).add(this);
        this.labels = toBeCopied.labels;
        this.numPorts = toBeCopied.numPorts;
    }
    
    /**
    * Return the Height of the label. [N7-R.Bossut, M.Quentin]
    */
    public int getLabelHeight() {
        return this.height;
    }
    
    /**
    * Return the Width of the label. [N7-R.Bossut, M.Quentin]
    */
    public int getLabelWidth() {
        return this.width;
    }

    public List<String> getLabels() {
        return this.labels;
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
    @Override
    public void setShape(DotShape shape) {
        if (super.shape() != shape && shape != null) {
            super.setShape(shape);
            updown = (-1);
        }
    }

    /**
     * Changes the node color, then invalidate the computed bounds.
     */
    @Override
    public void setColor(Color color) {
        if (super.getColor() != color && color != null) {
            super.setColor(color);
            updown = (-1);
        }
    }

    /**
     * Changes the line style, then invalidate the computed bounds.
     */
    @Override
    public void setStyle(DotStyle style) {
        if (super.getStyle() != style && style != null) {
            super.setStyle(style);
            updown = (-1);
        }
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
    @Override
    Rectangle2D getBoundingBox(int xfluff, int yfluff) {
        if (updown < 0) {
            calcBounds();
        }
        return super.getBoundingBox(xfluff, yfluff);
    }

    /**
     * Returns the amount of space we need to reserve on the right hand side for
     * the self edges (0 if this has no self edges now)
     */
    @Override
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
     * Determines if coordinates are inside/on the element in the given reference.
     * @param x x coordinate to test
     * @param y y coordinate to test
     * @param reference the given reference
     * @return true if the point (x,y) is inside the shape of the element
     */
    boolean contains(double x, double y, GraphNode reference) {
        double realx = transformX(x, reference, this.getFather());
        double realy = transformY(y, reference, this.getFather());
        return contains(realx, realy);
    }

    /**
     * Draws this node at its current (x, y) location; this method will call
     * calcBounds() if necessary.
     */
    @Override
    void draw(Artist gr, double scale, Object group) {
        // There is nothing to draw => return
        if (shape() == null) {
            return;
        }
        
        calcBounds();
        
        final int top = graph.getTop(), left = graph.getLeft();
        final int subTop  = (subGraph == null ? 0 : subGraph.getTop()),
                  subLeft = (subGraph == null ? 0 : subGraph.getLeft());
        
        gr.set(this.getStyle(), scale);
        
        int xgn = x(), ygn = y();
        AbstractGraphNode gn = this;
        while (gn.getFather() != null) {
            gn = gn.getFather();
            xgn += gn.x();
            ygn += gn.y();
        }
        xgn -= gn.graph.getLeft();
        ygn -= gn.graph.getTop();
        
        boolean cond=false;
        for (GraphEdge e : ins) {
            if (e.highlight()) {
                cond = true;
                break;
            }
        }
        for (GraphEdge e : outs) {
            if (e.highlight()) {
                cond = true;
                break;
            }
        }

        gr.setFont(this.getFontBoldness());
        
        int transX = x() - left, transY = y() - top;
        if (this.highlight()) {
            if (getFather() != null && cond) {
                transX = xgn;
                transY = ygn;
            }              
            gr.setColor(COLOR_CHOSENNODE);
        } else if (this.needHighlight) {
            gr.setColor(COLOR_DIFFNODE);
        } else {
            gr.setColor(this.getColor());
        }

        gr.translate(transX, transY);
        
        // Draw node
        if (shape() == DotShape.CIRCLE || shape() == DotShape.M_CIRCLE || shape() == DotShape.DOUBLE_CIRCLE) {
            int hw = width / 2, hh = height / 2;
            int radius = ((int) (sqrt(hw * ((double) hw) + ((double) hh) * hh))) + 2;
            if (shape() == DotShape.DOUBLE_CIRCLE) {
                radius = radius + 5;
            }
            gr.fillCircle(radius);
            gr.setColor(Color.BLACK);
            gr.drawCircle(radius);
            if (this.getStyle() == DotStyle.DOTTED || this.getStyle() == DotStyle.DASHED) {
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
            gr.draw(poly, true);
            if(needHighlight){
                gr.setColor(Color.RED);
            } else {
                gr.setColor(Color.BLACK);
            }
            gr.draw(poly, false);
            if (poly2 != null) {
                gr.draw(poly2, false);
            }
            if (poly3 != null) {
                gr.draw(poly3, false);
            }
            if (this.getStyle() == DotStyle.DOTTED || this.getStyle() == DotStyle.DASHED) {
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

        // Draw subgraph
        if (hasChild()) {
            if (maxDepth > 0) {
                Object high = null;
                for (AbstractGraphNode n : getChildren()){
                    if (n.highlight()){
                        high = n;
                        break;
                    }
                }

                gr.translate(subLeft, subTop);
                subGraph.draw(gr, scale, group, true);
                gr.translate(-subLeft, -subTop);
            } else { // Draw a "hider"
                gr.setFont(true);
                gr.set(DotStyle.SOLID, scale);
                int clr = getColor().getRGB() & 0xFFFFFF;
                gr.setColor((clr == 0x000000 || clr == 0xff0000 || clr == 0x0000ff) ? Color.WHITE : Color.BLACK);
                if (labels != null && labels.size() > 0) {
                    int x = (-width / 2), y = -updown - labels.size()/2;
                    String t = "...";
                    int w = ((int) (getBounds(true, t).getWidth()));
                    if (width > w) {
                        w = (width - w) / 2;
                    } else {
                        w = 0;
                    }
                    gr.drawString(t, x + w, y + Artist.getMaxAscent());
                }
            }
        }
        
        // Draw label
        gr.set(DotStyle.SOLID, scale);
        int clr = getColor().getRGB() & 0xFFFFFF;
        gr.setColor((clr == 0x000000 || clr == 0xff0000 || clr == 0x0000ff) ? Color.WHITE : Color.BLACK);
        if (labels != null && labels.size() > 0) {
            if (hasChild() && maxDepth > 0) {
                int maxWidth=0;
                for (int i=0; i < labels.size(); i++ ) {
                    maxWidth = Math.max(maxWidth, (int) getBounds(true, labels.get(i)).getWidth());
                }
                width = maxWidth;
            }
            int x = (-width/2), y = yJumpNode/2 -updown + (labels.size() / 2);
            
            for (int i = 0; i < labels.size(); i++) {
                String t = labels.get(i);
                int w = ((int) (getBounds((this.getFontBoldness() | hasChild()), t).getWidth())) + 1; // Round it up
                if (width > w) {
                    w = (width - w) / 2;
                } else {
                    w = 0;
                }
                
                if (hasChild() && maxDepth > 0)
                    gr.setFont(true);

                gr.drawString(t, x + w, y + Artist.getMaxAscent());
                y = y + ad;
            }
        }
        
        // [DEBUG]
        //drawDebug(gr);
        
        // [N7-G. Dupont] Draw each ports
        for (GraphPort p : this.ports) {
            p.draw(gr, scale, group);
        }
             
        gr.translate(-transX, -transY);
    }
    
    public void drawTooltips(Artist gr) {
        final int top = graph.getTop(), left = graph.getLeft();
        gr.set(this.getStyle(), 1.0);
        gr.translate(x() - left, y() - top);
        for (GraphPort gp : this.ports) {
            gp.drawTooltip(gr, 0.5);
        }
        gr.translate(left - x(), top - y());
    }
    
    /**
     * [N7-G.Dupont] (debug) Print bounding box, center and points of the polygon.
     */
    private void drawDebug(Artist gr) {
        // Print bounding box and center
        gr.setColor(Color.RED);
        gr.draw(new Rectangle(-this.side, -this.updown, 2*this.side, 2*this.updown), false);
        gr.fillCircle(3);
        
        if (subGraph != null && maxDepth > 0) {
            gr.setColor(Color.GREEN);
            gr.draw(new Rectangle(
                    this.subGraph.getLeft(),  this.subGraph.getTop(), 
                    this.subGraph.getTotalWidth(), this.subGraph.getTotalHeight()
            ), false);
        }
        
        // Print each point of the polygon
        gr.setColor(Color.BLUE);
        if (this.poly instanceof Polygon) {
            int xp[] = ((Polygon)this.poly).xpoints;
            int yp[] = ((Polygon)this.poly).ypoints;
            for (int i = 0; i < ((Polygon)this.poly).npoints; i++) {
                gr.translate(xp[i], yp[i]);
                gr.fillCircle(2);
                gr.drawString("M" + i + "(" + xp[i] + "," + yp[i] + ")", 3, -3);
                gr.translate(-xp[i], -yp[i]);
            }
        }
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
     * Method to layout the edges bewtween the node and a node from another graph.
     */
    private void shift_edges() {
        
        for (GraphEdge e : this.outs) {
            AbstractGraphNode b = e.b();
            if (b.graph != graph) {
                b.graph.relayout_edges(false);
            }
        }
        for (GraphEdge e : this.ins) {
            AbstractGraphNode a = e.a();
            if (a.graph != graph) {
                a.graph.relayout_edges(false);
            }
        }
        
        if (hasChild()) {
            for (AbstractGraphNode child : getChildren()) {
                for (GraphEdge e : child.outs) {
                    AbstractGraphNode a = e.a();
                    a.graph.relayout_edges(false);
                    
                    AbstractGraphNode b = e.b();
                    for (GraphEdge e2 : b.ins) {
                        e2.a().graph.relayout_edges(false);
                    }
                }
                for (GraphEdge e : child.ins) {
                    AbstractGraphNode b = e.b();
                    b.graph.relayout_edges(false);
                    
                    AbstractGraphNode a = e.a();
                    for (GraphEdge e2 : a.outs) {
                        e2.b().graph.relayout_edges(false);
                    }
                }
            }
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
        if (ph == null) return;
        y = y - ph[i] / 2; // y is now the top-most edge of this layer
        for (i++; i < graph.layers(); i++) {
            List<GraphNode> list = graph.layer(i);
            if (!list.isEmpty()) { 
                GraphNode first = list.get(0);
                if (first.y() + ph[i] / 2 + yJump > y) {
                    setY(i, y - ph[i] / 2 - yJump);
                }
                y = first.y() - ph[i] / 2;
            }
        }
        graph.relayout_edges(false);      
        shift_edges();
    }

    /**
     * Helper method that shifts a node down.
     */
    private void shiftDown(int y) {
        final int[] ph = graph.layerPH;
        final int yJump = Graph.yJump / 6;
        int i = layer();
        setY(i, y);
        if (ph == null) return;
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
        shift_edges();
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
            graph.relayout_edges(false);
            //graph.relayout_edges(layer());
            shift_edges();
        }
        if(getFather() != null){
            tweakFather();
            nestedNodeBounds();
        }else{
            graph.recalcBound(false);
        }        
    }

    /**
     * This manage to pass on the changes on the subgraph to fathers graphs.
     */
    void tweakFather(){
        if (getFather() == null)
            return;
        
        if (!(getFather() instanceof GraphNode)) {
            throw new IllegalArgumentException("The father of this element is not a node!");
        }
        GraphNode father = (GraphNode)getFather();

        //Computes new bounds of the father.
        father.nestedNodeBounds();
        father.shiftUp(father.y());
        father.shiftDown(father.y());
        //Changes of the bounds of the father can make other nodes of the father's graph move.
        father.adaptLayer();
        father.calcBounds();
        father.tweakFather();
        //If father is in the englobing graph, we have to recalc bound to avoid nodes on top to go out of the window.
        if (father.getFather() == null)
            father.graph.recalcBound(false);
    }


    //[N7-R.Bossut]
    /**
     * Helper method that will move other nodes of the layer if they are intersecting with this.
     */
    private void adaptLayer(){
          //We get the list of nodes in the same layer as this.
          List<GraphNode> layer = graph.layer(layer());
          final int n = layer.size();
          int i;
          //We get the position of this node in the layer list.
          for (i = 0; i < n; i++) {
              if (layer.get(i) == this) {
                break;
              }
          }

          int j = i;
          int borderFix, borderMoved, d;
          GraphNode moved;
          GraphNode fix = this;
          while (j > 0) { //We must see if nodes on the left of this one have to be moved.
            borderFix = fix.x() - ((fix.shape() == null) ? 0 : fix.getWidth()/2);
            moved = layer.get(j-1);
            borderMoved = ((moved.shape() == null) ? 0 : moved.getWidth()/2) + moved.x() + moved.getReserved();
            d = borderFix - borderMoved;
            if (d < 0){
              moved.setX(moved.x() + d);
              fix = moved;
              j--;
            }else{
              break;
            }
          }
          fix = this;
          while (i < n-1) { //We must see if nodes on the right of this one have to be moved.
            borderFix = fix.x() + ((fix.shape() == null) ? 0 : fix.getWidth()/2) + fix.getReserved();
            moved = layer.get(i+1);
            borderMoved = moved.x() - ((moved.shape() == null) ? 0 : moved.getWidth()/2);
            d = borderFix - borderMoved; 
            if (d > 0){
              moved.setX(moved.x() + d);
              fix = moved;
              i++;
            }else{
              break;
            }
          }
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

        Polygon newPoly = new Polygon();
        if (labels != null) {
            for (int i = 0; i < labels.size(); i++) {
                String t = labels.get(i);
                Rectangle2D rect = getBounds(this.getFontBoldness(), t);
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

        nestedNodeBounds(); // [N7-M.Quentin]
        portBounds(); // [N7-G.Dupont]
        
        hh = updown;
        hw = side;

        switch (shape()) {
            case HOUSE: {
                yShift = ad / 2;
                updown = updown + yShift;
                newPoly.addPoint(-hw, yShift - hh);
                newPoly.addPoint(0, -updown);
                newPoly.addPoint(hw, yShift - hh);
                newPoly.addPoint(hw, yShift + hh);
                newPoly.addPoint(-hw, yShift + hh);
                break;
            }
            case INV_HOUSE: {
                yShift = -ad / 2;
                updown = updown - yShift;
                newPoly.addPoint(-hw, yShift - hh);
                newPoly.addPoint(hw, yShift - hh);
                newPoly.addPoint(hw, yShift + hh);
                newPoly.addPoint(0, updown);
                newPoly.addPoint(-hw, yShift + hh);
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
                    newPoly.addPoint(-hw - dx, updown);
                    newPoly.addPoint(0, -updown);
                    newPoly.addPoint(hw + dx, updown);
                } else {
                    yShift = -dy / 2;
                    newPoly.addPoint(-hw - dx, -updown);
                    newPoly.addPoint(hw + dx, -updown);
                    newPoly.addPoint(0, updown);
                }
                break;
            }
            case HEXAGON: {
                side += ad;
                newPoly.addPoint(-hw - ad, 0);
                newPoly.addPoint(-hw, -hh);
                newPoly.addPoint(hw, -hh);
                newPoly.addPoint(hw + ad, 0);
                newPoly.addPoint(hw, hh);
                newPoly.addPoint(-hw, hh);
                break;
            }
            case TRAPEZOID: {
                side += ad;
                newPoly.addPoint(-hw, -hh);
                newPoly.addPoint(hw, -hh);
                newPoly.addPoint(hw + ad, hh);
                newPoly.addPoint(-hw - ad, hh);
                break;
            }
            case INV_TRAPEZOID: {
                side += ad;
                newPoly.addPoint(-hw - ad, -hh);
                newPoly.addPoint(hw + ad, -hh);
                newPoly.addPoint(hw, hh);
                newPoly.addPoint(-hw, hh);
                break;
            }
            case PARALLELOGRAM: {
                side += ad;
                newPoly.addPoint(-hw, -hh);
                newPoly.addPoint(hw + ad, -hh);
                newPoly.addPoint(hw, hh);
                newPoly.addPoint(-hw - ad, hh);
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
                newPoly.addPoint(-hw - hh, 0);
                newPoly.addPoint(0, -hh - hw);
                newPoly.addPoint(hw + hh, 0);
                newPoly.addPoint(0, hh + hw);
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
                newPoly.addPoint(-hw - 4, -hh - 4);
                newPoly.addPoint(hw + 4, -hh - 4);
                newPoly.addPoint(hw + 4, hh + 4);
                newPoly.addPoint(-hw - 4, hh + 4);
                break;
            }
            case OCTAGON:
            case DOUBLE_OCTAGON:
            case TRIPLE_OCTAGON: {
                int dx = (width) / 3, dy = ad;
                updown += dy;
                newPoly.addPoint(-hw, -hh);
                newPoly.addPoint(-hw + dx, -hh - dy);
                newPoly.addPoint(hw - dx, -hh - dy);
                newPoly.addPoint(hw, -hh);
                newPoly.addPoint(hw, hh);
                newPoly.addPoint(hw - dx, hh + dy);
                newPoly.addPoint(-hw + dx, hh + dy);
                newPoly.addPoint(-hw, hh);
                if (shape() == DotShape.OCTAGON) {
                    break;
                }
                double c = sqrt(dx * dx + dy * dy), a = (dx * dy) / c, k = ((a + 5) * dy) / dx, r = sqrt((a + 5) * (a + 5) + k * k) - dy;
                double dx1 = ((r - 5) * dx) / dy, dy1 = -(((dx + 5D) * dy) / dx - dy - r);
                int x1 = (int) (round(dx1)), y1 = (int) (round(dy1));
                updown += 5;
                side += 5;
                poly2 = newPoly;
                newPoly = new Polygon();
                newPoly.addPoint(-hw - 5, -hh - y1);
                newPoly.addPoint(-hw + dx - x1, -hh - dy - 5);
                newPoly.addPoint(hw - dx + x1, -hh - dy - 5);
                newPoly.addPoint(hw + 5, -hh - y1);
                newPoly.addPoint(hw + 5, hh + y1);
                newPoly.addPoint(hw - dx + x1, hh + dy + 5);
                newPoly.addPoint(-hw + dx - x1, hh + dy + 5);
                newPoly.addPoint(-hw - 5, hh + y1);
                if (shape() == DotShape.DOUBLE_OCTAGON) {
                    break;
                }
                updown += 5;
                side += 5;
                poly3 = newPoly;
                newPoly = new Polygon();
                x1 = (int) (round(dx1 * 2));
                y1 = (int) (round(dy1 * 2));
                newPoly.addPoint(-hw - 10, -hh - y1);
                newPoly.addPoint(-hw + dx - x1, -hh - dy - 10);
                newPoly.addPoint(hw - dx + x1, -hh - dy - 10);
                newPoly.addPoint(hw + 10, -hh - y1);
                newPoly.addPoint(hw + 10, hh + y1);
                newPoly.addPoint(hw - dx + x1, hh + dy + 10);
                newPoly.addPoint(-hw + dx - x1, hh + dy + 10);
                newPoly.addPoint(-hw - 10, hh + y1);
                break;
            }
            case M_CIRCLE:
            case CIRCLE:
            case DOUBLE_CIRCLE: {
                //int radius = ((int) (sqrt(hw * ((double) hw) + ((double) hh) * hh))) + 2;
                int radius = hw; //[N7-G.Dupont] Correct bounding box for circles
                if (shape() == DotShape.DOUBLE_CIRCLE) {
                    radius = radius + 5;
                }
                int L = /*((int) (radius / cos18)) + 2*/radius, a = (int) (L * sin36), b = (int) (L * cos36), c = (int) (radius * tan18);
                newPoly.addPoint(-L, 0);
                newPoly.addPoint(-b, a);
                newPoly.addPoint(-c, L);
                newPoly.addPoint(c, L);
                newPoly.addPoint(b, a);
                newPoly.addPoint(L, 0);
                newPoly.addPoint(b, -a);
                newPoly.addPoint(c, -L);
                newPoly.addPoint(-c, -L);
                newPoly.addPoint(-b, -a);
                updown = radius;
                side = radius;
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
                break;
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
                newPoly.addPoint(-this.side, -this.updown);
                newPoly.addPoint( this.side, -this.updown);
                newPoly.addPoint( this.side,  this.updown);
                newPoly.addPoint(-this.side,  this.updown);
            }
        }
        if (shape() != DotShape.EGG && shape() != DotShape.ELLIPSE) {
            this.poly = newPoly;
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
     * [N7- R Bossut, M Quentin] Recalculate the boundaries of the nested
     * nodes given the current boundaries and the ones of its children.
     */
    void nestedNodeBounds() {
        if (!(hasChild() && (maxDepth > 0)))
          return;

        for (AbstractGraphNode gn : getChildren()) {
            gn.getWidth();
        }

        if (!layout){
          subGraph.layoutSubGraph();
          layout = true;
        }
        subGraph.recalcBound(true);
        recenterSubgraph();

        // Compute the max width of the labels 
        int maxLabelWidth = 0;
        List<String> labels = getLabels();
        for (int l = 0; l < labels.size(); l++) {
          maxLabelWidth = Math.max(maxLabelWidth, (int) getBounds(true, labels.get(l)).getWidth());
        }
        
        int height = subGraph.getTotalHeight() + getLabelHeight();
        int width = Math.max(subGraph.getTotalWidth(), maxLabelWidth) + GraphNode.xJumpNode;

        this.updown = height / 2;
        this.side = width / 2;

        subGraph.move((getLabelHeight()-yJumpNode)/2, -xJumpNode/2);
 
        graph.recalcLayerPH();
        
        //if (getFather() != null)
        //  getFather().nestedNodeBounds();

    }

    //[N7-R.Bossut, M.Quentin]
    /**
     * Recenter the subgraph of the node.
     * Changes top and left attributes of the subgraph so that they are equal to the x and the y of this node.
     */
    public void recenterSubgraph(){
      if (!hasChild()) {
        return;
      }
      int displacementTop = (- subGraph.getTotalHeight()/2 - subGraph.getTop()); 
      int displacementLeft = (- subGraph.getTotalWidth()/2 - subGraph.getLeft());
      subGraph.move(displacementTop, displacementLeft);
    }

    /**
     * [N7-G Dupont] Recalculate the boundaries of the node given the current
     * boundaries and the ports.
     */
    private void portBounds() {
        int vPort = 0, hPort = 0;
        
        // Padding due to port and label
        int maxVport = -1, maxHport = -1;
        for (GraphPort port : this.ports) {
            switch (port.getOrientation()) {
                case East:
                case West:
                    hPort = 1;
                    if (maxHport < port.getWidth())
                        maxHport = port.getWidth();
                    break;
                case South:
                case North:
                    vPort = 1;
                    if (maxVport < port.getHeight())
                        maxVport = port.getHeight();
                    break;
                default: // NE, NW, SE & SW
                    vPort = 1;
                    hPort = 1;
                    if (maxHport < port.getWidth())
                        maxHport = port.getWidth();
                    if (maxVport < port.getHeight())
                        maxVport = port.getHeight();
            }
        }
        
        if (vPort == 0 && hPort == 0) // No ports = no tweak
            return;
       
        int paddedSide = hPort*maxHport;
        int paddedUpdown = vPort*maxVport;
        
        // Padding due to port spacing
        // To simplify, we will say that we count the port as they would be on a
        // rectangle, with nortwest counting for both north and west
        int numN = 0, numE = 0, numS = 0, numW = 0;
        for (GraphPort port : this.ports) {
            switch (port.getOrientation()) {
                case North:
                    numN++;
                    break;
                case East:
                    numE++;
                    break;
                case South:
                    numS++;
                    break;
                case West:
                    numW++;
                    break;
                case NorthEast:
                    numN++; numE++;
                    break;
                case NorthWest:
                    numN++; numW++;
                    break;
                case SouthEast:
                    numS++; numE++;
                    break;
                case SouthWest:
                    numS++; numW++;
                    break;
                default:
                    //nop
            }
        }
        
        int minSide = (Math.max(numN, numS))*GraphPort.PortDistance;
        int minUpdown = (Math.max(numE, numW))*GraphPort.PortDistance;
        
        this.side += Math.max(paddedSide, minSide);
        this.updown += Math.max(paddedUpdown, minUpdown);
    }

   //===================================================================================================
    

    //===================================================================================================
    /**
     * Returns a DOT representation of this node (or "" if this is a dummy node)
     */
    @Override
    public String toString() {
        if (shape() == null) {
            return ""; // This means it's a virtual node
        }
        int rgb = this.getColor().getRGB() & 0xFFFFFF;
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
        out.append(", style = \"filled, " + this.getStyle().getDotText() + "\"");
        out.append("]\n");
        return out.toString();
    }
    
    @Override
    public void setHighlight(boolean h) {
        super.setHighlight(h);
        for (GraphPort p : this.ports) {
            p.setHighlight(h);
        }
    }
    
    /**
     * [N7] @Louis Fauvarque
     * Getter and Setter for needHighlight
     */
    
    public void setNeedHighlight(boolean newval){
        needHighlight = newval;
    }
    
    public boolean getNeedHighlight(){
        return needHighlight;
    }
}
