var SDL = SDL || {};

SDL.imageviewer = (function () {
	
	var init = function() {	
		SDL.image.setZoomType('fit')
		SDL.image.setContrastRate(-40)
		SDL.transcription_io.setPageObject('OriginalLetterURL');		
	}
	
	
	return {
		init			: init		
	};
	// --- expose public ---
	
	
})();


SDL.shared.registerOnload('imageviewer', SDL.imageviewer.init);