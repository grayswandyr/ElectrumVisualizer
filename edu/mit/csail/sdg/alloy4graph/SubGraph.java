/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.csail.sdg.alloy4graph;

import edu.mit.csail.sdg.alloy4.Pair;
import static edu.mit.csail.sdg.alloy4graph.Artist.getBounds;
import static edu.mit.csail.sdg.alloy4graph.Graph.selfLoopA;
import static edu.mit.csail.sdg.alloy4graph.Graph.selfLoopGL;
import static edu.mit.csail.sdg.alloy4graph.Graph.selfLoopGR;
import static edu.mit.csail.sdg.alloy4graph.GraphNode.sqrt3;
import java.awt.Color;
import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import static java.lang.StrictMath.round;
import static java.lang.StrictMath.sqrt;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mquentin
 */
public strictfp class SubGraph extends GraphNode {

    //================================ adjustable options ========================================================================//

   /** The maximum ascent and descent. We deliberately do NOT make this field "static" because only AWT thread can call Artist. */
   private final int ad = Artist.getMaxAscentAndDescent();
   
   /** This determines the minimum width of a dummy node. */
   private static final int dummyWidth = 30;

   /** This determines the minimum height of a dummy node. */
   private static final int dummyHeight = 10;

   /** This determines the minimum amount of padding added above, left, right, and below the text label. */
   private static final int labelPadding = 5;

   /** Color to use to show a highlighted node. */
   private static final Color COLOR_CHOSENNODE = Color.LIGHT_GRAY;

   //=============================== fields ======================================================================================//

   /** The node body */
   final Graph body;

   // =============================== these fields do not affect the computed bounds ===============================================

   /** The X coordinate of the center of the node;  modified by tweak(), layout_computeX(), layout(), and relayout_edges() */
   private int centerX = 0;

   /** The Y coordinate of the center of the node;  modified by tweak(), layout_computeX(), layout(), and relayout_edges() */
   private int centerY = 0;

   /** The layer that this node is in;  must stay in sync with Graph.layerlist */
   private int layer = 0;

   // =============================== these fields affect the computed bounds ======================================================
   
      /** The font boldness.
    * <p> When this value changes, we should invalidate the previously computed bounds information.
    */
   private boolean fontBold = false;

   /** The node labels; if null or empty, then the node has no labels.
    * <p> When this value changes, we should invalidate the previously computed bounds information.
    */
   private List<String> labels = null;

   /** The node color; never null.
    * <p> When this value changes, we should invalidate the previously computed bounds information.
    */
   private Color color = Color.WHITE;

   /** The line style; never null.
    * <p> When this value changes, we should invalidate the previously computed bounds information.
    */
   private DotStyle style = DotStyle.SOLID;

   /** The node shape; if null, then the node is a dummy node.
    * <p> When this value changes, we should invalidate the previously computed bounds information.
    */
   private DotShape shape = DotShape.BOX;
   
    // ============================ these fields are computed by calcBounds() =========================================

   /** If (updown>=0), this is the distance from the center to the top edge. */
   private int updown = (-1);

   /** If (updown>=0), this is the distance from the center to the left edge. */
   private int side = 0;

   /** If (updown>=0), this is the vertical distance between the center of the text label and the center of the node. */
   private int yShift = 0;

   /** If (updown>=0), this is the width of the text label. */
   private int width = 0;

   /** If (updown>=0), this is the height of the text label. */
   private int height = 0;

   /** If (updown>=0), this is the amount of space on the right set-aside for self-loops (which is 0 if node has no self loops) */
   private int reserved = 0;

   /** If (updown>=0 and shape!=null), this is the bounding polygon.
    * Note: if not null, it must be either a GeneralPath or a Polygon.
    */
   private Shape poly = null;

   /** If (updown>=0 and shape!=null and poly2!=null), then poly2 will also be drawn during the draw() method.
    * Note: if not null, it must be either a GeneralPath or a Polygon.
    */
   private Shape poly2 = null;

   /** If (updown>=0 and shape!=null and poly3!=null), then poly3 will also be drawn during the draw() method.
    * Note: if not null, it must be either a GeneralPath or a Polygon.
    */
   private Shape poly3 = null;
   
   //============================================================================================================================//
   
    public SubGraph(Graph graph, Object uuid, double defaultScale, String... labels) {
        super(graph, uuid, labels);
        this.body = new Graph(defaultScale);   
    }
    
    /** Changes the layer that this node is in; the new layer must be 0 or greater.
    * <p> If a node is removed from a layer, the order of the other nodes in that layer remain unchanged.
    * <p> If a node is added to a new layer, then it is added to the right of the original rightmost node in that layer.
    */
   void setLayer(int newLayer) {
      if (newLayer < 0) throw new IllegalArgumentException("The layer cannot be negative!");
      if (layer == newLayer) return;
      graph.layerlist.get(layer).remove(this);
      layer = newLayer;
      while(layer >= graph.layerlist.size()) graph.layerlist.add(new ArrayList<GraphNode>());
      graph.layerlist.get(layer).add(this);
   }

   /** Returns an unmodifiable view of the list of "in" edges. */
   public List<GraphEdge> inEdges() { return Collections.unmodifiableList(ins); }

   /** Returns an unmodifiable view of the list of "out" edges. */
   public List<GraphEdge> outEdges() { return Collections.unmodifiableList(outs); }

   /** Returns an unmodifiable view of the list of "self" edges. */
   public List<GraphEdge> selfEdges() { return Collections.unmodifiableList(selfs); }

   /** Returns the node's current position in the node list, which is always between 0 and node.size()-1 */
   int pos() { return pos; }

   /** Returns the layer that this node is in. */
   int layer() { return layer; }

   /** Returns the X coordinate of the center of the node. */
   public int x() { return centerX; }

   /** Returns the Y coordinate of the center of the node. */
   public int y() { return centerY; }

   /** Changes the X coordinate of the center of the node, without invalidating the computed bounds. */
   void setX(int x) { centerX = x;}

   /** Changes the Y coordinate of the center of the node, without invalidating the computed bounds. */
   void setY(int y) { centerY = y; }

   /** Returns the node shape (or null if the node is a dummy node). */
   DotShape shape() { return shape; }

   /** Changes the node shape (where null means change the node into a dummy node), then invalidate the computed bounds. */
   public GraphNode set(DotShape shape) {
      if (this.shape!=shape) { this.shape = shape; updown = (-1); }
      return this;
   }

   /** Changes the node color, then invalidate the computed bounds. */
   public GraphNode set(Color color) {
      if (this.color!=color && color!=null) { this.color = color; updown = (-1); }
      return this;
   }

   /** Changes the line style, then invalidate the computed bounds. */
   public GraphNode set(DotStyle style) {
      if (this.style!=style && style!=null) { this.style = style; updown = (-1); }
      return this;
   }

   /** Changes the font boldness, then invalidate the computed bounds. */
   public GraphNode setFontBoldness(boolean bold) {
      if (this.fontBold != bold) { this.fontBold = bold; updown = (-1); }
      return this;
   }

   /** Add the given label after the existing labels, then invalidate the computed bounds. */
   public GraphNode addLabel(String label) {
      if (label==null || label.length()==0) return this;
      if (labels==null) labels=new ArrayList<String>();
      labels.add(label);
      updown = (-1);
      return this;
   }

   /** Returns the node height. */
   int getHeight()  { if (updown<0) calcBounds(); return updown+updown; }

   /** Returns the node width. */
   int getWidth()  { if (updown<0) calcBounds(); return side+side; }

   /** Returns the bounding rectangle (with 2*xfluff added to the width, and 2*yfluff added to the height) */
   Rectangle2D getBoundingBox(int xfluff, int yfluff) {
      if (updown<0) calcBounds();
      return new Rectangle2D.Double(x()-side-xfluff, y()-updown-yfluff, side+side+xfluff+xfluff, updown+updown+yfluff+yfluff);
   }

   /** Returns the amount of space we need to reserve on the right hand side for the self edges (0 if this has no self edges now) */
   int getReserved() {
      if (selfs.isEmpty()) return 0; else if (updown<0) calcBounds();
      return reserved;
   }

   /** Returns true if the node contains the given point or not. */
   boolean contains(double x, double y) {
      if (shape==null) return false; else if (updown<0) calcBounds();
      return poly.contains(x-centerX, y-centerY);
   }

   /** Draws this node at its current (x, y) location; this method will call calcBounds() if necessary. */
   void draw(Artist gr, double scale, boolean highlight) {
      if (shape==null) return; else if (updown<0) calcBounds();
      final int top = graph.getTop(), left = graph.getLeft();
      gr.set(style, scale);
      gr.translate(centerX-left, centerY-top);
      gr.setFont(fontBold);
      if (highlight) gr.setColor(COLOR_CHOSENNODE); else gr.setColor(color);
      
      /*
      // DotShape
      if (shape==DotShape.CIRCLE || shape==DotShape.M_CIRCLE || shape==DotShape.DOUBLE_CIRCLE) {
         int hw=width/2, hh=height/2;
         int radius = ((int) (sqrt( hw*((double)hw) + ((double)hh)*hh ))) + 2;
         if (shape==DotShape.DOUBLE_CIRCLE) radius=radius+5;
         gr.fillCircle(radius);
         gr.setColor(Color.BLACK);
         gr.drawCircle(radius);
         if (style==DotStyle.DOTTED || style==DotStyle.DASHED) gr.set(DotStyle.SOLID, scale);
         if (shape==DotShape.M_CIRCLE && 10*radius>=25 && radius>5) {
            int d = (int) sqrt(10*radius - 25.0D);
            if (d>0) { gr.drawLine(-d,-radius+5,d,-radius+5); gr.drawLine(-d,radius-5,d,radius-5); }
         }
         if (shape==DotShape.DOUBLE_CIRCLE) gr.drawCircle(radius-5);
      } else {
         gr.draw(poly,true);
         gr.setColor(Color.BLACK);
         gr.draw(poly,false);
         if (poly2!=null) gr.draw(poly2,false);
         if (poly3!=null) gr.draw(poly3,false);
         if (style==DotStyle.DOTTED || style==DotStyle.DASHED) gr.set(DotStyle.SOLID, scale);
         if (shape==DotShape.M_DIAMOND) {
            gr.drawLine(-side+8, -8, -side+8, 8); gr.drawLine(-8, -side+8, 8, -side+8);
            gr.drawLine(side-8, -8, side-8, 8); gr.drawLine(-8, side-8, 8, side-8);
         }
         if (shape==DotShape.M_SQUARE) {
            gr.drawLine(-side, -side+8, -side+8, -side); gr.drawLine(side, -side+8, side-8, -side);
            gr.drawLine(-side, side-8, -side+8, side); gr.drawLine(side, side-8, side-8, side);
         }
      }
      
      // DotStyle
      gr.set(DotStyle.SOLID, scale);
      int clr = color.getRGB() & 0xFFFFFF;
      gr.setColor((clr==0x000000 || clr==0xff0000 || clr==0x0000ff) ? Color.WHITE : Color.BLACK);
      if (labels!=null && labels.size()>0) {
         int x=(-width/2), y=yShift+(-labels.size()*ad/2);
         for(int i=0; i<labels.size(); i++) {
            String t = labels.get(i);
            int w = ((int) (getBounds(fontBold, t).getWidth())) + 1; // Round it up
            if (width>w) w=(width-w)/2; else w=0;
            gr.drawString(t, x+w, y+Artist.getMaxAscent());
            y=y+ad;
         }
      }
      gr.translate(left-centerX, top-centerY);
      */
      
      // draw method of Graph
      body.draw(gr, scale, this, false);
      
   }

   /** Helper method that sets the Y coordinate of every node in a given layer. */
   private void setY(int layer, int y) {
      for(GraphNode n: graph.layer(layer)) 
          if (n instanceof SubGraph) {
              ((SubGraph) n).centerY = y;
          }
   }

   /** Helper method that shifts a node up. */
   private void shiftUp(int y) {
      final int[] ph = graph.layerPH;
      final int yJump = Graph.yJump/6;
      int i=layer();
      setY(i,y);
      y=y-ph[i]/2; // y is now the top-most edge of this layer
      for(i++; i<graph.layers(); i++) {
         List<GraphNode> list=graph.layer(i);
         GraphNode first=list.get(0);
         if (first instanceof SubGraph) {
            if (((SubGraph) first).centerY+ph[i]/2+yJump > y) setY(i, y-ph[i]/2-yJump);
            y=((SubGraph) first).centerY-ph[i]/2;
         }
      }
      graph.relayout_edges(false);
   }

   /** Helper method that shifts a node down. */
   private void shiftDown(int y) {
      final int[] ph = graph.layerPH;
      final int yJump = Graph.yJump/6;
      int i=layer();
      setY(i,y);
      y=y+ph[i]/2; // y is now the bottom-most edge of this layer
      for(i--; i>=0; i--) {
         List<GraphNode> list=graph.layer(i);
         GraphNode first=list.get(0);
         if (first instanceof SubGraph) {
            if (((SubGraph) first).centerY-ph[i]/2-yJump < y) setY(i, y+ph[i]/2+yJump);
            y=((SubGraph) first).centerY+ph[i]/2;
         }
      }
      graph.relayout_edges(false);
   }

   /** Helper method that shifts a node left. */
   private void shiftLeft(List<GraphNode> peers, int i, int x) {
      final int xJump = Graph.xJump/3;
      centerX = x;
      x=x-(shape==null?0:side); // x is now the left-most edge of this node
      for(i--;i>=0;i--) {
         GraphNode node=peers.get(i);
         if (node instanceof SubGraph) {
            SubGraph nodeGraph = (SubGraph) node;
            int side=(nodeGraph.shape==null?0:nodeGraph.side);
            if (nodeGraph.centerX+side+node.getReserved()+xJump>x) nodeGraph.centerX=x-side-node.getReserved()-xJump;
            x=nodeGraph.centerX-side;
         }
      }
   }

   /** Helper method that shifts a node right. */
   private void shiftRight(List<GraphNode> peers, int i, int x) {
      final int xJump = Graph.xJump/3;
      centerX = x;
      x=x+(shape==null?0:side)+getReserved(); // x is now the right most edge of this node
      for(i++;i<peers.size();i++) {
         GraphNode node=peers.get(i);
         if (node instanceof SubGraph) {
            SubGraph nodeGraph = (SubGraph) node; 
            int side=(nodeGraph.shape==null?0:nodeGraph.side);
            if (nodeGraph.centerX-side-xJump<x) nodeGraph.centerX=x+side+xJump;
            x=nodeGraph.centerX+side+node.getReserved();
         }
      }
   }

   /** Helper method that swaps a node towards the left. */
   private void swapLeft(List<GraphNode> peers, int i, int x) {
      int side=(shape==null ? 2 : this.side);
      int left=x-side;
      while(true) {
         if (i==0) { centerX=x; return; } // no clash possible
         GraphNode other=peers.get(i-1);
         if (other instanceof SubGraph) {
            SubGraph otherGraph = (SubGraph) other;
            int otherSide=(otherGraph.shape==null ? 0 : otherGraph.side);
            int otherRight=otherGraph.centerX+otherSide+other.getReserved();
            if (otherRight<left) { centerX=x; return; } // no clash
            graph.swapNodes(layer(), i, i-1);
            i--;
            if (otherGraph.shape!=null) otherGraph.shiftRight(peers, i+1, x + side + getReserved() + otherSide); 
         }
      }
   }

   /** Helper method that swaps a node towards the right. */
   private void swapRight(List<GraphNode> peers, int i, int x) {
      int side = (shape==null ? 2 : this.side);
      int right=x+side+getReserved();
      while(true) {
         if (i==peers.size()-1) { centerX=x; return; } // no clash possible
         GraphNode other=peers.get(i+1);
         if (other instanceof SubGraph) {
            SubGraph otherGraph = (SubGraph) other;
            int otherSide=(otherGraph.shape==null ? 0 : otherGraph.side);
            int otherLeft=otherGraph.centerX-otherSide;
            if (otherLeft>right) { centerX=x; return; } // no clash
            graph.swapNodes(layer(), i, i+1);
            i++;
            if (otherGraph.shape!=null) otherGraph.shiftLeft(peers, i-1, x - side - otherGraph.getReserved() - otherSide);
         }
      }
   }

   /** Assuming the graph is already laid out, this shifts this node (and re-layouts nearby nodes/edges as necessary) */
   void tweak(int x, int y) {
      if (centerX==x && centerY==y) return; // If no change, then return right away
      List<GraphNode> layer = graph.layer(layer());
      final int n = layer.size();
      int i;
      for(i=0; i<n; i++) if (layer.get(i)==this) break; // Figure out this node's position in its layer
      if (centerX>x) swapLeft(layer,i,x); else if (centerX<x) swapRight(layer,i,x);
      if (centerY>y) shiftUp(y); else if (centerY<y) shiftDown(y); else graph.relayout_edges(layer());
      graph.recalcBound(false);
   }

   //===================================================================================================

   /** (Re-)calculate this node's bounds. */
   void calcBounds() {
      reserved=(yShift=0);
      width=2*labelPadding; if (width<dummyWidth) side=dummyWidth/2;
      height=width;         if (height<dummyHeight) updown=dummyHeight/2;
      poly=(poly2=(poly3=null));
      if (shape==null) return;
      Polygon poly=new Polygon();
      if (labels!=null) for(int i=0; i<labels.size(); i++) {
         String t = labels.get(i);
         Rectangle2D rect = getBounds(fontBold, t);
         int ww = ((int)(rect.getWidth())) + 1; // Round it up
         if (width<ww) width=ww;
         height=height+ad;
      }
      int hw=((width+1)/2)+labelPadding;  if (hw<ad/2) hw=ad/2; width=hw*2; side=hw;
      int hh=((height+1)/2)+labelPadding; if (hh<ad/2) hh=ad/2; height=hh*2; updown=hh;
      switch(shape) {
         case HOUSE: {
            yShift = ad/2;
            updown = updown + yShift;
            poly.addPoint(-hw,yShift-hh); poly.addPoint(0,-updown); poly.addPoint(hw,yShift-hh);
            poly.addPoint(hw,yShift+hh); poly.addPoint(-hw,yShift+hh);
            break;
         }
         case INV_HOUSE: {
            yShift = -ad/2;
            updown = updown - yShift;
            poly.addPoint(-hw,yShift-hh); poly.addPoint(hw,yShift-hh); poly.addPoint(hw,yShift+hh);
            poly.addPoint(0,updown); poly.addPoint(-hw,yShift+hh);
            break;
         }
         case TRIANGLE: case INV_TRIANGLE: {
            int dx = (int) (height/sqrt3); dx=dx+1; if (dx<6) dx=6;
            int dy = (int) (hw*sqrt3);     dy=dy+1; if (dy<6) dy=6; dy=(dy/2)*2;
            side += dx; updown += dy/2;
            if (shape==DotShape.TRIANGLE) {
               yShift = dy/2;
               poly.addPoint(0, -updown); poly.addPoint(hw+dx, updown); poly.addPoint(-hw-dx, updown);
            } else {
               yShift = -dy/2;
               poly.addPoint(0, updown); poly.addPoint(hw+dx, -updown); poly.addPoint(-hw-dx, -updown);
            }
            break;
         }
         case HEXAGON: {
            side += ad;
            poly.addPoint(-hw-ad, 0); poly.addPoint(-hw, -hh); poly.addPoint(hw, -hh);
            poly.addPoint(hw+ad, 0); poly.addPoint(hw, hh); poly.addPoint(-hw, hh);
            break;
         }
         case TRAPEZOID: {
            side += ad;
            poly.addPoint(-hw,-hh); poly.addPoint(hw,-hh); poly.addPoint(hw+ad,hh); poly.addPoint(-hw-ad,hh);
            break;
         }
         case INV_TRAPEZOID: {
            side += ad;
            poly.addPoint(-hw-ad, -hh); poly.addPoint(hw+ad, -hh); poly.addPoint(hw, hh); poly.addPoint(-hw, hh);
            break;
         }
         case PARALLELOGRAM: {
            side += ad;
            poly.addPoint(-hw, -hh); poly.addPoint(hw+ad, -hh); poly.addPoint(hw, hh); poly.addPoint(-hw-ad, hh);
            break;
         }
         case M_DIAMOND: case DIAMOND: {
            if (shape==DotShape.M_DIAMOND) {
               if (hw<10) { hw=10; side=10; width=20; }
               if (hh<10) { hh=10; updown=10; height=20; }
            }
            updown += hw; side += hh;
            poly.addPoint(-hw-hh, 0); poly.addPoint(0, -hh-hw); poly.addPoint(hw+hh, 0); poly.addPoint(0, hh+hw);
            break;
         }
         case M_SQUARE: {
            if (hh<hw) hh=hw; else hw=hh;
            if (hh<6) { hh=6; hw=6; }
            this.width=hw*2;  this.side=hw;
            this.height=hh*2; this.updown=hh;
            side += 4; updown +=4;
            poly.addPoint(-hw-4,-hh-4); poly.addPoint(hw+4,-hh-4); poly.addPoint(hw+4,hh+4); poly.addPoint(-hw-4,hh+4);
            break;
         }
         case OCTAGON: case DOUBLE_OCTAGON: case TRIPLE_OCTAGON: {
            int dx=(width)/3, dy=ad;
            updown += dy;
            poly.addPoint(-hw, -hh); poly.addPoint(-hw+dx, -hh-dy); poly.addPoint(hw-dx, -hh-dy); poly.addPoint(hw, -hh);
            poly.addPoint(hw, hh); poly.addPoint(hw-dx, hh+dy); poly.addPoint(-hw+dx, hh+dy); poly.addPoint(-hw, hh);
            if (shape==DotShape.OCTAGON) break;
            double c=sqrt(dx*dx+dy*dy), a=(dx*dy)/c, k=((a+5)*dy)/dx, r=sqrt((a+5)*(a+5)+k*k)-dy;
            double dx1=((r-5)*dx)/dy, dy1=-(((dx+5D)*dy)/dx-dy-r);
            int x1=(int)(round(dx1)), y1=(int)(round(dy1));
            updown+=5; side+=5;
            poly2=poly; poly=new Polygon();
            poly.addPoint(-hw-5, -hh-y1); poly.addPoint(-hw+dx-x1, -hh-dy-5); poly.addPoint(hw-dx+x1, -hh-dy-5);
            poly.addPoint(hw+5, -hh-y1); poly.addPoint(hw+5, hh+y1); poly.addPoint(hw-dx+x1, hh+dy+5);
            poly.addPoint(-hw+dx-x1, hh+dy+5); poly.addPoint(-hw-5, hh+y1);
            if (shape==DotShape.DOUBLE_OCTAGON) break;
            updown+=5; side+=5;
            poly3=poly; poly=new Polygon(); x1=(int)(round(dx1*2)); y1=(int)(round(dy1*2));
            poly.addPoint(-hw-10, -hh-y1); poly.addPoint(-hw+dx-x1, -hh-dy-10); poly.addPoint(hw-dx+x1, -hh-dy-10);
            poly.addPoint(hw+10, -hh-y1); poly.addPoint(hw+10, hh+y1); poly.addPoint(hw-dx+x1, hh+dy+10);
            poly.addPoint(-hw+dx-x1, hh+dy+10); poly.addPoint(-hw-10, hh+y1);
            break;
         }
         case M_CIRCLE: case CIRCLE: case DOUBLE_CIRCLE: {
            int radius = ((int) (sqrt( hw*((double)hw) + ((double)hh)*hh ))) + 2;
            if (shape==DotShape.DOUBLE_CIRCLE) radius=radius+5;
            int L = ((int) (radius / cos18))+2, a = (int) (L * sin36), b = (int) (L * cos36), c = (int) (radius * tan18);
            poly.addPoint(-L,0); poly.addPoint(-b,a); poly.addPoint(-c,L); poly.addPoint(c,L); poly.addPoint(b,a);
            poly.addPoint(L,0); poly.addPoint(b,-a); poly.addPoint(c,-L); poly.addPoint(-c,-L); poly.addPoint(-b,-a);
            updown=L; side=L;
            break;
         }
         case EGG: case ELLIPSE: {
            int pad = ad/2;
            side+=pad;
            updown+=pad;
            int d = (shape==DotShape.ELLIPSE) ? 0 : (ad/2);
            GeneralPath path=new GeneralPath();
            path.moveTo(-side,d);
            path.quadTo(-side,-updown,0,-updown); path.quadTo(side,-updown,side,d);
            path.quadTo(side,updown,0,updown); path.quadTo(-side,updown,-side,d);
            path.closePath();
            this.poly=path;
         }
         default: { // BOX
            if (shape!=DotShape.BOX) { int d=ad/2; hw=hw+d; side=hw; hh=hh+d; updown=hh; }
            poly.addPoint(-hw,-hh); poly.addPoint(hw,-hh); poly.addPoint(hw,hh); poly.addPoint(-hw,hh);
         }
      }
      if (shape!=DotShape.EGG && shape!=DotShape.ELLIPSE) this.poly = poly;
      for(int i=0; i<selfs.size(); i++) {
         if (i==0) { reserved=side+selfLoopA; continue; }
         String label = selfs.get(i-1).label();
         reserved=reserved+(int)(getBounds(false,label).getWidth())+selfLoopGL+selfLoopGR;
      }
      if (reserved>0) {
         String label = selfs.get(selfs.size()-1).label();
         reserved=reserved+(int)(getBounds(false,label).getWidth())+selfLoopGL+selfLoopGR;
      }
   }
   
   //===================================================================================================
   
   /** Returns a DOT representation of this SubGraph (or "" if this is a dummy node) */
   @Override public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("digraph \"graph\" {\n" + "graph [fontsize=12]\n" + "node [fontsize=12]\n" + "edge [fontsize=12]\n" + "rankdir=TB;\n");
      for (GraphEdge e: body.edges) sb.append(e);
      for (GraphNode n: body.nodes) sb.append(n);
      sb.append("}\n");
      return sb.toString();
   }
}

   
