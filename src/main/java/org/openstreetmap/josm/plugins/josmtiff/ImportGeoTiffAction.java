package org.openstreetmap.josm.plugins.josmtiff;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.MainLayerManager;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

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

        new ImportGeoTiffTask(chooser.getSelectedFile()).run();
    }

    private static final class ImportGeoTiffTask extends PleaseWaitRunnable {

        private final File file;
        private GeoTiffData data;

        ImportGeoTiffTask(File file) {
            super(tr("Importing GeoTIFF..."), false);
            this.file = file;
        }

        @Override
        protected void cancel() {
            // GeoTIFF decoding cannot be interrupted yet; ignore the loaded result in finish().
        }

        @Override
        protected void realRun() throws IOException {
            getProgressMonitor().indeterminateSubTask(tr("Reading {0}...", file.getName()));
            data = GeoTiffLoader.read(file);
        }

        @Override
        protected void finish() {
            if (getProgressMonitor().isCanceled() || data == null) {
                return;
            }

            MainLayerManager layerManager = MainApplication.getLayerManager();
            OsmDataLayer dataLayer = new OsmDataLayer(new DataSet(), OsmDataLayer.createNewName(), null);
            layerManager.addLayer(dataLayer, false);

            GeoTiffLayer layer = new GeoTiffLayer(data);
            layerManager.addLayer(layer, true);
        }
    }
}
