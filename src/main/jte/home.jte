@import com.nimbusds.login.web.Messages
@import java.util.Map

@param Messages msg
@param String authSessionId = null
@param Map<String, Object> authenticatedUser = null
@param boolean logoutPrompt = false
@param boolean logoutEnd = false
@param String logoutError = null
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("global.title"), msg = msg, authSessionId = authSessionId,
	authenticatedUser = authenticatedUser, logoutUrl = contextPath + "/logout/local", contextPath = contextPath, imagesPath = imagesPath,
	stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	<div class="row mt-5">
		<div class="col-md-12 text-center">
			@if(authenticatedUser != null)
				<h1>${msg.get("global.welcome", String.valueOf(authenticatedUser.get("name")))}</h1>

				@if(logoutError != null)
					<div class="alert alert-danger mt-2">
						<p>${msg.get("logout." + logoutError)}</p>
					</div>
				@endif
				<form method="post" action="${contextPath}/logout/local" class="mt-2">
						<button type="submit" class="btn btn-info">${msg.get("global.logout")}</button>
					</form>
			@else
				<h1>${msg.get("global.title")}</h1>
				@if(logoutEnd)
					<div class="alert alert-success mt-5">
						<h4>${msg.get("logout.success")}</h4>
					</div>
				@else
					<div class="alert alert-danger mt-5">
						<h4>${msg.get("global.messages.badrequest")}</h4>
					</div>
				@endif
			@endif
		</div>
	</div>
`)
