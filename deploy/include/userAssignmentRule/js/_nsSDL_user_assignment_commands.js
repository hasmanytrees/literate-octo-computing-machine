/**
 * User Assignment Rule UI
 *
 * Created by cslack on 10/5/2015.
 */

var SDL = SDL || {};


SDL.uarUICommands = (function() {

    /**
     * Return a list of workgroups for the select box
     */
    var showWorkgroups = function() {

        // Get the jQuery object for the workgroup select box
        var $workgroupSelectBox = $('#workgroupSelect');

        // Get a list of workgroups from WorldServer
        SDL.shared.executeWSRequest('getWorkgroups', function(result) {


                // Look through the workgroups
                $(result.sort(SortByName)).each(function(iIndex, oWorkgroup) {

                    var option = [];

                    // Add an option to the select box for each workgroup
                    option.push('<option value ="' + oWorkgroup['id'] + '">' +
                        oWorkgroup['name'] + '</option>');

                    // If the option is value then add it to the select box
                    if (option.length > 0) {
                        $workgroupSelectBox.append(option.join('\n'));
                    }
                });
            }, {}
        );
    };


    /**
     * Sets the assignment rule in WorldServer
     */
    var setAssignmentRule = function( workgroupID, ruleType, complexityLevel, ruleValue) {

        // Execute the command to set assignment rule on WorldServer
        SDL.shared.executeWSRequest('setAssignmentRule', function() {
            },
            {   workgroupID:        workgroupID,
                ruleType:           ruleType,
                complexityLevel:    complexityLevel,
                ruleValue:          ruleValue
            }
        );
    };


    /**
     * Displays all of the assignment rules
     */
    var showAssignmentRules= function(iWorkgroupId) {

        // Clear the rules first
        $("#assignmentRules").empty();

        var strRuleDefault;

        // Get the default rule values from the Workgroup custom attribute default
        SDL.shared.executeWSRequest('getDefaultRule', function(result) {

            strRuleDefault = result;

        } , {});

        SDL.shared.executeWSRequest('getAssignmentRule', function(result) {

            var RULE_TYPES = ["Translate", "QC"];
            var RULE_LEVELS = [1, 2, 3];
            var RATING_STRINGS = ["Trainee", "Beginner", "Intermediate", "Advanced", "Expert"];
            var RATING_LEVELS = [
                "Trainee,Beginner,Intermediate,Advanced,Expert",
                "Beginner,Intermediate,Advanced,Expert",
                "Intermediate,Advanced,Expert",
                "Advanced,Expert",
                "Expert"
            ];

            // Loop through the rule types
            $.each(RULE_TYPES, function(i, strRuleName) {

                // Add an assignment rule section tag to the DOM
                var $assignmentRuleSection = $("<section>",
                    {class: "assignmentRule"}).appendTo($("#assignmentRules"));

                // Add a header to the assignment rule section
                $("<header>",
                    {html: "<h2>" + strRuleName + " Rules</h2>" +
                        "<p class='instructions'>Select the minimum translator ability level for each complexity level.</p>"
                    })
                    .appendTo($assignmentRuleSection);

                // Add a main section to the assignment rule section
                var $ruleMain = $("<main>").appendTo($assignmentRuleSection);

                // Add a list of complexity values to the main section
                var $ruleMainUl = $("<ul class='complexityLevelList'>").appendTo($ruleMain);

                // Loop through the complexity levels for this rule type
                $.each(RULE_LEVELS, function(n, iComplexityLevel) {

                    var strComplexityID = strRuleName + "." + iComplexityLevel;

                    var strCurrentLevels = $(result).find("rule[type='" + strRuleName + "']")
                        .find("complexity[level='" + iComplexityLevel + "']").text();

                    var $complexityLi = $("<li>")
                        .attr("data-complexity-level", iComplexityLevel)
                        .attr("data-rule-type", strRuleName)
                        .appendTo($ruleMainUl);

                    // Create the left column
                    $("<div>", {
                        class: "leftcol", html: "<p>Complexity Level " + iComplexityLevel + "</p>"
                    }).appendTo($complexityLi);

                    //Create the right column
                    var $complexityRight = $("<div>", {
                        class: "rightcol", html: "<div class='linediv'></div>"
                    }).appendTo($complexityLi);

                    // Loop through ratings
                    $.each( RATING_STRINGS, function(idx, rating) {

                        // Create the radio button group
                        var $radioGroup = $("<div>", {
                            class: "radiogroup"
                        }).appendTo($complexityRight);

                        // Insert Rating Radio Button
                        $('<input id="' + strComplexityID + idx +
                            '" name="' + strComplexityID + '" type="radio" value="' + RATING_LEVELS[idx] +
                            '"/>')
                            .attr("data-complexity-level", iComplexityLevel)
                            .attr("data-rule-type", strRuleName)
                            .appendTo($radioGroup);

                        // Insert Radio Button label
                        $('<label for="' + strComplexityID + idx + '"><span></span><br>' +
                            rating + '</label>').appendTo($radioGroup);

                        // Set the value of the radio buttons to the retrieved value
                        $('input[name="' + strComplexityID + '"][value="' + strCurrentLevels.trim() + '"]').prop('checked', true);

                    });

                    // Highlight all of the buttons greater than the selected one to indicate the translator ratings
                    // that apply to this complexity level
                    var $radioButton =  $("input[name='" + strComplexityID + "']:checked");

                    if($radioButton.length > 0) {
                        SDL.uarUIBindings.highlightRadioButtons($radioButton);
                    }

                    /**
                     Determine whether the initial value of the "Use Default" checkbox should be checked or unchecked
                     It is unchecked if a value is found in the Workgroup attribute, it is checked if we are using
                     the value found in the default for the attribute.
                     */
                    var checkStatus = false;

                    if(strCurrentLevels == "") {
                        checkStatus = true;
                    }

                    // Insert "Use Default" checkbox
                    var $defaultCheckbox = $('<input type="checkbox" name="default-complexity" class="default-checkbox">')
                        .attr("data-rule-type", strRuleName)
                        .attr("data-complexity-level", iComplexityLevel)
                        .appendTo($complexityLi)
                        .prop("checked", checkStatus);

                    $('<label for="default-complexity" class="default-label">Use Global Default</label>')
                        .appendTo($complexityLi);

                    // Check to see if we are using default values
                    if(checkStatus == true) {

                        var strRadioGroup = strRuleName + "." + iComplexityLevel;

                        // Get the default value
                        var ruleValue = $(strRuleDefault)
                            .find("rule[type='" + strRuleName + "']")
                            .find("complexity[level='" + iComplexityLevel + "']").text().trim();

                        // Simulate clicking on the radio button with that value
                        var $currentButton = $('input[name="' + strRadioGroup + '"][value="' + ruleValue + '"]');
                        $currentButton.trigger("click");

                        // Highlight the radio buttons greater than that button
                        SDL.uarUIBindings.highlightRadioButtons($currentButton);

                        // Add the class to the row which shows it is default
                        $defaultCheckbox.parent().addClass("default-row");

                        // Disable all radio buttons
                        $(".default-row").find("input[type='radio']").attr("disabled", true);
                    }
                });
            });

            // Action when target lang delete button is clicked
            SDL.uarUIBindings.bindDefaultCheck(strRuleDefault);

            // Action on any change in radio button status
            SDL.uarUIBindings.bindRatingRadioChange();

        }, { workgroupId: iWorkgroupId });

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
    };

    return {
        showWorkgroups          : showWorkgroups,
        setAssignmentRule       : setAssignmentRule,
        showAssignmentRules     : showAssignmentRules
    };
}());