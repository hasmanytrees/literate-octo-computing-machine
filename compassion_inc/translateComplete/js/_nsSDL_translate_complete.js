/**
 * Translator Complete UI
 *
 * Created by cslack on 3/17/2016.
 */

var SDL = SDL || {};


SDL.translateComplete = (function() {

    /**
     * Initialize the translator UI
     */
    var init = function() {

        // Get the translation complete option in the DOM
        var $translationComplete = $("input[value='Translation Completed']");

        // Disable the Radio Button
        $translationComplete.prop("disabled", true);

        // Deselect all radio buttons
        $translationComplete.parent().children("input").prop("checked", false);

        // Display warning message and format label
        $translationComplete.siblings("label")
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