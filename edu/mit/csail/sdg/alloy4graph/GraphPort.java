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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the concept of port.
 * 
 * A port is basically a tiny polygon attached to a node and that serves as
 * interface with the environment. This is only a way of representing things,
 * and can be set by the user (via a checkbox in the theme editor).
 * 
 * @author G. Dupont
 */
public class GraphPort extends AbstractGraphNode {
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
     * Font size for the port label.
     */
    private int fontSize = 8;
    
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
     * The node this ports belongs to.
     */
    final GraphNode node;
    
    /// Graphical ///
    /**
     * Indicate that we should recalculate the position of the port.
     */
    private boolean needRecalc = true;
    
    /// Position ///
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
    public GraphPort(GraphNode node, Object uuid, String label, Orientation or) {
        super(node.graph, uuid);
        this.node = node;
        this.label = label;
        this.orientation = or;
        this.node.ports.add(this);
        
        this.order = this.node.incNumPorts(or);
    }
    
    /**
     * Determines if coordinates are inside the port
     * @param x x coordinate
     * @param y y coordinate
     * @return true if (x,y) is in the port [CURRENTLY: FALSE]
     */
    @Override
    boolean contains(double x, double y) {
        return false;
    }
    
    /**
     * Draw the port component with the Artist.
     * @param gr the artist with which to draw
     * @param scale scale of the drawing
     * @param highlight whether or not the node is highlighted
     */
    @Override
    void draw(Artist gr, double scale) {
        // Check if the requested shape is available
        boolean available = false;
        for (DotShape s : GraphPort.AvailableShapes) {
            if (shape().equals(s))
                available = true;
        }
        
        if (!available) {
            System.out.println(
                "Shape " + shape() + " is unavailable. \n" +
                "Available shapes are : " + String.join(",", GraphPort.AvailableShapesString())
            );
        }
        
        // If we need to recalc (boundaries)
        if (this.needRecalc)
            recalc();
        
        // Set style
        gr.set(this.style, scale);
        gr.setFont(this.fontBold);
        if (this.highlight()) {
            gr.setColor(GraphPort.COLOR_CHOSENNODE);
        } else {
            gr.setColor(this.color);
        }
        
        // Translate to the bottom left hand corner of the node (easier for calculi)
        gr.translate(-this.node.getWidth() / 2, -this.node.getHeight() / 2);
        
        // Draw port itself
        switch (shape()) {
            case BOX:
                drawBox(gr);
                break;
            case CIRCLE:
                drawCircle(gr);
                break;
            default:
                // nop
        }
        
        // Draw label with requested font size
        int fontTemp = gr.getFontSize();
        gr.setFontSize(this.fontSize);
        gr.drawString(this.label, this.labelX, this.labelY, this.labelTheta);
        gr.setFontSize(fontTemp);
        
        // Translate back the system where it was before (hopefully)
        gr.translate(this.node.getWidth() / 2, this.node.getHeight() / 2);
    }
    
    /**
     * Draw the port as a circle
     * @param gr Artist on which to draw the port
     */
    private void drawCircle(Artist gr) {
        // Select center
        gr.translate(x(), y());
        // Draw a full circle of the requested radius and color
        gr.fillCircle(this.radius);
        // Draw a black hollow circle of the requested radius
        gr.setColor(Color.BLACK);
        gr.drawCircle(this.radius);
        // Restore system
        gr.translate(-x(), -y());
    }
    
    /**
     * Draw the port as a box
     * @param gr Artist on which to draw the port
     */
    private void drawBox(Artist gr) {
        // Create the actual shape, a square of side 2*radius
        Shape s = new Rectangle(
                0,
                0,
                2*this.radius,
                2*this.radius
        );
        
        // Translate to have the center of the square at (centerX,centerY)
        gr.translate(x() - this.radius, y() - this.radius);
        // Draw the shape filled with requested color
        gr.draw(s, true);
        // Draw the outline of the shape
        gr.setColor(Color.BLACK);
        gr.draw(s, false);
        // Restore system
        gr.translate(this.radius - x(), this.radius - y());
    }
    
    /**
     * Recalculate position of the port according to data in the parent node.
     */
    void recalc() {
        int dist = getDistance();
        int labelWidth = getLabelSize();
        
        // Recalculate width and height
        this.width = 2*this.radius;
        this.height = 2*this.radius;
        
        // Compute port and label position
        switch (this.orientation) {
            case East:
                setX(this.node.getWidth());
                setY(dist);
                this.labelX = x() - this.radius - LabelPadding - labelWidth;
                this.labelY = y() + this.radius;
                this.labelTheta = 0.0;
                break;
            case West:
                setX(0);
                setY(dist);
                this.labelX = x() + this.radius + LabelPadding;
                this.labelY = y() + this.radius;
                this.labelTheta = 0.0;
                break;
            case South:
                setX(dist);
                setY(this.node.getHeight());
                this.labelX = x() + this.radius;
                this.labelY = y() - this.radius - LabelPadding;
                this.labelTheta = -Math.PI/2.0;
                break;
            case North:
                setX(dist);
                setY(0);
                this.labelX = x() + this.radius;
                this.labelY = y() + this.radius + LabelPadding + labelWidth;
                this.labelTheta = -Math.PI/2.0;
                break;
            default:
                // nop
        }
        
        this.needRecalc = false;
    }
    
    /**
     * Get the vertical (E/W) or horizontal (N/S) distance between the center of the port
     * and the origin of the node
     * @return the requested distance
     */
    private int getDistance() {
        int length;
        if (this.orientation == Orientation.North || this.orientation == Orientation.South)
            length = this.node.getWidth();
        else
            length = this.node.getHeight();
        
        int numports = this.node.numPorts.get(this.orientation);
        
        return length*(this.order+1)/(numports+1); // Simple barycenter calculus
    }
    
    /**
     * Get the label width (which is a vertical distance for N/S ports and a horizontal one for E/W). 
     * @return the label size
     */
    int getLabelSize() {
        if (this.label.length() == 0)
            return 0;
        
        Rectangle2D rect = Artist.getBounds(this.fontBold, this.label, this.fontSize);
        return (int)rect.getWidth();
    }
    
    /**
     * Get the size of the port (just the shape).
     * This is a horizontal distance if port is E/W and a vertical one if it is N/S
     * @return the size of the shape
     */
    int getPortSize() {
        if (this.orientation.equals(Orientation.North) || this.orientation.equals(Orientation.South))
            return getHeight();
        else
            return getWidth();
    }
    
    /**
     * Get the full size of the port (including label and padding).
     * This is a horizontal distance if port is E/W and a vertical one if it is N/S
     * @return the full size
     */
    int getSize() {
        return this.getLabelSize() + this.getPortSize() + LabelPadding;
    }
    
    /// Accessors/Mutators ///
    /**
     * Get the side on which the port is.
     * @return the orientation
     */
    public Orientation getOrientation() {
        return orientation;
    }
    
    /**
     * Set the side on which the port is.
     * The port will be appended to the existing ports on that side.
     * @param orientation the new orientation
     */
    public void setOrientation(Orientation orientation) {
        // We need to reflect the change on other port (determine a new order, etc.)
        // Properly remove the port from old side
        this.node.decNumPorts(this.orientation);
        for (GraphPort gp : this.node.ports) {
            if (gp.getOrientation().equals(this.orientation) && gp.order > this.order)
                gp.order--;
        }
        
        // Append the port to the right side
        this.orientation = orientation;
        this.order = this.node.incNumPorts(orientation);
        
        // In the end, update attributes
        this.needRecalc = true;
    }

    /**
     * Test if the font is bold
     * @return true if font is bold
     */
    public boolean isFontBold() {
        return fontBold;
    }

    /**
     * Set the boldness of the font
     * @param fontBold new boldness (true = bold)
     */
    public void setFontBold(boolean fontBold) {
        this.fontBold = fontBold;
    }
    
    /**
     * Get the font size
     * @return font size
     */
    public int getFontSize() {
        return fontSize;
    }
    
    /**
     * Set the font size
     * @param fs new font size
     */
    public void setFontSize(int fs) {
        this.fontSize = fs;
    }

    /**
     * Get port color
     * @return port colro
     */
    public Color getColor() {
        return color;
    }

    /**
     * Set port color
     * @param color new color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Get port line style
     * @return the port line style
     */
    public DotStyle getStyle() {
        return style;
    }

    /**
     * Set port line style
     * @param style new port line style
     */
    public void setStyle(DotStyle style) {
        this.style = style;
    }
    
    /**
     * Get the order of the port on its side
     * @return the order of the port
     */
    public int getOrder() {
        return this.order;
    }

    /**
     * Set the shape of the port
     * @param shape new shape
     */
    @Override
    public void setShape(DotShape shape) {
        super.setShape(shape);
        this.needRecalc = true;
    }
    
    /**
     * Retrieve port width
     * @return port width
     */
    @Override
    public int getWidth() {
        if (needRecalc) recalc();
        return this.width;
    }
    
    /**
     * Retrieve port height
     * @return 
     */
    @Override
    public int getHeight() {
        if (needRecalc) recalc();
        return this.height;
    }
    
}

