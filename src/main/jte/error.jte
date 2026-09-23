@import com.nimbusds.login.web.Messages

@param Messages msg
@param String errorMessage = null

@template.layout(title = msg.get("error.title"), msg = msg, content = @`
	<div class="row voffset6">
		<div class="col-md-12 text-center">
			<h1>${msg.get("error.title")}</h1>
			@if(errorMessage != null)
				<div class="voffset4">
					<div class="alert alert-danger"><h4>${errorMessage}</h4></div>
				</div>
			@endif
		</div>
	</div>
`)
