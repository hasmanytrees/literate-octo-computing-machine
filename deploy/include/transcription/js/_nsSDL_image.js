var SDL = SDL || {};

SDL.image = (function () {

    var htmlImageObj = new Image();
	var strCurrentImgURL;
    var htmlCanvas;
    var htmlContext;
	var htmlRawImageData;
	var fLoadCallback;
	
    var bIsZoomed = false;
    var bEnhanced = false;	
	var bExpanded = true;
    
    var sharpen = function () {

        htmlRawImageData = htmlContext.getImageData(0, 0, $(htmlCanvas).attr('width'), $(htmlCanvas).attr('height'));

        var byteArrayPixels = htmlRawImageData.data;
        var iNumPixels = ((htmlRawImageData.width - 1) * 5 + ((htmlRawImageData.height) * 4));

        for (var i = 0, n = iNumPixels; i < n; i += 4) {

            var sharpen = (byteArrayPixels[i] * 0 + byteArrayPixels[i + 1] * 0 + byteArrayPixels[i + 2] * 0) +
                (byteArrayPixels[i] * -1 + byteArrayPixels[i + 1] * -1 + byteArrayPixels[i + 2] * -1) +
                (byteArrayPixels[i] * 0 + byteArrayPixels[i + 1] * 0 + byteArrayPixels[i + 2] * 0) +
                (byteArrayPixels[i] * -1 + byteArrayPixels[i + 1] * -1 + byteArrayPixels[i + 2] * -1) +
                (byteArrayPixels[i] * 5 + byteArrayPixels[i + 1] * 5 + byteArrayPixels[i + 2] * 5) +
                (byteArrayPixels[i] * -1 + byteArrayPixels[i + 1] * -1 + byteArrayPixels[i + 2] * -1) +
                (byteArrayPixels[i] * 0 + byteArrayPixels[i + 1] * 0 + byteArrayPixels[i + 2] * 0) +
                (byteArrayPixels[i] * -1 + byteArrayPixels[i + 1] * -1 + byteArrayPixels[i + 2] * -1) +
                (byteArrayPixels[i] * 0 + byteArrayPixels[i + 1] * 0 + byteArrayPixels[i + 2] * 0);

            byteArrayPixels[i] = sharpen; // red
            byteArrayPixels[i + 1] = sharpen; // green
            byteArrayPixels[i + 2] = sharpen; // blue
        }

    }

    var contrast = function (iVal) {
        iVal = iVal * -1;

        htmlRawImageData = htmlContext.getImageData(0, 0, $(htmlCanvas).attr('width'), $(htmlCanvas).attr('height'));
        var data = htmlRawImageData.data;

        for (var i = 0; i < data.length; i += 4) {
            data[i] += (255 - data[i]) * iVal / 255;            // red
            data[i + 1] += (255 - data[i + 1]) * iVal / 255;    // green
            data[i + 2] += (255 - data[i + 2]) * iVal / 255;    // blue
        }
    }


    var enhance = function (bApplyExisting) {

        if (bApplyExisting) {
            if (bEnhanced) _enhance();
        } else {
            if (!bEnhanced) {
                _enhance();
            } else {
                refreshImage();
            }
            bEnhanced = !bEnhanced;
        }
    }

    var reapply = function (strExcept) {
        if (strExcept != 'enhance') enhance(true);
        if (strExcept != 'zoom') zoom(true);
    }

    var _enhance = function () {
        sharpen();
        contrast(300);
        refreshImage();
    }
	
	var expand = function () {
		$('#left').css('width',  (bExpanded ? '100%' : '70%'));
		if (bExpanded) { $('#right').hide(); } else { $('#right').show(); }		 
		bExpanded = !bExpanded;
		$(window).resize();
	}

    var zoom = function (bApplyExisting) {

		if (!bApplyExisting) bIsZoomed = !bIsZoomed;
		
		var fitToWidth = $('#imageScroller').width() / htmlImageObj.naturalWidth;
		var fitToHeight = $('#imageScroller').height() / htmlImageObj.naturalHeight;

		var zoom  = (bIsZoomed ? fitToWidth : (fitToHeight > fitToWidth / 2 ? fitToHeight : fitToWidth / 2));		
        var relativePosition = $('#imageScroller').prop("scrollHeight") / $('#imageScroller').scrollTop();
		        
        $(htmlCanvas).attr('width', htmlImageObj.naturalWidth*zoom);
        $(htmlCanvas).attr('height', htmlImageObj.naturalHeight*zoom);

        htmlContext.scale(zoom,zoom);

        refreshImage();
        enhance(true);
		
		var newScrollPosition =  ($('#imageScroller').prop("scrollHeight") / relativePosition);// + $('#imageViewer').height()/2;		
		if(!bApplyExisting) $('#imageScroller').scrollTop(newScrollPosition);

    }
	
	var mapImageCache = {};

    var updateImage = function (imgURL, myhtmlCanvas, fCallback) {
	strCurrentImgURL = imgURL;
		
        
		setHtmlCanvas(myhtmlCanvas);
		
		if (mapImageCache[imgURL] != null) {		
			
			htmlImageObj = mapImageCache[imgURL];
			$('#imageLoading').show();
			fLoadCallback = fCallback;
			imageLoad();
		} else if (imgURL != null && htmlImageObj.src != imgURL) {
			
			htmlImageObj = new Image();
			strCurrentImgURL = imgURL;
			
			$('#imageLoading').show();
			fLoadCallback = fCallback;
			
			htmlImageObj.onload = function () {	
				imageLoad();
			};
		
			htmlImageObj.onerror = function() {							
				imageError();
			}
			
            htmlImageObj.src = imgURL;								
			
        } else {
            reapply();
        }
		
    }

    var setHtmlCanvas = function (htmlCanvasToSet) {
        htmlCanvas = htmlCanvasToSet[0];
        htmlContext = htmlCanvas.getContext("2d");
    }

    var refreshImage = function () {
        if (htmlRawImageData != null) {
            htmlContext.putImageData(htmlRawImageData, 0, 0);
            htmlRawImageData = null;
        } else {
            htmlContext.drawImage(htmlImageObj, 0, 0, htmlImageObj.naturalWidth, htmlImageObj.naturalHeight);
        }
    }
	
	var init = function() {
		// ignore for now
	}
	
	var imageLoad = function() {
		mapImageCache[strCurrentImgURL] = htmlImageObj;
		
		$(htmlCanvas).attr('width', htmlImageObj.naturalWidth);
		$(htmlCanvas).attr('height', htmlImageObj.naturalHeight);
		
		htmlContext.drawImage(htmlImageObj, 0, 0, htmlImageObj.naturalWidth, htmlImageObj.naturalHeight);
		
		
		reapply();
		$('#imageScroller').hide();
		$('#imageScroller').show();
		$('#imageLoading').hide();
		
		if (fLoadCallback != null) fLoadCallback(true);
	}
	
	var imageError = function() {
		$(htmlCanvas).attr('width', 0);
		$(htmlCanvas).attr('height', 0);
		$('#imageLoading').hide();
		SDL.transcription.displayStatus('Failed to load image!');
		if (fLoadCallback != null) fLoadCallback(false);
	}

    return {
        zoom		: zoom,
        contrast	: contrast,
        enhance		: enhance,
        sharpen		: sharpen,
		expand		: expand,
        updateImage	: updateImage,
		init		: init
    };
    // --- expose public ---


})();

SDL.shared.registerOnload('image', SDL.image.init);