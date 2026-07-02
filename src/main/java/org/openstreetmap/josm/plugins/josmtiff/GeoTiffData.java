package org.openstreetmap.josm.plugins.josmtiff;

import java.awt.image.BufferedImage;
import java.io.File;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Holds a reprojected GeoTIFF image and its bounds in JOSM east/north coordinates.
 */
public final class GeoTiffData {

    private final File sourceFile;
    private final BufferedImage image;
    private final EastNorth min;
    private final EastNorth max;

    public GeoTiffData(File sourceFile, BufferedImage image, EastNorth min, EastNorth max) {
        this.sourceFile = sourceFile;
        this.image = image;
        this.min = min;
        this.max = max;
    }

    public File getSourceFile() {
        return sourceFile;
    }

    public BufferedImage getImage() {
        return image;
    }

    public EastNorth getMin() {
        return min;
    }

    public EastNorth getMax() {
        return max;
    }
}
