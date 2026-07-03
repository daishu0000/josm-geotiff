package org.openstreetmap.josm.plugins.josmtiff;

import java.io.File;

/**
 * Geographic metadata and pixel-to-coordinate mapping for a GeoTIFF file.
 */
public final class GeoTiffGeoInfo {

    private final File file;
    private final int width;
    private final int height;
    private final double minLon;
    private final double minLat;
    private final double maxLon;
    private final double maxLat;
    private final double originLon;
    private final double originLat;
    private final double scaleLon;
    private final double scaleLat;
    private final double[] affine;

    public GeoTiffGeoInfo(File file, int width, int height, double minLon, double minLat, double maxLon, double maxLat,
            double originLon, double originLat, double scaleLon, double scaleLat, double[] affine) {
        this.file = file;
        this.width = width;
        this.height = height;
        this.minLon = minLon;
        this.minLat = minLat;
        this.maxLon = maxLon;
        this.maxLat = maxLat;
        this.originLon = originLon;
        this.originLat = originLat;
        this.scaleLon = scaleLon;
        this.scaleLat = scaleLat;
        this.affine = affine;
    }

    public File getFile() {
        return file;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public double getMinLon() {
        return minLon;
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public double getMaxLat() {
        return maxLat;
    }

    /**
     * Computes the source-pixel rectangle for a geographic bounds, clamped to the image.
     */
    public PixelRect geoBoundsToPixelRect(double geoMinLon, double geoMinLat, double geoMaxLon, double geoMaxLat) {
        double[][] corners = {
                {geoMinLon, geoMinLat},
                {geoMaxLon, geoMinLat},
                {geoMinLon, geoMaxLat},
                {geoMaxLon, geoMaxLat}
        };

        double minPx = Double.POSITIVE_INFINITY;
        double minPy = Double.POSITIVE_INFINITY;
        double maxPx = Double.NEGATIVE_INFINITY;
        double maxPy = Double.NEGATIVE_INFINITY;
        for (double[] corner : corners) {
            double[] pixel = latLonToPixel(corner[0], corner[1]);
            minPx = Math.min(minPx, pixel[0]);
            minPy = Math.min(minPy, pixel[1]);
            maxPx = Math.max(maxPx, pixel[0]);
            maxPy = Math.max(maxPy, pixel[1]);
        }

        int x = clamp((int) Math.floor(minPx), 0, width - 1);
        int y = clamp((int) Math.floor(minPy), 0, height - 1);
        int x2 = clamp((int) Math.ceil(maxPx), x + 1, width);
        int y2 = clamp((int) Math.ceil(maxPy), y + 1, height);
        return new PixelRect(x, y, x2 - x, y2 - y);
    }

    private double[] latLonToPixel(double lon, double lat) {
        if (affine != null) {
            double dx = lon - affine[0];
            double dy = lat - affine[3];
            double det = affine[1] * affine[5] - affine[2] * affine[4];
            if (Math.abs(det) < 1e-12) {
                return new double[] {0, 0};
            }
            double px = (dx * affine[5] - dy * affine[2]) / det;
            double py = (dy * affine[1] - dx * affine[4]) / det;
            return new double[] {px, py};
        }
        return new double[] {
                (lon - originLon) / scaleLon,
                (lat - originLat) / scaleLat
        };
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Source-pixel rectangle within the GeoTIFF.
     */
    public static final class PixelRect {
        public final int x;
        public final int y;
        public final int width;
        public final int height;

        public PixelRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean isEmpty() {
            return width <= 0 || height <= 0;
        }
    }
}
