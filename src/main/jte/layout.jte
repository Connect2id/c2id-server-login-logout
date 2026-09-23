@import gg.jte.Content
@import com.nimbusds.login.web.Messages

@param String title
@param Messages msg
@param Content content

<!DOCTYPE html>
<html lang="en">
<head>
	<meta charset="utf-8">
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>${title}</title>
	<link rel="icon" href="/favicon.ico" type="image/x-icon">
	<link rel="stylesheet" href="/webjars/bootstrap/5.3.8/css/bootstrap.min.css">
	<link rel="stylesheet" href="/css/main.css">
</head>
<body>
<div class="container">
${content}
</div>
</body>
</html>
