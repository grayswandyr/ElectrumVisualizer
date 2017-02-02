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
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the concept of port.
 * 
 * A port is basically a tiny polygon attached to a node and that serves as
 * interface with the environment. This is only a way of representing things,
 * and can be set by the user (via a checkbox in the theme editor).
 * 
 * @author G. Dupont
 */
public class GraphPort {
    /** General Options **/
    
    /**
     * Defines the padding added arround the label.
     */
    private static final int LabbelPadding = 5;
    
    /**
     * Defines the color for representing a selected port.
     */
    private static final Color COLOR_CHOSENNODE = Color.LIGHT_GRAY;
    
    /**
     * This enumeration represents where is the port drawn.
     * This assume the node's shape is not too "funky"
     */
    enum Orientation {
        North("North"), // The node is on the top edge
        South("South"), // The node is on the bottom edge
        East("East"),  // The node is on the right edge
        West("West");  // The node is on the left edge
        
        private final String name;
        private Orientation(String s) {
            this.name = s;
        }
        
        public String toString() {
            return this.name;
        }
    }
    
    /**
     * List of available shapes.
     * Chosing a shape out of this list will result in an invisible node
     */
    public static final DotShape AvailableShapes[] = {
        DotShape.BOX,
        DotShape.CIRCLE
    };
    
    /**
     * Make a string list out of the available shapes (useful for debug and comboboxes).
     * @return A list of human readable string that represents the available shapes
     */
    public static List<String> AvailableShapesString() {
        List<String> result = new ArrayList<String>();
        for (DotShape s : GraphPort.AvailableShapes) {
            result.add(s.toString());
        }
        return result;
    }
    
    /** Attributes **/
    
    /// Non graphical ///
    /**
     * A user-provided annotation that will be associated with this node (can be
     * null) (need not be unique).
     */
    public final Object uuid;
    
    /**
     * The node this ports belongs to.
     */
    final GraphNode node;
    
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
    
    /// Graphical ///
    /**
     * Indicate that we should recalculate the position of the port.
     */
    private boolean needRecalc = true;
    
    /// Position ///
    /**
     * Position of the center of the port.
     */
    private int centerX, centerY;
    
    /**
     * Orientation of the port on the parent node.
     */
    private Orientation orientation;
    
    /**
     * Order in the ports.
     */
    private int order;
    
    /// Label ///
    /**
     * The port label; if null or empty, then the node has no labels.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private String label = null;
    
    /// Styling ///
    /**
     * The font boldness.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private boolean fontBold = false;

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

    /**
     * The port shape; if null, then the port is a dummy node.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private DotShape shape = DotShape.BOX;
    
    /**
     * "radius" of the port.
     */
    private int radius;
    
    /**
     * Cstr.
     * @param node
     * @param uuid 
     */
    public GraphPort(GraphNode node, Object uuid, String label, int order, Orientation or) {
        this.node = node;
        this.uuid = uuid;
        this.label = label;
        this.order = order;
        this.orientation = or;
        this.node.ports.add(this);
    }
    
    /**
     * Draw the port component with the Artist.
     * @param gr the artist with which to draw
     * @param scale scale of the drawing
     * @param highlight whether or not the node is highlighted
     */
    void draw(Artist gr, double scale, boolean highlight) {
        boolean available = false;
        for (DotShape s : GraphPort.AvailableShapes) {
            if (this.shape.equals(s))
                available = true;
        }
        
        if (!available) {
            System.out.println(
                "Shape " + this.shape + " is unavailable. \n" +
                "Available shapes are : " + String.join(",", GraphPort.AvailableShapesString())
            );
        }
        
        if (this.needRecalc)
            recalc();
        
        // Set style
        gr.set(this.style, scale);
        gr.setFont(this.fontBold);
        if (highlight) {
            gr.setColor(GraphPort.COLOR_CHOSENNODE);
        } else {
            gr.setColor(this.color);
        }
        
        // Draw port itself
        switch (this.shape) {
            case BOX:
                drawBox(gr);
                break;
            case CIRCLE:
                drawCircle(gr);
                break;
            default:
                // nop
        }
        
        System.out.println("Port [" + this.orientation + "," + this.order + "] drawn");
    }
    
    private void drawCircle(Artist gr) {
        gr.drawCircle(this.radius);
    }
    
    private void drawBox(Artist gr) {
        Shape s = new Rectangle(
                this.centerX - this.radius,
                this.centerY - this.radius,
                2*this.radius,
                2*this.radius
        );
        gr.draw(s, false);
    }
    
    /**
     * Recalculate position of the port according to data in the parent node.
     */
    void recalc() {
        int dist = getDistance();
        
        switch (this.orientation) {
            case East:
                this.centerX = this.node.getWidth();
                this.centerY = dist;
                break;
            case West:
                this.centerX = 0;
                this.centerY = dist;
                break;
            case North:
                this.centerX = dist;
                this.centerY = 0;
                break;
            case South:
                this.centerX = dist;
                this.centerY = this.node.getHeight();
                break;
            default:
                // nop
        }
        
        this.needRecalc = false;
    }
    
    private int getDistance() {
        int length;
        if (this.orientation == Orientation.North || this.orientation == Orientation.South)
            length = this.node.getWidth();
        else
            length = this.node.getHeight();
        int numports = this.node.numPorts(this.orientation);
        
        return length*(this.order+1)/(numports+1);
    }
    
    /// Accessors/Mutators ///
    public Orientation getOrientation() {
        return orientation;
    }

    public void setOrientation(Orientation orientation) {
        this.orientation = orientation;
        this.needRecalc = true;
    }

    public boolean isFontBold() {
        return fontBold;
    }

    public void setFontBold(boolean fontBold) {
        this.fontBold = fontBold;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public DotStyle getStyle() {
        return style;
    }

    public void setStyle(DotStyle style) {
        this.style = style;
    }

    public DotShape getShape() {
        return shape;
    }

    public void setShape(DotShape shape) {
        this.shape = shape;
        this.needRecalc = true;
    }
    
}

