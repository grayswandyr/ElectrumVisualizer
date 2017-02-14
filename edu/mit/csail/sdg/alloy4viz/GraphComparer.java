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


/**
 * This class compares two graphs and sets the nodes which differ to be 
 * highlighted.
 * [N7] @Louis Fauvarque
 */
public class GraphComparer {
    
    private Graph graph1, graph2;
    
    public GraphComparer(Graph graph1, Graph graph2){
        this.graph1 = graph1;
        this.graph2 = graph2;
    }
    
    public void compare(){
        for(GraphNode n : graph1.nodes){
            n.setHighlight(false);
        }
        for(GraphNode n : graph2.nodes){
            n.setHighlight(true);
        }
        for(GraphNode n1 : graph1.nodes){
            AlloyAtom at1 = StaticGraphMaker.getAtomFromNode(n1);
            boolean found = false;
            for(GraphNode n2 : graph2.nodes){
                AlloyAtom at2 = StaticGraphMaker.getAtomFromNode(n2);
                if(at1.compareTo(at2) == 0){
                    found = true;
                    n2.setHighlight(false);
                    break;
                }
            }
            n1.setHighlight(!found);
        }
    }
    
}
