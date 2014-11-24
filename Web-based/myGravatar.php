<?php
	header("HTTP/1.1 303 See Other");
	@header("Location: http://www.gravatar.com/avatar/".md5(strtolower(trim($_REQUEST['email']))));
?>