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
	authenticatedUser = authenticatedUser, contextPath = contextPath, imagesPath = imagesPath,
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
				@if(!logoutPrompt)
					<form method="post" action="${contextPath}/logout" class="mt-2">
						<button type="submit" class="btn btn-info">${msg.get("global.logout")}</button>
					</form>
				@else
					<h4 class="mt-3">${msg.get("logout.prompt")}</h4>
					<form method="post" action="${contextPath}/logout/confirm" class="d-inline">
						<button type="submit" class="btn btn-success">${msg.get("global.yes")}</button>
					</form>
					<form method="post" action="${contextPath}/logout/cancel" class="d-inline">
						<button type="submit" class="btn btn-danger">${msg.get("global.no")}</button>
					</form>
				@endif
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
