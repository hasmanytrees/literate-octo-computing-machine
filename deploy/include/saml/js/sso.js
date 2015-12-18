$(document).ready(function(){

    if (autoRedirect) {
        ssoLogin();
        return;
    }

    // add new SSO login button
    var loginWrapper = $('.login-button-wrapper');

    var ssoWrapper = loginWrapper.clone();
    ssoWrapper.attr('id','sso-login');
    loginWrapper.after(ssoWrapper);

    // replace button; can't change type, so just replace with new HTML
    ssoWrapper.children('#loginButton').remove();
    ssoWrapper.before('<p><br><em>For Internal Users:</em><p>');
    ssoWrapper.append('<button id="sso-login" type="button">CI Login</button>');

    // if sso call-back; associated server check will validate and provide
    // internal SSO result with secure parameters to automate login
    if (location.href.indexOf('sso') >= 0) {
        executeWSLogin();
    }

    // dispatch call to external SSO (e.g., Okta)
    $('#sso-login').on('click', function(){
        ssoLogin();
    });



});

function executeWSLogin() {

    $('#loginForm').hide();
    var ssoToken = ssoRawToken + '::' + ssoDate + '::' + ssoEncodedToken;

    $('input.user').val(ssoUser);
    $('input.password').val(ssoToken);

    var action = $('#loginForm').attr('action').replace('?sso','?');

    //todo: ------->
    $('#loginForm').attr('action', action);
    $('input.username').attr('type','text');
    $('input.password').after('<input class="ssoPassword" type="text" value="" id="ssoPassword" name="password">');
    $('input#ssoPassword').val(ssoToken);
    $('input.password').remove();
    //todo: --------^

    $('#loginButton').click();
}

function ssoLogin() {
    location.href = redirectTo;
}
 
 