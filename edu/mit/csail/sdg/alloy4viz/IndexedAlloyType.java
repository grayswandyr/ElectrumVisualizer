/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.csail.sdg.alloy4viz;

/**
 *
 * @author mquentin
 */
public class IndexedAlloyType extends AlloyType {

    int index;
    
    public IndexedAlloyType(String name, boolean isOne, boolean isAbstract, boolean isBuiltin, boolean isPrivate, boolean isMeta, boolean isEnum, int index) {
        super(name, isOne, isAbstract, isBuiltin, isPrivate, isMeta, isEnum);
        this.index = index;
    }
    
    public String toString() {
        return this.getName() + "(" + index + ")";
    }
    
    
}
