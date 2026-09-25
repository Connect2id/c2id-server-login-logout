@import com.nimbusds.login.web.Messages
@import java.util.List

@param Messages msg
@param boolean sessionClosed = false
@param String redirectUrl = null
@param List<String> frontChannelLogoutUrls
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("logout.title"), msg = msg, contextPath = contextPath,
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	@if(frontChannelLogoutUrls != null)
		@for(var url : frontChannelLogoutUrls)
			<iframe src="${url}" title="${msg.get("logout.notification")}" class="logout-iframe"></iframe>
		@endfor
	@endif
	<div class="login-page logout-page">
		<div class="login-panel text-center">
			<h1 class="h3">${msg.get("logout.success")}</h1>
			@if(redirectUrl != null)
				<meta http-equiv="refresh" content="3;url=${redirectUrl}">
				<span class="form-text login-hint">
					<a href="${redirectUrl}">${redirectUrl}</a>
				</span>
			@else
				<span class="form-text login-hint">${msg.get("logout.closeWindow")}</span>
			@endif
		</div>
	</div>
`)
