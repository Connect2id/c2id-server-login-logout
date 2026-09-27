@import com.nimbusds.login.web.Messages
@import java.util.Map
@import com.nimbusds.login.web.Utils

@param Messages msg
@param String authSessionId
@param Map<String, Object> authenticatedUser = null
@param String contextPath
@param String logoutUrl

@if(authenticatedUser != null)
	<div class="user-card">
		<div class="user-avatar" aria-hidden="true">
			@if(authenticatedUser.get("picture") != null
				&& (String.valueOf(authenticatedUser.get("picture")).startsWith("https://")
					|| String.valueOf(authenticatedUser.get("picture")).startsWith("http://")
					|| String.valueOf(authenticatedUser.get("picture")).startsWith("data:image/")))
				<img src="${String.valueOf(authenticatedUser.get("picture"))}" alt="Profile picture">
			@else
				<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" width="50" height="50">
					<path d="M12,19.2C9.5,19.2 7.29,17.92 6,16C6.03,14 10,12.9 12,12.9C14,12.9 17.97,14 18,16C16.71,17.92 14.5,19.2 12,19.2M12,5A3,3 0 0,1 15,8A3,3 0 0,1 12,11A3,3 0 0,1 9,8A3,3 0 0,1 12,5M12,2A10,10 0 0,0 2,12A10,10 0 0,0 12,22A10,10 0 0,0 22,12C22,6.47 17.5,2 12,2Z" />
				</svg>
			@endif
		</div>
		<div class="user-details">
			<div class="user-name">${String.valueOf(authenticatedUser.get("name"))}</div>
			@if(authenticatedUser.get("email") != null)
			<div class="user-email">${Utils.renderString(authenticatedUser.get("email"))}</div>
			@endif
			<form method="post" action="${logoutUrl}" class="user-logout">
				<input type="hidden" name="authSessionId" value="${authSessionId}">
				@if(logoutUrl != null)
				<button type="submit" class="btn btn-link">${msg.get("global.logout")}</button>
				@endif
			</form>
		</div>
	</div>
@endif
