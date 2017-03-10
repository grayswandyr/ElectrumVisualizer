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

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JPanel;

import edu.mit.csail.sdg.alloy4.ErrorFatal;
import edu.mit.csail.sdg.alloy4.Util;
import edu.mit.csail.sdg.alloy4graph.AbstractGraphNode;
import edu.mit.csail.sdg.alloy4graph.DotColor;
import edu.mit.csail.sdg.alloy4graph.DotDirection;
import edu.mit.csail.sdg.alloy4graph.DotPalette;
import edu.mit.csail.sdg.alloy4graph.DotShape;
import edu.mit.csail.sdg.alloy4graph.DotStyle;
import edu.mit.csail.sdg.alloy4graph.Graph;
import edu.mit.csail.sdg.alloy4graph.GraphEdge;
import edu.mit.csail.sdg.alloy4graph.GraphNode;
import edu.mit.csail.sdg.alloy4graph.GraphPort;
import edu.mit.csail.sdg.alloy4graph.GraphPort.Orientation;
import edu.mit.csail.sdg.alloy4graph.GraphRelation;
import edu.mit.csail.sdg.alloy4graph.GraphViewer;
import java.util.*;
import java.time.Clock;

/**
 * This utility class generates a graph for a particular index of the
 * projection.
 *
 * <p>
 * <b>Thread Safety:</b> Can be called only by the AWT event thread.
 */
public final class StaticGraphMaker {

    /**
     * The theme customization.
     */
    private final VizState view;

    /**
     * The projected instance for the graph currently being generated.
     */
    private final AlloyInstance instance;

    /**
     * The projected model for the graph currently being generated.
     */
    private final AlloyModel model;

    /**
     * The map that contains all edges and what the AlloyTuple that each edge
     * corresponds to.
     */
    private final Map<GraphEdge, AlloyTuple> edges = new LinkedHashMap<GraphEdge, AlloyTuple>();

    //Not used for the moment...
    /**
     * The map that contains all nodes and what the AlloyAtom that each node
     * corresponds to.
     */
    private final Map<GraphNode, AlloyAtom> nodes = new LinkedHashMap<GraphNode, AlloyAtom>();
    
    /**
     * [N7] @Julien Richer
     * The map that contains all nodes and what the AlloyAtom that each port
     * corresponds to.
     */
    private final Map<GraphPort, AlloyAtom> ports = new LinkedHashMap<GraphPort, AlloyAtom>();

    /**
     * This maps each atom to the node representing it; if an atom doesn't have
     * a node, it won't be in the map.
     */
    private final Map<AlloyAtom, List<AbstractGraphNode>> atom2node = new LinkedHashMap<AlloyAtom, List<AbstractGraphNode>>();

    /**
     * [N7] @Louis Fauvarque
     * This maps each port to the atom representing it
     */
    private final Map<AlloyAtom, List<AbstractGraphNode>> atom2port = new LinkedHashMap<AlloyAtom,List<AbstractGraphNode>>();
    
    /**
     * This stores a set of additional labels we want to add to an existing
     * node.
     */
    private final Map<AbstractGraphNode, Set<String>> attribs = new LinkedHashMap<AbstractGraphNode, Set<String>>();

    /**
     * The resulting graph.
     */
    private final Graph graph;

    //[N7-R.Bossut, M.Quentin]
    /**
     * The mapthat contains every container AlloyAtom, and the list of list
     * representing the rest of the tuple it contains.
     */
    private final Map<AlloyAtom, List<List<AlloyAtom>>> containmentTuples = new LinkedHashMap<AlloyAtom, List<List<AlloyAtom>>>();

    //[N7-R.Bossut, M.Quentin]
    /**
     * The map that contains every contained AlloyAtoms and the list of Alloy
     * Atoms it is contained in.
     */
    private final Map<AlloyAtom, Set<AlloyAtom>> containedInMap = new LinkedHashMap<AlloyAtom, Set<AlloyAtom>>();
    /**
     * Produces a single Graph from the given Instance and View and choice of
     * Projection
     */
    public static JPanel produceGraph(AlloyInstance instance, VizState view, AlloyProjection proj) throws ErrorFatal {
        view = new VizState(view);
        if (proj == null) {
            proj = new AlloyProjection();
        }
        Graph graph = new Graph(view.getFontSize() / 12.0D);
        StaticGraphMaker sgm = new StaticGraphMaker(graph, instance, view, proj);
        if (graph.nodes.size() == 0) {
            new GraphNode(graph, "", "Due to your theme settings, every atom is hidden.", "Please click Theme and adjust your settings.");
        }
        return new GraphViewer(graph,instance,view);
    }

    /**
     * The list of colors, in order, to assign each legend.
     */
    static final List<Color> colorsClassic = Util.asList(
            new Color(228, 26, 28), new Color(166, 86, 40), new Color(255, 127, 0), new Color(77, 175, 74), new Color(55, 126, 184), new Color(152, 78, 163)
    );

    /**
     * The list of colors, in order, to assign each legend.
     */
    static final List<Color> colorsStandard = Util.asList(
            new Color(227, 26, 28), new Color(255, 127, 0), new Color(251 * 8 / 10, 154 * 8 / 10, 153 * 8 / 10), new Color(51, 160, 44), new Color(31, 120, 180)
    );

    /**
     * The list of colors, in order, to assign each legend.
     */
    static final List<Color> colorsMartha = Util.asList(
            new Color(231, 138, 195), new Color(252, 141, 98), new Color(166, 216, 84), new Color(102, 194, 165), new Color(141, 160, 203)
    );

    /**
     * The list of colors, in order, to assign each legend.
     */
    static final List<Color> colorsNeon = Util.asList(
            new Color(231, 41, 138), new Color(217, 95, 2), new Color(166, 118, 29), new Color(102, 166, 30), new Color(27, 158, 119), new Color(117, 112, 179)
    );
    
    /**
     * Get the actual color palette as a list of colors from a palette name.
     */
    static List<Color> GetPalette(DotPalette p) {
        List<Color> res = null;
        switch (p) {
            case CLASSIC:
                res = colorsClassic;
                break;
            case STANDARD:
                res = colorsStandard;
                break;
            case MARTHA:
                res = colorsMartha;
                break;
            default:
                res = colorsNeon;
        }
        return res;
    }

    /**
     * The constructor takes an Instance and a View, then insert the generate
     * graph(s) into a blank cartoon.
     */
    private StaticGraphMaker(Graph graph, AlloyInstance originalInstance, VizState view, AlloyProjection proj) throws ErrorFatal {
        final boolean hidePrivate = view.hidePrivate();
        final boolean hideMeta = view.hideMeta();
        final Map<AlloyRelation, Color> magicColor = new TreeMap<AlloyRelation, Color>();
        final Map<AlloyRelation, Color> magicPortColor = new TreeMap<AlloyRelation, Color>(); // [N7] @Julien Richer Ports magic colors
        final Map<AlloyRelation, Integer> rels = new TreeMap<AlloyRelation, Integer>();
        this.graph = graph;
        this.view = view;
        instance = StaticProjector.project(originalInstance, proj);
        model = instance.model;
        
        for (AlloyRelation rel : model.getRelations()) {
            rels.put(rel, null);
        }

        this.graph.sgm = this;
        this.graph.instance = originalInstance;
        
        /**
         * [N7] Modified by @Louis Fauvarque @Julien Richer
         * Make the ports not visible
         * Create blank arrows to link the nodes that are connected through ports
         */
        
        ArrayList<AlloyRelation> portRelations = view.isPort.getKeysFromValue(true);
        ArrayList<AlloyAtom> portList = getPorts(portRelations, instance);
        
        for (AlloyAtom port : portList) {
            view.nodeVisible.put(port.getType(), Boolean.FALSE);
        }
        
        // Node magic colors
        List<Color> colors = GetPalette(view.getEdgePalette());
        
        // Ports magic colors
        List<Color> portColors = GetPalette(view.getPortPalette());
        
        
         //[N7-R.Bossut, M.Quentin]
        //Creation of a Map to store atoms that are instances of a containment relation.
        // The key of the Map is an AlloyAtom which is the container of the containmentTuple.
        // The value is a List of List of AlloyAtoms; each List represents the rest of a containmentTuple (contained in key).
        for (AlloyRelation rel : model.getRelations()) {
            if (view.containmentRel.resolve(rel)) {
                //The relation is a containment one.
                for (AlloyTuple tuple : instance.relation2tuples(rel)) {
                    ArrayList<AlloyAtom> atoms = new ArrayList<AlloyAtom>(tuple.getAtoms());
                    AlloyAtom a = atoms.get(0);
                    atoms.remove(0);

                    //containmentTuples
                    List<List<AlloyAtom>> otherTuples = containmentTuples.get(a);
                    if (otherTuples == null) {
                        otherTuples = new ArrayList<List<AlloyAtom>>();
                    }

                    otherTuples.add(atoms);
                    containmentTuples.put(a, otherTuples);

                    //containedInMap
                    for (AlloyAtom atom : atoms) {
                        Set<AlloyAtom> containedIn = containedInMap.get(atom);
                        if (containedIn == null) {
                            containedIn = new TreeSet<AlloyAtom>();
                        }
                        containedIn.add(a);
                        containedInMap.put(atom, containedIn);
                    }
                    //We also add the container in the map, but seeing this relation, it is not contained in anything.
                    Set<AlloyAtom> containedIn = containedInMap.get(a);
                    if (containedIn == null) {
                        containedIn = new TreeSet<AlloyAtom>();
                    }
                    containedInMap.put(a, containedIn);
                }
            }
        }

        //Verify there is no cycle in containment relations.
        for (AlloyAtom atom : containedInMap.keySet()) {
            if (!isNotCycle(atom)) {
                new GraphNode(graph, "", "The containment relations you have precised are creating a cycle.", "Please click Theme and adjust your settings.");
                return;
            }
        }
        //Call createContainingNode for every atom that is in a containmentTuple but is not contained anywhere.
        for (AlloyAtom atom : containedInMap.keySet()) {
            if (containedInMap.get(atom).isEmpty()) //The atom is not contained in any other atom.
            {
                createContainingNode(hidePrivate, hideMeta, atom, containmentTuples.get(atom), null, view.getDepthMax());
            }
        }
        
        
        
        int cj = 0;
        for (AlloyRelation rel : portRelations) {
            DotColor c = view.portColor.resolve(rel);
            Color cc = (c == DotColor.MAGIC) ? portColors.get(cj) : c.getColor(view.getPortPalette());
            magicPortColor.put(rel, cc);
            cj = (cj + 1) % (portColors.size());
        }
        
        /**
         * For each portRelation :
         * Add the non port atom to the box list
         * And keep to which port they are connected (Hashmap)
         * 
         * WARNING : ArrayList becomes a reserved group type for Edges linked with ports
         */
        
        // List of GraphRelations that link a node, one of its ports and the relation between them
        // (relation,port,node)
        List<GraphRelation> relList = new ArrayList<GraphRelation>();
        
        Set<AlloyRelation> relations = view.getCurrentModel().getRelations();
        Set<AlloyTuple> tupleSet = null;
        
        for(AlloyRelation rel : portRelations){
            Color magicol = magicPortColor.get(rel);
            tupleSet = instance.relation2tuples(rel);
            for(AlloyTuple tuple : tupleSet){
                AlloyAtom ts = tuple.getStart();
                AlloyAtom te = tuple.getEnd();
                
                // Create a new GraphRelation and stock it in the list
                relList.add(new GraphRelation(rel,te,ts));
                
                // Create a new port if necessary
                // Output port
                if(isPort(portRelations,ts)) {
                    if (atom2node.get(te) == null || atom2node.get(te).isEmpty()) {
                        createNode(view.hidePrivate(), view.hideMeta(), te);
                    }
                    for (AbstractGraphNode n : atom2node.get(te)){
                        if (n != null && n instanceof GraphNode) {
                            Orientation defaultOri = GraphPort.AvailableOrientations.get(n.shape())[0];
                            GraphPort port = createPort(ts, (GraphNode)n, rel, ts.toString(), defaultOri);
                            setPortColor(port,rel,magicol);
                        }
                    }
                }
                // Input port
                if(isPort(portRelations,te)) {
                    if (atom2node.get(ts) == null || atom2node.get(ts).isEmpty()) {
                        createNode(view.hidePrivate(), view.hideMeta(), ts);                        
                    }
                    for (AbstractGraphNode n : atom2node.get(ts)){
                        if (n != null && n instanceof GraphNode) {
                            Orientation defaultOri = GraphPort.AvailableOrientations.get(n.shape())[0];
                            GraphPort port = createPort(te, (GraphNode)n, rel, te.toString(), defaultOri);
                            setPortColor(port,rel,magicol);
                        }
                    }
                }
            }
        }
        
        // All relations
        for(AlloyRelation rel : relations){
            Color magicol = magicPortColor.get(rel);
            // Non port relations
            if(!isIn(portRelations,rel)){
                tupleSet = instance.relation2tuples(rel);
                // Tuples of the relation
                for(AlloyTuple tuple : tupleSet){
                    // Check that each side of the tuple is a port
                    if(isPort(portRelations,tuple.getStart()) && isPort(portRelations,tuple.getEnd())){
                        AlloyAtom atomStart = null;
                        AlloyRelation relStart = null;
                        AlloyAtom atomEnd = null;
                        AlloyRelation relEnd = null;
                        
                        // Get the 2 relations and their extremities
                        // NB : grel.getStart() is a port
                        for(GraphRelation grel : relList) {
                            if(grel.getStart()==tuple.getStart()) {
                                atomStart = grel.getEnd();
                                relStart = grel.getRelation();
                            }
                            if(grel.getStart()==tuple.getEnd()) {
                                atomEnd = grel.getEnd();
                                relEnd = grel.getRelation();
                            }
                        }
                        
                        // Create the 2 nodes and the 2 ports
                        if(atomStart!=null && atomEnd!=null && relStart!=null && relEnd!=null) {
                            if (atom2node.get(atomStart) == null || atom2node.get(atomStart).isEmpty()) {
                                createNode(view.hidePrivate(), view.hideMeta(), atomStart);
                            }
                            List<AbstractGraphNode> startNodes = atom2node.get(atomStart);
                            if (atom2node.get(atomEnd) == null || atom2node.get(atomEnd).isEmpty()) {
                                createNode(view.hidePrivate(), view.hideMeta(), atomEnd);
                            }
                            List<AbstractGraphNode> endNodes = atom2node.get(atomEnd);
                            GraphPort startPort = null;
                            GraphPort endPort = null;

                            // Output port
                            for (AbstractGraphNode n : startNodes){
                                if (n != null && n instanceof GraphNode) {
                                    Orientation defaultStartOri = GraphPort.AvailableOrientations.get(n.shape())[0];
                                    startPort = createPort(tuple.getStart(), (GraphNode)n, relStart, tuple.getStart().toString(), defaultStartOri);
                                }
                            }
                            
                            // Input port
                            for (AbstractGraphNode n : endNodes){
                                if (n != null && n instanceof GraphNode) {
                                    Orientation defaultEndOri = GraphPort.AvailableOrientations.get(n.shape())[0];
                                    endPort = createPort(tuple.getEnd(), (GraphNode)n, relEnd, tuple.getEnd().toString(), defaultEndOri);
                                }
                            }

                            // Create the blank edge between the 2 nodes connected through the 2 ports
                            boolean sameGraph = false;
                            //If there is one start and one end in a same graph, we shall not draw edges between different graphs.
                            for (AbstractGraphNode start : startNodes) {
                                for (AbstractGraphNode end : endNodes) {
                                    if (end.graph == start.graph){
                                        sameGraph = true;
                                        break;
                                    }
                                }
                            }
                            for (AbstractGraphNode start : startNodes){
                                for (AbstractGraphNode end : endNodes){
                                    if (start != null && end != null && (!sameGraph || start.graph == end.graph)){
                                        ArrayList<AbstractGraphNode> couple = new ArrayList<AbstractGraphNode>();
                                        couple.add(startPort);
                                        couple.add(endPort);
                                        new GraphEdge(start, end, graph, null, "Blank" + atomStart.toString() + atomEnd.toString(), couple).setStyle(DotStyle.BLANK);
                                    }
                                }
                            }
                        }
                    }
                    // Check that start is a node and end is a port
                    else if(!isPort(portRelations,tuple.getStart()) && isPort(portRelations,tuple.getEnd())){
                        AlloyAtom atomStart = tuple.getStart();
                        AlloyAtom atomEnd = null;
                        AlloyRelation relEnd = null;
                        
                        // Get the right relation and its extremity
                        // NB : grel.getStart() is a port
                        for(GraphRelation grel : relList) {
                            if(grel.getStart()==tuple.getEnd()) {
                                atomEnd = grel.getEnd();
                                relEnd = grel.getRelation();
                            }
                        }
                        
                        // Create the 2 nodes and the port
                        if(atomStart!=null && atomEnd!=null && relEnd!=null) {
                            if (atom2node.get(atomStart) == null || atom2node.get(atomStart).isEmpty()) {
                                createNode(view.hidePrivate(), view.hideMeta(), atomStart);
                            }   
                            List<AbstractGraphNode> startNodes = atom2node.get(atomStart);
                            if (atom2node.get(atomEnd) == null || atom2node.get(atomEnd).isEmpty()) {
                                createNode(view.hidePrivate(), view.hideMeta(), atomEnd);
                            }
                            List<AbstractGraphNode> endNodes = atom2node.get(atomEnd);
                            
                            GraphPort endPort = null;
     
                            // Input port
                            if (endNodes != null){
                                for (AbstractGraphNode n : endNodes){
                                    if (n != null && n instanceof GraphNode) {
                                        Orientation defaultEndOri = GraphPort.AvailableOrientations.get(n.shape())[0];
                                        endPort = createPort(tuple.getEnd(), (GraphNode)n, relEnd, tuple.getEnd().toString(), defaultEndOri);
                                        setPortColor(endPort,rel,magicol);
                                    }
                                }
                            }

                            // Create the blank edge between the 2 nodes connected through the port
                            boolean sameGraph = false;
                            //If there is one start and one end in a same graph, we shall not draw edges between different graphs.
                            if (startNodes != null && endNodes != null){
                                for (AbstractGraphNode start : startNodes) {
                                    for (AbstractGraphNode end : endNodes) {
                                        if (end.graph == start.graph){
                                            sameGraph = true;
                                            break;
                                        }
                                    }
                                }
                                for (AbstractGraphNode start : startNodes){
                                    for (AbstractGraphNode end : endNodes){
                                        if (start != null && end != null && (!sameGraph || start.graph == end.graph)){
                                            ArrayList<AbstractGraphNode> couple = new ArrayList<AbstractGraphNode>();
                                            couple.add(start);
                                            couple.add(endPort);
                                            new GraphEdge(start, end, graph, null, "Blank" + atomStart.toString() + atomEnd.toString(), couple).setStyle(DotStyle.BLANK);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Check that start is a port and end is a node
                    else if(isPort(portRelations,tuple.getStart()) && !isPort(portRelations,tuple.getEnd())){
                        AlloyAtom atomStart = null;
                        AlloyRelation relStart = null;
                        AlloyAtom atomEnd = tuple.getEnd();
                        
                        // Get the relation and its extremity
                        // NB : grel.getStart() is a port
                        for(GraphRelation grel : relList) {
                            if(grel.getStart()==tuple.getStart()) {
                                atomStart = grel.getEnd();
                                relStart = grel.getRelation();
                            }
                        }
                        
                        // Create the 2 nodes and the port
                        if(atomStart!=null && atomEnd!=null && relStart!=null) {
                            if (atom2node.get(atomStart) == null || atom2node.get(atomStart).isEmpty())
                                createNode(view.hidePrivate(), view.hideMeta(), atomStart);
                            List<AbstractGraphNode> startNodes = atom2node.get(atomStart);
                            if (atom2node.get(atomEnd) == null || atom2node.get(atomEnd).isEmpty())
                                createNode(view.hidePrivate(), view.hideMeta(), atomEnd);
                            List<AbstractGraphNode> endNodes = atom2node.get(atomEnd);
                            
                            GraphPort startPort = null;
                            
                            // Output port
                            for (AbstractGraphNode n : startNodes){
                                if (n != null && n instanceof GraphNode) {
                                    Orientation defaultStartOri = GraphPort.AvailableOrientations.get(n.shape())[0];
                                    startPort = createPort(tuple.getStart(), (GraphNode)n, relStart, tuple.getStart().toString(), defaultStartOri);
                                    setPortColor(startPort,rel,magicol);
                                }
                            }

                            // Create the blank edge between the 2 nodes connected through the port
                            boolean sameGraph = false;
                            //If there is one start and one end in a same graph, we shall not draw edges between different graphs.
                            for (AbstractGraphNode start : startNodes) {
                                for (AbstractGraphNode end : endNodes) {
                                    if (end.graph == start.graph){
                                        sameGraph = true;
                                        break;
                                    }
                                }
                            }
                            for (AbstractGraphNode start : startNodes){
                                for (AbstractGraphNode end : endNodes){
                                    if (start != null && end != null && (!sameGraph || start.graph == end.graph)){
                                        ArrayList<AbstractGraphNode> couple = new ArrayList<AbstractGraphNode>();
                                        couple.add(startPort);
                                        couple.add(end);
                                        new GraphEdge(start, end, graph, null, "Blank" + atomStart.toString() + atomEnd.toString(), couple).setStyle(DotStyle.BLANK);
                                    }
                                }
                            }
                        }
                    }
                }
            }/* // (isIn(port))
            else if (view.containmentRel.resolve(rel)) {
                //The relation is a containment one.
                for (AlloyTuple tuple : instance.relation2tuples(rel)) {
                    ArrayList<AlloyAtom> atoms = new ArrayList<AlloyAtom>(tuple.getAtoms());
                    AlloyAtom a = atoms.get(0);
                    atoms.remove(0);

                    //containmentTuples
                    List<List<AlloyAtom>> otherTuples = containmentTuples.get(a);
                    if (otherTuples == null) {
                        otherTuples = new ArrayList<List<AlloyAtom>>();
                    }

                    otherTuples.add(atoms);
                    containmentTuples.put(a, otherTuples);

                    //containedInMap
                    for (AlloyAtom atom : atoms) {
                        Set<AlloyAtom> containedIn = containedInMap.get(atom);
                        if (containedIn == null) {
                            containedIn = new TreeSet<AlloyAtom>();
                        }
                        containedIn.add(a);
                        containedInMap.put(atom, containedIn);
                    }
                    //We also add the container in the map, but seeing this relation, it is not contained in anything.
                    Set<AlloyAtom> containedIn = containedInMap.get(a);
                    if (containedIn == null) {
                        containedIn = new TreeSet<AlloyAtom>();
                    }
                    containedInMap.put(a, containedIn);
                }
            }*/
        }
/*
        for (AlloyRelation rel : model.getRelations()) {
            rels.put(rel, null);
        }
        //Verify there is no cycle in containment relations.
        for (AlloyAtom atom : containedInMap.keySet()) {
            if (!isNotCycle(atom)) {
                new GraphNode(graph, "", "The containment relations you have precised are creating a cycle.", "Please click Theme and adjust your settings.");
                return;
            }
        }
        //Call createContainingNode for every atom that is in a containmentTuple but is not contained anywhere.
        for (AlloyAtom atom : containedInMap.keySet()) {
            if (containedInMap.get(atom).isEmpty()) //The atom is not contained in any other atom.
            {
                createContainingNode(hidePrivate, hideMeta, atom, containmentTuples.get(atom), null, view.getDepthMax());
            }
        }*/

        //Iteration over relations of the model:
        // Creates edges and nodes that are linked by them.
        int ci = 0;
        for (AlloyRelation rel : model.getRelations()) {
            DotColor c = view.edgeColor.resolve(rel);
            Color cc = (c == DotColor.MAGIC) ? colors.get(ci) : c.getColor(view.getEdgePalette());
            int count = ((hidePrivate && rel.isPrivate) || !view.edgeVisible.resolve(rel)) ? 0 : edgesAsArcs(graph, hidePrivate, hideMeta, rel, colors.get(ci));
            rels.put(rel, count);
            magicColor.put(rel, cc);
            if (count > 0) {
                ci = (ci + 1) % (colors.size());
            }
        }

        //Iteration over the atoms of the instance:
        // Creates unconnected nodes that are visibles and not hidden-when-unconnected.
        for (AlloyAtom atom : instance.getAllAtoms()) {
            if (atom2node.get(atom) == null) {
                List<AlloySet> sets = instance.atom2sets(atom); //Gets a sorted list of AlloySets containing atom.
                if (sets.size() > 0) {
                    for (AlloySet s : sets) {
                        if (view.nodeVisible.resolve(s) && !view.hideUnconnected.resolve(s)) {
                            //We now have to check if the node we are creating does not exist in any graph.
                            createNode(hidePrivate, hideMeta, atom);
                            break;
                        }
                    }
                } else if (view.nodeVisible.resolve(atom.getType()) && !view.hideUnconnected.resolve(atom.getType())) {
                    createNode(hidePrivate, hideMeta, atom);
                }
            }
        }
        //Iteration over relations of the model to handle those that have to be shown as an attribute
        for (AlloyRelation rel : model.getRelations()) {
            if (!(hidePrivate && rel.isPrivate)) {
                if (view.attribute.resolve(rel)) {
                    edgesAsAttribute(rel);
                }
            }
        }

        //For each not-null entry of attribs, we add labels (each not null string of the value-set) to the GraphNode of the realation. 
        for (Map.Entry<AbstractGraphNode, Set<String>> e : attribs.entrySet()) {
            Set<String> set = e.getValue();
            if (set != null) {
                for (String s : set) {
                    if (s.length() > 0) {
                        if (e.getKey() instanceof GraphNode)
                            ((GraphNode)e.getKey()).addLabel(s);
                    }
                }
            }
        }

        //For each relation registered in rels, we add the legend to the relation in the graph.
        for (Map.Entry<AlloyRelation, Integer> e : rels.entrySet()) {
            Color c = magicColor.get(e.getKey());
            if (c == null) {
                c = Color.BLACK;
            }
            int n = e.getValue();
            if (n > 0) {
                graph.addLegend(e.getKey(), e.getKey().getName() + ": " + n, c);
            } else {
                graph.addLegend(e.getKey(), e.getKey().getName(), null);
            }
        }
    }

    /**
     * Return the node for a specific AlloyAtom (create it if it doesn't exist
     * yet, and adds it to nodes).
     *
     * @return null if the atom is explicitly marked as "Don't Show".
     */
     private AbstractGraphNode createNode(final boolean hidePrivate, final boolean hideMeta, final AlloyAtom atom) {
        return createNode(hidePrivate, hideMeta, atom, graph, 0);
    }

    /**
     * Return the node for a specific AlloyAtom (create it if it doesn't exist
     * yet, and adds it to nodes).
     *
     * @return null if the atom is explicitly marked as "Don't Show".
     */
    private AbstractGraphNode createNode(final boolean hidePrivate, final boolean hideMeta, final AlloyAtom atom, Graph g, int maxDepth) {
        List<AbstractGraphNode> nodesAtom = atom2node.get(atom);
        if (nodesAtom != null) {            
            //If there are nodes for this atom, we check if there is one with the same graph. 
            for (AbstractGraphNode n : nodesAtom) {
                if (n.isInGraph(g)) { //If such a node exist, we don't create it again and return it.
                    return n;
                }
            }
        }
        if ((hidePrivate && atom.getType().isPrivate)
                || (hideMeta && atom.getType().isMeta)
                || !view.nodeVisible(atom, instance)) {
            return null;
        }
        // Make the nodereturn
        DotColor color = view.nodeColor(atom, instance);
        DotStyle style = view.nodeStyle(atom, instance);
        DotShape shape = view.shape(atom, instance);
        String label = atomname(atom, false);

        GraphNode node = new GraphNode(g, atom, maxDepth, label);
        node.setShape(shape);
        node.setColor(color.getColor(view.getNodePalette()));
        node.setStyle(style);
        
        // Get the label based on the sets and relations
        String setsLabel = "";
        boolean showLabelByDefault = view.showAsLabel.get(null);
        for (AlloySet set : instance.atom2sets(atom)) {
            String x = view.label.get(set);
            if (x.length() == 0) {
                continue;
            }
            Boolean showLabel = view.showAsLabel.get(set);
            if ((showLabel == null && showLabelByDefault) || (showLabel != null && showLabel.booleanValue())) {
                setsLabel += ((setsLabel.length() > 0 ? ", " : "") + x);
            }
        }
        if (setsLabel.length() > 0) {
            Set<String> list = attribs.get(node);
            if (list == null) {
                attribs.put(node, list = new TreeSet<String>());
            }
            list.add("(" + setsLabel + ")");
        }
        nodes.put(node, atom);
        if (nodesAtom == null) {
            nodesAtom = new ArrayList<AbstractGraphNode>();
        }
        nodesAtom.add(node);
        atom2node.put(atom, nodesAtom);
        return node;
    }

    //[N7-R.Bossut, M.Quentin]
    /**
     * Function detecting if there is a cycle in the containment relation.
     *
     * @param contained atom whose parents will be tested.
     * @param cantBeContained a set of atoms that can't be container of
     * contained, at any level.
     */
    private boolean isNotCycle(AlloyAtom contained, Set<AlloyAtom> cantBeContainer) {
        Set<AlloyAtom> containers = containedInMap.get(contained);
        if (containers == null) {
            return true; //Should not happen since every atom involved in a containment relation is a key in containedInMap. 
        }
        TreeSet<AlloyAtom> newCantBeContainer;
        if (!containers.isEmpty()) {
            for (AlloyAtom a : cantBeContainer) {
                if (containers.contains(a)) {
                    return false; //If any atom from cantBeContainer is a container, there is a cycle.
                }
            }
            //If no cycle has been detected at this level, we have to check at next level, adding contained to the list of atoms that can't be container.
            for (AlloyAtom a : containers) {
                newCantBeContainer = new TreeSet(cantBeContainer);
                newCantBeContainer.add(a);
                if (!isNotCycle(a, newCantBeContainer)) {
                    return false;
                }
            }
        }
        //If we arrive at this point, it means there are no cycle.
        return true;
    }

    //[N7-R.Bossut, M.Quentin]
    /**
     * Function verifying there is no cycle starting from a specified atom.
     *
     * @param contained the starting point atom for the detection of cycles.
     */
    private boolean isNotCycle(AlloyAtom contained) {
        TreeSet<AlloyAtom> set = new TreeSet<AlloyAtom>();
        set.add(contained);
        return isNotCycle(contained, set);
    }

    //[N7-R.Bossut, M.Quentin]
    /**
     * Create the node for a specific AlloyAtom and each of his children, and
     * their childrens, and so on.
     *
     * @param containedInGraph the graph in which the node that has to be
     * created is. If null, then the node is in the global graph directly.
     * @param maxDepth the maximum depth level of representation of the subGraph
     * of the created node.
     * @return the englobing AlloyNode created, null if marked as "Don't Show".
     */
    private AbstractGraphNode createContainingNode(final boolean hidePrivate, final boolean hideMeta, final AlloyAtom father, List<List<AlloyAtom>> directChilds, Graph containedInGraph, int maxDepth) {
        AbstractGraphNode containingNode;
        if (containedInGraph == null) {
            containingNode = createNode(hidePrivate, hideMeta, father, graph, maxDepth);
        } else {
            containingNode = createNode(hidePrivate, hideMeta, father, containedInGraph, maxDepth);
        }
        if (containingNode == null) return null;
        if (!(directChilds == null)) { //If the given atom has childrens, we have to create corresponding node.
            for (List<AlloyAtom> childs : directChilds) {
                for (AlloyAtom child : childs) {
                    //We use a recursive call because the childrens can also be father.
                    AbstractGraphNode childNode = createContainingNode(hidePrivate, hideMeta, child, containmentTuples.get(child), containingNode.getSubGraph(), maxDepth - 1);
                    if (!(containingNode == null || childNode == null)) //We add the created child to the father childs.
                    {
                        childNode.setFather(containingNode);
                        containingNode.addChild(childNode);
                    }
                }
            }
        }
        return containingNode;
    }

    /**
     * Create an edge for a given tuple from a relation (if neither start nor
     * end node is explicitly invisible)
     *
     * @return the number of edges created, 0 if none.
     */
    private int createEdge(Graph parent, final boolean hidePrivate, final boolean hideMeta, AlloyRelation rel, AlloyTuple tuple, boolean bidirectional, Color magicColor) {
        // This edge represents a given tuple from a given relation.
        //
        // If the tuple's arity==2, then the label is simply the label of the relation.
        //
        // If the tuple's arity>2, then we append the node labels for all the intermediate nodes.
        // eg. Say a given tuple is (A,B,C,D) from the relation R.
        // An edge will be drawn from A to D, with the label "R [B, C]"
        if ((hidePrivate && tuple.getStart().getType().isPrivate)
                || (hideMeta && tuple.getStart().getType().isMeta)
                || !view.nodeVisible(tuple.getStart(), instance)) {
            return 0;
        }
        if ((hidePrivate && tuple.getEnd().getType().isPrivate)
                || (hideMeta && tuple.getEnd().getType().isMeta)
                || !view.nodeVisible(tuple.getEnd(), instance)) {
            return 0;
        }

        AlloyAtom atomStart = tuple.getStart();
        AlloyAtom atomEnd = tuple.getEnd();
        int edgeArity = tuple.getArity();
        //Check if this is a containment relation.
        boolean isContainment = view.containmentRel.resolve(rel);
        if (isContainment) {
            if (edgeArity < 3) {
                //If there is only 2 atoms in the tuple and one of them is the container, we don't create any edge. 
                return 0;
            }
            atomStart = tuple.getAtoms().get(1); //First atom after the container (starting atom for the edge).
            edgeArity--; //If we have to create an edge for a containment relation, the arity is 1 lower.
        }

        //If no node corresponding to the atom has already been created, we create it here.
        List<AbstractGraphNode> starts = atom2node.get(atomStart);
        List<AbstractGraphNode> ends = atom2node.get(atomEnd);
        if (starts == null) {
            createNode(hidePrivate, hideMeta, atomStart);
            starts = atom2node.get(atomStart);
        }
        if (ends == null) {
            createNode(hidePrivate, hideMeta, atomEnd);
            ends = atom2node.get(atomEnd);
        }
        if (starts == null || ends == null) {
            return 0;
        }
        int r = 0;
        boolean sameGraph = false;
        //If there is one start and one end in a same graph, we shall not draw edges between different graphs.
        for (AbstractGraphNode start : starts) {
            for (AbstractGraphNode end : ends) {
                if (end.graph == start.graph){
                    sameGraph = true;
                    break;
                }
            }
        }
            
        for (AbstractGraphNode start : starts) {
          for (AbstractGraphNode end : ends) {
            if (!start.isContainedIn(end) && !end.isContainedIn(start)){
              if (!sameGraph || (start.graph == end.graph)){
                boolean layoutBack = view.layoutBack.resolve(rel);
                String label = view.label.get(rel);
                if (edgeArity > 2) {
                    StringBuilder moreLabel = new StringBuilder();
                    List<AlloyAtom> atoms = tuple.getAtoms();
                    // Label of the edge: if it represents a containment relation, we have to adapt the label.
                    // we do not add the label of the container in the [] of the label.
                    boolean comma = false;
                    for (int i = 1; i < atoms.size() - 1; i++) {
                      if (!(isContainment && i == 1)){
                        if (comma) {
                          moreLabel.append(", ");
                        }
                        moreLabel.append(atomname(atoms.get(i), false));
                        comma = true;
                      }
                    }
                    if (label.length() == 0) { /* label=moreLabel.toString(); */ } else {
                        label = label + (" [" + moreLabel + "]");
                    }
                }
                DotDirection dir = bidirectional ? DotDirection.BOTH : (layoutBack ? DotDirection.BACK : DotDirection.FORWARD);
                DotStyle style = view.edgeStyle.resolve(rel);
                DotColor color = view.edgeColor.resolve(rel);
                int weight = view.weight.get(rel);
                GraphEdge e = new GraphEdge((layoutBack ? end : start), (layoutBack ? start : end), parent, tuple, label, rel);
                if (color == DotColor.MAGIC && magicColor != null) {
                    e.set(magicColor);
                } else {
                    e.set(color.getColor(view.getEdgePalette()));
                }
                e.set(style);
                e.set(dir != DotDirection.FORWARD, dir != DotDirection.BACK); // Set the arrowhead
                e.set(weight < 1 ? 1 : (weight > 100 ? 10000 : 100 * weight));
                edges.put(e, tuple);
                r++;
              }
            }
          }
        }
        return r;
    }

    /**
     * [N7] @Julien Richer
     * Return the port for a specific AlloyAtom (create it if it doesn't exist
     * yet).
     * Color has to be set with the following setPortColor method
     */
    private GraphPort createPort(AlloyAtom atom, GraphNode node, AlloyRelation rel, String label, GraphPort.Orientation ori) {
        if (node == null) {
            return null;
        }
        
        GraphPort port = null;
        List<AbstractGraphNode> lports = atom2port.get(atom);
        boolean create = true;
        if (lports != null){
            for(AbstractGraphNode n : lports){
                if (n instanceof GraphPort && ((GraphPort)n).getNode() == node){
                    create = false;
                    break;
                }
            }
        }
        if (lports == null || create){
            // Get the label based on the sets and relations
            String setsLabel = "";
            boolean showLabelByDefault = view.showAsLabel.get(null);
            for (AlloySet set : instance.atom2sets(atom)) {
                String x = view.label.get(set);
                if (x.length() == 0) {
                    continue;
                }
                Boolean showLabel = view.showAsLabel.get(set);
                if ((showLabel == null && showLabelByDefault) || (showLabel != null && showLabel.booleanValue())) {
                    setsLabel += ((setsLabel.length() > 0 ? ", " : "") + x);
                }
            }
            if (setsLabel.length() > 0) {
                Set<String> list = attribs.get(node);
                if (list == null) {
                    attribs.put(node, list = new TreeSet<String>());
                }
                list.add("(" + setsLabel + ")");
            }

            // Make the port
            port = new GraphPort(node, label + "(" + rel.getName() + ")", label, ori);

            // Add it to the maps
            ports.put(port, atom);
            if (lports == null)
                lports = new ArrayList<AbstractGraphNode>();
            lports.add(port);
            atom2port.put(atom, lports);
            
            // Erase the node that became a port
            view.nodeVisible.put(atom.getType(), Boolean.FALSE);       
        }

        GraphPort result = null;
        for (AbstractGraphNode n : atom2port.get(atom)){
            if (n instanceof GraphPort){
                port = (GraphPort)n;
            
                // Set the port orientation
                GraphPort.Orientation orient = view.orientations.get(rel);
                if(orient!=null) {
                    port.setOrientation(orient);
                }
                else {
                    // Default orientation
                    port.setOrientation(ori);
                }

                // Set the port shape
                DotShape shape = view.portShape.resolve(rel);
                if(shape!=null) {
                    port.setShape(shape);
                }
                else {
                    // Default shape
                    port.setShape(DotShape.BOX);
                }

                // Set the label visibility
                port.setHideLabel(view.portHideLabel.resolve(rel));
                if (port.getNode() == node){
                    result = port;
                }
            }
        }
        return result;
    }
    
    /**
     * [N7] @Julien Richer
     * Set the port color
     */
    private void setPortColor(GraphPort port, AlloyRelation rel, Color magicColor) {
        // Set the port color
        DotColor color = view.portColor.resolve(rel);
        if (color == DotColor.MAGIC && magicColor != null) {
            port.setColor(magicColor);
        }
        else if(color!=null) {
            port.setColor(color.getColor(view.getPortPalette()));
        }
        else {
            // Default color
            port.setColor(Color.red);
        }
    }

    /**
     * Create edges for every visible tuple in the given relation.
     */
    private int edgesAsArcs(Graph parent, final boolean hidePrivate, final boolean hideMeta, AlloyRelation rel, Color magicColor) {
        int count = 0;
        int nbNewEdges;
        if (!view.mergeArrows.resolve(rel)) {
            // If we're not merging bidirectional arrows, simply create edges for each tuple 
            // (due to the duplication of some nodes in subgraph, there can be more than one edge for a tuple).
            for (AlloyTuple tuple : instance.relation2tuples(rel)) {
                nbNewEdges = createEdge(parent, hidePrivate, hideMeta, rel, tuple, false, magicColor);
                if (nbNewEdges > 0) {
                    count = count + nbNewEdges;
                }
            }
            return count;
        }
        // Otherwise, find bidirectional arrows and only create one edge for each pair.
        Set<AlloyTuple> tuples = instance.relation2tuples(rel);
        Set<AlloyTuple> ignore = new LinkedHashSet<AlloyTuple>();
        for (AlloyTuple tuple : tuples) {
            if (!ignore.contains(tuple)) {
                AlloyTuple reverse = tuple.getArity() > 2 ? null : tuple.reverse();
                // If the reverse tuple is in the same relation, and it is not a self-edge, then draw it as a <-> arrow.
                if (reverse != null && tuples.contains(reverse) && !reverse.equals(tuple)) {
                    ignore.add(reverse);
                    nbNewEdges = createEdge(parent, hidePrivate, hideMeta, rel, tuple, true, magicColor);
                    if (nbNewEdges > 0) {
                        count = count + 2 * nbNewEdges;
                    }
                } else {
                    nbNewEdges = createEdge(parent, hidePrivate, hideMeta, rel, tuple, false, magicColor);
                    if (nbNewEdges > 0) {
                        count = count + nbNewEdges;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Attach tuple values as attributes to existing nodes.
     */
    private void edgesAsAttribute(AlloyRelation rel) {
        // If this relation wants to be shown as an attribute,
        // then generate the annotations and attach them to each tuple's starting node.
        // Eg.
        //   If (A,B) and (A,C) are both in the relation F,
        //   then the A node would have a line that says "F: B, C"
        // Eg.
        //   If (A,B,C) and (A,D,E) are both in the relation F,
        //   then the A node would have a line that says "F: B->C, D->E"
        // Eg.
        //   If (A,B,C) and (A,D,E) are both in the relation F, and B belongs to sets SET1 and SET2,
        //   and SET1's "show in relational attribute" is on,
        //   and SET2's "show in relational attribute" is on,
        //   then the A node would have a line that says "F: B (SET1, SET2)->C, D->E"
        //
        Map<AbstractGraphNode, String> map = new LinkedHashMap<AbstractGraphNode, String>();
        for (AlloyTuple tuple : instance.relation2tuples(rel)) {
            List<AbstractGraphNode> starts = atom2node.get(tuple.getStart());
            if (starts == null) {
                continue; // null means the node won't be shown, so we can't show any attributes
            }
            String attr = "";
            List<AlloyAtom> atoms = tuple.getAtoms();
            for (int i = 1; i < atoms.size(); i++) {
                if (i > 1) {
                    attr += "->";
                }
                attr += atomname(atoms.get(i), true);
            }
            if (attr.length() == 0) {
                continue;
            }
            for (AbstractGraphNode start : starts) {
                String oldattr = map.get(start);
                if (oldattr != null && oldattr.length() > 0) {
                    attr = oldattr + ", " + attr;
                }
                if (attr.length() > 0) {
                    map.put(start, attr);
                }
            }
        }
        for (Map.Entry<AbstractGraphNode, String> e : map.entrySet()) {
            AbstractGraphNode node = e.getKey();
            Set<String> list = attribs.get(node);
            if (list == null) {
                attribs.put(node, list = new TreeSet<String>());
            }
            String attr = e.getValue();
            if (view.label.get(rel).length() > 0) {
                attr = view.label.get(rel) + ": " + attr;
            }
            list.add(attr);
        }
    }

    /**
     * Return the label for an atom.
     *
     * @param atom - the atom
     * @param showSets - whether the label should also show the sets that this
     * atom belongs to
     *
     * <p>
     * eg. If atom A is the 3rd atom in type T, and T's label is "Person", then
     * the return value would be "Person3".
     *
     * <p>
     * eg. If atom A is the only atom in type T, and T's label is "Person", then
     * the return value would be "Person".
     *
     * <p>
     * eg. If atom A is the 3rd atom in type T, and T's label is "Person", and T
     * belongs to the sets Set1, Set2, and Set3. However, only Set1 and Set2
     * have "show in relational attribute == on", then the return value would be
     * "Person (Set1, Set2)".
     */
    public String atomname(AlloyAtom atom, boolean showSets) {
        String label = atom.getVizName(view, view.number.resolve(atom.getType()));
        if (!showSets) {
            return label;
        }
        String attr = "";
        boolean showInAttrByDefault = view.showAsAttr.get(null);
        for (AlloySet set : instance.atom2sets(atom)) {
            String x = view.label.get(set);
            if (x.length() == 0) {
                continue;
            }
            Boolean showAsAttr = view.showAsAttr.get(set);
            if ((showAsAttr == null && showInAttrByDefault) || (showAsAttr != null && showAsAttr)) {
                attr += ((attr.length() > 0 ? ", " : "") + x);
            }
        }
        if (label.length() == 0) {
            return (attr.length() > 0) ? ("(" + attr + ")") : "";
        }
        return (attr.length() > 0) ? (label + " (" + attr + ")") : label;
    }

    static String esc(String name) {
        if (name.indexOf('\"') < 0) {
            return name;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '\"') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.toString();
    }

    /**
     * [N7] Modified by @Louis Fauvarque Indicate if a relation is in an array
     * of relations
     *
     * @param elementsArray
     * @param elt
     * @return boolean res
     */
    private boolean isIn(ArrayList<AlloyRelation> elementsArray, AlloyRelation elt) {
        for (AlloyRelation eltA : elementsArray) {
            if (eltA != null) {
                if (eltA.compareTo(elt) == 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * [N7] Modified by @Louis Fauvarque
     * Indicate if an atom is designed to be a port
     *
     * @param portRelations
     * @param atom
     * @return true if atom represents a port in portRelations
     */
    public boolean isPort(ArrayList<AlloyRelation> portRelations, AlloyAtom atom) {
        for (AlloyRelation eltA : portRelations) {
            if (eltA != null) {
                List<AlloyType> lst = eltA.getTypes();
                if (lst.contains(atom.getType()) && lst.indexOf(atom.getType()) == lst.lastIndexOf(atom.getType()) && lst.indexOf(atom.getType()) != 0) {
                    return true;
                }
            }
        }
        return false;
    }

    
    private ArrayList<AlloyAtom> getPorts(ArrayList<AlloyRelation> portRelations, AlloyInstance instance){
        ArrayList<AlloyAtom> res = new ArrayList<AlloyAtom>();
            for(AlloyAtom atom : instance.getAllAtoms()){
                if(isPort(portRelations, atom)){
                    res.add(atom);
                }
            }
        return res;
    }
    
    public List<AbstractGraphNode> getPortsFromAtom(AlloyAtom at){
        return atom2port.get(at);
    }
            
    public List<AbstractGraphNode> getNodesFromAtom(AlloyAtom at){
        return atom2node.get(at);
    }
}
