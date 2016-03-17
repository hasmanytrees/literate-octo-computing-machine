/**
 * Translator Payment Report
 *
 * Created by cslack on 10/5/2015.
 */


var SDL = SDL || {};

SDL.tPaymentBindings = (function() {

    /**
     * Action when detailed report button is clicked
     */
    var bindDetailedReportButton = function () {

        // Intercept "Detailed Report" button click
        $("#detailedReportButton").click(function(oEvent) {

            // Stop the button click from doing anything defaulty
            oEvent.preventDefault();

            var iStartDate = processDate($("#start_date").val());
            var iEndDate = processDate($("#end_date").val());
            var asWorkgroups = $("#workgroupSelect").val();

            // Display the report with detailed data and titles
            SDL.tPaymentCommands.showDetailedReport(iStartDate, iEndDate, asWorkgroups);

        });
    };


    /**
     * Action when summary report button is clicked
     */
    var bindSummaryReportButton = function() {

        // Intercept "Summary Report" button click
        $("#summaryReportButton").click( function(oEvent) {

            // Stop the button click from doing anything defaulty
            oEvent.preventDefault();

            var iStartDate = processDate($("#start_date").val());
            //var iStartDate = new Date($("#start_date").val()).getTime();
            var iEndDate = processDate($("#end_date").val());
            var asWorkgroups = $("#workgroupSelect").val();

            // Display the report with summary data and titles
            SDL.tPaymentCommands.showSummaryReport(iStartDate, iEndDate, asWorkgroups);

        });
    };


    /**
     * Closes Window
     */
    var bindCloseButton = function() {

        // Capture the close button click
        $("#closeButton").click(function(oEvent) {

            // Stop the button click from doing anything defaulty
            oEvent.preventDefault();

            // Close the window
            window.close();
        });
    };

    /**
     * Adds a day minus 1 second to the date so that we can have an inclusive date range
     * @param date - Date
     * @returns {number}
     */
    var processDate = function (date) {

        // Number of milliseconds in one day
        var iOneDay = 60*60*24*1000 - 1;

        // Return the Unix time stamp of the date, since the Javascript date in the format
        // we are using returns a time of 00:00:00 and we want to be inclusive of the date
        // we need to add one day minus one second so that we get a time of 23:59:59
        return Math.round((new Date(date).getTime() + iOneDay));
    };

    return {
        bindDetailedReportButton     : bindDetailedReportButton,
        bindSummaryReportButton      : bindSummaryReportButton,
        bindCloseButton              : bindCloseButton
    };

})();