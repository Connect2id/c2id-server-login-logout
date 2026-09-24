@import com.nimbusds.login.web.Messages
@import java.util.Map

@param Messages msg
@param Map<String, Object> authenticatedUser = null
@param boolean logoutPrompt = false
@param boolean logoutEnd = false
@param String logoutError = null
@param String contextPath
@param String imagesPath
@param String stylesPath
@param String scriptsPath

@template.layout(title = msg.get("global.title"), msg = msg, contextPath = contextPath,
	imagesPath = imagesPath, stylesPath = stylesPath, scriptsPath = scriptsPath, content = @`
	<div class="row voffset6">
		<div class="col-md-12 text-center">
			@if(authenticatedUser != null)
				<h1>${msg.get("logout.title")}</h1>
				<img class="rounded-circle voffset2" width="50" height="50" alt="Profile picture"
						 src="data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCABQAFADASIAAhEBAxEB/8QAGwABAAIDAQEAAAAAAAAAAAAAAAEGBAUHAgP/xAAtEAABAwIEBAYBBQAAAAAAAAABAAIDBBEFITGBEkFRYQYTQnGRwVIiMqGx8P/EABQBAQAAAAAAAAAAAAAAAAAAAAD/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwDreSlN03QRzU2TdN0CybL7QUdTOLwwSvb1a0kfOimejqYBeaCVg6uabfOiD4bJsm6boGybJum6AiIgK0YBgrPKbU1jeJzs2MIyA6nutBhcAqcRp4nD9Ln5jsM7fC6H7IAAAAFgAhAIsdFKIK3j2CMdE+po28L25uYBYO9h1vWXTVz3F4BTYlURNyaHZDoDnZBiIiICIiDMwaUQ4rTSO0D7Z8r5fa6DsuYjVXPAMWZVxMhncBUtyzP7+/ug3abKEQTsqBjkomxape0i3Fw362AH0rNj2LMo4nRQuDqlwtYem/NUo63OpQSiIgbpui9QxvmlZHGCXuNgEH2oKOaun8uAX5ucdArhh2CUtGA4tEs35vGh7DksjC6JlBStiZm7VzralZqBum6Ig1WI4LS1gc4NEUx9bBqe45qn19HNQz+XOLc2uGhVRFh4pRMr6V0T8natdbQoOf7puvU0b4ZXxyAh7TYheUBb/wAIUvmVMlQ4ZRjhb7nX/d1oFdPCkYZhLXfm9x+Db6QblETZAUKVCCVCnZQgqfi+l4KmOoaMpBwu9xp/H9Kvq6eK4+PCXOPoe1w3NvtUtB//2Q==">
				<h3 class="mt-2">${String.valueOf(authenticatedUser.get("name"))}</h3>
				<h5><tt>${String.valueOf(authenticatedUser.get("email"))}</tt></h5>
				@if(logoutError != null)
					<div class="alert alert-danger voffset2">
						<p>${msg.get("logout." + logoutError)}</p>
					</div>
				@endif
				@if(!logoutPrompt)
				<form method="post" action="${contextPath}/logout" class="voffset2">
						<button type="submit" class="btn btn-info">${msg.get("global.logout")}</button>
					</form>
				@else
					<h4 class="voffset3">${msg.get("logout.prompt")}</h4>
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
					<div class="alert alert-success voffset5">
						<h4>${msg.get("logout.success")}</h4>
					</div>
				@else
					<div class="alert alert-danger voffset5">
						<h4>${msg.get("global.messages.badrequest")}</h4>
					</div>
				@endif
			@endif
		</div>
	</div>
`)
