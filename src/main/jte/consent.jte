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
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("consent.title"), msg = msg, contextPath = contextPath,
	authSessionId = authSessionId, authenticatedUser = authenticatedUser,
	logoutUrl = contextPath + "/consent/logout",
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	<div class="row mt-2">
		<div class="col-md-12 text-center">

			@if(clientName != null)
				<h4 class="mt-4">
					<strong>${clientName}</strong> (#${clientId})
					${msg.get("consent.subtitle")}:
				</h4>
			@endif
		</div>
	</div>

	@if(!newScopes.isEmpty() || !consentedScopes.isEmpty())
		<div class="card text-white bg-dark mt-4">
			<div class="card-header">${msg.get("consent.scope")}</div>
			<div class="card-body">
				<div class="row g-4">
				@if(!newScopes.isEmpty())
					<div class="col-md-6">
						<strong>${msg.get("consent.new")}</strong>:
						<div class="mt-1">
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
					<div class="col-md-6">
						<strong>${msg.get("consent.previous")}</strong>:
						<div class="mt-1">
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
		</div>
	@endif

	@if(!newEssentialClaims.isEmpty() || !newVoluntaryClaims.isEmpty()
		|| !consentedEssentialClaims.isEmpty() || !consentedVoluntaryClaims.isEmpty())
		<div class="card text-white bg-dark mt-4">
			<div class="card-header">${msg.get("consent.claims")}</div>
			<div class="card-body">
				<div class="row g-4">
					<div class="col-md-6">
						<strong>${msg.get("consent.new")}</strong>
						@if(!newEssentialClaims.isEmpty())
							<div class="mt-2">
								<strong>${msg.get("consent.essential")}:</strong>
								<div class="mt-1">
									@for(var claim : newEssentialClaims)
										<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}"> <tt>${claim}</tt></label>
									@endfor
								</div>
							</div>
						@endif
						@if(!newVoluntaryClaims.isEmpty())
							<div class="mt-2">
								<strong>${msg.get("consent.voluntary")}:</strong>
								<div class="mt-1">
									@for(var claim : newVoluntaryClaims)
										<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}"> <tt>${claim}</tt></label>
									@endfor
								</div>
							</div>
						@endif
					</div>
					<div class="col-md-6">
						<strong>${msg.get("consent.previous")}</strong>
						@if(!consentedEssentialClaims.isEmpty())
							<div class="mt-2">
								<strong>${msg.get("consent.essential")}:</strong>
								<div class="mt-1">
									@for(var claim : consentedEssentialClaims)
										<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}" checked> <tt>${claim}</tt></label>
									@endfor
								</div>
							</div>
						@endif
						@if(!consentedVoluntaryClaims.isEmpty())
							<div class="mt-2">
								<strong>${msg.get("consent.voluntary")}:</strong>
								<div class="mt-1">
									@for(var claim : consentedVoluntaryClaims)
										<label class="form-check-label me-3"><input class="form-check-input me-1" type="checkbox" form="consentForm" name="claim" value="${claim}" checked> <tt>${claim}</tt></label>
									@endfor
								</div>
							</div>
						@endif
					</div>
				</div>
			</div>
		</div>
	@endif

	<div class="login-actions mt-4">
		<div>
			<form id="consentForm" method="post" action="${contextPath}/consent">
				<input type="hidden" name="authSessionId" value="${authSessionId}">
				<input type="hidden" name="scope" value="openid">
				<button type="submit" class="btn btn-primary login-submit w-100">${msg.get("consent.form.button")}</button>
			</form>
		</div>
		<div>
			<form method="post" action="${contextPath}/consent/deny">
				<input type="hidden" name="authSessionId" value="${authSessionId}">
				<button type="submit" class="btn btn-outline-primary login-cancel w-100">${msg.get("global.cancel")}</button>
			</form>
		</div>
	</div>
`)
