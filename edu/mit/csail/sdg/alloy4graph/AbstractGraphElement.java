/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.mit.csail.sdg.alloy4graph;

import java.awt.Color;

/**
 *
 * @author gdupont
 */
public abstract class AbstractGraphElement {
    /**
     * Graph element's object uuid.
     * This can be null; this has not to be unique.
     */
    public final Object uuid;
    
    /**
     * The node color; never null.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private Color color = Color.WHITE;

    /**
     * The line style; never null.
     * <p>
     * When this value changes, we should invalidate the previously computed
     * bounds information.
     */
    private DotStyle style = DotStyle.SOLID;
    
    private int fontSize = 12;
    
    /**
     * Determine if the node is highlighted.
     */
    boolean highlight = false;
    
    public AbstractGraphElement(Object u) {
        this.uuid = u;
    }
    
    /**
     * Get port color
     * @return port colro
     */
    public Color getColor() {
        return color;
    }

    /**
     * Set port color
     * @param color new color
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Get port line style
     * @return the port line style
     */
    public DotStyle getStyle() {
        return style;
    }

    /**
     * Set port line style
     * @param style new port line style
     */
    public void setStyle(DotStyle style) {
        this.style = style;
    }
    
    /**
     * Get the font size
     * @return font size
     */
    public int getFontSize() {
        return fontSize;
    }
    
    /**
     * Set the font size
     * @param fs new font size
     */
    public void setFontSize(int fs) {
        this.fontSize = fs;
    }
    
    public boolean highlight() {
        return highlight;
    }
    
    public void setHighlight(boolean h) {
        this.highlight = h;
    }
}
