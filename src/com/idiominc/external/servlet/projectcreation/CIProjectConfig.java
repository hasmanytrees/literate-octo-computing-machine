package com.idiominc.external.servlet.projectcreation;

import java.util.HashMap;

/**
 * This is essentially a configuration file for Compassion project types, each combination of:
 *
 *   Project Type
 *   Processing Type
 *   Direction
 *   Global Partner Opt In/Out
 *
 * Make up a possible configuration which is written into a static HashMap.  This map is then
 * used to assign values to the project for processing.  This map is used by the CIProject.processProject()
 * method.
 *
 * Created by cslack on 3/21/2016.
 */
public class CIProjectConfig {

    public final static String _PROCESS_ISL = "ISL";
    public final static String _PROCESS_TRANSLATION = "Translation";
    public final static String _PROCESS_TRANSCRIPTION = "Transcription";

    public final static String _B2S = "Beneficiary To Supporter";
    public final static String _S2B = "Supporter To Beneficiary";
    public final static String _3PL = "Third Party Letter";

    public final static String _PROJECT_SAME_LANGUAGE_PAIR = "Same Language Pair";
    public final static String _PROJECT_ENGLISH_AS_TARGET = "English As Target";
    public final static String _PROJECT_ENGLISH_AS_SOURCE = "English As Source";
    public final static String _PROJECT_DIFFERENT_LANGUAGE_PAIR = "Different Language Pair";

    public final static String _GP_OPT_IN = "true";
    public final static String _GP_OPT_OUT = "false";

    private static HashMap<String, CIProjectInfo> projectConfig = new HashMap<>();


    /**
     * The different possible exceptions are broken up logically just for readibility reasons
     */
    static {
        sameLanguagePair();
        englishAsSource();
        englishAsTarget();
        differentLanguagePair();
    }


    /**
     * Same Language Pair
     */
    private static void sameLanguagePair() {

        /**************************************************************************************
         * Project Type: Same Language Pair
         * Process: Translation
         * Direction: B2S
         * GP: Opt In/Out
         *
         * Description: a. FO Translator only does the Content Check passes it
         *                 forward or reject (for uncorrectable errors).
         *                 Correctable errors cannot be corrected by the Translator
         *              b. No Composition required
         *              c. GP receives the Original Letter Image
         *
         **************************************************************************************/
        CIProjectInfo SL_Translation_B2S = new CIProjectInfo();

        SL_Translation_B2S.setSecondStepProjectRequired(false);
        SL_Translation_B2S.setQcNotRequiredOverride(true);
        SL_Translation_B2S.setReturnTextRequirements("false|true|true");
        SL_Translation_B2S.setTargetWorkflowName("Compassion Translation and Review Workflow-SameLang");
        SL_Translation_B2S.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);

        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _B2S, _GP_OPT_OUT),
                SL_Translation_B2S);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _B2S, _GP_OPT_IN),
                SL_Translation_B2S);


        /**************************************************************************************
         * Project Type: Same Language Pair
         * Process: ISL/Transcription
         * Direction: B2S
         * GP: Opt In/Out
         **************************************************************************************/
        CIProjectInfo SL_ISL_Trasnc_B2S = new CIProjectInfo();

        SL_ISL_Trasnc_B2S.setException("Same language pair. Direction is " + _B2S
                + ". Requested process is not Translation which is not supported");

        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_ISL, _B2S, _GP_OPT_IN),
                SL_ISL_Trasnc_B2S);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_IN),
                SL_ISL_Trasnc_B2S);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_ISL, _B2S, _GP_OPT_OUT),
                SL_ISL_Trasnc_B2S);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_OUT),
                SL_ISL_Trasnc_B2S);


        /**************************************************************************************
         * Project Type: Same Language Pair
         * Process: ISL
         * Direction: S2B
         * GP: Opt In/Out
         * Description:  Create one-step project for FO for content-check only:
         *                  a. GP send the Original Letter without Content Check
         *                  b. FO receives just the letter image and does the Content Check and pass it forward or
         *                     reject (for uncorrectable errors). Correctable errors cannot be corrected by the
         *                     Translator
         *                  c. No Composition required
         *
         **************************************************************************************/
        CIProjectInfo SL_ISL_S2B = new CIProjectInfo();

        SL_ISL_S2B.setSecondStepProjectRequired(false);
        SL_ISL_S2B.setQcNotRequiredOverride(true);
        SL_ISL_S2B.setReturnTextRequirements("true|false|false");
        SL_ISL_S2B.setTargetWorkflowName("Compassion Translation and Review Workflow-SameLang");
        SL_ISL_S2B.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);
        SL_ISL_S2B.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);

        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_ISL, _S2B, _GP_OPT_IN),
                SL_ISL_S2B);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_ISL, _S2B, _GP_OPT_OUT),
                SL_ISL_S2B);


        /**************************************************************************************
         * Project Type: Same Language Pair
         * Process: ISL
         * Direction: S2B
         * GP: Opt In/Out
         **************************************************************************************/
        CIProjectInfo SL_nonISL_S2B = new CIProjectInfo();

        SL_nonISL_S2B.setSecondStepProjectRequired(false);
        SL_nonISL_S2B.setQcNotRequiredOverride(true);
        SL_nonISL_S2B.setReturnTextRequirements("false|true|true");
        SL_nonISL_S2B.setTargetWorkflowName("Compassion Translation and Review Workflow-SameLang");
        SL_nonISL_S2B.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);
        SL_nonISL_S2B.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);

        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_IN),
                SL_nonISL_S2B);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_OUT),
                SL_nonISL_S2B);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _S2B, _GP_OPT_IN),
                SL_nonISL_S2B);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _S2B, _GP_OPT_OUT),
                SL_nonISL_S2B);


        /**************************************************************************************
         * Project Type: Same Language Pair
         * Process: ISL
         * Direction: Third Party Letter
         * GP: Opt In/Out
         *
         **************************************************************************************/
        CIProjectInfo SL_ISL_3PL = new CIProjectInfo();

        SL_ISL_3PL.setSecondStepProjectRequired(false);
        SL_ISL_3PL.setQcNotRequiredOverride(true);
        SL_ISL_3PL.setReturnTextRequirements("true|false|true");
        SL_ISL_3PL.setTargetWorkflowName("Compassion 3rd Party Letter Workflow");
        SL_ISL_3PL.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);
        SL_ISL_3PL.setDirectSourceLocaleName(CIProjectInfo.CIDirectSource._DIRECT_SOURCE_ORIGINAL_SRC);

        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_ISL, _3PL, _GP_OPT_OUT),
                SL_ISL_3PL);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_ISL, _3PL, _GP_OPT_IN),
                SL_ISL_3PL);


        /**************************************************************************************
         * Project Type: Same Language Pair
         * Process: Translation/Transcription
         * Direction: Third Party Letters
         * GP: Opt In/Out
         * Description: Third party letters are always electronic so the two step process does
         *              not ever apply.
         **************************************************************************************/
        CIProjectInfo sameLang_Translation_Transcription_3PL = new CIProjectInfo();

        sameLang_Translation_Transcription_3PL.setException("Two step process. Direction is " + _3PL
                + ". This is not supported");

        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_IN),
                sameLang_Translation_Transcription_3PL);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _3PL, _GP_OPT_IN),
                sameLang_Translation_Transcription_3PL);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_OUT),
                sameLang_Translation_Transcription_3PL);
        projectConfig.put(mapIndex(_PROJECT_SAME_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _3PL, _GP_OPT_OUT),
                sameLang_Translation_Transcription_3PL);
    }


    /**
     * When the original language is English
     */
    private static void englishAsSource() {

        /**************************************************************************************
         * Project Type: English as Source
         * Process: Transcription/ISL
         * Direction: B2S
         * GP: Opt In
         * Description:
         *          a. FO Translator only does the Content Check pass it forward or reject (for uncorrectable errors).
         *             Correctable errors cannot be corrected by the Translator
         *          b. GP Translator does the English to Final Language translation using the Manual Translation Process
         *          c. XMPie composes the letter with the final translation
         *          d. GP receives the Composed Letter and Final language blob
         *
         **************************************************************************************/
        CIProjectInfo engSource_Transl_ISL_B2S_OptIn = new CIProjectInfo();

        engSource_Transl_ISL_B2S_OptIn.setSecondStepProjectRequired(true);
        engSource_Transl_ISL_B2S_OptIn.setQcNotRequiredOverride(true);
        engSource_Transl_ISL_B2S_OptIn.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2MTP");
        engSource_Transl_ISL_B2S_OptIn.setReturnTextRequirements("false|false|true");
        engSource_Transl_ISL_B2S_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);


        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSLATION, _B2S, _GP_OPT_IN),
                engSource_Transl_ISL_B2S_OptIn);


        /**************************************************************************************
         * Project Type: English as Source
         * Process: Transcription/ISL
         * Direction: B2S
         * GP: Opt Out
         * Description:
         *          a. FO Translator only does the Content Check pass it forward or reject (for uncorrectable errors).
         *             Correctable errors cannot be corrected by the Translator
         *          b. GP receives just the Original letter image through the Handshake
         *          c. GP Translator does the English to Final Language translation using the English Letter Image
         *             through THEIR (non-SDL) Translation tool
         *
         **************************************************************************************/
        CIProjectInfo engSource_Transl_ISL_B2S_OptOut = new CIProjectInfo();

        engSource_Transl_ISL_B2S_OptOut.setSecondStepProjectRequired(false);
        engSource_Transl_ISL_B2S_OptOut.setQcNotRequiredOverride(true);
        engSource_Transl_ISL_B2S_OptOut.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2MTP");
        engSource_Transl_ISL_B2S_OptOut.setReturnTextRequirements("false|false|false");
        engSource_Transl_ISL_B2S_OptOut.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);


        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSLATION, _B2S, _GP_OPT_OUT),
                engSource_Transl_ISL_B2S_OptOut);


        /**************************************************************************************
         * Project Type: English as Source
         * Process: Translation
         * Direction: B2S
         * GP: Opt In/Out
         **************************************************************************************/
        CIProjectInfo engSource_Transcription_ISL_B2S = new CIProjectInfo();

        engSource_Transcription_ISL_B2S.setException("English Original, B2S. Expected process to be "
                + _PROCESS_TRANSLATION + " but it is not.");

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_IN),
                engSource_Transcription_ISL_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_OUT),
                engSource_Transcription_ISL_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_ISL, _B2S, _GP_OPT_IN),
                engSource_Transcription_ISL_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_ISL, _B2S, _GP_OPT_OUT),
                engSource_Transcription_ISL_B2S);


        /**************************************************************************************
         * Project Type: English as Source
         * Process: Transcription/Translation
         * Direction: S2B
         * GP: Opt In/Out
         * Description:
         *          a. FO receives the letter image
         *          b. FO Translator will do the English to FO Language using the
         *             SDL Manual Translation Process and also do the Content Check
         *          c. XMPie composes the letter with the final translation
         *
         **************************************************************************************/
        CIProjectInfo engSource_Translation_Transcription_S2B = new CIProjectInfo();

        engSource_Translation_Transcription_S2B.setSecondStepProjectRequired(false);
        engSource_Translation_Transcription_S2B.setQcNotRequiredOverride(false);
        engSource_Translation_Transcription_S2B.setReturnTextRequirements("false|false|true");
        engSource_Translation_Transcription_S2B.setTargetWorkflowName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2MTP");
        engSource_Translation_Transcription_S2B.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);
        engSource_Translation_Transcription_S2B.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);
        engSource_Translation_Transcription_S2B.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_TRANSLATION_TARGET_DIRECT);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSLATION, _S2B, _GP_OPT_OUT),
                engSource_Translation_Transcription_S2B);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSLATION, _S2B, _GP_OPT_IN),
                engSource_Translation_Transcription_S2B);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_OUT),
                engSource_Translation_Transcription_S2B);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_IN),
                engSource_Translation_Transcription_S2B);


        /**************************************************************************************
         * Project Type: English as Source
         * Process: ISL
         * Direction: S2B
         * GP: Opt In/Out
         * Description:
         *          a. GP sends the ISL letter Images and the text blob
         *          b. FO receives Text blob does the Content Check and English to FO Language
         *             translation using CAT
         *          c. XMPie composes the letter with the final translation
         *
         **************************************************************************************/
        CIProjectInfo engSource_ISL_S2B = new CIProjectInfo();

        engSource_ISL_S2B.setSecondStepProjectRequired(false);
        engSource_ISL_S2B.setQcNotRequiredOverride(false);
        engSource_ISL_S2B.setReturnTextRequirements("true|true|true");
        engSource_ISL_S2B.setTargetWorkflowName("Compassion ISL Translation and Review Workflow-TwoStep - Step 2");
        engSource_ISL_S2B.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);
        engSource_ISL_S2B.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);
        engSource_ISL_S2B.setDirectSourceLocaleName(CIProjectInfo.CIDirectSource._DIRECT_SOURCE_ORIGINAL_SRC);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_ISL, _S2B, _GP_OPT_OUT),engSource_ISL_S2B);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_ISL, _S2B, _GP_OPT_IN),engSource_ISL_S2B);


        /**************************************************************************************
         * Project Type: English as Source
         * Process: ISL
         * Direction: 3PL
         * GP: Opt In/Out
         * Description:
         *          a. GP requests the 3rd party letter
         *          b. XMPie composes the letter with the final translation
         *
         **************************************************************************************/
        CIProjectInfo engSource_ISL_3PL = new CIProjectInfo();

        engSource_ISL_3PL.setSecondStepProjectRequired(false);
        engSource_ISL_3PL.setQcNotRequiredOverride(false);
        engSource_ISL_3PL.setReturnTextRequirements("true|false|true");
        engSource_ISL_3PL.setTargetWorkflowName("Compassion 3rd Party Letter Workflow");
        engSource_ISL_3PL.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);
        engSource_ISL_3PL.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_GLOBAL_PARTNER);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_ISL, _3PL, _GP_OPT_OUT),engSource_ISL_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_ISL, _3PL, _GP_OPT_IN),engSource_ISL_3PL);


        /**************************************************************************************
         * Project Type: English original
         * Process: All
         * Direction: Third Party Letters
         * GP: Opt In/Out
         * Description: Third party letters are always electronic so the two step process does
         *              not ever apply which means there should never been an intermediate step
         *              so English as original does not matter
         **************************************************************************************/
        CIProjectInfo engSource_TranslationTranscription_3PL = new CIProjectInfo();

        engSource_TranslationTranscription_3PL.setException("English as Source exception. Direction is " + _3PL
                + ". This is not supported");

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_IN),
                engSource_TranslationTranscription_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSLATION, _3PL, _GP_OPT_IN),
                engSource_TranslationTranscription_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_OUT),
                engSource_TranslationTranscription_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_SOURCE, _PROCESS_TRANSLATION, _3PL, _GP_OPT_OUT),
                engSource_TranslationTranscription_3PL);
    }


    /**
     * When the target language is English
     */
    private static void englishAsTarget() {

        /**************************************************************************************
         * Project Type: English as Target
         * Process: Transcription/ISL
         * Direction: B2S
         * GP: Opt In/Out
         * Description:
         *          a. FO does the Content Check and FO Language to English translation using the Manual Translation
         *             Process
         *          b. XMPie composes the letter with the final translation
         *          c. GPA to receive the Original letter image, Composed letter and the English text blob
         *
         **************************************************************************************/
        CIProjectInfo engTarget_Translation_B2S = new CIProjectInfo();

        engTarget_Translation_B2S.setSecondStepProjectRequired(false);
        engTarget_Translation_B2S.setQcNotRequiredOverride(false);
        engTarget_Translation_B2S.setReturnTextRequirements("false|true|true");
        engTarget_Translation_B2S.setTargetWorkflowName("Compassion Translation and Review Workflow-OneStep-MTP");
        engTarget_Translation_B2S.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        engTarget_Translation_B2S.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSLATION, _B2S, _GP_OPT_OUT),
                engTarget_Translation_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSLATION, _B2S, _GP_OPT_IN),
                engTarget_Translation_B2S);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: Translation
         * Direction: B2S
         * GP: Opt In/Out
         **************************************************************************************/
        CIProjectInfo engTarget_Transcription_ISL_B2S = new CIProjectInfo();

        engTarget_Transcription_ISL_B2S.setException("English as Target - Type 2, B2S. Expected process to be "
                + _PROCESS_TRANSLATION + " but it is not.");

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_IN),
                engTarget_Transcription_ISL_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_OUT),
                engTarget_Transcription_ISL_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_ISL, _B2S, _GP_OPT_IN),
                engTarget_Transcription_ISL_B2S);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_ISL, _B2S, _GP_OPT_OUT),
                engTarget_Transcription_ISL_B2S);

        /**************************************************************************************
         * Project Type: English as Target
         * Process: Translation
         * Direction: S2B
         * GP: Opt In
         * Description:
         *          a. GP Translator will do the GP Language to English using SDL Manual Translation Process
         *          b. FO receives the Original Letter Image and English text
         *          c. FO does the Content Check
         *          d. XMPie composes the letter with the English translation
         *
         **************************************************************************************/
        CIProjectInfo engTarget_Translation_S2B_OptIn = new CIProjectInfo();

        engTarget_Translation_S2B_OptIn.setSecondStepProjectRequired(true) ;
        engTarget_Translation_S2B_OptIn.setQcNotRequiredOverride(false);
        engTarget_Translation_S2B_OptIn.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_Translation_S2B_OptIn.setReturnTextRequirements("false|true|true");
        engTarget_Translation_S2B_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        engTarget_Translation_S2B_OptIn.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSLATION, _S2B, _GP_OPT_IN),
                engTarget_Translation_S2B_OptIn);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: Transcription/Translation
         * Direction: S2B
         * GP: Opt Out
         * Description:
         *          a. GP Translator will do the GP Language to English
         *          b. FO receives the Original Letter Image and English text
         *          c. FO does the Content Check
         *          d. XMPie composes the letter with the English translation
         *          **************ISL Letters************
         *          a. GP Translator will do the GP Language to English
         *          b. GP sends the ISL letter Images and the text blob
         *          c. FO receives Text blob does the Content Check
         *          d. XMPie composes the letter with the final translation
         *
         *          NB: From FO prospective, there will be no different process,
         *          so same code for ISL or none-ISL below
         *
         **************************************************************************************/
        CIProjectInfo engTarget_Translation_Transcription_S2B_OptOut = new CIProjectInfo();

        engTarget_Translation_Transcription_S2B_OptOut.setSecondStepProjectRequired(false);
        engTarget_Translation_Transcription_S2B_OptOut.setQcNotRequiredOverride(true);
        engTarget_Translation_Transcription_S2B_OptOut.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_Translation_Transcription_S2B_OptOut.setReturnTextRequirements("false|true|true");
        engTarget_Translation_Transcription_S2B_OptOut.setTargetWorkflowName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_Translation_Transcription_S2B_OptOut.setTargetLocaleType(
                CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        engTarget_Translation_Transcription_S2B_OptOut.setWorkgroupLocation(
                CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);
        engTarget_Translation_Transcription_S2B_OptOut.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSLATION, _S2B, _GP_OPT_OUT),
                engTarget_Translation_Transcription_S2B_OptOut);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_OUT),
                engTarget_Translation_Transcription_S2B_OptOut);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: Transcription
         * Direction: S2B
         * GP: Opt In
         * Description:
         *          a. GP Translator will do the GP Language to English using the SDL CAT
         *          b. GP sends the ISL letter Images and the text blob
         *          c. FO receives Text blob, does the Content Check
         *          d. XMPie composes the letter with the final translation
         *
         **************************************************************************************/
        CIProjectInfo engTarget_Transcription_S2B_OptIn = new CIProjectInfo();

        engTarget_Transcription_S2B_OptIn.setSecondStepProjectRequired(true);
        engTarget_Transcription_S2B_OptIn.setQcNotRequiredOverride(false);
        engTarget_Transcription_S2B_OptIn.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_Transcription_S2B_OptIn.setReturnTextRequirements("false|true|true");
        engTarget_Transcription_S2B_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_IN),
                engTarget_Transcription_S2B_OptIn);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: ISL
         * Direction: S2B
         * GP: Opt In
         **************************************************************************************/
        CIProjectInfo engTarget_ISL_S2B_OptIn = new CIProjectInfo();

        engTarget_ISL_S2B_OptIn.setSecondStepProjectRequired(true);
        engTarget_ISL_S2B_OptIn.setQcNotRequiredOverride(false);
        engTarget_ISL_S2B_OptIn.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_ISL_S2B_OptIn.setReturnTextRequirements("true|true|true");
        engTarget_ISL_S2B_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);


        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_ISL, _S2B, _GP_OPT_IN),
                engTarget_ISL_S2B_OptIn);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: ISL
         * Direction: S2B
         * GP: Opt Out
         **************************************************************************************/
        CIProjectInfo engTarget_ISL_S2B_OptOut = new CIProjectInfo();

        engTarget_ISL_S2B_OptOut.setSecondStepProjectRequired(false);
        engTarget_ISL_S2B_OptOut.setQcNotRequiredOverride(true);
        engTarget_ISL_S2B_OptOut.setWorkflowOverrideName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_ISL_S2B_OptOut.setReturnTextRequirements("true|true|true");
        engTarget_ISL_S2B_OptOut.setTargetWorkflowName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2Type2S2B");
        engTarget_ISL_S2B_OptOut.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        engTarget_ISL_S2B_OptOut.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);
        engTarget_ISL_S2B_OptOut.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_ISL, _S2B, _GP_OPT_OUT),
                engTarget_ISL_S2B_OptOut);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: ISL
         * Direction: Third Party Letter
         * GP: Opt In
         **************************************************************************************/
        CIProjectInfo engTarget_ISL_3PL_OptIn = new CIProjectInfo();

        engTarget_ISL_3PL_OptIn.setSecondStepProjectRequired(false);
        engTarget_ISL_3PL_OptIn.setQcNotRequiredOverride(false);
        engTarget_ISL_3PL_OptIn.setReturnTextRequirements("true|false|true");
        engTarget_ISL_3PL_OptIn.setTargetWorkflowName("Compassion 3rd Party Letter Workflow");
        engTarget_ISL_3PL_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_TARGET);

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_ISL, _3PL, _GP_OPT_IN),
                engTarget_ISL_3PL_OptIn);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_ISL, _3PL, _GP_OPT_OUT),
                engTarget_ISL_3PL_OptIn);


        /**************************************************************************************
         * Project Type: English as Target
         * Process: Translation, Transcription
         * Direction: Third Party Letters
         * GP: Opt In/Out
         * Description: Third party letters are always electronic so these don't  apply.
         **************************************************************************************/
        CIProjectInfo engTarget_Translation_Transcription_3PL = new CIProjectInfo();

        engTarget_Translation_Transcription_3PL.setException("Different Language Translation. Direction is " + _3PL
                + ". This is not supported");

        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSLATION, _3PL, _GP_OPT_IN),
                engTarget_Translation_Transcription_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSLATION, _3PL, _GP_OPT_OUT),
                engTarget_Translation_Transcription_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_IN),
                engTarget_Translation_Transcription_3PL);
        projectConfig.put(mapIndex(_PROJECT_ENGLISH_AS_TARGET, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_OUT),
                engTarget_Translation_Transcription_3PL);
    }


    /**
     * This is the standard
     */
    private static void differentLanguagePair() {

        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: Translation/ISL
         * Direction: B2S
         * GP: Opt In
         * Description:
         *          a. FO does the Content Check and FO Language to English translation using the SDL Manual
         *             Translation Process
         *          b. SDL triggers the GP Translation process
         *          c. GP Translator does the English to GP Language translation using CAT
         *             (GP Translator will have the ability to see the Original Letter Image)
         *          d. XMPie composes the letter with the final translation
         *          e. GP receives the Original Letter Image, Composed Letter Image and Final translated text
         *
         **************************************************************************************/
        CIProjectInfo diffLang_Translation_ISL_B2S_OptIn = new CIProjectInfo();

        diffLang_Translation_ISL_B2S_OptIn.setSecondStepProjectRequired(true);
        diffLang_Translation_ISL_B2S_OptIn.setReturnTextRequirements("false|true|true");
        diffLang_Translation_ISL_B2S_OptIn.setTargetLocaleType(
                CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        diffLang_Translation_ISL_B2S_OptIn.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT);

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_ISL, _B2S, _GP_OPT_IN),
                diffLang_Translation_ISL_B2S_OptIn);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _B2S, _GP_OPT_IN),
                diffLang_Translation_ISL_B2S_OptIn);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: Transcription
         * Direction: B2S
         * GP: Opt In
         **************************************************************************************/
        CIProjectInfo diffLang_Transcription_B2S_OptIn = new CIProjectInfo();

        diffLang_Transcription_B2S_OptIn.setSecondStepProjectRequired(true);
        diffLang_Transcription_B2S_OptIn.setReturnTextRequirements("false|true|true");
        diffLang_Transcription_B2S_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        diffLang_Transcription_B2S_OptIn.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_ORIGINAL_SRC);

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_IN),
                diffLang_Transcription_B2S_OptIn);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: Translation/ISL
         * Direction: B2S
         * GP: Opt Out
         * Description:
         *          a. FO does the Content Check and FO Language to English translation
         *             using the SDL Manual Translation Process
         *          b. GP receives the Original Letter Image and English text
         *
         **************************************************************************************/
        CIProjectInfo diffLang_Translation_ISL_B2S_OptOut = new CIProjectInfo();

        diffLang_Translation_ISL_B2S_OptOut.setSecondStepProjectRequired(false);
        diffLang_Translation_ISL_B2S_OptOut.setReturnTextRequirements("false|true|false");
        diffLang_Translation_ISL_B2S_OptOut.setTargetLocaleType(
                CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        diffLang_Translation_ISL_B2S_OptOut.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT);

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_ISL, _B2S, _GP_OPT_OUT),
                diffLang_Translation_ISL_B2S_OptOut);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _B2S, _GP_OPT_OUT),
                diffLang_Translation_ISL_B2S_OptOut);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: Transcription
         * Direction: B2S
         * GP: Opt Out
         **************************************************************************************/
        CIProjectInfo diffLang_Transcription_B2S_OptOut = new CIProjectInfo();

        diffLang_Transcription_B2S_OptOut.setSecondStepProjectRequired(false);
        diffLang_Transcription_B2S_OptOut.setReturnTextRequirements("false|true|false");
        diffLang_Transcription_B2S_OptOut.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        diffLang_Transcription_B2S_OptOut.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_ORIGINAL_SRC);

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _B2S, _GP_OPT_OUT),
                diffLang_Transcription_B2S_OptOut);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: ISL/Transcription
         * Direction: S2B
         * GP: Opt In
         * Description:
         *          a. GP sends the Orginal Letter Image
         *          b. GP Translator does the GP Language to English translation using the SDL Manual Translation
         *             Process
         *          c. SDL triggers the FO Translation process
         *          d. FO does the Content Check and English to FO Language translation using the Auto-translation
         *          e. XMPie composes the letter with the final translation
         *          f. FO receives the Composed Letter
         *          **************ISL Letters********************************************************
         *          a. GP does the GP Language to English translation through their Translation tool
         *          b. FO receives Original ISL Image and the English Text blob does the Content Check and English to
         *             FO Language translation using CAT
         *          c. XMPie composes the letter with the final translation
         *
         **************************************************************************************/
        CIProjectInfo diffLang_Transcription_ISL_S2B_OptIn = new CIProjectInfo();

        diffLang_Transcription_ISL_S2B_OptIn.setSecondStepProjectRequired(true);
        diffLang_Transcription_ISL_S2B_OptIn.setReturnTextRequirements("true|true|true");
        diffLang_Transcription_ISL_S2B_OptIn.setTargetLocaleType(
                CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        diffLang_Transcription_ISL_S2B_OptIn.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_PAYLOAD_SRC_LOCALE);

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_ISL, _S2B, _GP_OPT_IN),
                diffLang_Transcription_ISL_S2B_OptIn);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_IN),
                diffLang_Transcription_ISL_S2B_OptIn);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: Translation
         * Direction: S2B
         * GP: Opt In
         **************************************************************************************/
        CIProjectInfo diffLang_Translation_S2B_OptIn = new CIProjectInfo();

        diffLang_Translation_S2B_OptIn.setSecondStepProjectRequired(true);
        diffLang_Translation_S2B_OptIn.setReturnTextRequirements("false|true|true");
        diffLang_Translation_S2B_OptIn.setTargetLocaleType(CIProjectInfo.CITargetLocale._TARGET_LOCALE_INTERMEDIARY);
        diffLang_Translation_S2B_OptIn.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE_DIRECT);

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _S2B, _GP_OPT_IN),
                diffLang_Translation_S2B_OptIn);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: ISL
         * Direction: S2B
         * GP: Opt Out
         * Description:
         *          a. GP sends the Orginal Letter Image and the English translation blob
         *          b. SDL triggers the FO Translation process
         *          c. FO does the Content Check and English to FO Language translation using the Auto-translation
         *          d. XMPie composes the letter with the final translation
         *          e. FO receives the Composed Letter
         *
         *          **************ISL Letters************
         *          a. GP does the GP Language to English translation through their Translation tool
         *          b. FO receives Original ISL Image and the English Text blob, does the Content Check and English to
         *             FO Language translation using CAT
         *          c. XMPie composes the letter with the final translation
         *
         **************************************************************************************/
        CIProjectInfo diffLang_ISL_S2B_OptOut = new CIProjectInfo();

        diffLang_ISL_S2B_OptOut.setSecondStepProjectRequired(true);
        diffLang_ISL_S2B_OptOut.setReturnTextRequirements("true|true|true");
        diffLang_ISL_S2B_OptOut.setTargetWorkflowName("Compassion Translation and Review Workflow-TwoStep - Step 2");
        diffLang_ISL_S2B_OptOut.setTargetLocaleType(CIProjectInfo.CITargetLocale._PAYLOAD_LOCALES);
        diffLang_ISL_S2B_OptOut.setWorkgroupLocation(CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);
        diffLang_ISL_S2B_OptOut.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE);
        diffLang_ISL_S2B_OptOut.setLanguageExceptionType("Type3_S2B_NoGP");

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_ISL, _S2B, _GP_OPT_OUT),
                diffLang_ISL_S2B_OptOut);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: Translation/Transcription
         * Direction: S2B
         * GP: Opt Out
         **************************************************************************************/
        CIProjectInfo diffLang_Translation_Transcription_S2B_OptOut = new CIProjectInfo();

        diffLang_Translation_Transcription_S2B_OptOut.setSecondStepProjectRequired(true);
        diffLang_Translation_Transcription_S2B_OptOut.setReturnTextRequirements("false|true|true");
        diffLang_Translation_Transcription_S2B_OptOut.setTargetWorkflowName(
                "Compassion Translation and Review Workflow-TwoStep - Step 2");
        diffLang_Translation_Transcription_S2B_OptOut.setTargetLocaleType(CIProjectInfo.CITargetLocale._PAYLOAD_LOCALES);
        diffLang_Translation_Transcription_S2B_OptOut.setWorkgroupLocation(
                CIProjectInfo.CIWorkgroups._WORKGROUP_FIELD_OFFICE);
        diffLang_Translation_Transcription_S2B_OptOut.setDirectSourceLocaleName(
                CIProjectInfo.CIDirectSource._DIRECT_SOURCE_INTERMEDIARY_LOCALE);
        diffLang_Translation_Transcription_S2B_OptOut.setLanguageExceptionType("Type3_S2B_NoGP");

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _S2B, _GP_OPT_OUT),
                diffLang_Translation_Transcription_S2B_OptOut);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _S2B, _GP_OPT_OUT),
                diffLang_Translation_Transcription_S2B_OptOut);


        /**************************************************************************************
         * Project Type: Different language Pair
         * Process: All
         * Direction: Third Party Letters
         * GP: Opt In/Out
         * Description: Third party letters are always electronic so the two step process does
         *              not ever apply.
         **************************************************************************************/
        CIProjectInfo diffLang_All_3PL = new CIProjectInfo();

        diffLang_All_3PL.setException("Two step process. Direction is " + _3PL
                + ". This is not supported");

        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_ISL, _3PL, _GP_OPT_IN),
                diffLang_All_3PL);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_IN),
                diffLang_All_3PL);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _3PL, _GP_OPT_IN),
                diffLang_All_3PL);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_ISL, _3PL, _GP_OPT_OUT),
                diffLang_All_3PL);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSCRIPTION, _3PL, _GP_OPT_OUT),
                diffLang_All_3PL);
        projectConfig.put(mapIndex(_PROJECT_DIFFERENT_LANGUAGE_PAIR, _PROCESS_TRANSLATION, _3PL, _GP_OPT_OUT),
                diffLang_All_3PL);

    }

    public static String mapIndex(String projectType, String processType, String directionType, String gpOptIn) {
        return projectType + "|" + processType + "|" + directionType + "|" + gpOptIn;
    }

    public CIProjectInfo getProjectSettings(String key) {
        return projectConfig.get(key);
    }
}



