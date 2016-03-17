/**
 * Translator Complete UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.translateComplete = (function() {

    /**
     * Initialize the translator UI
     */
    var init = function() {

        //SDL.shared.setRequestURL('translator_ui_ajax_commands');

        // Disable the Radio Button
        $("input[value='Translation Complete']").prop("disabled", true);

        // Deselect all radio buttons
        $("input[value='Translation Complete']").parent().children("input").prop("checked", false);

        // Display warning message and format label
        $("input[value='Translation Complete']").siblings("label")
            .css("font-style", "italic")
            .css("color", "#aaa")
            .after("<p style='color: darkorange;'>" +
                "Cannot complete translation, one or more translation fields are blank.</p>")
            .prop("disabled", true);
    };

    return {
        init              : init
    };

})();


/**
 * Run this when the document is ready
 */
$(document).ready(function() {

    // Initialize the Translator Rating UI
    SDL.translateComplete.init();
});