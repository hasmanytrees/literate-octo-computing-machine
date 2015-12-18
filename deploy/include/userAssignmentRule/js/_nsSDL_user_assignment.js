/**
 * Translator Rating UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.userAssignmentRuleUI = (function() {

    /**
     * Initialize the translator UI
     */
    var init = function() {

        SDL.shared.setRequestURL('user_assignment_ajax_commands');

        // Populate the workgroups select box
        SDL.uarUICommands.showWorkgroups();

        // Setup select boxes with Select2 styling and bindings
        SDL.uarUIBindings.select2Setup();

    };


    /**
     * Update the user rating screen
     */
    var updateUser = function() {

        // Update the translator rating info based on the currently selected translator
        SDL.uarUICommands.showAssignmentRules($("#workgroupSelect").val());

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
    SDL.userAssignmentRuleUI.init();
});

