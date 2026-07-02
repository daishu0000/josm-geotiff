package org.openstreetmap.josm.plugins.josmtiff;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Logging;

/**
 * Action to import a GeoTIFF file and add it as a layer.
 */
public class ImportGeoTiffAction extends JosmAction {

    public ImportGeoTiffAction() {
        super(tr("Import GeoTIFF"), "imagery_menu", tr("Import a georeferenced GeoTIFF file as a layer"),
                null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(tr("Select GeoTIFF file"));
        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }
                String name = f.getName().toLowerCase();
                return name.endsWith(".tif") || name.endsWith(".tiff");
            }

            @Override
            public String getDescription() {
                return tr("GeoTIFF files (*.tif, *.tiff)");
            }
        });

        if (chooser.showOpenDialog(MainApplication.getMainFrame()) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = chooser.getSelectedFile();
        try {
            GeoTiffData data = GeoTiffLoader.read(file);
            MainLayerManager layerManager = MainApplication.getLayerManager();

            OsmDataLayer dataLayer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            layerManager.addLayer(dataLayer, false);

            GeoTiffLayer layer = new GeoTiffLayer(data);
            layerManager.addLayer(layer, true);
        } catch (IOException ex) {
            Logging.error(ex);
            JOptionPane.showMessageDialog(
                    MainApplication.getMainFrame(),
                    tr("Failed to import GeoTIFF:\n{0}", ex.getMessage()),
                    tr("Import GeoTIFF"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
