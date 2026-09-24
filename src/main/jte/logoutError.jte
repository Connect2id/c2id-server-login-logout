@import com.nimbusds.login.web.Messages

@param Messages msg
@param String errorMessage = null
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("logout.title"), msg = msg, contextPath = contextPath,
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	<div class="login-page logout-page">
		<div class="login-panel">
			<h1 class="h3">${msg.get("logout.invalid_logout_request")}</h1>
			@if(errorMessage != null)
				<div class="alert alert-danger login-alert" role="alert">${errorMessage}</div>
			@endif
		</div>
	</div>
`)
