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

import edu.mit.csail.sdg.alloy4graph.*;
import java.util.List;

/**
 * This class compares two graphs and sets the nodes which differ to be
 * highlighted. [N7] @Louis Fauvarque
 */
public class GraphComparer {

    private Graph graph1, graph2;
    private VizGraphPanel vgp1,vgp2;

    public GraphComparer(VizGraphPanel vgp1, VizGraphPanel vgp2) {
        vgp1 = this.vgp1;
        vgp2 = this.vgp2;
    }

    public void compare() {
        graph1 = vgp1.getGraph();
        graph2 = vgp2.getGraph();
        System.out.println("Chaussette");
        if (graph1 != null && graph2 != null) {
            for (GraphNode n : graph1.nodes) {
                n.setHighlight(false);
            }
            for (GraphNode n : graph2.nodes) {
                n.setHighlight(false);
            }
            for (GraphNode n1 : graph1.nodes) {
                AlloyAtom at1 = StaticGraphMaker.getAtomFromNode(n1);
                boolean found = false;
                for (GraphNode n2 : graph2.nodes) {
                    AlloyAtom at2 = StaticGraphMaker.getAtomFromNode(n2);
                    if (at1 != null && at2 != null && at1.equals(at2) && !setStringCompare(n1.getLabels(),n2.getLabels())) {
                        found = true;
                        n2.setHighlight(false);
                        break;
                    }
                }
                n1.setHighlight(!found);
            }
            /*for(GraphEdge e1 : graph1.edges){
                AlloyAtom at11 = StaticGraphMaker.getAtomFromNode(e1.a());
                AlloyAtom at12 = StaticGraphMaker.getAtomFromNode(e1.b());
                boolean found = false;
                for(GraphEdge e2 : graph2.edges){
                    AlloyAtom at21 = StaticGraphMaker.getAtomFromNode(e2.a());
                    AlloyAtom at22 = StaticGraphMaker.getAtomFromNode(e2.b());
                    if(!(at11.equals(at21)) || !(at12.equals(at22))){
                        found = true;
                        e2.a().setHighlight(false);
                        e2.b().setHighlight(false);
                        break;
                    }
                }
                e1.a().setHighlight(!found);
                e1.b().setHighlight(!found);
            }*/
        }
        vgp1.remakeAll();
        vgp2.remakeAll();
    }

    public void setGraphPanel1(VizGraphPanel vgp1) {
        this.vgp1 = vgp1;
    }

    public void setGraphPanel2(VizGraphPanel vgp2) {
        this.vgp2 = vgp2;
    }

    
    /**
     * 
     * @param labels1
     * @param labels2
     * @return true if the two list are composed of the same elements
     */
    private boolean setStringCompare(List<String> labels1, List<String> labels2) {
        for(String s1 : labels1){
            boolean found = false;
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
