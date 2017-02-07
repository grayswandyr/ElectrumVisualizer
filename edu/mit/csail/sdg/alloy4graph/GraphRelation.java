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

import edu.mit.csail.sdg.alloy4viz.AlloyAtom;
import edu.mit.csail.sdg.alloy4viz.AlloyRelation;

/**
 * [N7] This class represents the link between 2 atoms connected through a binary relation.
 * @author Julien Richer
 */
public class GraphRelation {
    private AlloyRelation rel;
    private AlloyAtom start;
    private AlloyAtom end;
    
    // Constructor
    public GraphRelation(AlloyRelation r, AlloyAtom s, AlloyAtom e) {
        this.rel = r;
        this.start = s;
        this.end = e;
    }
    
    
    // Accessors and Mutators
    public AlloyRelation getRelation() {
        return this.rel;
    }
    
    public void setRelation(AlloyRelation r) {
        this.rel = r;
    }
    
    public AlloyAtom getStart() {
        return this.start;
    }
    
    public void setStart(AlloyAtom s) {
        this.start = s;
    }
    
    public AlloyAtom getEnd() {
        return this.end;
    }
    
    public void setEnd(AlloyAtom e) {
        this.end = e;
    }
}
