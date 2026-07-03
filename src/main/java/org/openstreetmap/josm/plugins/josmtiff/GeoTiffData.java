package org.openstreetmap.josm.plugins.josmtiff;

import java.awt.image.BufferedImage;
import java.io.File;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Holds a GeoTIFF image, its geographic bounds, and the corresponding JOSM east/north bounds.
 */
public final class GeoTiffData {

    private final File sourceFile;
    private final BufferedImage image;
    private final double minLon;
    private final double minLat;
    private final double maxLon;
    private final double maxLat;
    private EastNorth min;
    private EastNorth max;

    public GeoTiffData(File sourceFile, BufferedImage image, double minLon, double minLat, double maxLon, double maxLat) {
        this.sourceFile = sourceFile;
        this.image = image;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        reprojectBounds();
    }

    /**
     * Recalculates east/north bounds from the stored geographic bounds using the current projection.
     */
    public void reprojectBounds() {
        EastNorth[] corners = GeoTiffLoader.toEastNorthCorners(minLon, minLat, maxLon, maxLat);
        this.min = GeoTiffLoader.minCorner(corners);
        this.max = GeoTiffLoader.maxCorner(corners);
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
