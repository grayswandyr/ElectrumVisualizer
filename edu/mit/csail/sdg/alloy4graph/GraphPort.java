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
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
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
        NorthWest("North West", "nw"),
        North    ("North"     , "n"),
        NorthEast("North East", "ne"),
        East     ("East"      , "e"),
        SouthEast("South East", "se"),
        South    ("South"     , "s"),
        SouthWest("South West", "sw"),
        West     ("West"      , "w");
        
        
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
    
    /**
     * Remark : orientation are always given CLOCKWISE and starting from NORTHWEST.
     * This is very important if we want to correctly guess the good side of the
     * poly where to place the port.
     */
    public static final Map<DotShape,Orientation[]> AvailableOrientations;
    static {
        // These combinations are pretty common
        final Orientation NESW[] = makeArray(Orientation.North, Orientation.East, Orientation.South, Orientation.West);
        final Orientation Every[] = makeArray(Orientation.NorthWest, Orientation.North, Orientation.NorthEast, Orientation.East, Orientation.SouthEast, Orientation.South, Orientation.SouthWest, Orientation.West);
        final Orientation Odds[] = makeArray(Orientation.NorthWest, Orientation.NorthEast, Orientation.SouthEast, Orientation.SouthWest);
        final Orientation None[] = {};
        
        AvailableOrientations = new HashMap<>();
        AvailableOrientations.put(DotShape.ELLIPSE       , NESW);
        AvailableOrientations.put(DotShape.BOX           , NESW);
        AvailableOrientations.put(DotShape.CIRCLE        , NESW);
        AvailableOrientations.put(DotShape.EGG           , None);
        AvailableOrientations.put(DotShape.TRIANGLE      , makeArray(Orientation.NorthWest, Orientation.NorthEast, Orientation.South));
        AvailableOrientations.put(DotShape.DIAMOND       , Odds);
        AvailableOrientations.put(DotShape.TRAPEZOID     , NESW);
        AvailableOrientations.put(DotShape.PARALLELOGRAM , NESW);
        AvailableOrientations.put(DotShape.HOUSE         , makeArray(Orientation.NorthWest, Orientation.NorthEast, Orientation.East, Orientation.South, Orientation.West));
        AvailableOrientations.put(DotShape.HEXAGON       , makeArray(Orientation.NorthWest, Orientation.North, Orientation.NorthEast, Orientation.SouthEast, Orientation.South, Orientation.SouthWest));
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
        DotShape.CIRCLE,
        DotShape.DIAMOND,
        DotShape.ELLIPSE,
        DotShape.TRIANGLE,
        DotShape.INV_TRIANGLE,
        DotShape.TRAPEZOID,
        DotShape.INV_TRAPEZOID
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
    
    /**
     * Return the postion of the point on the line (a,b) that is the kth
     * of n evenly spaced points.
     * E.g. : (with 3 points)
     *    A                   B
     *    +----+----+----+----+
     *         ^    ^    ^
     *         0    1    2
     * @param k number of the point
     * @param n number of points on the line
     * @param a first point of the line
     * @param b second point of the line
     * @return the cartesian coordinates of the requested point
     */
    static Point bar(int k, int n, Point a, Point b) {
        double coeff = (double)(k+1)/(double)(n+1);
        Point r = new Point();
        r.setLocation(
            coeff*(b.getX() - a.getX()) + a.getX(),
            coeff*(b.getY() - a.getY()) + a.getY()
        );
        return r;
    }
    
    /**
     * Get the cartesian coordinates of a point (M) on an ellipse (charaterized by
     * its demi axis a and b) from an angular measure.
     * @param theta angle between the abscisse (Ox) and the requeted point (OM)
     * @param a semiaxis major
     * @param b semiaxis minor
     * @return the coordinated of M
     */
    static Point angular(double theta, double a, double b) {
        Point r = new Point();
        r.setLocation(
            a*Math.cos(theta),
            -b*Math.sin(theta)
        );
        return r;
    }
    
    /**
     * Retrieve the angular offset given by an orientation (that is, the angle
     * between the abscisse line Ox and what is considered to be the beginning
     * of the arc corresponding to the requested orientation).
     * @param or requested orientation
     * @param a semiaxis major of the ellipse
     * @param b semiaxis minor of the ellipse
     * @return angular offset corresponding to the orientation
     * Remark: as a and b are divided, they can also be the axis major and axis
     * minor
     */
    static double orientAngle(Orientation or, double a, double b) {
        double r = 0.0;
        double th = Math.atan(b/a);
        switch (or) {
            case North:
                r += th;
                break;
            case East:
                r -= th;
                break;
            case South:
                r += Math.PI + th;
                break;
            case West:
                r += Math.PI - th;
                break;
            default:
                // this function will not be called with other orientations
                // so it is fine to stick with result = 0.0
        }
        return r;
    }
    
    /**
     * Return the angle between what is considered as the beginning of the arc
     * on target orientation and the kth point among n point evenly distributed
     * (that is, separated with the same angle) on an ellipse of semiaxis a and b.
     * @param k number of the point
     * @param n number of point on this arc
     * @param or requested orientation
     * @param a semiaxis major
     * @param b semiaxis minor
     * @return the angle of the requested point
     * Remark: as a and b are divided, they can also be the axis major and axis
     * minor
     */
    static double angularBarycenter(int k, int n, Orientation or, double a, double b) {
        double alpha = 0.0;
        double coeff = (double)(k+1)/(double)(n+1);
        if (or.equals(Orientation.North) || or.equals(Orientation.South)) {
            alpha = 2*Math.atan(a/b);
        } else if (or.equals(Orientation.East) || or.equals(Orientation.West)) {
            alpha = 2*Math.atan(b/a);
        }
        
        return alpha * coeff;
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
        int absoluteX = this.node.x() + this.x(),
            absoluteY = this.node.y() + this.y();
        switch(this.shape()) {
            case CIRCLE:
                double dx = (x - (double)absoluteX),
                       dy = (y - (double)absoluteY);
                result =
                        Math.sqrt(dx*dx + dy*dy) < this.radius
                ;
                break;
            default: // simple shape ~ box
                result =
                        (absoluteX - this.radius <= x && x <= absoluteX + this.radius) &&
                        (absoluteY - this.radius <= y && y <= absoluteY + this.radius)
                ;
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
        //gr.translate(-this.node.getWidth() / 2, -this.node.getHeight() / 2);
        
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
            case DIAMOND:
                drawDiamond(gr);
                break;
            case ELLIPSE:
                drawEllipse(gr);
                break;
            case TRIANGLE:
                drawTriangle(gr, false);
                break;
            case INV_TRIANGLE:
                drawTriangle(gr, true);
                break;
            case TRAPEZOID:
                drawTrapezoid(gr, false);
                break;
            case INV_TRAPEZOID:
                drawTrapezoid(gr, true);
                break;
            default:
                // nop
        }
        
        if (hovered)
            drawTooltip(gr, scale);
        
        gr.translate(-x(), -y());
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
     * [N7] @Julien Richer
     * Draw the port as a diamond
     * @param gr Artist on which to draw the port
     */
    private void drawDiamond(Artist gr) {
        // Create the diamond
        Polygon poly = new Polygon();
        poly.addPoint(-this.radius, 0);
        poly.addPoint(0, -this.radius);
        poly.addPoint(this.radius, 0);
        poly.addPoint(0, this.radius);
        // Draw a full diamond of the requested radius and color
        gr.draw(poly, true);
        // Draw a black hollow diamond of the requested radius
        gr.setColor(Color.BLACK);
        gr.draw(poly, false);
    }
    
    /**
     * [N7] @Julien Richer
     * Draw the port as an ellipse
     * @param gr Artist on which to draw the port
     */
    private void drawEllipse(Artist gr) {
        // Create the ellipse
        double side = this.radius;
        double updown = this.radius/1.5;
        GeneralPath path = new GeneralPath();
        path.moveTo(-side, 0);
        path.quadTo(-side, -updown, 0, -updown);
        path.quadTo(side, -updown, side, 0);
        path.quadTo(side, updown, 0, updown);
        path.quadTo(-side, updown, -side, 0);
        path.closePath();
        Shape ellipse = path;
        // Draw a full ellipse of the requested radius and color
        gr.draw(ellipse, true);
        // Draw a black hollow ellipse of the requested radius
        gr.setColor(Color.BLACK);
        gr.draw(ellipse, false);
    }
    
    /**
     * [N7] @Julien Richer
     * Draw the port as a triangle
     * @param gr Artist on which to draw the port
     */
    private void drawTriangle(Artist gr, boolean inv) {
        // Create the triangle
        Polygon poly = new Polygon();
        if(inv) {
            poly.addPoint(0, this.radius);
            poly.addPoint(this.radius, -this.radius);
            poly.addPoint(-this.radius, -this.radius);
        }
        else {
            poly.addPoint(0, -this.radius);
            poly.addPoint(this.radius, this.radius);
            poly.addPoint(-this.radius, this.radius);
        }
        // Draw a full triangle of the requested radius and color
        gr.draw(poly, true);
        // Draw a black hollow triangle of the requested radius
        gr.setColor(Color.BLACK);
        gr.draw(poly, false);
    }
    
    /**
     * [N7] @Julien Richer
     * Draw the port as a trapezoid
     * @param gr Artist on which to draw the port
     */
    private void drawTrapezoid(Artist gr, boolean inv) {
        // Create the trapezoid
        int w = this.radius-2;
        Polygon poly = new Polygon();
        if(inv) {
            poly.addPoint(w, this.radius);
            poly.addPoint(this.radius, -this.radius);
            poly.addPoint(-this.radius, -this.radius);
            poly.addPoint(-w, this.radius);
        }
        else {
            poly.addPoint(w, -this.radius);
            poly.addPoint(this.radius, this.radius);
            poly.addPoint(-this.radius, this.radius);
            poly.addPoint(-w, -this.radius);
        }
        // Draw a full trapezoid of the requested radius and color
        gr.draw(poly, true);
        // Draw a black hollow trapezoid of the requested radius
        gr.setColor(Color.BLACK);
        gr.draw(poly, false);
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
    private void recalc() {
        this.node.getWidth(); // Cause recalc
        int numports = this.node.numPorts.get(this.orientation);
        int x = 0, y = 0;
        
        if (this.node.shape().equals(DotShape.ELLIPSE) || this.node.shape().equals(DotShape.CIRCLE)) {
            double a = this.node.getWidth(), b = this.node.getHeight(); // In any case, we will divide this by 2
            
            if (this.node.shape().equals(DotShape.CIRCLE)) {
                // With the circle shape, the "bounding box" given is actually the insquare
                // of the circle, so we have 2*R = sqrt(w*w + h*h) (if w is the width of the
                // bounding box and h is its height)
                a = Math.sqrt(a*a + b*b);
                b = a;
            }
            a /= 2.0; // semiaxis major
            b /= 2.0; // semiaxis minor
            
            double offset = GraphPort.orientAngle(this.orientation, a, b);
            double angbar = GraphPort.angularBarycenter(this.order, numports, this.orientation, a, b);
            Point pt = GraphPort.angular(offset + angbar, a, b);
            x = pt.x;
            y = pt.y;
        } else if (this.node.poly() instanceof Polygon) {
            /*
             * We know that north xor northwest is always the first side of the polygon, and
             * that every polygon is formed so that the first side is the north xor northwest one
             * and with its sides put clockwise.
             * 
             * Our goal is to get the two adjacent points forming the side of the requested
             * orientation so that we can apply the barycenter to them.
             * 
             * So the principle is to guess the side number based on the available orientations
             * for this node shape, and then get the two adjacent points of this shape corresponding
             * to this side "number" (we factorize code by going through these points and these
             * orientations).
             */
            Orientation ors[] = GraphPort.AvailableOrientations.get(this.node.shape());
            int polyx[] = ((Polygon)this.node.poly()).xpoints;
            int polyy[] = ((Polygon)this.node.poly()).ypoints;
            int numsides = polyx.length;
            Point a = new Point(), b = new Point();
            
            int i = -1;
            do {
                i++;
                a.x = polyx[i]; a.y = polyy[i];
                b.x = polyx[(i+1)%numsides]; b.y = polyy[(i+1)%numsides];
            } while (i < ors.length && !ors[i].equals(this.orientation));
            
            if (i >= ors.length) { // have not found the orientation in the authorized one... oops ?
                System.err.println("Error: this port is not on a valid orientation");
                return;
            }
            
            // Now, [a,b] is the side corresponding to the requested orientation
            // Just have to apply the magic barycenter function !
            Point g = GraphPort.bar(this.order, numports, a, b);
            x = g.x;
            y = g.y;
        } else {
            System.err.println("Error: cannot recalc port position because parent node is neither an ellipse/circle nor a standard polygon");
            return;
        }
        
        this.setX(x);
        this.setY(y);
        this.needRecalc = false;
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

