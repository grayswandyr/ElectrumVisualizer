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
import java.awt.geom.Rectangle2D;
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
    static final int LabelPadding = 5;
    
    /**
     * Defines the padding that separate a port from the center label of the node.
     */
    static final int PortPadding = 10;
    
    /**
     * Defines the minimal distance between two adjacent ports or from a port and
     * the side of the node.
     * (From center)
     */
    static final int PortDistance = 10;
    
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
    private int radius = 5;
    
    /**
     * Width and height (total) of the port.
     */
    private int width, height;
    
    /**
     * Coordinates of the label.
     */
    private int labelX, labelY;
    private double labelTheta;
    
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
        
        gr.translate(-this.node.getWidth() / 2, -this.node.getHeight() / 2);
        
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
        
        // Draw label
        gr.drawString(this.label, this.labelX, this.labelY, this.labelTheta);
        
        gr.translate(this.node.getWidth() / 2, this.node.getHeight() / 2);
    }
    
    private void drawCircle(Artist gr) {
        gr.translate(centerX, centerY);
        gr.fillCircle(this.radius);
        gr.setColor(Color.BLACK);
        gr.drawCircle(this.radius);
        gr.translate(-centerX, -centerY);
    }
    
    private void drawBox(Artist gr) {
        Shape s = new Rectangle(
                0,
                0,
                2*this.radius,
                2*this.radius
        );
        gr.translate(centerX - this.radius, centerY - this.radius);
        gr.draw(s, true);
        gr.setColor(Color.BLACK);
        gr.draw(s, false);
        gr.translate(this.radius - centerX, this.radius - centerY);
    }
    
    /**
     * Recalculate position of the port according to data in the parent node.
     */
    void recalc() {
        int dist = getDistance();
        
        // TODO: with label ?
        this.width = 2*this.radius;
        this.height = 2*this.radius;
        
        Rectangle2D labelRect = Artist.getBounds(this.fontBold, this.label);
        int labelWidth = (int)labelRect.getWidth(), labelHeight = (int)labelRect.getHeight();
        
        switch (this.orientation) {
            case East:
                this.centerX = this.node.getWidth();
                this.centerY = dist;
                this.labelX = this.centerX - this.radius - LabelPadding - labelWidth;
                this.labelY = this.centerY + this.radius;
                this.labelTheta = 0.0;
                break;
            case West:
                this.centerX = 0;
                this.centerY = dist;
                this.labelX = this.centerX + this.radius + LabelPadding;
                this.labelY = this.centerY + this.radius;
                this.labelTheta = 0.0;
                break;
            case South:
                this.centerX = dist;
                this.centerY = this.node.getHeight();
                this.labelX = this.centerX + this.radius;
                this.labelY = this.centerY - this.radius - LabelPadding;
                this.labelTheta = -Math.PI/2.0;
                break;
            case North:
                this.centerX = dist;
                this.centerY = 0;
                this.labelX = this.centerX + this.radius;
                this.labelY = this.centerY + this.radius + LabelPadding + labelWidth;
                this.labelTheta = -Math.PI/2.0;
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
    
    int getLabelSize() {
        if (this.label.length() == 0)
            return 0;
        
        Rectangle2D rect = Artist.getBounds(this.fontBold, this.label);
        
        /*if (this.orientation.equals(Orientation.North) || this.orientation.equals(Orientation.South))
            return (int)rect.getHeight();
        else
            return (int)rect.getWidth();*/
        return (int)rect.getWidth();
    }
    
    int getPortSize() {
        if (this.orientation.equals(Orientation.North) || this.orientation.equals(Orientation.South))
            return getHeight();
        else
            return getWidth();
    }
    
    int getSize() {
        return this.getLabelSize() + this.getPortSize() + LabelPadding;
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
    
    public int getWidth() {
        return this.width;
    }
    
    public int getHeight() {
        return this.height;
    }
    
}

