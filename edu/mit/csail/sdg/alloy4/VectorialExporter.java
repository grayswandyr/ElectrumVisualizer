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
package edu.mit.csail.sdg.alloy4;

import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * [N7-G.Dupont] This class is a small GUI for exporting an instance with a
 * vectorial format.
 */
public class VectorialExporter extends javax.swing.JPanel {
    /**
     * This class represent page formats (this helps for creating and manipulating
     * the format combobox model).
     */
    private static class PageFormat {
        /**
         * Name of the format.
         */
        public String name;
        
        /**
         * Dimensions of the format (in mm).
         */
        public int width, height;
        
        /**
         * Specify if it is possible for the user to edit the dimensions of this
         * format.
         * Typically, we won't let the user edit the dimensions of A4 landscape;
         * but a "custom" format can be used to specify more "exotic" dimensions.
         */
        public boolean editable;
        
        /**
         * Constructor.
         * @param n name of the format
         * @param w width of the format in mm
         * @param h height of the format in mm
         * @param e editability of the format
         */
        public PageFormat(String n, int w, int h, boolean e) {
            name = n;
            width = w;
            height = h;
            editable = e;
        }
    }
    
    /**
     * Defines the available page formats.
     */
    private static final PageFormat pageFormats[] = {
        new PageFormat("A3 Portrait" , 297, 420, false),
        new PageFormat("A3 Landscape", 420, 297, false),
        new PageFormat("A4 Portrait" , 210, 297, false),
        new PageFormat("A4 Landscape", 297, 210, false),
        new PageFormat("Custom", 100, 100, true)
    };
    /**
     * Default page format.
     */
    private static final int defaultPageFormat = 2; //A4, Portrait
    
    /**
     * Default image margins.
     */
    private static final int defaultMargin = 20;
    
    /**
     * Specify the possible output formats.
     * That is, the actual form the file will take : PDF, SVG, etc.
     */
    public enum OutputFormat {
        PDF(".pdf", "PDF files"),
        SVG(".svg", "Scalable Vector Graphics files"),
        EPS(".eps", "Encapsulated PostScript files");
        
        /**
         * Two fields for this enumeration (mostly for easy use in the rest of
         * the form) : the extension (.*) and the description.
         */
        final String extension, description;
        
        /**
         * Constructor
         * @param extension format extension
         * @param description format description
         */
        private OutputFormat(String extension, String description) {
            this.extension = extension;
            this.description = description;
        }
        
        /**
         * Get the extension of the format
         * @return a string
         */
        public String extension() {
            return this.extension;
        }
        
        /**
         * Get the extension of the description
         * @return a string
         */
        public String description() {
            return this.description;
        }
    }
    
    /**
     * Selected output format.
     */
    private OutputFormat format = OutputFormat.PDF;
    
    /**
     * This class represent an export callback.
     * This callback is called when the user quits the window via
     * the "export" button.
     */
    public interface ExportCallback {
        /**
         * Export action; to be implemented.
         * @param filename name of the file
         * @param format format of the file
         * @param width width of the page/canvas
         * @param height height of the page/canvas
         * @param marginleft margin on the left of the page/canvas
         * @param marginright margin on the right of the page/canvas
         * @param margintop margin on the top of the page/canvas
         * @param marginbottom margin on the bottom of the page/canvas
         */
        public void exportAction(
                String filename,
                OutputFormat format,
                Integer width, Integer height,
                Integer marginleft, Integer marginright,
                Integer margintop, Integer marginbottom);
    }
    
    /**
     * The export callback of this object.
     */
    private ExportCallback exportCallback;

    /**
     * Constructor.
     * @param ec export callback to bind to this object
     */
    public VectorialExporter(ExportCallback ec) {
        this.exportCallback = ec;
        
        // Init UI
        initComponents();
        
        // Button group
        formatGroup.add(pdfSelect);
        formatGroup.add(svgSelect);
        formatGroup.add(epsSelect);
        
        // Page format combobox
        Vector<String> model = new Vector<String>();
        for (PageFormat f : pageFormats) {
            model.add(f.name);
        }
        formatComboBox.setModel(new DefaultComboBoxModel(model));
        formatComboBox.setSelectedIndex(defaultPageFormat);
        
        // Spinners
        widthSpinner.setValue(pageFormats[defaultPageFormat].width);
        heightSpinner.setValue(pageFormats[defaultPageFormat].height);
        widthSpinner.setEnabled(pageFormats[defaultPageFormat].editable);
        heightSpinner.setEnabled(pageFormats[defaultPageFormat].editable);
        
        marginLeftSpinner.setValue(defaultMargin);
        marginRightSpinner.setValue(defaultMargin);
        marginTopSpinner.setValue(defaultMargin);
        marginBottomSpinner.setValue(defaultMargin);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        formatGroup = new javax.swing.ButtonGroup();
        formatPanel = new javax.swing.JPanel();
        formatFieldLabel = new javax.swing.JLabel();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        pdfSelect = new javax.swing.JRadioButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        svgSelect = new javax.swing.JRadioButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        epsSelect = new javax.swing.JRadioButton();
        filePanel = new javax.swing.JPanel();
        fileFieldLabel = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 0), new java.awt.Dimension(5, 32767));
        fileField = new javax.swing.JTextField();
        fileSearchButton = new javax.swing.JButton();
        sizeLabelPanel = new javax.swing.JPanel();
        sizeLabel = new javax.swing.JLabel();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        sizePanelWrapper = new javax.swing.JPanel();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        sizePanel = new javax.swing.JPanel();
        formatLabel = new javax.swing.JLabel();
        formatComboBox = new javax.swing.JComboBox();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(1, 1), new java.awt.Dimension(1, 1), new java.awt.Dimension(1, 1));
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(1, 1), new java.awt.Dimension(1, 1), new java.awt.Dimension(1, 1));
        widthLabel = new javax.swing.JLabel();
        widthSpinner = new javax.swing.JSpinner();
        heightLabel = new javax.swing.JLabel();
        heightSpinner = new javax.swing.JSpinner();
        marginLeftLabel = new javax.swing.JLabel();
        marginLeftSpinner = new javax.swing.JSpinner();
        marginRightLabel = new javax.swing.JLabel();
        marginRightSpinner = new javax.swing.JSpinner();
        marginTopLabel = new javax.swing.JLabel();
        marginTopSpinner = new javax.swing.JSpinner();
        marginBottomLabel = new javax.swing.JLabel();
        marginBottomSpinner = new javax.swing.JSpinner();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));
        buttonPanel = new javax.swing.JPanel();
        filler11 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        exportButton = new javax.swing.JButton();
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        cancelButton = new javax.swing.JButton();
        filler12 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        formatPanel.setLayout(new javax.swing.BoxLayout(formatPanel, javax.swing.BoxLayout.LINE_AXIS));

        formatFieldLabel.setText("Format :");
        formatPanel.add(formatFieldLabel);
        formatPanel.add(filler2);

        pdfSelect.setSelected(true);
        pdfSelect.setText("PDF");
        pdfSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pdfSelectActionPerformed(evt);
            }
        });
        formatPanel.add(pdfSelect);
        formatPanel.add(filler3);

        svgSelect.setText("SVG");
        svgSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                svgSelectActionPerformed(evt);
            }
        });
        formatPanel.add(svgSelect);
        formatPanel.add(filler4);

        epsSelect.setText("EPS");
        epsSelect.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                epsSelectActionPerformed(evt);
            }
        });
        formatPanel.add(epsSelect);

        add(formatPanel);

        filePanel.setLayout(new javax.swing.BoxLayout(filePanel, javax.swing.BoxLayout.LINE_AXIS));

        fileFieldLabel.setText("File :");
        filePanel.add(fileFieldLabel);
        filePanel.add(filler1);

        fileField.setMaximumSize(new java.awt.Dimension(2147483647, 28));
        filePanel.add(fileField);

        fileSearchButton.setText("...");
        fileSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileSearchButtonActionPerformed(evt);
            }
        });
        filePanel.add(fileSearchButton);

        add(filePanel);

        sizeLabelPanel.setLayout(new javax.swing.BoxLayout(sizeLabelPanel, javax.swing.BoxLayout.LINE_AXIS));

        sizeLabel.setText("Size :");
        sizeLabelPanel.add(sizeLabel);
        sizeLabelPanel.add(filler5);

        add(sizeLabelPanel);

        sizePanelWrapper.setLayout(new javax.swing.BoxLayout(sizePanelWrapper, javax.swing.BoxLayout.LINE_AXIS));
        sizePanelWrapper.add(filler6);

        sizePanel.setLayout(new java.awt.GridLayout(4, 4, 10, 0));

        formatLabel.setText("Paper size :");
        sizePanel.add(formatLabel);

        formatComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatComboBoxActionPerformed(evt);
            }
        });
        sizePanel.add(formatComboBox);
        sizePanel.add(filler7);
        sizePanel.add(filler8);

        widthLabel.setText("Width :");
        sizePanel.add(widthLabel);

        widthSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        sizePanel.add(widthSpinner);

        heightLabel.setText("Height :");
        sizePanel.add(heightLabel);

        heightSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        sizePanel.add(heightSpinner);

        marginLeftLabel.setText("Margin left :");
        sizePanel.add(marginLeftLabel);

        marginLeftSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        sizePanel.add(marginLeftSpinner);

        marginRightLabel.setText("Margin right :");
        sizePanel.add(marginRightLabel);

        marginRightSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        sizePanel.add(marginRightSpinner);

        marginTopLabel.setText("Margin top :");
        sizePanel.add(marginTopLabel);

        marginTopSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        sizePanel.add(marginTopSpinner);

        marginBottomLabel.setText("Margin bottom :");
        sizePanel.add(marginBottomLabel);

        marginBottomSpinner.setModel(new javax.swing.SpinnerNumberModel());
        sizePanel.add(marginBottomSpinner);

        sizePanelWrapper.add(sizePanel);

        add(sizePanelWrapper);
        add(filler9);

        buttonPanel.setLayout(new javax.swing.BoxLayout(buttonPanel, javax.swing.BoxLayout.LINE_AXIS));
        buttonPanel.add(filler11);

        exportButton.setText("Export");
        exportButton.setMaximumSize(new java.awt.Dimension(100, 28));
        exportButton.setMinimumSize(new java.awt.Dimension(100, 28));
        exportButton.setPreferredSize(new java.awt.Dimension(100, 28));
        exportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(exportButton);
        buttonPanel.add(filler10);

        cancelButton.setText("Cancel");
        cancelButton.setMaximumSize(new java.awt.Dimension(100, 28));
        cancelButton.setMinimumSize(new java.awt.Dimension(100, 28));
        cancelButton.setPreferredSize(new java.awt.Dimension(100, 28));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        buttonPanel.add(cancelButton);
        buttonPanel.add(filler12);

        add(buttonPanel);
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Event raised when the "..." (file search button) is pressed.
     * @param evt 
     */
    private void fileSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileSearchButtonActionPerformed
        File file = OurDialog.askFile(false, null, format.extension(), format.description());
        if (file==null) return;
        if (file.exists() && !OurDialog.askOverwrite(file.getAbsolutePath())) return;
        fileField.setText(file.getAbsolutePath());
    }//GEN-LAST:event_fileSearchButtonActionPerformed

    /**
     * Event raised when 'SVG' is clicked.
     * @param evt 
     */
    private void svgSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_svgSelectActionPerformed
        format = OutputFormat.SVG;
    }//GEN-LAST:event_svgSelectActionPerformed

    /**
     * Event raised when 'PDF' is clicked.
     * @param evt 
     */
    private void pdfSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pdfSelectActionPerformed
        format = OutputFormat.PDF;
    }//GEN-LAST:event_pdfSelectActionPerformed

    /**
     * Event raised when 'EPS' is clicked.
     * @param evt 
     */
    private void epsSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_epsSelectActionPerformed
        format = OutputFormat.EPS;
    }//GEN-LAST:event_epsSelectActionPerformed

    /**
     * Event raised when "Export" button is clicked.
     * This basically cause the callback to be called and the window to close.
     * @param evt 
     */
    private void exportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportButtonActionPerformed
        exportCallback.exportAction(
                fileField.getText(), format,
                (Integer)widthSpinner.getValue(), (Integer)heightSpinner.getValue(),
                (Integer)marginLeftSpinner.getValue(), (Integer)marginRightSpinner.getValue(),
                (Integer)marginTopSpinner.getValue(), (Integer)marginBottomSpinner.getValue());
        quit();
    }//GEN-LAST:event_exportButtonActionPerformed

    /**
     * Event raised when "Cancel" button is clicked.
     * This close the window without running the callback.
     * @param evt 
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        quit();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Close this window.
     */
    private void quit() {
        Window parent = SwingUtilities.getWindowAncestor(this);
        parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
    }
    
    /**
     * Event raised when the user set the value in the combobox.
     * @param evt 
     */
    private void formatComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formatComboBoxActionPerformed
        // Retrieve selected item's index
        int id = formatComboBox.getSelectedIndex();
        
        // Get the corresponding page format
        final PageFormat pf = pageFormats[id];
        
        // Apply page format attribute (dimension + editability) to the width and
        // height spinners.
        widthSpinner.setValue(pf.width);
        widthSpinner.setEnabled(pf.editable);
        heightSpinner.setValue(pf.height);
        heightSpinner.setEnabled(pf.editable);
    }//GEN-LAST:event_formatComboBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JRadioButton epsSelect;
    private javax.swing.JButton exportButton;
    private javax.swing.JTextField fileField;
    private javax.swing.JLabel fileFieldLabel;
    private javax.swing.JPanel filePanel;
    private javax.swing.JButton fileSearchButton;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler10;
    private javax.swing.Box.Filler filler11;
    private javax.swing.Box.Filler filler12;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.Box.Filler filler9;
    private javax.swing.JComboBox formatComboBox;
    private javax.swing.JLabel formatFieldLabel;
    private javax.swing.ButtonGroup formatGroup;
    private javax.swing.JLabel formatLabel;
    private javax.swing.JPanel formatPanel;
    private javax.swing.JLabel heightLabel;
    private javax.swing.JSpinner heightSpinner;
    private javax.swing.JLabel marginBottomLabel;
    private javax.swing.JSpinner marginBottomSpinner;
    private javax.swing.JLabel marginLeftLabel;
    private javax.swing.JSpinner marginLeftSpinner;
    private javax.swing.JLabel marginRightLabel;
    private javax.swing.JSpinner marginRightSpinner;
    private javax.swing.JLabel marginTopLabel;
    private javax.swing.JSpinner marginTopSpinner;
    private javax.swing.JRadioButton pdfSelect;
    private javax.swing.JLabel sizeLabel;
    private javax.swing.JPanel sizeLabelPanel;
    private javax.swing.JPanel sizePanel;
    private javax.swing.JPanel sizePanelWrapper;
    private javax.swing.JRadioButton svgSelect;
    private javax.swing.JLabel widthLabel;
    private javax.swing.JSpinner widthSpinner;
    // End of variables declaration//GEN-END:variables

    /**
     * Create a modal dialog from this JPanel.
     * @param parent the parent of the modal dialog
     * @param title title of the dialog
     * @param cb event callback to pass to the object
     */
    public static void asModalDialog(Window parent, String title, ExportCallback cb) {
        final JDialog dlg = new JDialog(parent, title, Dialog.ModalityType.APPLICATION_MODAL);
        dlg.getContentPane().add(new VectorialExporter(cb));
        dlg.pack();
        dlg.setLocationRelativeTo(parent);
        dlg.setVisible(true);
    }
}
