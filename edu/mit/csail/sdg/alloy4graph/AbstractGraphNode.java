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
    public final Object uuid;
    private int centerX, centerY;
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
     * The "self" edges; must stay in sync with GraphEdge.a and GraphEdge.b
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    final LinkedList<GraphEdge> selfs = new LinkedList<GraphEdge>();
    
    Graph graph;
    
    public AbstractGraphNode(Graph graph, Object uuid) {
        this.graph = graph;
        this.uuid = uuid;
    }
    
    abstract void draw(Artist gr, double scale, boolean highlights);
    
    abstract boolean contains(double x, double y);
    
    public int x() {
        return centerX;
    }
    
    public void setX(int x) {
        centerX = x;
    }
    
    public int y() {
        return centerY;
    }
    
    public void setY(int y) {
        centerY = y;
    }
    
    public DotShape shape() {
        return shape;
    }
    
    public void setShape(DotShape s) {
        shape = s;
    }
    
    public abstract int getWidth();
    public abstract int getHeight();
}
