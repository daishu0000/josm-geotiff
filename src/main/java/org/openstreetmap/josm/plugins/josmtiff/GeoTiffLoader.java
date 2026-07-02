package org.openstreetmap.josm.plugins.josmtiff;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageParser;
import org.apache.commons.imaging.formats.tiff.TiffImagingParameters;
import org.apache.commons.imaging.formats.tiff.constants.GeoTiffTagConstants;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;

/**
 * Reads GeoTIFF files and converts their bounds to the current JOSM projection.
 */
public final class GeoTiffLoader {

    private static final int GEOGRAPHIC_TYPE_GEO_KEY = 2048;
    private static final int PROJECTED_CRS_TYPE_GEO_KEY = 3072;

    private GeoTiffLoader() {
    }

    /**
     * Loads a GeoTIFF file and converts its bounds to JOSM east/north coordinates.
     *
     * @param file the GeoTIFF file
     * @return the loaded data
     * @throws IOException if the file cannot be read or lacks georeferencing
     */
    public static GeoTiffData read(File file) throws IOException {
        try {
            TiffImagingParameters params = new TiffImagingParameters();
            TiffImageParser parser = new TiffImageParser();
            BufferedImage image = parser.getBufferedImage(file, params);
            if (image == null) {
                throw new IOException("Cannot decode TIFF image: " + file.getAbsolutePath());
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            TiffImageMetadata metadata;
            try (InputStream in = new ByteArrayInputStream(bytes)) {
                metadata = (TiffImageMetadata) Imaging.getMetadata(in, file.getName());
            }
            if (metadata == null || metadata.getDirectories().isEmpty()) {
                throw new IOException("Missing TIFF metadata: " + file.getAbsolutePath());
            }

            TiffImageMetadata.Directory directory = (TiffImageMetadata.Directory) metadata.getDirectories().get(0);
            GeoBounds geoBounds = readGeoBounds(directory, image.getWidth(), image.getHeight());
            EastNorth[] corners = toEastNorthCorners(geoBounds);
            EastNorth min = minCorner(corners);
            EastNorth max = maxCorner(corners);
            return new GeoTiffData(file, image, min, max);
        } catch (Exception e) {
            throw new IOException("Failed to read GeoTIFF: " + e.getMessage(), e);
        }
    }

    private static GeoBounds readGeoBounds(TiffImageMetadata.Directory directory, int width, int height)
            throws Exception {
        double[] transform = readModelTransformation(directory);
        if (transform != null) {
            return boundsFromTransformation(transform, width, height);
        }

        TiffField tiePointField = directory.findField(GeoTiffTagConstants.EXIF_TAG_MODEL_TIEPOINT_TAG);
        TiffField scaleField = directory.findField(GeoTiffTagConstants.EXIF_TAG_MODEL_PIXEL_SCALE_TAG);
        if (tiePointField == null || scaleField == null) {
            throw new IOException("GeoTIFF has no georeferencing tags (ModelTiepoint / ModelPixelScale / ModelTransformation).");
        }

        double[] tiePoints = tiePointField.getDoubleArrayValue();
        double[] scale = scaleField.getDoubleArrayValue();
        if (tiePoints.length < 6 || scale.length < 2) {
            throw new IOException("Invalid GeoTIFF georeferencing tags.");
        }

        double originX = tiePoints[3];
        double originY = tiePoints[4];
        double scaleX = scale[0];
        double scaleY = scale[1];
        double minX = Math.min(originX, originX + width * scaleX);
        double maxX = Math.max(originX, originX + width * scaleX);
        double minY = Math.min(originY, originY + height * scaleY);
        double maxY = Math.max(originY, originY + height * scaleY);

        int epsg = readEpsg(directory);
        return new GeoBounds(minX, minY, maxX, maxY, epsg);
    }

    private static GeoBounds boundsFromTransformation(double[] m, int width, int height) {
        double[] xs = {0, width, 0, width};
        double[] ys = {0, 0, height, height};
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 4; i++) {
            double x = m[0] + m[1] * xs[i] + m[2] * ys[i];
            double y = m[3] + m[4] * xs[i] + m[5] * ys[i];
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }
        return new GeoBounds(minX, minY, maxX, maxY, 0);
    }

    private static double[] readModelTransformation(TiffImageMetadata.Directory directory) throws Exception {
        TiffField field = directory.findField(GeoTiffTagConstants.EXIF_TAG_MODEL_TRANSFORMATION_TAG);
        if (field == null) {
            return null;
        }
        double[] matrix = field.getDoubleArrayValue();
        if (matrix.length < 16) {
            return null;
        }
        return matrix;
    }

    private static int readEpsg(TiffImageMetadata.Directory directory) throws Exception {
        TiffField field = directory.findField(GeoTiffTagConstants.EXIF_TAG_GEO_KEY_DIRECTORY_TAG);
        if (field == null) {
            return 4326;
        }
        Object value = field.getValue();
        if (!(value instanceof short[] geoKeys) || geoKeys.length < 4) {
            return 4326;
        }
        int numberOfKeys = geoKeys[3] & 0xFFFF;
        for (int i = 0; i < numberOfKeys; i++) {
            int base = 4 + i * 4;
            if (base + 3 >= geoKeys.length) {
                break;
            }
            int keyId = geoKeys[base] & 0xFFFF;
            int geoKeyValue = geoKeys[base + 3] & 0xFFFF;
            if (keyId == GEOGRAPHIC_TYPE_GEO_KEY || keyId == PROJECTED_CRS_TYPE_GEO_KEY) {
                return geoKeyValue;
            }
        }
        return 4326;
    }

    private static EastNorth[] toEastNorthCorners(GeoBounds bounds) throws IOException {
        Projection projection = ProjectionRegistry.getProjection();
        if (bounds.epsg == 4326 || bounds.epsg == 0) {
            return new EastNorth[] {
                    projection.latlon2eastNorth(new LatLon(bounds.minY, bounds.minX)),
                    projection.latlon2eastNorth(new LatLon(bounds.minY, bounds.maxX)),
                    projection.latlon2eastNorth(new LatLon(bounds.maxY, bounds.minX)),
                    projection.latlon2eastNorth(new LatLon(bounds.maxY, bounds.maxX))
            };
        }
        throw new IOException("Unsupported GeoTIFF CRS: EPSG:" + bounds.epsg
                + ". Currently only geographic coordinates (EPSG:4326) are supported.");
    }

    private static EastNorth minCorner(EastNorth[] corners) {
        double east = Double.POSITIVE_INFINITY;
        double north = Double.POSITIVE_INFINITY;
        for (EastNorth corner : corners) {
            east = Math.min(east, corner.east());
            north = Math.min(north, corner.north());
        }
        return new EastNorth(east, north);
    }

    private static EastNorth maxCorner(EastNorth[] corners) {
        double east = Double.NEGATIVE_INFINITY;
        double north = Double.NEGATIVE_INFINITY;
        for (EastNorth corner : corners) {
            east = Math.max(east, corner.east());
            north = Math.max(north, corner.north());
        }
        return new EastNorth(east, north);
    }

    private static final class GeoBounds {
        private final double minX;
        private final double minY;
        private final double maxX;
        private final double maxY;
        private final int epsg;

        private GeoBounds(double minX, double minY, double maxX, double maxY, int epsg) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.epsg = epsg;
        }
    }
}
