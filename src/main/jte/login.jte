@import com.nimbusds.login.web.Messages

@param Messages msg
@param String actionUrl
@param String cancelUrl
@param boolean authenticationError = false
@param String username = null

@template.layout(title = msg.get("login.title"), msg = msg, content = @`
	<div class="row voffset6">
		<div class="col-md-12 text-center">
			<h1>${msg.get("login.title")}</h1>
		</div>
	</div>
	<div class="row">
		<div class="col-sm-8 offset-sm-2 voffset3 text-center">
			@if(authenticationError)
				<div class="alert alert-danger">
					<p><strong>${msg.get("error.authentication")}</strong></p>
				</div>
			@endif
		</div>
	</div>
	<div class="row">
		<div class="col-sm-6 offset-sm-3">
			<form class="form" role="form" method="post" action="${actionUrl}">
				<div class="mb-3">
					<label for="username">${msg.get("login.form.username")}</label>
					<input type="text" class="form-control" id="username" name="username" value="${username}"
						placeholder="${msg.get("login.form.username.placeholder")}" required autofocus>
				</div>
				<div class="mb-3">
					<label for="password">${msg.get("login.form.password")}</label>
					<input type="password" class="form-control" id="password" name="password"
						placeholder="${msg.get("login.form.password.placeholder")}" required>
				</div>
				<p class="form-text">${msg.get("login.hint")}</p>
				<div class="row">
					<div class="col-6">
						<button type="submit" class="btn btn-primary w-100">${msg.get("global.login")}</button>
					</div>
					<div class="col-6">
						<a href="${cancelUrl}" class="btn btn-outline-primary w-100" role="button">${msg.get("global.cancel")}</a>
					</div>
				</div>
			</form>
		</div>
	</div>
`)
