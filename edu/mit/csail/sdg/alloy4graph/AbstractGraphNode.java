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

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import javax.swing.JLabel;
import java.util.LinkedList;

/**
 * This class represent an abstract graph node.
 * It is a super class for GraphNode and GraphPort (so that an edge can
 * connect to a port).
 */
public abstract class AbstractGraphNode extends AbstractGraphElement {
    /**
     * Coordinates of the center of the element.
     */
    private int centerX, centerY;
    
    /**
     * General shape of the element.
     */
    private DotShape shape = DotShape.BOX;
    
    /**
     * The "in" edges not including "self" edges.
     * Must stay in sync with GraphEdge.a and GraphEdge.b
     */
    final LinkedList<GraphEdge> ins = new LinkedList<GraphEdge>();

    /**
     * The "out" edges not including "self" edges.
     * Must stay in sync with GraphEdge.a and GraphEdge.b
     */
    final LinkedList<GraphEdge> outs = new LinkedList<GraphEdge>();
    
    /**
     * The "self" edges; must stay in sync with GraphEdge.a and GraphEdge.b .
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    final LinkedList<GraphEdge> selfs = new LinkedList<GraphEdge>();
    
    /// Graphical attributes ///
    /**
     * The font boldness.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private boolean fontBoldness = false;
    
    //===================================================================================================  
    // Attributes needed for imbrication.
    /**
     * This field contains the children of this node if they exist.
     */
    private HashSet<AbstractGraphNode> children = new HashSet<>(); //[N7-R. Bossut, M. Quentin];

    /**
     * This field contains the subGraph of the node if it exists.
     */
    /*package*/ Graph subGraph = null;
    /**
     * The integer representing the maximum depth level of representation of
     * subgraphs of this GraphNode.
     */
    /*package*/ int maxDepth;
    
    /**
     * This field contains the father of the node if it exists.
     */
    private AbstractGraphNode father = null;
    
    /**
     * The current position of this node in the graph's node list; must stay in
     * sync with Graph.nodelist.
     */
    int pos;
    
    /**
     * Constructor.
     * @param graph the graph this element belongs to
     * @param uuid the element's uuid
     */
    public AbstractGraphNode(Graph graph, Object uuid, AbstractGraphNode fat) {
        super(graph, uuid);
        this.father = fat;
        this.maxDepth = 1;
    }
    
    /**
     * Get the children of the Node. [N7-R. Bossut, M. Quentin]
     */
    public HashSet<AbstractGraphNode> getChildren() {
        return this.children;
    }

    /**
     * Get the SubGraph of the Node. [N7-R. Bossut, M. Quentin]
     */
    public Graph getSubGraph() {
        subGraph = (subGraph == null) ? new Graph(1.0, this.graph.sgm, this.graph.instance) : this.subGraph;
        return (subGraph);
    }

    /**
     * Set the father of the Node. [N7-R. Bossur, M. Quentin]
     */
    public void setFather(AbstractGraphNode father) {
        this.father = father;
    }

    public boolean hasChild() {
        return !(children.isEmpty());
    }

    /**
     * Add a child to the family of the Node. [N7-R. Bossut, M. Quentin]
     */
    public void addChild(AbstractGraphNode gn) {
        this.children.add(gn);
        if (subGraph == null) subGraph = new Graph(1.0, this.graph.sgm, this.graph.instance);
        if (!(subGraph.nodelist.contains(gn))) {
            subGraph.nodelist.add(gn);
        }
    }

    /** 
     * Get the maxDepth of the element.
     * This function is abstract as it depends of the type of node.
     * @return the maximum depth of this element.
     */
    public int getMaxDepth() {
        return maxDepth;
    }
    
    /**
     * Draw the element thanks to given Artist.
     * @param gr the artist with which to draw the element
     * @param scale the scale to set the artist
     * @param highGroup the group that is highlighted when we are drawing this element.
     */
    abstract void draw(Artist gr, double scale, Object highgroup);
    
    /**
     * Determines if coordinates are inside/on the element.
     * @param x x coordinate to test
     * @param y y coordinate to test
     * @return true if the point (x,y) is inside the shape of the element
     */
    abstract boolean contains(double x, double y);
    
    /**
     * Get the x coordinate of the center of the element.
     * @return centerX
     */
    public int x() {
        return centerX;
    }
    
    /**
     * Set the x coordinate of the center of the element
     * @param x new x coordinate
     */
    public void setX(int x) {
        centerX = x;
    }
    
    /**
     * Get the y coordinate of the center of the element.
     * @return centerY
     */
    public int y() {
        return centerY;
    }
    
    /**
     * Set the y coordinate of the center of the element
     * @param y new y coordinate
     */
    public void setY(int y) {
        centerY = y;
    }

    /**
     * Moves the node following the given displacement.
     * @param dx displacement for x.
     * @param dy displacement for y
     */
    public void move(int dx, int dy){
      centerX += dx;
      centerY += dy;
    }
    
    /**
     * Get the shape of the element
     * @return shape of the element
     */
    public DotShape shape() {
        return shape;
    }
    
    /**
     * Set the shape of the element
     * @param s new shape for the element
     */
    public void setShape(DotShape s) {
        if(s == DotShape.DUMMY){
            shape = null;
        } else {
            shape = s;
        }
    }
    
    public boolean getFontBoldness() {
        return fontBoldness;
    }
    
    public void setFontBoldness(boolean fb) {
        fontBoldness = fb;
    }
    
    /**
     * Get the width of the element.
     * This function is abstract as it depends on various specific graphic attributes :
     * label, font, padding, etc.
     * @return the width of the element.
     */
    public abstract int getWidth();
    
    /**
     * Get the height of the element.
     * This function is abstract as it depends on various specific graphic attributes :
     * label, font, padding, etc.
     * @return the height of the element.
     */
    public abstract int getHeight();
    
    /**
     * Gets the layer of the element
     * @return number of the layer
     */
    abstract int layer();
    
    /**
     * Get the father of this node.
     * In the case where it is a node, it will return the father.
     * In the case where it is a port, it will return the father of the parent node.
     */
    public AbstractGraphNode getFather() {
        return this.father;
    }
    
    /**
     * Returns true if the given node is an ancestor of this.
     * @param n the node we want to know if this is in it.
     */
    public boolean isContainedIn(AbstractGraphNode n) {
        return (getFather() == n) || (getFather() != null && getFather().isContainedIn(n));
    }
    
    /**
     * Returns true if the GraphNode is in the given Graph.
     *
     * @param graph the graph in which we want to know if the GraphNode is.
     */
    public boolean isInGraph(Graph g) {
        return (this.graph == g);
    }
    
    /**
     * Express an X coordinate in a reference into another reference
     * @param startx initial coordinate
     * @param rstart start reference
     * @param rtarget target reference
     * @return new coordinate
     */
    public static double transformX(double startx, AbstractGraphNode rstart, AbstractGraphNode rtarget) {
        double x = startx;
        AbstractGraphNode n = rstart;
        while (n != null && n.getFather() != null) {
            n = n.getFather();
            x += n.x();
        }
        n = rtarget;
        while (n != null) {
            x -= n.x();
            n = n.getFather();
        }
        return x;
    }
    
    /**
     * Express an Y coordinate in a reference into another reference
     * @param starty initial coordinate
     * @param rstart start reference
     * @param rtarget target reference
     * @return new coordinate
     */
    public static double transformY(double starty, AbstractGraphNode rstart, AbstractGraphNode rtarget) {
        double y = starty;
        AbstractGraphNode n = rstart;
        while (n != null && n.getFather() != null) {
            n = n.getFather();
            y += n.y();
        }
        n = rtarget;
        while (n != null) {
            y -= n.y();
            n = n.getFather();
        }
        return y;
    }
    
    /**
     * Express the X coordinate of this node relatively to another node
     * @param root the other node to express the coordinate in
     * @return the relative X coordinate
     */
    public double relativeX(AbstractGraphNode root) {
        return transformX(this.x(), this, root);
    }
    
    /**
     * Express the Y coordinate of this node relatively to another node
     * @param root the other node to express the coordinate in
     * @return the relative Y coordinate
     */
    public double relativeY(AbstractGraphNode root) {
        return transformY(this.y(), this, root);
    }
    
    /**
     * Express the global X coordinate
     * @return the global X coordinate in the root reference
     */
    public double absoluteX() {
        return relativeX(null);
    }
    
    /**
     * Express the global Y coordinate
     * @return the global Y coordinate in the root reference
     */
    public double absoluteY() {
        return relativeY(null);
    }
    
    /**
     * Returns the bounding rectangle (with 2*xfluff added to the width, and
     * 2*yfluff added to the height)
     */
    Rectangle2D getBoundingBox(int xfluff, int yfluff) {
        return new Rectangle2D.Double(x() - getWidth()/2 - xfluff, y() - getHeight()/2 - yfluff, getWidth() + xfluff + xfluff, getHeight() + yfluff + yfluff);
    }
    
    /**
     * Returns the amount of space we need to reserve on the right hand side for
     * the self edges (0 if this has no self edges now)
     */
    abstract int getReserved();
}
