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

import java.util.LinkedList;

/**
 * This class represent an abstract graph node.
 * It is a super class for GraphNode and GraphPort (so that an edge can
 * connect to a port).
 */
public abstract class AbstractGraphNode {
    /**
     * Graph element's object uuid.
     * This can be null; this has not to be unique.
     */
    public final Object uuid;
    
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
    
    /**
     * The graph the element belongs to.
     */
    Graph graph;
    
    /**
     * Constructor.
     * @param graph the graph this element belongs to
     * @param uuid the element's uuid
     */
    public AbstractGraphNode(Graph graph, Object uuid) {
        this.graph = graph;
        this.uuid = uuid;
    }
    
    /**
     * Draw the element thanks to given Artist.
     * @param gr the artist with which to draw the element
     * @param scale the scale to set the artist
     * @param highlights indicate if the element is highlighted (ie: selected/hovered)
     */
    abstract void draw(Artist gr, double scale, boolean highlights);
    
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
        shape = s;
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
}
