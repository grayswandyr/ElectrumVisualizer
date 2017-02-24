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
     * The list of colors, in order, to assign each legend.
     */
    private static final List<Color> colorsClassic = Util.asList(
            new Color(228, 26, 28), new Color(166, 86, 40), new Color(255, 127, 0), new Color(77, 175, 74), new Color(55, 126, 184), new Color(152, 78, 163)
    );

    /**
     * The list of colors, in order, to assign each legend.
     */
    private static final List<Color> colorsStandard = Util.asList(
            new Color(227, 26, 28), new Color(255, 127, 0), new Color(251 * 8 / 10, 154 * 8 / 10, 153 * 8 / 10), new Color(51, 160, 44), new Color(31, 120, 180)
    );

    /**
     * The list of colors, in order, to assign each legend.
     */
    private static final List<Color> colorsMartha = Util.asList(
            new Color(231, 138, 195), new Color(252, 141, 98), new Color(166, 216, 84), new Color(102, 194, 165), new Color(141, 160, 203)
    );

    /**
     * The list of colors, in order, to assign each legend.
     */
    private static final List<Color> colorsNeon = Util.asList(
            new Color(231, 41, 138), new Color(217, 95, 2), new Color(166, 118, 29), new Color(102, 166, 30), new Color(27, 158, 119), new Color(117, 112, 179)
    );
    
    private Graph graph1, graph2;
    private VizGraphPanel vgp1,vgp2;
    private VizState vizState;
    int curIndex = 0;
    boolean timeLinked = false;

    public GraphComparer(VizGraphPanel vgp1, VizGraphPanel vgp2, VizState vizState) {
        this.vgp1 = vgp1;
        this.vgp2 = vgp2;
        this.vizState = vizState;
    }

    /**
     * Compares the two graphs and highlight the nodes that differ
     */
    public void compare() {
        graph1 = vgp1.getGraph();
        graph2 = vgp2.getGraph();
        if (graph1 != null && graph2 != null) {
            for (GraphNode n : graph2.nodes) {
                n.setHighlight(true);
            }
            for (GraphNode n1 : graph1.nodes) {
                boolean found = false;
                for (GraphNode n2 : graph2.nodes) {
                    if (setStringCompare(n1.getLabels(),n2.getLabels())) {
                        found = true;
                        n2.setHighlight(false);
                        break;
                    }
                }
                n1.setHighlight(!found);
            }
        }
        harmonize();
        vgp1.remakeAll();
        vgp2.remakeAll();
    }
    
    public void resetHighlight(){
        for (GraphNode n : graph2.nodes) {
            n.setHighlight(false);
        }
        for (GraphNode n : graph1.nodes) {
            n.setHighlight(false);
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
        //graph1 = vgp1.getGraph();
        //graph2 = vgp2.getGraph();
        HashMap<Object,Color> groupMap = new HashMap<Object,Color>();
        HashMap<Object,Set<GraphEdge>> treatLater = new HashMap<Object,Set<GraphEdge>>();
        // We stock the color of each group
        for(GraphEdge e : graph1.edges){
            if(e.group != null){
                groupMap.put(e.group, e.color());
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
        List<Color> colors;
        if (vizState.getEdgePalette() == DotPalette.CLASSIC) {
            colors = colorsClassic;
        } else if (vizState.getEdgePalette() == DotPalette.STANDARD) {
            colors = colorsStandard;
        } else if (vizState.getEdgePalette() == DotPalette.MARTHA) {
            colors = colorsMartha;
        } else {
            colors = colorsNeon;
        }
        List<Color> availableColors = new ArrayList<Color>(colors);
        for(Color col : groupMap.values()){
            availableColors.remove(col);
            
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
    
    /**
     * Compares two lists of strings
     * @param labels1
     * @param labels2
     * @return true if the two list are composed of the same elements
     */
    private boolean setStringCompare(List<String> labels1, List<String> labels2) {
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

}
