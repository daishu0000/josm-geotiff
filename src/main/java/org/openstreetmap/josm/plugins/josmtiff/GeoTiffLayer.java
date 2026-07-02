package org.openstreetmap.josm.plugins.josmtiff;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.io.File;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

/**
 * Layer that displays a georeferenced GeoTIFF image.
 */
public class GeoTiffLayer extends Layer {

    private final File sourceFile;
    private java.awt.image.BufferedImage image;
    private EastNorth min;
    private EastNorth max;

    public GeoTiffLayer(GeoTiffData data) {
        super(data.getSourceFile().getName());
        this.sourceFile = data.getSourceFile();
        this.image = data.getImage();
        this.min = data.getMin();
        this.max = data.getMax();
        setAssociatedFile(sourceFile);
        setBackgroundLayer(true);
    }

    @Override
    public void paint(Graphics2D g2, MapView mv, Bounds box) {
        if (image == null || !isVisible()) {
            return;
        }

        Point minPt = mv.getPoint(min);
        Point maxPt = mv.getPoint(max);
        int x = minPt.x;
        int y = maxPt.y;
        int width = maxPt.x - minPt.x;
        int height = minPt.y - maxPt.y;
        if (width <= 0 || height <= 0) {
            return;
        }

        Composite oldComposite = g2.getComposite();
        if (getOpacity() < 1.0) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) getOpacity()));
        }
        g2.drawImage(image, x, y, width, height, null);
        g2.setComposite(oldComposite);
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor visitor) {
        visitor.visit(min);
        visitor.visit(max);
    }

    @Override
    public Icon getIcon() {
        return ImageProvider.get("imagery_menu", ImageSizes.LAYER);
    }

    @Override
    public String getToolTipText() {
        return tr("GeoTIFF: {0}", sourceFile.getName());
    }

    @Override
    public void mergeFrom(Layer from) {
        // not supported
    }

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public Object getInfoComponent() {
        return null;
    }

    @Override
    public Action[] getMenuEntries() {
        return new Action[0];
    }

    @Override
    public boolean isSavable() {
        return false;
    }

    @Override
    public void projectionChanged(Projection oldValue, Projection newValue) {
        try {
            GeoTiffData data = GeoTiffLoader.read(sourceFile);
            this.image = data.getImage();
            this.min = data.getMin();
            this.max = data.getMax();
            invalidate();
        } catch (Exception e) {
            Logging.error(e);
        }
    }
}
