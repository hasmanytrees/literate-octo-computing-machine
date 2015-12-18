/**
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.translatorUI = (function() {

    /**
     * Initialize the translator UI
     */
    var init = function() {

        SDL.shared.setRequestURL('translator_ui_ajax_commands');

        var limitTranslators = $("input#limitToWorkgroup").is(':checked');


        /** Populate the Translators select box */
        showTranslators(limitTranslators);

        showLocales();

    };

    var updateUser = function() {
        SDL.translatorUI.showUserRating($("#translatorSelect").val());
    };

    /**
     * Return a list of translators for the select box
     */
    var showTranslators = function( limitTranslators ) {


        var translatorSelectBox = $('#translatorSelect');

        $('#translatorSelect').empty();

        SDL.shared.executeWSRequest('getTranslators', function(result) {
                $(result).each(function(idx,translatorUser) {
                    var option = [];

                    option.push('<option value ="' + translatorUser['id'] + '">' +
                        translatorUser['name'] + '</option>');


                    if (option.length > 0) {
                        translatorSelectBox.append(option.join('\n'));
                    }
                });
            }, { limitTranslators: limitTranslators }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var showLocales = function() {


        var localeSelectBox = $('#source_language');

        SDL.shared.executeWSRequest('getLocales', function(result) {
                $(result).each(function(idx,locale) {
                    var option = [];

                    option.push('<option value ="' + locale['id'] + '">' +
                        locale['name'] + '</option>');


                    if (option.length > 0) {
                        localeSelectBox.append(option.join('\n'));
                    }
                });
            }, { test: null }
            // { 'project': SDL.shared.getOpenerParameter('project')}
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var addSourceLanguage = function( selectedLanguage, userId) {

        $("#ratings").empty();

        SDL.shared.executeWSRequest('addSourceLanguage', function(result) {
                SDL.translatorUI.updateUser();
            },

            { sourceLanguage: selectedLanguage,
              userId: userId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var addTargetLanguage = function( sourceLanguage, targetLanguage, userId) {

        //var selectedLanguage = $('#source_language').text();
        $("#ratings").empty();

        SDL.shared.executeWSRequest('addTargetLanguage', function(result) {
                SDL.translatorUI.updateUser();

            },

            { sourceLanguage: sourceLanguage,
                targetLanguage: targetLanguage,
                userId: userId
            }
        );
    };

    /**
     * Return a list of translators for the select box
     */
    var addTargetRating = function( sourceLanguage, targetLanguage, rating, userId) {

        SDL.shared.executeWSRequest('addTargetLanguage', function(result) {
                //SDL.translatorUI.updateUser();

            },

            { sourceLanguage: sourceLanguage,
                targetLanguage: targetLanguage,
                rating: rating,
                userId: userId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var deleteSourceLanguage = function( sourceLanguage, userId) {

        $("#ratings").empty();

        console.log("Deleting source lang " + sourceLanguage + " for userid = " + userId);

        SDL.shared.executeWSRequest('deleteSourceLanguage', function(result) {
                SDL.translatorUI.updateUser();

            },

            { sourceLanguage: sourceLanguage,
                userId: userId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var deleteTargetLanguage = function( sourceLanguage, targetLanguage, userId) {

        $("#ratings").empty();

        console.log("Deleting target lang " + sourceLanguage + " to " + targetLanguage + " for userid = " + userId);

        SDL.shared.executeWSRequest('deleteTargetLanguage', function(result) {
                SDL.translatorUI.updateUser();

            },

            { sourceLanguage: sourceLanguage,
                targetLanguage: targetLanguage,
                userId: userId
            }
        );
    };

    /**
     * Return a list of translators for the select box
     */
    var showUserRating = function( userId ) {

        /** Clearn the ratings first */
        $("#ratings").empty();

        SDL.shared.executeWSRequest('getUserRating', function(result) {

                var RATING_STRINGS = ["Trainee", "Beginner", "Intermediate", "Advanced", "Expert"];

                $(result).find("language_pair").each(function(i, languagePair) {

                    var sourceLanguage = $(languagePair).attr("source");

                    var $ratingSourceSection = $("<section>",
                        {class: "source_language"}).appendTo($("#ratings"));

                    var $sourceHeader = $("<header>",
                        {html: "<h2>From | " +  $(languagePair).attr("source") + "</h2>"})
                        .appendTo($ratingSourceSection);

                    var $sourceDeleteButton = $("<a href='#' class='source-delete-button'></a>")
                        .attr("data-source-lang", sourceLanguage)
                        .appendTo($ratingSourceSection);

                    var $sourceButton = $("<aside>",
                        {html: "<a class='addLanguageButton addTargetButton' href='#' data-source-lang='" + sourceLanguage + "'>Add New Target</a>"})
                            .attr("data-source-lang", sourceLanguage)
                            .appendTo($ratingSourceSection);

                    var $sourceMain = $("<main>").appendTo($ratingSourceSection);
                    var $sourceMainUl = $("<ul class='languagePairList'>").appendTo($sourceMain);

                    $(languagePair).find("target").each(function(n, target) {

                        var translationID = $(languagePair).attr("source") + "." + $(target).text();
                        var targetLanguage = $(target).text();

                        var $targetLi = $("<li>")
                            .attr("data-source-lang", sourceLanguage)
                            .attr("data-target-lang", targetLanguage)
                            .appendTo($sourceMainUl);

                        var $targetLeft = $("<div>",
                            {class: "leftcol",
                                html: "<p>To | " + targetLanguage + "</p>"}).appendTo($targetLi);

                        var $targetRight = $("<div>",
                            {class: "rightcol",
                                html: "<div class='linediv'></div>"})
                            .appendTo($targetLi);


                        /**
                         * Loop through ratings
                         */
                        $.each( RATING_STRINGS, function(idx, rating) {

                            var $radioGroup = $("<div>",
                                {class: "radiogroup"}).appendTo($targetRight);

                            var $radioButton = $('<input id="' + translationID + idx +
                                '" name="' + translationID + '" type="radio" value="' + rating +
                                '"/>')
                                .attr("data-source-lang", sourceLanguage)
                                .attr("data-target-lang", targetLanguage)
                                .appendTo($radioGroup);

                            var $radioButtonLabel = $('<label for="' + translationID + idx + '"><span></span><br>' +
                                rating + '</label>').appendTo($radioGroup);

                            $('input[name="' + translationID + '"]').val([$(target).attr("rating")]);

                        });

                        var $deleteButton = $("<a href='#' class='target-delete-button'><span></span></a>")
                            .attr("data-source-lang", sourceLanguage)
                            .attr("data-target-lang", targetLanguage)
                            .appendTo($targetLi);
                    });
                });

            /**
             *  Source Delete Button
             */
            $(".source-delete-button").click( function(e) {

                e.preventDefault();

                var sourceLang = $(this).attr("data-source-lang");

                $("#deleteConfirm").dialog({
                    title: "Remove Source Language",
                    dialogClass: "no-close",
                    modal: true,
                    resizable: false,
                    buttons: {
                        "OK": function () {

                            deleteSourceLanguage(sourceLang,
                                $("#translatorSelect").val());

                            $(this).dialog("close");

                        },
                        "Cancel": function () {
                            $(this).dialog("close");
                        }
                    }
                });
            });

            /**
             * Target Delete Button
             */
            $(".target-delete-button").click(function(e) {

                e.preventDefault();

                var sourceLang = $(this).parent().attr("data-source-lang");
                var targetLang = $(this).parent().attr("data-target-lang");

                $("#deleteConfirm").dialog({
                    title: "Remove Target Language",
                    dialogClass: "no-close",
                    modal: true,
                    resizable: false,
                    buttons: {
                        "OK": function () {

                            deleteTargetLanguage(sourceLang,
                                targetLang,
                                $("#translatorSelect").val());

                            $(this).dialog("close");

                        },
                        "Cancel": function () {
                            $(this).dialog("close");
                        }
                    }
                });
            });

            /**
             * Add Language Button
             */
            $(".addTargetButton").click(function(e) {

                e.preventDefault();

                var sourceLang = $(this).parent().attr("data-source-lang");

                $("#dialog").dialog({
                    dialogClass: "no-close",
                    modal: true,
                    resizable: false,
                    buttons: {
                        "Submit": function () {

                            addTargetLanguage(sourceLang,
                                $("#source_language :selected").text(),
                                $("#translatorSelect").val());

                            $(this).dialog("close");

                        },
                        "Cancel": function () {
                            $(this).dialog("close");
                        }
                    }
                });
            });

            $("input[type='radio']").change(function(e) {

                addTargetRating( $(this).attr("data-source-lang"),
                    $(this).attr("data-target-lang"),
                    $(this).val(),
                    $("#translatorSelect").val());

            });

        }, { userId: userId });
    };

    return {
        init              : init,
        updateUser        : updateUser,
        showUserRating    : showUserRating,
        addSourceLanguage : addSourceLanguage,
        showLocales       : showLocales
    };

})();

$(document).ready(function() {

    /**
     * Setup nicer looking select boxes using Select2
     */
    $("#translatorSelect").select2({
        placeholder: "Select a translator",
        width: '267px'
    })
        .on("change", function(e) {
            $("#addSourceButton").show();
            SDL.translatorUI.updateUser();

        });

    $("#source_language").select2({
        placeholder: "Select a language",
        width: '267px'
    });

    $("#addSourceButton").click(function () {
        $("#dialog").dialog({
            title: "Choose Language",
            dialogClass: "no-close",
            modal: true,
            resizable: false,
            buttons: {
                "Submit": function () {
                    SDL.translatorUI.addSourceLanguage($("#source_language :selected").text(), $("#translatorSelect").val());
                    $(this).dialog("close");

                },
                "Cancel": function () {
                    $(this).dialog("close");
                }
            }
        });
    });

    $("#limitToWorkgroup").change(function () {
        SDL.translatorUI.init();
    });

    SDL.translatorUI.init();
});

