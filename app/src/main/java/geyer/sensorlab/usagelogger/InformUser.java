package geyer.sensorlab.usagelogger;

import java.util.EmptyStackException;

public class InformUser extends DirectAppInitialization {

    ResearcherInput researcherInput = new ResearcherInput();
    InformUser(MainActivity MainActivityContext) {
        super(MainActivityContext);
    }

    public StringBuilder constructMessageOne(){


        StringBuilder toRelay = new StringBuilder();
        toRelay.append("This app is intended to help scientists better understand how you use your smartphone" +"\n").
                append("\n").append("This app functions by ");
        if(researcherInput.RetrospectiveLoggingEmployed){
            toRelay.append("gathering data about how you previously have used your phone");
            if(researcherInput.PerformCrossSectionalAnalysis){
                toRelay.append(", getting a snapshot view of how you are using your phone");
            }
            if(researcherInput.ProspectiveLoggingEmployed){
                toRelay.append(" and logging how you will use your phone in the future.");
            }else{
                toRelay.append(".");
            }
        }
        return toRelay;

    }

    public StringBuilder constructMessageTwo() throws ErrorReference {

        StringBuilder toRelay = new StringBuilder();


        toRelay.append("\n").append("\n")
                .append("This app will gather a variety of data, it will gather:")
                .append("\n");

        if(researcherInput.ProspectiveLoggingEmployed){
            if(researcherInput.UseUsageEvents){
                toRelay.append("- a highly detailed account of how you have used your phone over the last ").append(researcherInput.NumberOfDaysForUsageEvents).append(" days.").append("\n");
            }
            if(researcherInput.UseUsageStatics){
                toRelay.append("- how much you've used every un/installed app for the past ").append(researcherInput.NumberOfDaysForUsageStats).append(" days").append("\n");
            }
        }

        if(researcherInput.PerformCrossSectionalAnalysis){
            switch (researcherInput.LevelOfCrossSectionalAnalysis){
                case 1: toRelay.append("- what apps you have installed on your phone");
                    break;
                case 2: toRelay.append("- what apps you have installed on your phone and what permissions are requested");
                    break;
                case 3: toRelay.append("- what apps you have installed on your phone, what permission are requested and if you have approved the permissions");
                    break;
                default:
                    toRelay.append("- an error occurred please inform the researcher");
                    /**
                     * Work with the error exception
                     */
                    throw new ErrorReference("an error occured please inform the researcher");
            }
        }

        if(researcherInput.ProspectiveLoggingEmployed){
            toRelay
                    .append("- when the screen is on and off")
                    .append("\n")
                    .append("- when the phone is off and on")
                    .append("\n");
            switch (researcherInput.LevelOfProspectiveLogging){
                case 1:
                    break;
                case 2:
                    toRelay.append("- what apps you use")
                            .append("\n");
                    break;
                case 3:
                    toRelay
                            .append("- which apps send you notification and when you delete them")
                            .append("\n");
                    break;
                case 4:
                    toRelay.append("- what apps you use")
                            .append("\n")
                            .append("- which apps send you notification and when you delete them")
                            .append("\n");
                    break;
            }
        }
            return toRelay;
    }

    public StringBuilder constructMessageThree(){
        StringBuilder toRelay = new StringBuilder();
        toRelay.append("The phone has adopted a number of security measures to protect your privacy and data.").append("\n").append("\n");
        if(researcherInput.PasswordRequired){
            toRelay.append("Your data will be stored on at least a password protected 128-bit encryption. You will be tasked with constructing a password to assist with the security. More on this later");
        }
        if(detectPermissionsState() < 6){
            toRelay.append(" you will also be asked to provide permissions for the application to access specific data subsequently.");
        }
        toRelay.append(" A final note is that all data will be stored locally on the phone and the researcher or any other party will not receive any data until you email it. Please click on privacy policy in order to review this");
        return toRelay;
    }
}
