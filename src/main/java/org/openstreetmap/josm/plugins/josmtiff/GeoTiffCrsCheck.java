package org.openstreetmap.josm.plugins.josmtiff;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.GeoTiffTagConstants;

/**
 * Inspects GeoTIFF coordinate reference systems before import.
 */
public final class GeoTiffCrsCheck {

    /** Geographic WGS84 – currently the only supported CRS for import. */
    public static final int SUPPORTED_EPSG = 4326;

    private static final int GEOGRAPHIC_TYPE_GEO_KEY = 2048;
    private static final int PROJECTED_CRS_TYPE_GEO_KEY = 3072;

    private GeoTiffCrsCheck() {
    }

    /**
     * Reads CRS and georeferencing information from a GeoTIFF without decoding image pixels.
     *
     * @param file the GeoTIFF file
     * @return inspection result with a user-facing message when import is not possible
     */
    public static Result inspect(File file) {
        try {
            TiffImageMetadata metadata = GeoTiffLoader.readTiffMetadata(file);
            if (metadata == null || metadata.getDirectories().isEmpty()) {
                return Result.invalid(tr("Missing TIFF metadata."));
            }
            return inspectDirectory((TiffImageMetadata.Directory) metadata.getDirectories().get(0));
        } catch (IOException e) {
            return Result.invalid(e.getMessage());
        } catch (Exception e) {
            return Result.invalid(e.getMessage());
        }
    }

    /**
     * Inspects CRS and georeferencing from an already-parsed TIFF directory.
     */
    static Result inspectDirectory(TiffImageMetadata.Directory directory) throws Exception {
        if (!hasGeoreferencing(directory)) {
            return Result.missingGeoreference();
        }

        int epsg = readEpsg(directory);
        if (epsg != SUPPORTED_EPSG) {
            return Result.unsupported(epsg);
        }
        return Result.supported(epsg);
    }

    /**
     * @throws IOException if the CRS is not supported
     */
    static void ensureSupported(int epsg) throws IOException {
        if (epsg != SUPPORTED_EPSG) {
            throw new IOException(Result.unsupported(epsg).getMessage());
        }
    }

    static int readEpsg(TiffImageMetadata.Directory directory) throws Exception {
        TiffField field = directory.findField(GeoTiffTagConstants.EXIF_TAG_GEO_KEY_DIRECTORY_TAG);
        if (field == null) {
            return SUPPORTED_EPSG;
        }
        Object value = field.getValue();
        if (!(value instanceof short[] geoKeys) || geoKeys.length < 4) {
            return SUPPORTED_EPSG;
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
        return SUPPORTED_EPSG;
    }

    private static boolean hasGeoreferencing(TiffImageMetadata.Directory directory) throws Exception {
        if (directory.findField(GeoTiffTagConstants.EXIF_TAG_MODEL_TRANSFORMATION_TAG) != null) {
            return true;
        }
        return directory.findField(GeoTiffTagConstants.EXIF_TAG_MODEL_TIEPOINT_TAG) != null
                && directory.findField(GeoTiffTagConstants.EXIF_TAG_MODEL_PIXEL_SCALE_TAG) != null;
    }

    public enum Status {
        SUPPORTED,
        UNSUPPORTED_CRS,
        MISSING_GEOREFERENCE,
        INVALID_FILE
    }

    public static final class Result {
        private final Status status;
        private final int epsg;
        private final String message;

        private Result(Status status, int epsg, String message) {
            this.status = status;
            this.epsg = epsg;
            this.message = message;
        }

        static Result supported(int epsg) {
            return new Result(Status.SUPPORTED, epsg, null);
        }

        static Result unsupported(int epsg) {
            return new Result(Status.UNSUPPORTED_CRS, epsg, tr(
                    "This GeoTIFF uses EPSG:{0}, which is not supported.\n\n"
                            + "This plugin currently only supports geographic coordinates (EPSG:4326).\n\n"
                            + "Reproject the file before importing, for example:\n"
                            + "gdalwarp -t_srs EPSG:4326 \"{1}\" output_4326.tif",
                    epsg, "input.tif"));
        }

        static Result missingGeoreference() {
            return new Result(Status.MISSING_GEOREFERENCE, 0, tr(
                    "This file is not a georeferenced GeoTIFF.\n\n"
                            + "Missing georeferencing tags (ModelTiepoint / ModelPixelScale / ModelTransformation)."));
        }

        static Result invalid(String details) {
            return new Result(Status.INVALID_FILE, 0, tr("Failed to read GeoTIFF file:\n{0}", details));
        }

        public boolean canImport() {
            return status == Status.SUPPORTED;
        }

        public Status getStatus() {
            return status;
        }

        public int getEpsg() {
            return epsg;
        }

        public String getMessage() {
            return message;
        }

        public String getDialogTitle() {
            return switch (status) {
                case UNSUPPORTED_CRS -> tr("Unsupported coordinate system");
                case MISSING_GEOREFERENCE -> tr("Not a georeferenced GeoTIFF");
                case INVALID_FILE -> tr("Cannot read GeoTIFF");
                default -> tr("Import GeoTIFF");
            };
        }
    }
}
