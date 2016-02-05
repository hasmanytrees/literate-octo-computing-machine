/**
 * Translator Payment Commands
 *
 * Created by cslack on 1/26/2016.
 */

var SDL = SDL || {};

SDL.tPaymentCommands = function () {

    var oDataTable = null;

    /**
     * Return a list of translators for the select box
     */
    var showWorkgroups = function () {

        // Get the workgroup select jQuery element
        var $workgroupSelectBox = $('#workgroupSelect');

        // Empty the workgroup selecct box
        $workgroupSelectBox.empty();

        // Get workgroups from WorldServer
        SDL.shared.executeWSRequest('getWorkgroups', function (result) {

                // Loop through workgroups
                $(result.sort(SortByName)).each(function (iIndex, oWorkgroup) {

                    var asOption = [];

                    // Add workgroup to select box
                    asOption.push('<option value ="' + oWorkgroup['id'] + '">' +
                        oWorkgroup['name'] + '</option>');

                    // Check to see if any workgroups were returned
                    if (asOption.length > 0) {
                        // If so add them to the select box
                        $workgroupSelectBox.append(asOption.join('\n'));
                    }
                });
            }, {}
        );
    };


    /**
     * Displays the detailed report
     */
    var showDetailedReport = function (iStartDate, iEndDate, asWorkgroups) {

        // Set the titles we will use for the detailed report columns
        var aoDetailedTitles = [
            {title: "User Name"},
            {title: "Task Type"},
            {title: "Template"},
            {title: "Communication ID"},
            {title: "Source Locale"},
            {title: "Target Locale"},
            {title: "Translation Task Creation Date"},
            {title: "Task Start Date"},
            {title: "Task Completion Date"},
            {title: "Translation Task Completion Date"},
            {title: "Escalation"},
            {title: "Rework"},
            {title: "Returned to Queue"}
        ];

        // Check to make sure that all input items are present
        if(validateInput()) {

            // Get the detailed report array from WorldServer
            SDL.shared.executeWSRequest('getDetailedReport', function (oResult) {

                // Display the report
                showReport(oResult, aoDetailedTitles);

            }, {
                startDate: iStartDate,
                endDate: iEndDate,
                workgroups: asWorkgroups.join(",")
            });
        } else {
            // Display errors if validation fails
            $(".error-box").show();
        }
    };


    /**
     * Displays the summary report
     *
     * @param iStartDate - Start date for query
     * @param iEndDate - End date for query
     * @param asWorkgroups - List of workgroups to query
     */
    var showSummaryReport = function (iStartDate, iEndDate, asWorkgroups) {

        // Set the titles we will use for the summary report
        var aoSummaryTitles = [
            {title: "User Name"},
            {title: "Task Type"},
            {title: "Template"},
            {title: "Task Completed"},
            {title: "Escalation"},
            {title: "Rework"},
            {title: "Returned to Queue"}
        ];

        // Check to make sure that all input items are present
        if(validateInput()) {

            // Get summary report from WorldServer
            SDL.shared.executeWSRequest('getSummaryReport', function (oResult) {

                // Show summary report
                showReport(oResult, aoSummaryTitles);

            }, {
                startDate: iStartDate,
                endDate: iEndDate,
                workgroups: asWorkgroups.join(",")
            });

        } else {
            // Display errors if validation fails
            $(".error-box").show();
        }
    };


    /**
     * Displays the report table with the dataset and titles specified
     *
     * @param arrDataSet - The data to display
     * @param arrTitles - The column titles to display
     */
    var showReport = function(arrDataSet, arrTitles) {

        // Display the report table
        oDataTable = $('#translator_report').DataTable({
            data: arrDataSet,
            columns: arrTitles
        });

        // Set the currents for CSV creation
        SDL.tPaymentBindings.setCurrentTitles(arrTitles);
        SDL.tPaymentBindings.setCurrentDataSet(arrDataSet);
    };


    /**
     * Remove the report from the screen
     */
    var clearReport = function () {

        // Check to see if data table exists
        if (oDataTable != null) {
            // If so then clear it from the DOM
            $("#translator_report_wrapper").replaceWith("<table id='translator_report' class='display compact' cellspacing='0' width='100%'></table>");

            // Also destroy the object itself
            oDataTable.destroy();
        }
    };


    /**
     *
     * @returns {boolean}
     */
    var validateInput = function () {

        var $startDate = $("#start_date");
        var $endDate = $("#end_date");
        var strStartDate = $startDate.val();
        var strEndDate = $endDate.val();
        var arrWorkgroups = $("#workgroupSelect").val();
        var bInputValid = true;
        var d1, d2;
        var strErrorMessage = "";

        // Clear any error that might be displayed
        clearErrors();

        // Delete any existing report
        clearReport();

        // Validate the starting date
        if(strStartDate === "") {

            // Display the error message
            $startDate.addClass("input-error");

            // Set the validity flag to false
            bInputValid = false;

            // Specify the error message
            strErrorMessage += "Please enter start date.  ";

        } else {
            // If valid then set the date
            d1 = new Date(strStartDate);
        }

        // Validate the ending date
        if(strEndDate === "") {

            // Display the error message
            $endDate.addClass("input-error");

            // Set the validity flag to false
            bInputValid = false;

            // Specify the error message
            strErrorMessage += "Please enter end date.  ";

        } else {
            // If valid then set the date
            d2 = new Date(strEndDate);
        }

        // Check to see if the end date is after the start date
        if(bInputValid && (d1 > d2)) {
            // If not then display error
            $endDate.addClass("input-error");

            // Set the validity to false
            bInputValid = false;

            // Set error message
            strErrorMessage += "End date must be after start date.  ";
        }

        // Check to see if the workgroups are set
        if(arrWorkgroups === null) {
            // If not then display error
            $(".workgroups span.select2-selection--multiple").addClass("input-error");

            // Set validity flag to false
            bInputValid = false;

            // Display error
            strErrorMessage += "Please select at least 1 workgroup.";
        }

        // Set the error message in the error box
        $(".error-box").text(strErrorMessage);

        return bInputValid;
    };


    /**
     *  Clear all of the errors and hide the error message box.
     */
    var clearErrors = function () {

        var $errorBox = $(".error-box");
        $errorBox.hide();
        $errorBox.text("");

        // Remove all error
        $("#start_date").removeClass("input-error");
        $("#end_date").removeClass("input-error");
        $(".workgroups span.select2-selection--multiple").removeClass("input-error");
    };


    /**
     * Sort function
     *
     * @param a
     * @param b
     * @returns {number}
     * @constructor
     */

    var SortByName = function (a, b) {
        var aName = a.name.toLowerCase();
        var bName = b.name.toLowerCase();

        return ((aName < bName) ? -1 : ((aName > bName) ? 1 : 0));
    };

    return {
        showWorkgroups: showWorkgroups,
        showDetailedReport: showDetailedReport,
        showSummaryReport: showSummaryReport,
        SortByName: SortByName
    };
}();