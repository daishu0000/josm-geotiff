package org.openstreetmap.josm.plugins.josmtiff;

import java.awt.image.BufferedImage;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Holds a GeoTIFF image, its geographic metadata, and projected bounds for the current JOSM projection.
 */
public final class GeoTiffData {

    private final GeoTiffGeoInfo geoInfo;
    private final BufferedImage image;
    private EastNorth min;
    private EastNorth max;

    public GeoTiffData(GeoTiffGeoInfo geoInfo, BufferedImage image) {
        this.geoInfo = geoInfo;
        this.image = image;
        reprojectBounds();
    }

    /**
     * Recalculates east/north bounds from the stored geographic bounds using the current projection.
     */
    public void reprojectBounds() {
        EastNorth[] corners = GeoTiffLoader.toEastNorthCorners(
                geoInfo.getMinLon(), geoInfo.getMinLat(), geoInfo.getMaxLon(), geoInfo.getMaxLat());
        this.min = GeoTiffLoader.minCorner(corners);
        this.max = GeoTiffLoader.maxCorner(corners);
    }

    public GeoTiffGeoInfo getGeoInfo() {
        return geoInfo;
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
