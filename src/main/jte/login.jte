@import com.nimbusds.login.web.Messages
@import java.util.List
@import java.util.Map

@param Messages msg
@param String actionUrl
@param String cancelUrl
@param String authSessionId
@param boolean authenticationError = false
@param String username = null
@param String requiredSubject = null
@param boolean selectAccount = false
@param boolean createAccount = false
@param boolean accountSelectionError = false
@param List<Map<String, Object>> availableAccounts = null
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
				<div class="alert alert-info text-center" role="status">${msg.get("login.selectAccount")}</div>
			@endif
			@if(createAccount)
				<div class="alert alert-info text-center" role="status">${msg.get("login.createAccount")}</div>
			@endif
			@if(accountSelectionError)
				<div class="alert alert-danger login-alert text-center" role="alert">${msg.get("login.accountSelectionError")}</div>
			@endif
			@if(selectAccount)
				@if(availableAccounts != null && ((selectAccount && !availableAccounts.isEmpty()) || availableAccounts.size() > 1))
					<div class="account-list mb-4">
						@for(var account : availableAccounts)
							<form method="post" action="${contextPath}/login/select" class="account-option-form">
								<input type="hidden" name="authSessionId" value="${authSessionId}">
								<input type="hidden" name="slot" value="${String.valueOf(account.get("slot"))}">
								<button type="submit" class="account-option">
									@template.user(msg = msg, authSessionId = authSessionId, authenticatedUser = account, contextPath = contextPath, logoutUrl = null)
								</button>
							</form>
						@endfor
					</div>
				@endif
				<form method="post" action="${contextPath}/login/new-account" class="account-option-form">
					<input type="hidden" name="authSessionId" value="${authSessionId}">
					<button type="submit" class="btn btn-outline-primary login-cancel w-100 mt-4">
						${msg.get("login.newAccount")}
					</button>
				</form>
			@endif
			@if(authenticationError)
				<div class="alert alert-danger login-alert" role="alert">
					<p><strong>${msg.get("error.authentication")}</strong></p>
				</div>
			@endif
			@if(!selectAccount)
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
			@endif
		</div>
	</div>
`)
