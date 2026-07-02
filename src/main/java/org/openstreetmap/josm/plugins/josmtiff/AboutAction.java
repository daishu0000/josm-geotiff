package org.openstreetmap.josm.plugins.josmtiff;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.MainApplication;

public class AboutAction extends JosmAction {

    public AboutAction() {
        super(tr("About JOSM TIFF"), null, tr("Show JOSM TIFF plugin information"), null, false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                tr("JOSM TIFF plugin v0.0.1\nMinimal demo plugin."),
                tr("JOSM TIFF"),
                JOptionPane.INFORMATION_MESSAGE);
    }
}
