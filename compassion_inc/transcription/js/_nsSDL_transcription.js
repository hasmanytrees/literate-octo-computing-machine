var SDL = SDL || {};

SDL.transcription = (function() {

	var timerClickTypeControl = null;
	var iSelectedTaskId;

	var enumPosition = {
		LEFT: 	0,
		MIDDLE: 1,
		RIGHT: 	2
	};

	var enumClickType = {
		SINGLE: 	0,
		DOUBLE: 	1
	};

	var enumKeyboardShortcut = {
		TAB_NEXT	: 0,
		TAB_PREV	: 1,
		PAGE_NEXT	: 2,
		PAGE_PREV	: 3,
		PAGE_ALL	: 4,
		IMG_ZOOM	: 5,
		IMG_EXPAND	: 6,
		IMG_ENHANCE	: 7,
		SCROLL_UP	: 8,
		SCROLL_DOWN	: 9
		
	};
	
	var getSelectedTask = function() {				
		var selectedTasks = window.opener.$('input:checkbox:checked[name="checkbox"]');
		if (selectedTasks.length == 1) iSelectedTaskId = selectedTasks.val();
		
		return selectedTasks;
	}

	var init = function() {

		SDL.shared.setRequestURL('ajax_transcription_api');
	
		if (window.opener == null) {
			showCriticalError('Could not detect selected task or opening window. Please close this window.');
			return;
		}
		
		if (getSelectedTask().length != 1) {
			showCriticalError('Please select a single task');
			return;
		}

		$('.menu').hide();

        SDL.shared.executeWSRequest('getCheckAccess',
            function (response) {
				
				if (response.status != null && response.status != 200) {
					showCriticalError(response.responseText);
					return;
				}
					
				SDL.transcription_io.load(iSelectedTaskId);
				bindEvents();

            }, 
			{ taskId: iSelectedTaskId }
        );

	}
	
	var showCriticalError = function(strErrorMessage) {
		 $('body').html('<div style="color: red;"><i class="fa fa-exclamation-circle"></i>' + strErrorMessage + '</div>');
	}

	var bindEvents = function() {

		bindEvents_buttons();
		bindEvents_viewer();
		bindEvents_keyboard();
		$('textarea').on('focus',function() {updateProgress(); save(); });
	}

	var updateProgress  = function() {

		var iCompleteTranscriptions = $('i.statusOK').length;
		var iRemainingTranscriptions = $('i.statusPending').length;
		var iTotalTranscriptions = iRemainingTranscriptions + iCompleteTranscriptions;

		$('#transcriptionProgress').html('<span class="'+ (iCompleteTranscriptions == iTotalTranscriptions ? 'done' : 'pending' )+'">' + iCompleteTranscriptions + ' of ' + iTotalTranscriptions  + ' Complete</span>');
	}

	var bindEvents_keyboard  = function() {

		// transcription entry navigation
		Mousetrap.bind('ctrl+down', 	function(e) { 	return keyboardEvent( enumKeyboardShortcut.TAB_NEXT);  		});
		Mousetrap.bind('ctrl+up', 		function(e) { 	return keyboardEvent( enumKeyboardShortcut.TAB_PREV);  		});
		Mousetrap.bind('ctrl+left', 	function(e) { 	return keyboardEvent( enumKeyboardShortcut.PAGE_PREV); 		});
		Mousetrap.bind('ctrl+right', 	function(e) { 	return keyboardEvent( enumKeyboardShortcut.PAGE_NEXT); 		});
		Mousetrap.bind('ctrl+shift+a', 	function(e) {	return keyboardEvent( enumKeyboardShortcut.PAGE_ALL); 		});
		Mousetrap.bind('ctrl+enter', 	function(e) { 	return keyboardEvent( enumKeyboardShortcut.CONFIRM);   		});

		// image options
		Mousetrap.bind('ctrl+e', 		function(e) {	return keyboardEvent( enumKeyboardShortcut.IMG_ENHANCE);	});
		Mousetrap.bind('ctrl+z', 		function(e) { 	return keyboardEvent( enumKeyboardShortcut.IMG_ZOOM); 		});
		Mousetrap.bind('ctrl+x', 		function(e) { 	return keyboardEvent( enumKeyboardShortcut.IMG_EXPAND);		});

		// image navigation
		Mousetrap.bind('alt+up', 		function(e) { 	return keyboardEvent( enumKeyboardShortcut.SCROLL_UP); 		});
		Mousetrap.bind('alt+down', 		function(e) { 	return keyboardEvent( enumKeyboardShortcut.SCROLL_DOWN);	});

		Mousetrap.bind('alt+left', 		function(e) { 	return false; });
		Mousetrap.bind('alt+right', 	function(e) { 	return false; });
		Mousetrap.bind('alt+s+p+i+n+1', 	function(e) { 	$('span').addClass('fa-spin'); return false; /* for testing */ });

	}

	var keyboardEvent = function(iKeyboardEventType) {
		var htmlTab = $('.ui-tabs-active');

		switch (iKeyboardEventType) {

			case enumKeyboardShortcut.TAB_NEXT:
				(htmlTab.next('li').length != 0 ? htmlTab.next('li') :  $('li:first')).find('a').click();
				break;
			case enumKeyboardShortcut.TAB_PREV:
				(htmlTab.prev('li').length != 0 ? htmlTab.prev('li') :  $('li:visible(:first)')).find('a').click();
				break;
			case enumKeyboardShortcut.PAGE_NEXT:
				$('#btn_pageNext').click();
				break;
			case enumKeyboardShortcut.PAGE_PREV:
				$('#btn_pageBack').click();
				break;
			case enumKeyboardShortcut.PAGE_ALL:
				$('#btn_pageAll').click();
				break;
				
			case enumKeyboardShortcut.CONFIRM:

				if (htmlTab.find('i').hasClass('statusOK')) {

					if ($('div.tabs * .statusPending').length > 0) {
						var iNextPage = $('div.tabs * .statusPending:first').parents('div.tabs').attr('id').split('_')[1];
						SDL.transcription_io.page(parseInt(iNextPage));
						$('i.statusPending:first').prev('a').click();
					} else {
						displayStatus('All Complete ...');						
					}

				}
				break;
			case enumKeyboardShortcut.IMG_ENHANCE:
				$('#btn_enhance').click();
				break;
			case enumKeyboardShortcut.IMG_ZOOM:
				$('#btn_zoom').click();
				break;
			case enumKeyboardShortcut.IMG_EXPAND:
				$('#btn_expand').click();
				break;
			case enumKeyboardShortcut.SCROLL_UP:
				$('#imageScroller').scrollTop($('#imageScroller').scrollTop()-30);
				break;
			case enumKeyboardShortcut.SCROLL_DOWN:
				$('#imageScroller').scrollTop($('#imageScroller').scrollTop()+30);
				break;

		}

		$('textarea:visible').focus();
		//$('textarea:visible').val($('textarea:visible').val()); 
		updateProgress();
		return false; // prevents default event
	}
	
	var displayStatus  = function(strMsg) {
		
		$('#statusEntry').html(strMsg);
		$('#statusEntry').show();
		$('#statusEntry').fadeOut(3500);

	}

	var bindEvents_buttons  = function() {
		
		
		
		$('#btn_expand').on('click', function(e) {
			SDL.image.expand();
			e.preventDefault();
		});
		
		$('#btn_enhance').on('click', function(e) {
			SDL.image.enhance();
			e.preventDefault();
		});

		$('#btn_zoom').on('click', function(e) {
			SDL.image.zoom();
			e.preventDefault();
		});

		$('#btn_pageNext').on('click', function(e) {
			SDL.transcription_io.nextPage();
		});

		$('#btn_pageBack').on('click', function(e) {
			SDL.transcription_io.prevPage();
		});
		
		$('#btn_pageAll').on('click', function(e) {
			SDL.transcription_io.allPageMode();
			SDL.transcription_io.page(); 
		});
		
		$('#btn_help').on('click', function(e) {
			($('#help').is(':visible') ? $('#help').hide()  : $('#help').show());
			
			//alert('Not implemented');
		});
		
		$('#btn_save').on('click', function(e) {
		
			save();
		
			if ($('i.statusPending').length == 0) {
			
				var timerLaunchComplete = setTimeout(function(){ 
					window.opener.$('a[onmouseover*="Complete"]:first').click();
					window.close();			
				},500);
			}

			
		});
		
		$('#btn_close').on('click', function(e) {						
			window.close();			
		});
	}

	var save = function() {
		displayStatus('Saving ... ');
		var mapFields = $('textarea').map(function() { return $(this).val(); } )
		SDL.transcription_io.save(iSelectedTaskId,mapFields);
		
		
	}

	var bindEvents_viewer  = function() {
	
		$(window).bind('resizeEnd', function() { SDL.image.zoom(true); });

		//$('#imageViewer').on('mouseover', function(e) {$('.menu').fadeIn();});
		$('#imageViewer').on('mouseleave', function(e) {$('.menu').hide();});
		$('#imageScroller').on('mousedown', function(e){ e.preventDefault(); }, false);
		$('#imageScroller').on('dblclick', function(e) {
			checkClickType(enumClickType.DOUBLE, function() {$('#btn_enhance').click();});
		});
		
		$('#imageViewer').on('mousemove', function(e) {
			var iPosition = getPosition($('#imageViewer'), $(this), e);

			switch(iPosition) {
				case enumPosition.LEFT :					
					if ($('.menu').is(':hidden')) $('.menu').fadeIn();
					return false;
				default:
					$('.menu').hide();
					return false;
			}

		});

		$('#imageScroller').on('click', function(e) {

			var iPosition = getPosition($('#imageScroller'), $(this), e);
			
			checkClickType(enumClickType.SINGLE,
				function() { 
				
					switch(iPosition) {
						case enumPosition.LEFT :
							$('#btn_pageBack').click();
							return;
						case enumPosition.RIGHT :
							$('#btn_pageNext').click();
							return;
						case enumPosition.MIDDLE :
							$('#btn_zoom').click();							
							return;
					}	
				}
			);

			

		});

	}

	var checkClickType = function(iClickType, fDoSomething) {

		if (iClickType == enumClickType.SINGLE) {
			if (timerClickTypeControl != null)
				return;

			timerClickTypeControl = setTimeout(function(){ //create a timer
				timerClickTypeControl = null;
				fDoSomething();

			 },350);

		} else if (iClickType == enumClickType.DOUBLE) {
			clearTimeout(timerClickTypeControl);
			timerClickTypeControl = null;
			fDoSomething();
		}


	}

	var getPosition = function(jqHtmlComponent, eventParent, jsEvent) {

			var iViewerWidth = jqHtmlComponent.width();
			var iParentOffset = eventParent.offset();
			var iRelX = jsEvent.pageX - iParentOffset.left;

			if (iRelX < iViewerWidth / 4)  {
				return enumPosition.LEFT;
			}

			if (iRelX > (iViewerWidth - iViewerWidth / 4)) {
				return enumPosition.RIGHT;
			}

			return enumPosition.MIDDLE;
	}



	var buildInput = function(xmlPayload) {

		var htmlTranscriptionTabBase = $('.tabs').clone(true);
		$('.tabs').remove();

		var xmlNodeListPages = xmlPayload.find('Pages');
		var iPageCount = xmlNodeListPages.length;

		xmlNodeListPages.each(function(pageIdx) {

			var htmlPageTab = htmlTranscriptionTabBase.clone(true);
			htmlPageTab.attr('id','page_' + pageIdx);
			var xmlPage = $(this);

			var strImageURL = xmlPage.find('OriginalPageURL');
			var nlToTranscribe = xmlPage.find('OriginalText');

			if (nlToTranscribe.length == 0) {
				htmlPageTab.append('none');
				$('#transcriptionInput').append('<div class="tabs" style="text-align: center;">No transcription</div>');
				return true;
			}

			nlToTranscribe.each(function(tabIdx) {

				var existingValue = $(this).text();

				var arrayStrBuilderTabs = [];
				var arrayStrBuilderInputs = [];

				var strTabId= 'tabs' + pageIdx + '_' + tabIdx;
				arrayStrBuilderTabs.push(
					'<li>' +
					'<a class="contentTab" id="content_' + strTabId +'" href="#' + strTabId + '">' +
						'Text Field #' + (tabIdx+1) +
					'</a>' +
					'	<i class="statusIcon"></i>' +
					'</li>'
				);

				arrayStrBuilderInputs.push(
					'<div id="' + strTabId + '">' +
					'	<textarea placeholder="Please enter a translation">' + (existingValue != null ? existingValue : '') + '</textarea>' +
					'</div>'
				);

				htmlPageTab.find('ul').append(arrayStrBuilderTabs.join('\n'));
				htmlPageTab.find('#transcriptionEntry').append(arrayStrBuilderInputs.join('\n'));

				$('#transcriptionInput').append(htmlPageTab);

			});
		});

		$('.tabs:not(:first)').hide();

		initUIJqueryUI();		
		updateProgress();
		
		$('textarea').addClass('mousetrap');
		$('textarea:visible').focus();
		$('textarea').each(function() {setStatusIcon($(this));});//todo
		
		$('#description').text(
			xmlPayload.find('CompassionSBCId').text() + ' | ' +
			xmlPayload.find('Direction').text() + ' | ' +
			xmlPayload.find('OriginalLanguage').text()
		);
		
		addMetadata(xmlPayload);

		return true;
	}
	
	var addMetadata = function(xmlPayload) {
		
		var htmlMetadataViewport =  $('#metadata');
		
		arrayStrBuilderMetadata = [];
		
		arrayStrBuilderMetadata.push('<table>');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Name','Beneficiary Name');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Gender','Beneficiary Gender');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Age','Beneficiary Age');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Original Source','OriginalLanguage');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Translation Target','TranslationLanguage');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'SBC Type','SBCTypes');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Relationship Type','RelationshipType');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Supporter Flagged for Content?','MandatoryReviewRequired');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Attachment Not Mailed','ItemNotScannedNotEligible');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Field Office','FieldOffice');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Direction','Direction');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Template','Template');
			addMetadataRow(arrayStrBuilderMetadata,xmlPayload,'Pages','NumberOfPages');
		arrayStrBuilderMetadata.push('</table>');
		htmlMetadataViewport.append(arrayStrBuilderMetadata.join('\n'));
	
	}

	var addManualDataRow = function(arrayStrBuilderMetadata,strKey, strValue) {
		arrayStrBuilderMetadata.push('	<tr><td>' + strKey + '</td><td>' + strValue + '</td><tr>');
	}


	var addMetadataRow = function(arrayStrBuilderMetadata,xmlPayload,strKey,strValue) {
		arrayStrBuilderMetadata.push('	<tr><td>' + strKey + '</td><td>' + xmlPayload.find(strValue+':first').text() + '</td><tr>');
	}

	var setStatusIcon = function(jqTextArea) {
		var strTabId = jqTextArea.parent().attr('id');
		$('#content_' + strTabId).next('i').attr('class', (jqTextArea.val().length > 0 ? 'statusOK fa fa-check-circle-o' : 'fa fa-question statusPending'));
		updateProgress();
	}

	var initUIJqueryUI = function() {
		$( ".tabs" ).tabs().addClass( "ui-tabs-vertical ui-helper-clearfix" );
		$( ".tabs li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" );
		$('textarea').on('input propertychange', _.debounce( function() {save($(this))}, 5000)); // handles typed input and paste
		$('textarea').on('input propertychange', _.debounce( function() {setStatusIcon($(this)); }, 500)); // handles typed input and paste
		$('.contentTab').on('click', function() {
			//$('textarea:visible').focus();
			$(this).parent().parent().parent().find('textarea').focus(); //todo
		});
	}

	return {
		init			: init,
		displayStatus	: displayStatus,
		buildInput		: buildInput
	};
	// --- expose public ---

})();

SDL.shared.registerOnload('main', SDL.transcription.init);

	