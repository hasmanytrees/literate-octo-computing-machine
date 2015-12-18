/**
 * Translator Rating UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.tUICommands = (function() {

    /**
     * Return a list of translators for the select box
     */
    var showTranslators = function( bLimitTranslators ) {

        var $translatorSelectBox = $('#translatorSelect');

        $translatorSelectBox.empty();

        SDL.shared.executeWSRequest('getTranslators', function(result) {

                // Loop through translators
                $(result.sort(SortByName)).each(function(iIndex, oTranslatorUser) {

                    var asOption = [];

                    asOption.push('<option value ="' + oTranslatorUser['id'] + '">' +
                        oTranslatorUser['name'] + '</option>');


                    if (asOption.length > 0) {
                        $translatorSelectBox.append(asOption.join('\n'));
                    }
                });
            }, { limitTranslators: bLimitTranslators }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var showLocales = function() {

        var $localeSelectBox = $('#source_language');

        SDL.shared.executeWSRequest('getLocales', function(result) {
                $(result.sort(SortByName)).each(function(iIndex, oLocale) {
                    var option = [];

                    if (!oLocale['name'].match(/-Direct$/)) {

                        option.push('<option value ="' + oLocale['id'] + '">' +
                            oLocale['name'] + '</option>');


                        if (option.length > 0) {
                            $localeSelectBox.append(option.join('\n'));
                        }
                    }
                });
            }, {}
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var addSourceLanguage = function( strSourceLanguage, iUserId) {

        $("#ratings").empty();

        SDL.shared.executeWSRequest('addSourceLanguage', function() {
                SDL.translatorUI.updateUser();
            },

            { sourceLanguage: strSourceLanguage,
                userId: iUserId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var addTargetLanguage = function( strSourceLanguage, strTargetLanguage, iUserId) {

        // Clear the existing ratings
        $("#ratings").empty();

        SDL.shared.executeWSRequest('addTargetLanguage', function() {
                SDL.translatorUI.updateUser();
            },
            {   sourceLanguage: strSourceLanguage,
                targetLanguage: strTargetLanguage,
                userId: iUserId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var addTargetRating = function( strSourceLanguage, strTargetLanguage, strRating, iUserId) {

        SDL.shared.executeWSRequest('addTargetLanguage', function(result) {
            },

            {   sourceLanguage: strSourceLanguage,
                targetLanguage: strTargetLanguage,
                rating: strRating,
                userId: iUserId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var deleteSourceLanguage = function( strSourceLanguage, iUserId) {

        $("#ratings").empty();

        SDL.shared.executeWSRequest('deleteSourceLanguage', function() {
                SDL.translatorUI.updateUser();
            },

            {   sourceLanguage: strSourceLanguage,
                userId: iUserId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var deleteTargetLanguage = function( strSourceLanguage, strTargetLanguage, iUserId) {

        $("#ratings").empty();

        SDL.shared.executeWSRequest('deleteTargetLanguage', function() {
                SDL.translatorUI.updateUser();
            },

            {   sourceLanguage: strSourceLanguage,
                targetLanguage: strTargetLanguage,
                userId: iUserId
            }
        );
    };


    /**
     * Return a list of translators for the select box
     */
    var showUserRating = function(iUserId) {

        // Clearn the ratings first
        $("#ratings").empty();

        SDL.shared.executeWSRequest('getUserRating', function(result) {

            var RATING_STRINGS = ["Trainee", "Beginner", "Intermediate", "Advanced", "Expert"];

            $(result).find("language_pair").each(function(i, languagePair) {

                var strSourceLanguage = $(languagePair).attr("source");

                var $ratingSourceSection = $("<section>",
                    {class: "source_language"}).appendTo($("#ratings"));

                $("<header>",
                    {html: "<h2>From | " +  $(languagePair).attr("source") + "</h2>"})
                    .appendTo($ratingSourceSection);

                // Insert the delete button for source language
                $("<a href='#' class='source-delete-button'></a>")
                    .attr("data-source-lang", strSourceLanguage)
                    .appendTo($ratingSourceSection);

                // Insert the add button for source language
                $("<aside>",
                    {html: "<a class='addLanguageButton addTargetButton' href='#' data-source-lang='" + strSourceLanguage + "'>Add New Target</a>"})
                    .attr("data-source-lang", strSourceLanguage)
                    .appendTo($ratingSourceSection);

                var $sourceMain = $("<main>").appendTo($ratingSourceSection);
                var $sourceMainUl = $("<ul class='languagePairList'>").appendTo($sourceMain);

                $(languagePair).find("target").each(function(n, target) {

                    var translationID = $(languagePair).attr("source") + "." + $(target).text();
                    var strTargetLanguage = $(target).text();

                    var $targetLi = $("<li>")
                        .attr("data-source-lang", strSourceLanguage)
                        .attr("data-target-lang", strTargetLanguage)
                        .appendTo($sourceMainUl);

                    // Create the left column
                    $("<div>", {
                        class: "leftcol", html: "<p>To | " + strTargetLanguage + "</p>"
                    }).appendTo($targetLi);

                    //Create the right column
                    var $targetRight = $("<div>", {
                        class: "rightcol", html: "<div class='linediv'></div>"
                    }).appendTo($targetLi);


                    // Loop through ratings
                    $.each( RATING_STRINGS, function(idx, rating) {

                        // Create the radio button group
                        var $radioGroup = $("<div>", {
                            class: "radiogroup"
                        }).appendTo($targetRight);


                        // Insert Rating Radio Button
                        $('<input id="' + translationID + idx +
                            '" name="' + translationID + '" type="radio" value="' + rating +
                            '"/>')
                            .attr("data-source-lang", strSourceLanguage)
                            .attr("data-target-lang", strTargetLanguage)
                            .appendTo($radioGroup);

                        // Insert Radio Button label
                        $('<label for="' + translationID + idx + '"><span></span><br>' +
                            rating + '</label>').appendTo($radioGroup);

                        $('input[name="' + translationID + '"]').val([$(target).attr("rating")]);

                    });

                    // Insert target language Delete button
                    $("<a href='#' class='target-delete-button'><span></span></a>")
                        .attr("data-source-lang", strSourceLanguage)
                        .attr("data-target-lang", strTargetLanguage)
                        .appendTo($targetLi);
                });
            });

            // Action when Source lang delete button is clicked
            SDL.tUIBindings.bindSourceDeleteButton();

            // Action when target lang delete button is clicked
            SDL.tUIBindings.bindTargetDeleteButton();

            // Action when Target Lang add button is clicked
            SDL.tUIBindings.bindTargetAddButton();

            // Action on any change in radio button status
            SDL.tUIBindings.bindRatingRadioChange();

        }, { userId: iUserId });



    };

    /**
     * Sort function
     * @param a
     * @param b
     * @returns {number}
     * @constructor
     */

    var SortByName = function(a, b){
        var aName = a.name.toLowerCase();
        var bName = b.name.toLowerCase();

        return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
    };

    return {
        showTranslators         : showTranslators,
        showLocales             : showLocales,
        addSourceLanguage       : addSourceLanguage,
        addTargetLanguage       : addTargetLanguage,
        addTargetRating         : addTargetRating,
        deleteSourceLanguage    : deleteSourceLanguage,
        deleteTargetLanguage    : deleteTargetLanguage,
        showUserRating          : showUserRating,
        SortByName				: SortByName
    };
}());