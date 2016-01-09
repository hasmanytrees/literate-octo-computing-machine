/**
 * Translator Rating UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.uarUIBindings = (function() {

    /**
     * Action when the "Use Default" checkbox is clicked
     */
    var bindDefaultCheck = function (strRuleDefault) {

        $(".default-checkbox").click(function() {

            // Check to see if the checkbox is checked or unchecked
            if ($(this).prop("checked")) {


                $(this).parent().find("*").removeClass("appear-checked");

                // Set the class to indicate this is a row using default settings
                $(this).parent().addClass("default-row");

                // Clear all of the existing radio buttons
                var strRadioGroup = $(this).attr("data-rule-type") + "." + $(this).attr("data-complexity-level") ;
                $('input[name="' + strRadioGroup + '"]').prop('checked', false);

                // Get the default value for this rule type and complexity level from the default values XML
                var ruleValue = $(strRuleDefault)
                    .find("rule[type='" + $(this).attr("data-rule-type") + "']")
                    .find("complexity[level='" + $(this).attr("data-complexity-level") + "']").text().trim();

                // Simulate a "click" on the radio button corresponding to the default
                $('input[name="' + strRadioGroup + '"][value="' + ruleValue + '"]').trigger("click");

                // Disable all radio buttons in the row
                $(".default-row").find("input[type='radio']").attr("disabled", true);

                // When we are using the default we want to set the stored value to empty so that
                // the system will get the default value
                SDL.uarUICommands.setAssignmentRule( $("#workgroupSelect").val(),
                    $(this).attr("data-rule-type"),
                    $(this).attr("data-complexity-level"),
                    "");
            } else {

                // When not default remove the class from the row which signifies default
                $(this).parent().removeClass("default-row");

                // Enable all of the radio buttons
                $(this).parent().find("input[type='radio']").attr("disabled",false);
            }

        });
    };


    /**
     * Action when rating radio button changes occur
     */
    var bindRatingRadioChange = function() {

        $("input[type='radio']").change(function() {

            // Highlight all of the radio buttons greater than the current one
            highlightRadioButtons($(this));

            // Write the value into the Workgroup attribute XML in WorldServer
            SDL.uarUICommands.setAssignmentRule( $("#workgroupSelect").val(),
                $(this).attr("data-rule-type"),
                $(this).attr("data-complexity-level"),
                $(this).val());

        });
    };

    /**
     * Configures the dropdown list to use Select2.js
     */
    var select2Setup = function () {
        /**
         * Setup nicer looking select boxes using Select2
         */
        $("#workgroupSelect").select2({
            placeholder: "Select a Workgroup",
            width: '267px'
        })
            .on("change", function() {
                SDL.userAssignmentRuleUI.updateUser();
            });
    };


    /**
     * Highlights all of the radio buttons greater than the one passed in
     *
     * @param $buttonSelected - The current selected radio button
     */
    var highlightRadioButtons = function ( $buttonSelected ) {

        // Find all of the buttons in the group
        var siblings = $buttonSelected.parent().siblings(".radiogroup").andSelf();

        // Get just the ID number from the button ID's
        var iCurrentButtonId = $buttonSelected.attr('id').split('.')[1];

        // Loop through all of the buttons
        siblings.each(function() {

            // Get the ID value of the current button
            var iIdVal = $(this).children("input").attr('id').split(".")[1];

            // Check to see if it is greater than the button passed in
            if( iIdVal > iCurrentButtonId ) {
                // If so highlight it by adding the appear-checked class
                $(this).children("label").children("span").addClass("appear-checked");
            } else {
                // If not make sure the appear-checked class is not present
                $(this).children("label").children("span").removeClass("appear-checked");
            }
        });
    };

    return {
        bindDefaultCheck                 : bindDefaultCheck,
        bindRatingRadioChange            : bindRatingRadioChange,
        select2Setup                     : select2Setup,
        highlightRadioButtons            : highlightRadioButtons
    };

})();