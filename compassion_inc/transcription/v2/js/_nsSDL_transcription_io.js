var SDL = SDL || {};

SDL.transcription_io = (function () {

    var xmlPayload;
    var iPageCount = 4;
    var iCurrentPage = -1;
    var iTaskId;
	var isLoading = false;
	
	var bAllPageMode = false;
	
	var pageObjectName = 'Pages';
	
	var allPageMode = function() {
		bAllPageMode = !bAllPageMode;
	}
	
    var nextPage = function () {
        page(iCurrentPage + 1);
    }

    var prevPage = function () {
        page(iCurrentPage - 1);

    }

    var page = function (iPageNumber) {
		if (isLoading)
			return;
		
		isLoading = true;	
		
		
		if (iPageNumber == null) iPageNumber = 0;		
		if (iCurrentPage != iPageNumber) {
			iPageNumber = parseInt(iPageNumber);		
			if (iPageNumber > iPageCount - 1) iPageNumber = 0;
			if (iPageNumber < 0) iPageNumber = iPageCount - 1;
		}
		
		var fCallback = function(bSuccess) {
						
				if (bSuccess) {
					$('.tabs:not(:eq(' + iPageNumber + '))').hide();
					$('.tabs:eq(' + iPageNumber + ')').show();					
					iCurrentPage = iPageNumber;
					var htmlTranscriptionTextArea = $('.tabs:visible').find('textarea');
					htmlTranscriptionTextArea.focus();
				}
				
				
				$('#page_info').text((bAllPageMode ? 'All Images / ' : '') +'Page ' + (bSuccess ? (parseInt(iPageNumber) + 1) : '?') + ' of ' + iPageCount);						
				
				
				isLoading = false;
			}
        
		
		
		var strImgURL = 'ws_ext?servlet=ajax_transcription_api' + 
							'&token=' + SDL.shared.getToken() + 
							'&command=getPageImage&taskId=' + iTaskId +
                            '&width=800&quality=20' +
							//'&dpi=200' +
                            '&page=' + (bAllPageMode ? -1 : iPageNumber) +
							'&download=yes'
		
        SDL.image.updateImage(
			strImgURL,
			$('#viewCanvas'),
			fCallback
		);


    }

    var getPageCount = function () {
        return iPageCount;
    }

    var load = function (iMyTaskId, callback) {

        iTaskId = iMyTaskId;

        SDL.shared.executeWSRequest('getPageData',
            function (success,xmlPageInfo) {
                if (success) {
                    iPageCount = $(xmlPageInfo).find(pageObjectName).length;
                    SDL.transcription.buildInput($(xmlPageInfo));
                    page();
                    if (callback != null) callback();
                }

            }, {
                taskId: iTaskId,
                contentType: 'application/xml'
            }
        );
    }

    var save = function (iTaskId, mapFields, callback) {
        
        SDL.shared.executeWSRequestSendData('setPageData',
            callback,
            { taskId: iTaskId    },
            JSON.stringify(mapFields.get())
        );


    }
	
	var setPageObject = function(name) {		
		pageObjectName = name;
	}

    return {
        load			: load,
        save			: save,
        nextPage		: nextPage,
        prevPage		: prevPage,
        page			: page,
		allPageMode		: allPageMode,
        getPageCount	: getPageCount,
		setPageObject	: setPageObject
    };
    // --- expose public ---


})();
