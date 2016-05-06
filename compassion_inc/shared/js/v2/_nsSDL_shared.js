var SDL = SDL || {};
var onloadMap = {};
var realOnload = window.onload;



SDL.shared = (function() {

	$(window).resize(function() {
        if(this.resizeTO) clearTimeout(this.resizeTO);
        this.resizeTO = setTimeout(function() {
            $(this).trigger('resizeEnd');
        }, 100);
    });

	var isTransport = location.href.indexOf('transport') > 0; 
	// --- private --- 	
	
	var requestURL;
	var serverURL;
	
	window.onload = function() {
		if (realOnload != null)
			realOnload();
			
			console.log(1);
		customOnload();
	}
	
	var customOnload  = function() {
		console.log(onloadMap);
		$.each(onloadMap, function(id,customOnloadFunc) {			
			customOnloadFunc();
		});
	}
	var buildURL = function(cmd) {
	
		if (requestURL == null)
			requestURL = getParameter('servlet');

		var persisttoken = getParameter('persisttoken');			
		var retURI = (serverURL == null ? '' : serverURL) + (isTransport ? '../' : '') + 'ws_ext?servlet='+ requestURL +'&token=' + getToken() + '&command=' + cmd + '&random=' + new Date().getTime() + (persisttoken != null ? '&persisttoken=' + persisttoken : '');		
		
		return retURI;
	}

	var getParameters = function(additionalParams) {
		var params = {};

		if (additionalParams != null)
			$.extend(params, additionalParams);

		return params;
	}
	// --- end private ---
	
	// --- public ---
	var getToken = function() {
	
		var tokenId = '&token=';
		var tokenIdx = location.href.indexOf(tokenId);

		var endIdx = location.href.indexOf('&', tokenIdx + 1);
		if (endIdx < 0) endIdx = location.href.indexOf('#', tokenIdx + 1);
		
		if (endIdx < 0) endIdx = location.href.length;

		if (tokenIdx < 0) {
			return -1;
		}

		return location.href.substring(tokenIdx + tokenId.length, endIdx);
	
	}
	
	var getParameter = function(param) {

		var paramId = param +'=';
		var paramIdx = location.href.indexOf(paramId);
		var endIdx = location.href.indexOf('&', paramIdx + 1);
		
		
		if (endIdx < 0) endIdx = location.href.indexOf('#', paramIdx + 1);
		
		if (endIdx < 0) endIdx = location.href.length;

		if (paramIdx < 0) {
			return -1;
		}

		return location.href.substring(paramIdx + paramId.length, endIdx);
	
	}
	
	var executeWSRequest = function(cmd, rFunc, additionalParams, bCacheDate) {
		
		$.ajaxSetup({ cache: true });
		
		var url = buildURL(cmd);
		
		var error = false;
	
		$.ajax({	
			url: url,			
			data: getParameters(additionalParams), 
			contentType: 'application/json; charset=utf-8',
			dataType: (additionalParams['contentType'] != null ? additionalParams['contentType'].split('/')[1] : 'json'),
			success: function(result) {            				
				if (rFunc != null) rFunc(true,result);
			},
			error: function(jqxhr, status, error) {
				//if (rFunc != null) rFunc(jqxhr);
                if (rFunc != null) rFunc(false,error);
                //alert('Critical error during operation: '+ cmd +': '+ error);
                errorHandler(cmd,error);

                console.log(jqxhr);
				console.log(status);
				console.log(error);
			}}			
		);
	}		
	
	var executeWSRequestSendData = function(cmd, rFunc, additionalParams, data) {
		
		$.ajaxSetup({ cache: false });
		var queryStringParams = $.param(additionalParams);
		console.log(data);
		
		var url = buildURL(cmd);
		
		var error = false;
	
		$.ajax({	
			url: url + '&' + queryStringParams,
			contentType: 'application/json; charset=utf-8',
			data: data,
			dataType: 'json',
			//dataType: (additionalParams['contentType'] != null ? additionalParams['contentType'].split('/')[1] : 'json'),
			success: function(result) {
				if (rFunc != null) rFunc(true,result);
			},
			type: 'POST',
			processData: false,
			error: function(jqxhr, status, error) {
                if (rFunc != null) rFunc(false,error);
                //alert('Critical error during '+ cmd +': '+ error);
                errorHandler(cmd,error);
				console.log(jqxhr);
				console.log(status);
				console.log(error);
			}}			
		);
	}		
	
	var executeWSUpload = function (uploadFiles, rFunc, additionalParams) {

	if (uploadFiles == null || uploadFiles.length == 0) {
		rFunc();
		return;
	}
	// build a new form, with attribute names WS can use because of how
	// it handles multi-part requests
	var xhrUploadEntry = new FormData();
	var idx = 0;

	// populate the form with new attributes
	$.each(getParameters(additionalParams), function(k,v) {		

			console.log(additionalParams[k].length);
			if ($.isArray(v)) {
				$.each(v, function(idx,entry) {
					xhrUploadEntry.append(k+'[]',entry);
				});
			} else {
				xhrUploadEntry.append(k,v);
			}			
	});
	
	
	$.each(uploadFiles, function(i,file) {
			xhrUploadEntry.append('uploadedFile_' + idx++, file);
	});
	
	// XHR upload requests with new format data and attached files
	$.ajax({
		url: 'ws_ext?servlet=xhr_upload&token=' + SDL.shared.getToken(),
		data: xhrUploadEntry,
		cache: false,
		contentType: false, // form-data object supercedes this value
		processData: false,
		type: 'POST',
		success: function(data){	
			console.log(data);
			rFunc($.parseJSON(data));
		}
	});	

}
		
	var registerOnload  = function(id, func) {	
		onloadMap[id] = func;		
	}
	
	
	jQuery.fn['hasValue'] = function() {
		return (this.length > 0);
	};

	
	// a few places need special handling for IE; this is the recommend detection provide by MSFT
	var isIE = function () {
    var rv = -1; // Return value assumes failure.
    if (navigator.appName == 'Microsoft Internet Explorer') {
        var ua = navigator.userAgent;
        var re = new RegExp("MSIE ([0-9]{1,}[\\.0-9]{0,})");
        if (re.exec(ua) != null)
            rv = parseFloat(RegExp.$1);
    }
    return rv != -1;
}

function getString(id) {
	
	id = id.replace(/\./g,'\\.');	
    var str = $('#str\\.' + id).attr('value');
	
    if (str == null) str = id;
    return str;
}
	
	// --- end public ---
	
	
	// --- expose public ---
	
	var cachedToken = getToken();
    var errorHandler = function(cmd,error) {alert('Critical error during operation: '+ cmd +': '+ error);}
	
	return {
		getString: getString,
		getToken: function() {return cachedToken},
		getParameters: getParameters,
		getParameter: getParameter,
		executeWSRequest: executeWSRequest,
		executeWSUpload: executeWSUpload,
		executeWSRequestSendData: executeWSRequestSendData,
		registerOnload: registerOnload,
		isIE: isIE,
        setErrorHandler: function(errorHandlerToSet) {errorHandler = errorHandlerToSet},
		setRequestURL: function(urlToSet) { requestURL = urlToSet; },
		setServerURL: function(serverUrlToSet) { serverURL = serverUrlToSet; }
	};
	// --- expose public ---
	
	
})();





