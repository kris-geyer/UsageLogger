package geyer.sensorlab.usagelogger;

public final class ResearcherInput {

    /**
     * basic initialization direction
     */

    //is the app required to informed the user?
    Boolean InformUserRequired = true,

    /**
     * A password is required for the encryption of data and therefore the program will not function without a password.
     * Only edit this option if an alternative to the user providing the password is supplied.
     * Please anticipate extensive problems with the functionality of the app if this option is not set to true and
     * careful accounting of the impact across all aspects of the app.
     */

    //is the app required to prompt the user to provide password?
            PasswordRequired = true,


    /**
     * direction of temporal focus for behavioural logging
     */
    //is retrospective logging employed?
            RetrospectiveLoggingEmployed = true,
    //is cross sectional logging employed?
            PerformCrossSectionalAnalysis = true,
    //is prospective logging employed?
            ProspectiveLoggingEmployed = true,
    /**
     * direction for the retrospective logging
     */
    //are usage statistics employed?
            UseUsageStatics = true,

    //are usage events employed?
            UseUsageEvents = true;
    /*
      direction for usage statistics
     */

    /**
     * note the duration of the period that the researcher intends to sample determines the bin.
     * For the past 7 days then the duration bins will come in days
     * For the past 4 weeks the durations will come in weeks
     * For the past 6 months the duration will come in months
     * For the past 2 years the duration will come in years
     *
     * The data logging will be additive, therefore the duration of the seven days will be captured in days, weeks, months and years
     * if annual days are requested
     *
     * The request will be rounded to the nearest suitable response.
     * For example if the user requests 364 days are monitored the program will increase this to 356.
     */
    int NumberOfDaysForUsageStats = 365;
    //directions for the usage events

    /**
     * Max appears to be about 7 days. Program will return as many as is possible.
     */
    int NumberOfDaysForUsageEvents = 7,
    /**direction for cross sectional logging
     *
     *
     */
        /*
        what level of the sophistication will the app engage with?
        0 - nothing
        1 - document apps
        2 - document apps and permissions
        3 - document apps and permissions and user's response to permissions
        (performCrossSectionalAnalysis required to be true for function)
        */
        LevelOfCrossSectionalAnalysis = 3,
    /*
     * direction for prospective logging
     */

    /*
        What is the level of the prospective data required?
        1 - basic screen usage
        2 - foreground logging
        3 - notification listening
        4 - foreground and notification listening
         */

    /**
     * If the level is 2 or 4 then the app requires the usage statistics permission, 3 or above requires notification listening permission.
     */
        LevelOfProspectiveLogging = 4;

}
