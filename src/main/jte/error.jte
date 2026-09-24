@import com.nimbusds.login.web.Messages

@param Messages msg
@param String errorMessage = null
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("error.title"), msg = msg, contextPath = contextPath,
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	<div class="row mt-5">
		<div class="col-md-12 text-center">
			<h1>${msg.get("error.title")}</h1>
			@if(errorMessage != null)
				<div class="mt-4">
					<div class="alert alert-danger"><h4>${errorMessage}</h4></div>
				</div>
			@endif
		</div>
	</div>
`)
