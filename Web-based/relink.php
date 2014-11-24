<?php
	$ref = parse_url($_SERVER['HTTP_REFERER']);
	if ((preg_match("/alteredgaming.de$/", $ref['host']))
	 || (preg_match("/alters2.com$/", $ref['host']))
	 || (preg_match("/levelnull.de$/", $ref['host']))) {
		echo "<!DOCTYPE html><head><meta http-equiv='refresh' content='0; ".$_REQUEST['loc']."'></head></html>";
	}
?>