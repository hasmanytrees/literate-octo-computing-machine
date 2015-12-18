var SDL = SDL || {};

SDL.transcription_help = (function () {

	var init = function() {
		
		//$('#help').removeCss('visibility','');
		$('#help').hide();
		$('#helpClose').on('click', function() {$('#help').hide();});
	}
	
	
    return {
        init		: init
    };
    // --- expose public ---

})();

SDL.shared.registerOnload('help', SDL.transcription_help.init); 
