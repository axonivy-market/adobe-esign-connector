<html>
<%@ page import="ch.ivyteam.ivy.page.engine.jsp.IvyJSP"%>
<jsp:useBean id="ivy" class="ch.ivyteam.ivy.page.engine.jsp.IvyJSP" scope="session"/>
<head>

<script type="text/javascript">

function signatureEnd() {
	parent.closeSigningDlg();
}

</script>

</head>
<body onload="signatureEnd()">

Signature end
</body>
</html>
