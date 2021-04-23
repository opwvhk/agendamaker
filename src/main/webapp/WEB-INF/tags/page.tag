<%@ tag trimDirectiveWhitespaces="true" description="Page layout" %>
<%@ attribute name="title" required="true" description="The page title to use." %>
<%@ attribute name="extraHeader" fragment="true" description="Extra code to put before </head>" %>
<%@ attribute name="extraFooter" fragment="true" description="Extra code to put at the bottom of the page footer" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="Content-Type" content="application/xhtml+xml; charset=utf-8"/>
	<link rel="shortcut icon" href="<c:url value="/images/alliance.ico"/>"/>
	<link rel="stylesheet" href="<c:url value="/style/base.css"/>"/>
	<meta name="viewport" content="width=device-width, initial-scale=1.0, target-densitydpi=device-dpi"/>
	<meta http-equiv="Cache-Control" content="no-store"/>
	<title>${title}</title>
	<jsp:invoke fragment="extraHeader"/>
</head>
<body>
<header>
	<nav>
		<a href="<c:url value="/"/>">Home</a>
	</nav>
	${title}
</header>

<section>

	<jsp:doBody/>

</section>

<footer>
	<jsp:invoke fragment="extraFooter"/>
	&copy;2013 Oscar Westra van Holthe — Kind;
</footer>
</body>
</html>
