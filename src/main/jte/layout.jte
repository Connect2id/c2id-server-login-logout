@import gg.jte.Content
@import com.nimbusds.login.web.Messages
@import java.util.Map

@param String title
@param Messages msg
@param Content content
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@param String authSessionId = null
@param Map<String, Object> authenticatedUser = null
@param String logoutUrl = null

<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>${title}</title>
	<link rel="icon" href="${contextPath}/favicon.ico" type="image/x-icon">
	<link rel="stylesheet" href="${contextPath}/webjars/bootstrap/5.3.8/css/bootstrap.min.css">
	<link rel="stylesheet" href="${stylesPath}/main.css">
</head>
<body>
<header class="auth-header">
	<div class="container auth-header-inner">
		<a class="auth-brand" href="${contextPath}/" aria-label="${msg.get("global.connect2idHome")}">
			<img src="${imagesPath}/connect2id-logo.svg" width="158" height="19" alt="${msg.get("global.connect2idLogo")}">
		</a>
		@if(authenticatedUser != null)
			@template.user(msg = msg, authSessionId = authSessionId, authenticatedUser = authenticatedUser,
				contextPath = contextPath, logoutUrl = logoutUrl)
		@else
			<span class="auth-header-label">${msg.get("global.sampleLoginPage")}</span>
		@endif
	</div>
</header>
<main class="auth-main">
<div class="container">
${content}
</div>
</main>
</body>
</html>
