@import com.nimbusds.login.web.Messages
@import java.util.Map

@param Messages msg
@param String logoutSid
@param String authSessionId = null
@param Map<String, Object> authenticatedUser = null
@param String clientName = null
@param boolean idTokenHintPresent = false
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath
@param String csrfToken = ""


@template.layout(title = msg.get("logout.title"), msg = msg, contextPath = contextPath,
	authSessionId = authSessionId, authenticatedUser = authenticatedUser,
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, csrfToken = csrfToken, content = @`
	<div class="login-page logout-page">
		<div class="login-intro text-center">
			<h1>${msg.get("logout.title")}</h1>
		</div>
		<div class="login-panel">
			<h2 class="h5">${msg.get("logout.prompt")}</h2>
			@if(clientName != null)
				<p class="form-text login-hint">
					<strong>${clientName}</strong>
				</p>
			@endif
			@if(authenticatedUser != null)
				<p class="form-text login-hint">
					@template.user(msg = msg, authSessionId = authSessionId, authenticatedUser = authenticatedUser,
						contextPath = contextPath, logoutUrl = null)
				</p>
			@endif
			@if(!idTokenHintPresent)
				<div class="alert alert-warning login-alert" role="alert">
					${msg.get("logout.invalid_logout_request")}
				</div>
			@endif
			<form method="post" action="${contextPath}/logout/confirm" class="login-form">
				<input type="hidden" name="_csrf" value="${csrfToken}">
				<input type="hidden" name="logoutSid" value="${logoutSid}">
				<div class="login-actions mt-4">
					<button type="submit" name="opLogout" value="true" class="btn btn-primary login-submit">
						${msg.get("global.yes")}
					</button>
					<button type="submit" name="opLogout" value="false" class="btn btn-outline-primary login-cancel">
						${msg.get("global.no")}
					</button>
				</div>
			</form>
		</div>
	</div>
`)
