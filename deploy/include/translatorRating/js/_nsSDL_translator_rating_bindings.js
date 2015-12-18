/**
 * Translator Rating UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.tUIBindings = (function() {

    /**
     * Action when target lang delete button is clicked
     */
    var bindTargetDeleteButton = function () {

        /**
         * Target Delete Button
         */
        $(".target-delete-button").click(function(oEvent) {

            oEvent.preventDefault();

            var strSourceLanguage = $(this).parent().attr("data-source-lang");
            var strTargetLanguage = $(this).parent().attr("data-target-lang");

            $("#deleteConfirm").dialog({
                title: "Remove Target Language",
                dialogClass: "no-close",
                modal: true,
                resizable: false,
                buttons: {
                    "OK": function () {

                        SDL.tUICommands.deleteTargetLanguage(strSourceLanguage,
                            strTargetLanguage,
                            $("#translatorSelect").val());

                        $(this).dialog("close");

                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        });
    };


    /**
     * Action when source lang delete button is clicked
     */
    var bindSourceDeleteButton = function() {
        /**
         *  Source Delete Button
         */
        $(".source-delete-button").click( function(oEvent) {

            oEvent.preventDefault();

            var strSourceLanguage = $(this).attr("data-source-lang");

            $("#deleteConfirm").dialog({
                title: "Remove Source Language",
                dialogClass: "no-close",
                modal: true,
                resizable: false,
                buttons: {
                    "OK": function () {

                        SDL.tUICommands.deleteSourceLanguage(strSourceLanguage,
                            $("#translatorSelect").val());

                        $(this).dialog("close");

                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        });
    };


    /**
     * Action when source lang add button is clicked
     */
    var bindSourceAddButton = function () {
        $("#addSourceButton").click(function () {

            $("#source_language").select2("val", null);

            if ( $("#select2-translatorSelect-container .select2-selection__placeholder").text() == "" ||
                    $("#select2-translatorSelect-container .select2-selection__placeholder").text() == null) {

                $("#dialog").dialog({
                    title: "Choose Language",
                    dialogClass: "no-close",
                    modal: true,
                    resizable: false,
                    buttons: {
                        "Submit": function () {
                            if ( $("#select2-source_language-container .select2-selection__placeholder").text() == "") {

                                SDL.tUICommands.addSourceLanguage($("#source_language :selected").text(),
                                    $("#translatorSelect").val());
                                $(this).dialog("close");
                            }

                        },
                        "Cancel": function () {
                            $(this).dialog("close");
                        }
                    }
                });
            }
        });
    };


    /**
     * Action when target lang add button is clicked
     */
    var bindTargetAddButton = function() {

        /**
         * Add Language Button
         */
        $(".addTargetButton").click(function(oEvent) {

            oEvent.preventDefault();

            var strSourceLanguage = $(this).parent().attr("data-source-lang");

            $("#source_language").select2("val", null);

            $("#dialog").dialog({
                dialogClass: "no-close",
                modal: true,
                resizable: false,
                buttons: {
                    "Submit": function () {

                        if ( $("#select2-source_language-container .select2-selection__placeholder").text() == "") {

                            SDL.tUICommands.addTargetLanguage(strSourceLanguage,
                                $("#source_language :selected").text(),
                                $("#translatorSelect").val());

                            $(this).dialog("close");
                        }

                    },
                    "Cancel": function () {
                        $(this).dialog("close");
                    }
                }
            });
        });
    };


    /**
     * Action when rating radio button changes occur
     */
    var bindRatingRadioChange = function() {

        $("input[type='radio']").change(function() {

            SDL.tUICommands.addTargetRating( $(this).attr("data-source-lang"),
                $(this).attr("data-target-lang"),
                $(this).val(),
                $("#translatorSelect").val());
        });
    };

    /**
     * Action when limit workgroup checkbox changes
     */
    var bindLimitWorkgroupCheckboxChange = function() {

        $("#limitToWorkgroup").change(function () {
            SDL.translatorUI.init();
        });
    };

    var select2Setup = function () {
        /**
         * Setup nicer looking select boxes using Select2
         */
        $("#translatorSelect").select2({
            placeholder: "Select a translator",
            width: '267px'

        })
            .on("change", function() {
                $("#addSourceButton").show();
                SDL.translatorUI.updateUser();

            });

        $("#source_language").select2({
            placeholder: "Select a language",
            width: '267px'

        });
    };

    return {
        bindTargetDeleteButton           : bindTargetDeleteButton,
        bindSourceDeleteButton           : bindSourceDeleteButton,
        bindSourceAddButton              : bindSourceAddButton,
        bindTargetAddButton              : bindTargetAddButton,
        bindRatingRadioChange            : bindRatingRadioChange,
        bindLimitWorkgroupCheckboxChange : bindLimitWorkgroupCheckboxChange,
        select2Setup                     : select2Setup
    };

})();