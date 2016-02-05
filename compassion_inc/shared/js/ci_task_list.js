var fExistingOnloadList = window.onload;
var strToken = getToken();

var strHead = '<td nowrap="">';
var strTail = '</td>';

var strTranscription_uiName = 'Scanned Letter Editor'; // should output localized?
var strTranscription_servletName = 'transcription_ui';
var strTranscription_Link =
    strHead +
        '' +
        ' <a class=\'button button_in_table\' href=\'#\'' +
        ' onclick=\'handleButton(true, true, "ws_ext?servlet=' + strTranscription_servletName + '&token=' + strToken + '","' + strTranscription_servletName + '", "editorsize");\' ' +
        ' onmouseover=\'hideSubPopup(); window.status=\'' + strTranscription_uiName + '\'; return true;\'' +
        ' onmouseout=\'window.status=\'\';>' +
        ' &nbsp;' + strTranscription_uiName + '&nbsp;</a>' +
        strTail;

window.onload = function() {
	
    if (fExistingOnloadList != null) fExistingOnloadList();

    var htmlTranscriptionAnchor = $j('a[onmouseover*="Complete"]:first').parent();
    htmlTranscriptionAnchor.before(strTranscription_Link);
}

function getToken() {

    var strTokenId = '&token=';
    var iTokenIdx = location.href.indexOf(strTokenId);
    var iEndIdx = location.href.indexOf('&', iTokenIdx + 1);
    if (iEndIdx < 0) iEndIdx = location.href.length;

	
    if (iTokenIdx < 0) {
        return -1;
    }

    return location.href.substring(iTokenIdx + strTokenId.length, iEndIdx);
}


