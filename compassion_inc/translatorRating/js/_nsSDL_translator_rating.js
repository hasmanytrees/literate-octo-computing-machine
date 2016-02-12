/**
 * Translator Rating UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.translatorUI = (function() {

    /**
     * Initialize the translator UI
     */
    var init = function() {

        SDL.shared.setRequestURL('translator_ui_ajax_commands');

        // Check to see if we will limit translator list to just those in user's workgroup
        var bLimitTranslators = $("input#limitToWorkgroup").is(':checked');

        // Populate the Translators select box
        SDL.tUICommands.showTranslators(bLimitTranslators);

        // Get the locales from WorldServer and populate <select> box
        SDL.tUICommands.showLocales();

        // Setup select boxes with Select2 styling and bindings
        SDL.tUIBindings.select2Setup();

        // Bind action to the Add Source Language Button
        SDL.tUIBindings.bindSourceAddButton();

        // Bind action to changes to the "Limit Workgroup" checkbox
        SDL.tUIBindings.bindLimitWorkgroupCheckboxChange();

    };


    /**
     * Update the user rating screen
     */
    var updateUser = function() {

        // Update the translator rating info based on the currently selected translator
        SDL.tUICommands.showUserRating($("#translatorSelect").val());

    };

    return {
        init              : init,
        updateUser        : updateUser
    };

})();


/**
 * Run this when the document is ready
 */
$(document).ready(function() {

    // Initialize the Translator Rating UI
    SDL.translatorUI.init();
});

