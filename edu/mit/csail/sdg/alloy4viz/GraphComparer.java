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
package edu.mit.csail.sdg.alloy4viz;

import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4graph.*;
import edu.mit.csail.sdg.alloy4viz.VizGraphPanel.TypePanel;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class compares two graphs and sets the nodes which differ to be
 * highlighted. 
 * [N7] @Louis Fauvarque
 */
public class GraphComparer {
    /**
     * A comparer callback interface.
     * This interface is actually a AbstractGraphNode -> AbstractGraphNode -> boolean callback.
     * It shall return TRUE when the comparison criterion is FULLFILLED.
     * (e.g.: if the two nodes being compared are identical).
     * 
     * To get any comparison criterion done, you must implement this interface
     * (see "LabelComparer" for an example).
     */
    public interface Comparer {
        /**
         * Compare two elements and yield true where they are said to be equal
         * @param e1 first element
         * @param e2 second element
         * @return a boolean indicating that the elements are equal
         */
        boolean compare(AbstractGraphElement e1, AbstractGraphElement e2);
    }
    
    /**
     * A comparer callback implementation.
     * This implementation establishes equality via the comparison of the
     * labels of the compared nodes.
     */
    public static class LabelComparer implements Comparer {
        /**
         * Cstr. (empty)
         */
        public LabelComparer() {}
        
        /**
         * Compare two nodes, yielding true whenever their labels are identical.
         * @param n1 first node
         * @param n2 second node
         * @return a boolean indicating that the nodes are equal
         */
        @Override
        public boolean compare(AbstractGraphElement e1, AbstractGraphElement e2) {
            if (e1 instanceof GraphNode && e2 instanceof GraphNode) {
                return setStringCompare(((GraphNode)e1).getLabels(), ((GraphNode)e2).getLabels());
            } else if (e1 instanceof GraphEdge && e2 instanceof GraphEdge) {
                GraphEdge edge1 = (GraphEdge)e1;
                GraphEdge edge2 = (GraphEdge)e2;
                return ((GraphEdge)e1).label().equals(((GraphEdge)e2).label());
            } else { // "getLabels" does not have any sense regarding other elements than nodes
                return false;
            }
        }
        
        /**
        * Compares two lists of strings
        * @param labels1
        * @param labels2
        * @return true if the two list are composed of the same elements
        */
        private static boolean setStringCompare(List<String> labels1, List<String> labels2) {
            boolean found;
            if(labels1 == null || labels2 == null){
                return false;
            }
            if(labels1.size() != labels2.size()){
                return false;
            }
            for(String s1 : labels1){
                found = false;
                for(String s2 : labels2){
                    found = s1.equals(s2);
                    if(found){
                        break;
                    }
                }
                if(!found){
                    return false;
                }
            }
            return true;
        }
    }
    
    private Graph graph1, graph2;
    private VizGraphPanel vgp1,vgp2;
    private VizState vizState;
    int curIndex = 0;
    int maxIndex = 0;
    boolean timeLinked = false;
    Comparer comp;

    public GraphComparer(VizGraphPanel vgp1, VizGraphPanel vgp2, VizState vizState, Comparer comp) {
        this.vgp1 = vgp1;
        this.vgp2 = vgp2;
        this.vizState = vizState;
        this.comp = comp;
    }

    /**
     * Compares the two graphs and highlight the nodes that differ
     */
    public void compare() {
        graph1 = vgp1.getGraph();
        graph2 = vgp2.getGraph();
        if (graph1 != null && graph2 != null) {
            List<AbstractGraphElement> eltsGraph1 = new ArrayList<AbstractGraphElement>();
            List<AbstractGraphElement> eltsGraph2 = new ArrayList<AbstractGraphElement>();
            
            for (GraphEdge e : graph1.edges) {
                eltsGraph1.add(e);
            }
            for (GraphEdge e : graph1.portEdges) {
                eltsGraph1.add(e);
            }
            for (AbstractGraphNode n : graph1.nodes) {
                eltsGraph1.add(n);
            }
            
            for (GraphEdge e : graph2.edges) {
                eltsGraph2.add(e);
                e.setNeedHighlight(true);
            }
            for (GraphEdge e : graph2.portEdges) {
                eltsGraph2.add(e);
                e.setNeedHighlight(true);
            }
            for (AbstractGraphNode n : graph2.nodes) {
                eltsGraph2.add(n);
                n.setNeedHighlight(true);
            }
            
            for (AbstractGraphElement e1 : eltsGraph1) {
                boolean found = false;
                for (AbstractGraphElement e2 : eltsGraph2) {
                    if (comp.compare(e1, e2)) {
                        found = true;
                        e2.setNeedHighlight(false);
                    }
                }
                e1.setNeedHighlight(!found);
            }
        }
        harmonize();
        vgp1.remakeAll();
        vgp2.remakeAll();
    }
    
    public void resetHighlight(){
        for (GraphEdge e : graph1.edges) {
            e.setNeedHighlight(false);
        }
        for (GraphEdge e : graph1.portEdges) {
            e.setNeedHighlight(false);
        }
        for (AbstractGraphNode e : graph1.nodes) {
            e.setNeedHighlight(false);
        }
        for (GraphEdge e : graph2.edges) {
            e.setNeedHighlight(false);
        }
        for (GraphEdge e : graph2.portEdges) {
            e.setNeedHighlight(false);
        }
        for (AbstractGraphNode e : graph2.nodes) {
            e.setNeedHighlight(false);
        }
    }

    public void setGraphPanel1(VizGraphPanel vgp1) {
        this.vgp1 = vgp1;
    }

    public void setGraphPanel2(VizGraphPanel vgp2) {
        this.vgp2 = vgp2;
    }

    /**
     * This function matches the color of the edges in the second graph to the color
     * of the edges of corresponding group in the first graph
     */
    public void harmonize(){
        if(graph1 != null && graph2 != null){
            HashMap<Object,Color> groupMap = new HashMap<Object,Color>();
            HashMap<Object,Set<GraphEdge>> treatLater = new HashMap<Object,Set<GraphEdge>>();
            // We stock the color of each group
            for(GraphEdge e : graph1.edges){
                if(e.group != null){
                    groupMap.put(e.group, e.getColor());
                }
            }
            // Then we match the colors for each group matching in each graphs
            for(GraphEdge e : graph2.edges){
                if(e.group != null){
                    if(groupMap.containsKey(e.group)){
                        e.set(groupMap.get(e.group));
                    } else {
                        if(treatLater.containsKey(e.group)){
                            treatLater.get(e.group).add(e);
                        } else {
                            HashSet<GraphEdge> hs = new HashSet<GraphEdge>();
                            hs.add(e);
                            treatLater.put(e.group, hs);
                        }
                    }
                }
            }
            List<Color> colors = StaticGraphMaker.GetPalette(vizState.getEdgePalette());
            List<Color> availableColors = new ArrayList<Color>(colors);
            for(Color col : groupMap.values()){
                availableColors.remove(col);

            }
            if(availableColors.isEmpty()){
                availableColors = new ArrayList<Color>(colors);
            }
            // Then we put arbitrary colors to the remaining edges in the second graph
            for(Set<GraphEdge> set : treatLater.values()){
                Color c = availableColors.get(0);
                availableColors.remove(c);
                if(availableColors.isEmpty()){
                    availableColors = new ArrayList<Color>(colors);
                }
                for(GraphEdge e : set){
                    e.set(c);
                }
            }
        }
    }

    public void forwardTime() {
        if(timeLinked){
            TypePanel tp1 = vgp1.getType2Panel().get(AlloyType.TIME);
            TypePanel tp2 = vgp2.getType2Panel().get(AlloyType.TIME);
            if(tp2.getAtomCombo().getSelectedIndex() < tp2.getAtomCombo().getItemCount()-1){
                curIndex++;
                tp1.getAtomCombo().setSelectedIndex(curIndex);
                tp2.getAtomCombo().setSelectedIndex(curIndex+1);
            }
            vgp1.remakeAll();
            vgp2.remakeAll();
            tp1.getLeft().setEnabled(false);
            tp1.getRight().setEnabled(false);
            tp2.getLeft().setEnabled(false);
            tp2.getRight().setEnabled(false);
        }
    }

    public void backwardTime() {
        if(timeLinked){
            TypePanel tp1 = vgp1.getType2Panel().get(AlloyType.TIME);
            TypePanel tp2 = vgp2.getType2Panel().get(AlloyType.TIME);
            if(tp1.getAtomCombo().getSelectedIndex() > 0){
                curIndex--;
                tp1.getAtomCombo().setSelectedIndex(curIndex);
                tp2.getAtomCombo().setSelectedIndex(curIndex+1);
            }
            vgp1.remakeAll();
            vgp2.remakeAll();
            tp1.getLeft().setEnabled(false);
            tp1.getRight().setEnabled(false);
            tp2.getLeft().setEnabled(false);
            tp2.getRight().setEnabled(false);
        }
    }

    public void linkTime() {
        timeLinked = !timeLinked;
        TypePanel tp1 = vgp1.getType2Panel().get(AlloyType.TIME);
        TypePanel tp2 = vgp2.getType2Panel().get(AlloyType.TIME);
        if(timeLinked){
            curIndex = 0;
            maxIndex = tp2.getAtomCombo().getItemCount()-1;
            if(tp1.getAlloyAtoms().size() > 1){
                tp1.getAtomCombo().setSelectedIndex(curIndex);
                tp2.getAtomCombo().setSelectedIndex(curIndex+1);            
            }
        }
        tp1.getAtomCombo().setEnabled(!timeLinked);
        tp1.getLeft().setEnabled(!timeLinked && (curIndex > 0));
        tp1.getRight().setEnabled(!timeLinked && (curIndex < tp1.getAtomCombo().getItemCount()-1));
        tp2.getAtomCombo().setEnabled(!timeLinked);
        tp2.getLeft().setEnabled(!timeLinked && (curIndex > 0));
        tp2.getRight().setEnabled(!timeLinked && (curIndex < tp2.getAtomCombo().getItemCount()-1));
    }
    
    public void setComparer(Comparer c) {
        this.comp = c;
    }

}
