@import java.util.Map
@import com.nimbusds.login.web.Messages

@param String uri
@param Map<String, Object> form
@param Messages msg

<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<title>${msg.get("global.redirecting")}</title>
</head>
<body onload="document.getElementById('Form1').submit()">
	<form id="Form1" method="post" action="${uri}">
		@for(var entry : form.entrySet())
			<input type="hidden" name="${entry.getKey()}" value="${String.valueOf(entry.getValue())}">
		@endfor
		<noscript><input type="submit" value="Continue"></noscript>
	</form>
</body>
</html>
