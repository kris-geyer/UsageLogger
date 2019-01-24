package geyer.sensorlab.usagelogger;

public class DeterminePointOfDataExtraction {
    /**
     * Please identify the at what point the extraction for each involved aspect of data.
     * value 1 means that the data will be extracted at initialization of application.
     * value 2 means that the data will be extracted at export of files.
     *
     * !!! Note if you want to employ prospective logging then cross sectional logging must be set to 1. Program won't function otherwise
     */

    public static final int pointOfRetrospectiveLogging = 2,
        pointOfCrossSectionalLogging = 1;

}
