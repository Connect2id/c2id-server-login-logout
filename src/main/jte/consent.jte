@import com.nimbusds.login.web.Messages
@import java.util.List
@import java.util.Map

@param Messages msg
@param String authSessionId
@param Map<String, Object> authenticatedUser = null
@param String clientName = null
@param String clientId = null
@param List<String> newScopes
@param List<String> consentedScopes
@param List<String> newEssentialClaims
@param List<String> newVoluntaryClaims
@param List<String> consentedEssentialClaims
@param List<String> consentedVoluntaryClaims

@template.layout(title = msg.get("consent.title"), msg = msg, content = @`
	<div class="row voffset6">
		<div class="col-md-12 text-center">
			@if(authenticatedUser != null)
				<img class="rounded-circle" width="50" height="50" alt="Profile picture" src="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCABQAFADASIAAhEBAxEB/8QAGwABAAIDAQEAAAAAAAAAAAAAAAEGBAUHAgP/xAAtEAABAwIEBAYBBQAAAAAAAAABAAIDBBEFITGBEkFRYQYTQnGRwVIiMqGx8P/EABQBAQAAAAAAAAAAAAAAAAAAAAD/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwDreSlN03QRzU2TdN0CybL7QUdTOLwwSvb1a0kfOimejqYBeaCVg6uabfOiD4bJsm6boGybJum6AiIgK0YBgrPKbU1jeJzs2MIyA6nutBhcAqcRp4nD9Ln5jsM7fC6H7IAAAAFgAhAIsdFKIK3j2CMdE+po28L25uYBYO9h1vWXTVz3F4BTYlURNyaHZDoDnZBiIiICIiDMwaUQ4rTSO0D7Z8r5fa6DsuYjVXPAMWZVxMhncBUtyzP7+/ug3abKEQTsqBjkomxape0i3Fw362AH0rNj2LMo4nRQuDqlwtYem/NUo63OpQSiIgbpui9QxvmlZHGCXuNgEH2oKOaun8uAX5ucdArhh2CUtGA4tEs35vGh7DksjC6JlBStiZm7VzralZqBum6Ig1WI4LS1gc4NEUx9bBqe45qn19HNQz+XOLc2uGhVRFh4pRMr6V0T8natdbQoOf7puvU0b4ZXxyAh7TYheUBb/wAIUvmVMlQ4ZRjhb7nX/d1oFdPCkYZhLXfm9x+Db6QblETZAUKVCCVCnZQgqfi+l4KmOoaMpBwu9xp/H9Kvq6eK4+PCXOPoe1w3NvtUtB//2Q==">
				<h3 class="mt-2">${String.valueOf(authenticatedUser.get("name"))}</h3>
				<h5><tt>${String.valueOf(authenticatedUser.get("email"))}</tt></h5>
				<form method="post" action="/consent/logout" class="voffset1">
					<input type="hidden" name="authSessionId" value="${authSessionId}">
					<button type="submit" class="btn btn-link">${msg.get("global.logout")}</button>
				</form>
			@endif

			@if(clientName != null)
				<h4 class="voffset4">
					<strong>${clientName}</strong> (#${clientId})
					${msg.get("consent.subtitle")}:
				</h4>
			@endif
		</div>
	</div>

	@if(!newScopes.isEmpty() || !consentedScopes.isEmpty())
		<div class="card voffset4">
			<div class="card-header">${msg.get("consent.scope")}</div>
			<div class="card-body">
				@if(!newScopes.isEmpty())
					<div>
						<strong>${msg.get("consent.new")}</strong>:
						<div class="voffset1">
							@for(var scope : newScopes)
								@if(scope == "openid")
									<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="scope" value="${scope}" checked disabled> <tt>${scope}</tt></label>
								@else
									<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="scope" value="${scope}"> <tt>${scope}</tt></label>
								@endif
							@endfor
						</div>
					</div>
				@endif
				@if(!consentedScopes.isEmpty())
					<div class="voffset2">
						<strong>${msg.get("consent.previous")}</strong>:
						<div class="voffset1">
							@for(var scope : consentedScopes)
								@if(scope == "openid")
									<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="scope" value="${scope}" checked disabled> <tt>${scope}</tt></label>
								@else
									<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="scope" value="${scope}" checked> <tt>${scope}</tt></label>
								@endif
							@endfor
						</div>
					</div>
				@endif
			</div>
		</div>
	@endif

	@if(!newEssentialClaims.isEmpty() || !newVoluntaryClaims.isEmpty()
		|| !consentedEssentialClaims.isEmpty() || !consentedVoluntaryClaims.isEmpty())
		<div class="card voffset4">
			<div class="card-header">${msg.get("consent.claims")}</div>
			<div class="card-body">
				@if(!newEssentialClaims.isEmpty())
					<div>
						<strong>${msg.get("consent.new")}</strong> (${msg.get("consent.essential")}):
						<div class="voffset1">
							@for(var claim : newEssentialClaims)
								<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}"> <tt>${claim}</tt></label>
							@endfor
						</div>
					</div>
				@endif
				@if(!newVoluntaryClaims.isEmpty())
					<div class="voffset2">
						<strong>${msg.get("consent.new")}</strong> (${msg.get("consent.voluntary")}):
						<div class="voffset1">
							@for(var claim : newVoluntaryClaims)
								<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}"> <tt>${claim}</tt></label>
							@endfor
						</div>
					</div>
				@endif
				@if(!consentedEssentialClaims.isEmpty())
					<div class="voffset2">
						<strong>${msg.get("consent.previous")}</strong> (${msg.get("consent.essential")}):
						<div class="voffset1">
							@for(var claim : consentedEssentialClaims)
								<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}" checked> <tt>${claim}</tt></label>
							@endfor
						</div>
					</div>
				@endif
				@if(!consentedVoluntaryClaims.isEmpty())
					<div class="voffset2">
						<strong>${msg.get("consent.previous")}</strong> (${msg.get("consent.voluntary")}):
						<div class="voffset1">
							@for(var claim : consentedVoluntaryClaims)
								<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}" checked> <tt>${claim}</tt></label>
							@endfor
						</div>
					</div>
				@endif
			</div>
		</div>
	@endif

	<div class="row voffset4">
		<div class="col-6">
			<form id="consentForm" method="post" action="/consent">
				<input type="hidden" name="authSessionId" value="${authSessionId}">
				<input type="hidden" name="scope" value="openid">
				<button type="submit" class="btn btn-primary w-100">${msg.get("consent.form.button")}</button>
			</form>
		</div>
		<div class="col-6">
			<form method="post" action="/consent/deny">
				<input type="hidden" name="authSessionId" value="${authSessionId}">
				<button type="submit" class="btn btn-outline-primary w-100">${msg.get("global.cancel")}</button>
			</form>
		</div>
	</div>
`)
