var fExistingOnloadList = window.onload;
var strToken = getToken();

var strHead = '';
var strTail = '';

var strImageViewer_uiName = 'View Images...'; // should output localized?
var strImageViewer_servletName = 'imageviewer_ui&taskId=' + $('input[name="task_id"]').val();
var strImageViewer_Link =
    strHead +
    '' +
    ' <a href=\'#\' class="link_default" ' +
    ' onclick=\'handleButton(true, true, "ws_ext?servlet=' + strImageViewer_servletName + '&token=' + strToken + '","' + strImageViewer_servletName + '", "editorsize");\' ' +
    ' onmouseover=\'hideSubPopup(); window.status=\'' + strImageViewer_uiName + '\'; return true;\'' +
    ' onmouseout=\'window.status=\'\';>' +
    ''+ strImageViewer_uiName + '&nbsp;</a>&nbsp;&nbsp;' +
    strTail;

window.onload = function () {

    if (fExistingOnloadList != null) fExistingOnloadList();
    var htmlImageViewerAnchor = $j('a:contains("Viewer")');
    htmlImageViewerAnchor.before(strImageViewer_Link);

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


