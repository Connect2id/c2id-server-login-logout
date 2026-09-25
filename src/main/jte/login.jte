@import com.nimbusds.login.web.Messages

@param Messages msg
@param String actionUrl
@param String cancelUrl
@param boolean authenticationError = false
@param String username = null
@param String requiredSubject = null
@param boolean selectAccount = false
@param boolean createAccount = false
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("login.title"), msg = msg, contextPath = contextPath,
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	<div class="login-page">
		<div class="login-intro text-center">
			<h1>${msg.get("login.title")}</h1>
		</div>
		<div class="login-panel">
			@if(selectAccount)
				<div class="alert alert-info login-alert" role="status">${msg.get("login.selectAccount")}</div>
			@endif
			@if(createAccount)
				<div class="alert alert-warning login-alert" role="status">${msg.get("login.createAccount")}</div>
			@endif
			@if(authenticationError)
				<div class="alert alert-danger login-alert" role="alert">
					<p><strong>${msg.get("error.authentication")}</strong></p>
				</div>
			@endif
			<form class="login-form" role="form" method="post" action="${actionUrl}">
				<div class="form-field">
					<label for="username">${msg.get("login.form.username")}</label>
					<input type="text" class="form-control" id="username" name="username" value="${username}"
						readonly="${requiredSubject != null}"
						placeholder="${msg.get("login.form.username.placeholder")}" required autofocus>
				</div>
				<div class="form-field">
					<label for="password">${msg.get("login.form.password")}</label>
					<input type="password" class="form-control" id="password" name="password"
						placeholder="${msg.get("login.form.password.placeholder")}" required>
				</div>
				<p class="form-text login-hint">${msg.get("login.hint")}</p>
				<div class="login-actions">
					<button type="submit" class="btn btn-primary login-submit">${msg.get("global.login")}</button>
					<a href="${cancelUrl}" class="btn btn-outline-primary login-cancel" role="button">${msg.get("global.cancel")}</a>
				</div>
			</form>
		</div>
	</div>
`)
