/**
 * Translator Payment Report
 *
 * Created by cslack on 10/5/2015.
 */


var SDL = SDL || {};

SDL.tPaymentBindings = (function() {

    var aoCurrentDataSet = [];
    var asCurrentTitles = [];

    /**
     * Setter for titles
     *
     * @param asTitles
     */
    var setCurrentTitles = function(asTitles) {
        asCurrentTitles = asTitles;
    };

    /**
     * Setter for current data set
     * @param aData
     */
    var setCurrentDataSet = function(aData) {
        aoCurrentDataSet = aData;
    };


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
            var iEndDate = processDate($("#end_date").val());
            var asWorkgroups = $("#workgroupSelect").val();

            // Display the report with summary data and titles
            SDL.tPaymentCommands.showSummaryReport(iStartDate, iEndDate, asWorkgroups);

        });
    };


    /**
     * Action when Export to CSV button is clicked
     */
    var bindExportToCSVButton = function () {

        // Capture the "Generate CSV" button click
        $("#csvButton").click(function (oEvent) {

            // Stop the button click from doing anything defaulty
            oEvent.preventDefault();

            // Only do anything if a dataset is actually selected
            if(aoCurrentDataSet != null) {

                if ($("#csvDownload").length > 0) {
                    $("#csvDownload").remove();
                }

                // Set the header MIME type for the CSV file
                var strCSVContent = "data:text/csv;charset=utf-8,";

                // Add titles to CSV file, have to map the object values
                strCSVContent += asCurrentTitles.map(function (oElement) {
                        return oElement.title;
                    }).join(",") + "\n";

                // Add data to CSV file
                aoCurrentDataSet.forEach(function (dataArrayArg) {
                    var aoData = Array.prototype.slice.call(dataArrayArg);
                    strCSVContent += aoData.join(",") + "\n";
                });

                // Encode CSV for download
                var oEncodedUri = encodeURI(strCSVContent);

                // Create timestamp
                var oDate = new Date();
                var iMonth = oDate.getMonth() + 1;
                var iDay = oDate.getDate();
                var iYear = oDate.getFullYear();
                var iHours = oDate.getHours() + 1;
                var iMinutes = oDate.getMinutes();
                var strTimeStamp = iMonth + "-" + iDay + "-" + iYear + "_" + iHours + iMinutes;

                // Create filename
                var strFilename = "report_" + strTimeStamp + ".csv";

                $("body").append("<a href='" + oEncodedUri + "' id='csvDownload' style='display: none;' download='" + strFilename + "'>Test</a>");

                $("#csvDownload")[0].click();
            }
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
        bindExportToCSVButton        : bindExportToCSVButton,
        bindCloseButton              : bindCloseButton,
        setCurrentTitles             : setCurrentTitles,
        setCurrentDataSet            : setCurrentDataSet
    };

})();