/**
 * Translator Payment Report
 *
 * Created by cslack on 1/26/2016.
 */

var SDL = SDL || {};

SDL.translatorPayment = (function() {

	/**
	 * Initialize the translator UI
	 */
	var init = function() {

		// Setup nice date selection using jQueryUI
		$("#start_date").datepicker();
		$("#end_date").datepicker();

		// Format workgroup selection as select2.js multi-select
		$("#workgroupSelect").select2({
			placeholder: "Select FO / GP"
		});

		// Hide error messages by default
		$(".error-box").hide();

		// Set endpoint for connection
		SDL.shared.setRequestURL('translator_payment_ajax_commands');

		// Populate the Workgroups multi-select box
		SDL.tPaymentCommands.showWorkgroups();

		// Bind action to the Detailed Report Button
		SDL.tPaymentBindings.bindDetailedReportButton();

		// Bind action the Summary Report Button
		SDL.tPaymentBindings.bindSummaryReportButton();

		// Bind action to the Export to CSV Button
		SDL.tPaymentBindings.bindExportToCSVButton();

		// Bind action to the Close Button
		SDL.tPaymentBindings.bindCloseButton();
	};

	return {
		init              : init
	};

})();


/**
 * Run this when the document is ready
 */
$(document).ready(function() {

	// Initialize the Translator Payment Report
	SDL.translatorPayment.init();
});