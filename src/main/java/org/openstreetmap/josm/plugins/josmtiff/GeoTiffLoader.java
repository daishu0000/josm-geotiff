package org.openstreetmap.josm.plugins.josmtiff;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.GeoTiffTagConstants;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;

/**
 * Reads GeoTIFF metadata and converts bounds to the current JOSM projection.
 */
public final class GeoTiffLoader {

    private GeoTiffLoader() {
    }

    /**
     * Reads CRS information and GeoTIFF metadata in a single pass over the file.
     *
     * @param file the GeoTIFF file
     * @return inspection result and loaded data when import is possible
     * @throws IOException if the file cannot be read
     */
    public static LoadResult load(File file) throws IOException {
        try {
            TiffImageMetadata metadata = readTiffMetadata(file);
            if (metadata == null || metadata.getDirectories().isEmpty()) {
                throw new IOException("Missing TIFF metadata: " + file.getAbsolutePath());
            }

            TiffImageMetadata.Directory directory = (TiffImageMetadata.Directory) metadata.getDirectories().get(0);
            GeoTiffCrsCheck.Result crsCheck = GeoTiffCrsCheck.inspectDirectory(directory);
            if (!crsCheck.canImport()) {
                return new LoadResult(crsCheck, null);
            }
            return new LoadResult(crsCheck, buildGeoTiffData(file, directory));
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to read GeoTIFF metadata: " + e.getMessage(), e);
        }
    }

    static TiffImageMetadata readTiffMetadata(File file) throws IOException {
        try {
            return (TiffImageMetadata) Imaging.getMetadata(file);
        } catch (Exception e) {
            if (e instanceof IOException io) {
                throw io;
            }
            throw new IOException("Failed to read TIFF metadata: " + e.getMessage(), e);
        }
    }

    private static GeoTiffData buildGeoTiffData(File file, TiffImageMetadata.Directory directory) throws Exception {
        int width = readImageWidth(directory);
        int height = readImageHeight(directory);
        GeoTransform transform = readGeoTransform(directory);
        GeoBounds geoBounds = readGeoBounds(directory, width, height, transform);

        GeoTiffGeoInfo geoInfo = new GeoTiffGeoInfo(
                file, width, height,
                geoBounds.minX, geoBounds.minY, geoBounds.maxX, geoBounds.maxY,
                transform.originLon, transform.originLat, transform.scaleLon, transform.scaleLat,
                transform.affine);
        BufferedImage image = decodeImage(file);
        return new GeoTiffData(geoInfo, image);
    }

    private static BufferedImage decodeImage(File file) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(file)) {
            if (input == null) {
                throw new IOException("Failed to open TIFF file: " + file.getName());
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IOException("No TIFF ImageReader available for: " + file.getName());
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new IOException("Failed to decode TIFF image: " + file.getAbsolutePath());
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    public record LoadResult(GeoTiffCrsCheck.Result crsCheck, GeoTiffData data) {
    }

    private static int readImageWidth(TiffImageMetadata.Directory directory) throws Exception {
        TiffField field = directory.findField(TiffTagConstants.TIFF_TAG_IMAGE_WIDTH);
        if (field == null) {
            throw new IOException("Missing TIFF image width.");
        }
        return field.getIntValue();
    }

    private static int readImageHeight(TiffImageMetadata.Directory directory) throws Exception {
        TiffField field = directory.findField(TiffTagConstants.TIFF_TAG_IMAGE_LENGTH);
        if (field == null) {
            throw new IOException("Missing TIFF image height.");
        }
        return field.getIntValue();
    }

    private static GeoBounds readGeoBounds(TiffImageMetadata.Directory directory, int width, int height,
            GeoTransform transform) throws Exception {
        if (transform.affine != null) {
            GeoTiffCrsCheck.ensureSupported(GeoTiffCrsCheck.readEpsg(directory));
            return boundsFromTransformation(transform.affine, width, height);
        }

        double minX = Math.min(transform.originLon, transform.originLon + width * transform.scaleLon);
        double maxX = Math.max(transform.originLon, transform.originLon + width * transform.scaleLon);
        double minY = Math.min(transform.originLat, transform.originLat + height * transform.scaleLat);
        double maxY = Math.max(transform.originLat, transform.originLat + height * transform.scaleLat);

        int epsg = GeoTiffCrsCheck.readEpsg(directory);
        GeoTiffCrsCheck.ensureSupported(epsg);
        return new GeoBounds(minX, minY, maxX, maxY);
    }

    private static GeoTransform readGeoTransform(TiffImageMetadata.Directory directory) throws Exception {
        double[] affine = readModelTransformation(directory);
        if (affine != null) {
            return new GeoTransform(affine[0], affine[3], affine[1], affine[5], affine);
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

        // GeoTIFF spec: Y = TiepointY - (Row - TiepointJ) * ScaleY (ScaleY in tag is a positive magnitude)
        double tieI = tiePoints[0];
        double tieJ = tiePoints[1];
        double scaleX = scale[0];
        double scaleY = -Math.abs(scale[1]);
        double originLon = tiePoints[3] - tieI * scaleX;
        double originLat = tiePoints[4] - tieJ * scaleY;
        return new GeoTransform(originLon, originLat, scaleX, scaleY, null);
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
        return new GeoBounds(minX, minY, maxX, maxY);
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

    static EastNorth[] toEastNorthCorners(double minLon, double minLat, double maxLon, double maxLat) {
        Projection projection = ProjectionRegistry.getProjection();
        return new EastNorth[] {
                projection.latlon2eastNorth(new LatLon(minLat, minLon)),
                projection.latlon2eastNorth(new LatLon(minLat, maxLon)),
                projection.latlon2eastNorth(new LatLon(maxLat, minLon)),
                projection.latlon2eastNorth(new LatLon(maxLat, maxLon))
        };
    }

    static EastNorth minCorner(EastNorth[] corners) {
        double east = Double.POSITIVE_INFINITY;
        double north = Double.POSITIVE_INFINITY;
        for (EastNorth corner : corners) {
            east = Math.min(east, corner.east());
            north = Math.min(north, corner.north());
        }
        return new EastNorth(east, north);
    }

    static EastNorth maxCorner(EastNorth[] corners) {
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

        private GeoBounds(double minX, double minY, double maxX, double maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }

    private static final class GeoTransform {
        private final double originLon;
        private final double originLat;
        private final double scaleLon;
        private final double scaleLat;
        private final double[] affine;

        private GeoTransform(double originLon, double originLat, double scaleLon, double scaleLat, double[] affine) {
            this.originLon = originLon;
            this.originLat = originLat;
            this.scaleLon = scaleLon;
            this.scaleLat = scaleLat;
            this.affine = affine;
        }
    }
}
