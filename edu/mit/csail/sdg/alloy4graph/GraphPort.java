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

import edu.mit.csail.sdg.alloy4.OurUtil;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Icon;

/**
 * This class represents the concept of port.
 * 
 * A port is basically a tiny polygon attached to a node and that serves as
 * interface with the environment. This is only a way of representing things,
 * and can be set by the user (via a checkbox in the theme editor).
 * 
 * Note: the coordinates of the port are <b>relative to the node</b>, not to the
 * port. So the absolute coordinates of the center of the port is :
 *      (NodeLeft + PortX, NodeTop + PortY)
 * 
 * @author G. Dupont
 */
public class GraphPort extends AbstractGraphNode {
    /** General Options **/
    
    /**
     * Defines the padding added arround the label.
     */
    static final int LabelPaddingLeft = 7, LabelPaddingTop = 5;
    
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
    public enum Orientation {
        North    ("North"     , "n"),
        NorthEast("North East", "ne"),
        East     ("East"      , "e"),
        SouthEast("South East", "se"),
        South    ("South"     , "s"),
        SouthWest("South West", "sw"),
        West     ("West"      , "w"),
        NorthWest("North West", "nw");
        
        private final String name;
        private final Icon icon;
        
        private Orientation(String s, String sn) {
            this.name = s;
            this.icon = OurUtil.loadIcon("icons/OrientationIcons/arrow_" + sn + ".gif");
        }
        
        public String toString() {
            return this.name;
        }
        
        public Icon getIcon() {
            return this.icon;
        }
    }
    
    private static Orientation[] makeArray(Orientation... ors) {
        return ors;
    }
    
    public static final Map<DotShape,Orientation[]> AvailableOrientations;
    static {
        // These combinations are pretty common
        final Orientation NESW[] = makeArray(Orientation.North, Orientation.South, Orientation.West, Orientation.East);
        final Orientation Every[] = makeArray(Orientation.North, Orientation.NorthEast, Orientation.East, Orientation.SouthEast, Orientation.South, Orientation.SouthWest, Orientation.West, Orientation.NorthWest);
        final Orientation Odds[] = makeArray(Orientation.NorthEast, Orientation.SouthEast, Orientation.SouthWest, Orientation.NorthWest);
        final Orientation None[] = {};
        
        AvailableOrientations = new HashMap<>();
        AvailableOrientations.put(DotShape.ELLIPSE       , NESW);
        AvailableOrientations.put(DotShape.BOX           , NESW);
        AvailableOrientations.put(DotShape.CIRCLE        , NESW);
        AvailableOrientations.put(DotShape.EGG           , None);
        AvailableOrientations.put(DotShape.TRIANGLE      , makeArray(Orientation.NorthEast, Orientation.NorthWest, Orientation.South));
        AvailableOrientations.put(DotShape.DIAMOND       , Odds);
        AvailableOrientations.put(DotShape.TRAPEZOID     , NESW);
        AvailableOrientations.put(DotShape.PARALLELOGRAM , NESW);
        AvailableOrientations.put(DotShape.HOUSE         , makeArray(Orientation.NorthEast, Orientation.East, Orientation.South, Orientation.West, Orientation.NorthWest));
        AvailableOrientations.put(DotShape.HEXAGON       , makeArray(Orientation.North, Orientation.NorthEast, Orientation.SouthEast, Orientation.South, Orientation.SouthWest, Orientation.NorthWest));
        AvailableOrientations.put(DotShape.OCTAGON       , Every);
        AvailableOrientations.put(DotShape.DOUBLE_CIRCLE , NESW);
        AvailableOrientations.put(DotShape.DOUBLE_OCTAGON, Every);
        AvailableOrientations.put(DotShape.TRIPLE_OCTAGON, Every);
        AvailableOrientations.put(DotShape.INV_TRIANGLE  , makeArray(Orientation.North, Orientation.SouthEast, Orientation.SouthWest));
        AvailableOrientations.put(DotShape.INV_HOUSE     , makeArray(Orientation.North, Orientation.East, Orientation.SouthEast, Orientation.SouthWest, Orientation.West));
        AvailableOrientations.put(DotShape.INV_TRAPEZOID , NESW);
        AvailableOrientations.put(DotShape.M_DIAMOND     , Odds);
        AvailableOrientations.put(DotShape.M_SQUARE      , NESW);
        AvailableOrientations.put(DotShape.M_CIRCLE      , NESW);
        AvailableOrientations.put(DotShape.DUMMY         , None);
    }
    
    public static boolean IsShapeAuthorized(DotShape s) {
        return AvailableOrientations.get(s).length > 0;
    }
    
    public static boolean IsOrientationAuthorized(DotShape s, Orientation o) {
        Orientation ors[] = AvailableOrientations.get(s);
        for (Orientation or : ors) {
            if (or.equals(o))
                return true;
        }
        return false;
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
    
    private boolean hovered = false;
    
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
     * "radius" of the port.
     */
    private int radius = 5;
    
    /**
     * Width and height (total) of the port.
     */
    private int width, height;
    
    /**
     * Cstr.
     * @param node parent node for the port
     * @param uuid uuid of the port
     * @param label label of the port
     * @param or side of the node on which the port will be
     */
    public GraphPort(GraphNode node, Object uuid, String label, Orientation or) {
        super(node.graph, uuid);
        this.node = node;
        this.label = label;
        this.orientation = or;
        this.node.ports.add(this);
        
        this.order = this.node.incNumPorts(or);
        
        this.setFontSize(8);
        recalc();
    }
    
    /**
     * Determines if coordinates are inside the port
     * @param x x coordinate
     * @param y y coordinate
     * @return true if (x,y) is in the port
     */
    @Override
    boolean contains(double x, double y) {
        if (this.shape() == null)
            return false;
        
        boolean result = false;
        int absoluteX = this.node.x() - this.node.getWidth() / 2 + this.x(),
            absoluteY = this.node.y() - this.node.getHeight() / 2 + this.y();
        switch(this.shape()) {
            case BOX:
                result =
                        (absoluteX - this.radius <= x && x <= absoluteX + this.radius) &&
                        (absoluteY - this.radius <= y && y <= absoluteY + this.radius)
                ;
                break;
            case CIRCLE:
                double dx = (x - (double)absoluteX),
                       dy = (y - (double)absoluteY);
                result =
                        Math.sqrt(dx*dx + dy*dy) < this.radius
                ;
                break;
            default:
                // nop
        }
        return result;
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
        if (this.needRecalc){
            recalc();
            for (GraphEdge e : this.outs){
                e.resetPath();
            }
        }
        
        // Set style
        gr.set(this.getStyle(), scale);
        gr.setFont(this.getFontBoldness());
        if (highlight) {
            gr.setColor(GraphPort.COLOR_CHOSENNODE);
        } else {
            gr.setColor(this.getColor());
        }
        
        // Translate to the top left hand corner of the node (easier for calculi)
        gr.translate(-this.node.getWidth() / 2, -this.node.getHeight() / 2);
        
        // Then, translate to the center of the port
        gr.translate(x(), y());
        
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
        
        if (hovered)
            drawTooltip(gr, scale);
        
        // Translate back the system where it was before (hopefully)
        gr.translate(this.node.getWidth() / 2 - x(), this.node.getHeight() / 2 - y());
    }
    
    /**
     * Draw the port as a circle
     * @param gr Artist on which to draw the port
     */
    private void drawCircle(Artist gr) {
        // Draw a full circle of the requested radius and color
        gr.fillCircle(this.radius);
        // Draw a black hollow circle of the requested radius
        gr.setColor(Color.BLACK);
        gr.drawCircle(this.radius);
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
        gr.translate(-this.radius, -this.radius);
        // Draw the shape filled with requested color
        gr.draw(s, true);
        // Draw the outline of the shape
        gr.setColor(Color.BLACK);
        gr.draw(s, false);
        // Restore system
        gr.translate(this.radius, this.radius);
    }
    
    /**
     * Draw the port's label as a tooltip.
     */
    public void drawTooltip(Artist gr, double scale) {
        Rectangle2D rect = Artist.getBounds(this.getFontBoldness(), this.label);
        int width = (int)rect.getWidth(), height = (int)rect.getHeight();
        
        Rectangle s = new Rectangle(0, 0, width + 2*LabelPaddingLeft, height + 2*LabelPaddingTop);
        gr.translate(0, -s.height);
        gr.setColor(Color.LIGHT_GRAY);
        gr.draw(s, true);
        gr.setColor(Color.BLACK);
        gr.set(DotStyle.SOLID, scale);
        gr.draw(s, false);
        gr.drawString(this.label, LabelPaddingLeft, height + LabelPaddingTop, 0);
        gr.translate(0, s.height);
    }
    
    /**
     * Recalculate position of the port according to data in the parent node.
     */
    void recalc() {
        int dist = getDistance();
        
        // Compute port and label position
        switch (this.orientation) {
            case East:
                setX(this.node.getWidth());
                setY(dist);
                break;
            case West:
                setX(0);
                setY(dist);
                break;
            case South:
                setX(dist);
                setY(this.node.getHeight());
                break;
            case North:
                setX(dist);
                setY(0);
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
     * Get the full size of the port (including label and padding).
     * This is a horizontal distance if port is E/W and a vertical one if it is N/S
     * @return the full size
     */
    int getSize() {
        if (this.orientation.equals(Orientation.North) || this.orientation.equals(Orientation.South))
            return getHeight();
        else
            return getWidth();
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
        this.width = 2*this.radius;
        return this.width;
    }
    
    /**
     * Retrieve port height
     * @return 
     */
    @Override
    public int getHeight() {
        this.height = 2*this.radius;
        return this.height;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setHovered(boolean h) {
        this.hovered = h;
    }
    
}

