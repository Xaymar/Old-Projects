<?php
	$pAction = $_REQUEST['action'];
	
	// Check for missing folders.
	if (!file_exists("./cache")) { mkdir("./cache"); }
	if (!file_exists("./cache/ip")) { mkdir("./cache/ip"); }
	if (!file_exists("./cache/channel")) { mkdir("./cache/channel"); }
	
	// Clean up old files.
	if ($handle = opendir('./cache/ip')) {
		while (false !== ($file = readdir($handle))) {
			if ($file != "." && $file != ".."
				&& ((filemtime("./cache/ip/$file") + 180) - time() < 0)) {
				unlink("./cache/ip/$file");
			}
		}
		closedir($handle);
	}
	if ($handle = opendir('./cache/channel')) {
		while (false !== ($file = readdir($handle))) {
			if ($file != "." && $file != ".."
				&& ((filemtime("./cache/channel/$file") + 600) - time() < 0)) {
				unlink("./cache/channel/$file");
			}
		}
		closedir($handle);
	}
	
	switch($pAction) {
		case "url":
			$cacheURL = "./cache/twitch.tv";
			
			if (!file_exists($cacheURL) || ((time() > filemtime($cacheURL)+900))) {
				$headers = get_headers("http://www-cdn.jtvnw.net/widgets/live_embed_player.swf?channel=", true);
				if ($headers && isset($headers['Location'])) {
					file_put_contents($cacheURL, substr($headers["Location"], 0, strpos($headers["Location"], "?channel=")+9)."##CHANNEL##", LOCK_EX);
				}
			}
			
			$cacheURLTime = (filemtime($cacheURL)+900)-time();
			header("Expires: " . date(DATE_RFC822,strtotime("$cacheURLTime seconds")));
			header("Cache-Control: max-age=$cacheURLTime, must-revalidate");
			echo file_get_contents($cacheURL);
			break;
		case "data":
			if (isset($_REQUEST['channel'])) {
				$pChannel = urlencode($_REQUEST['channel']);
				
				$cacheIP = "./cache/ip/".$_SERVER['REMOTE_ADDR'];
				$cacheChannel = "./cache/channel/".$pChannel;
					
				if (file_exists($cacheChannel) && (time() < (filemtime($cacheChannel)+600))) {
					$cacheChannelTime = (filemtime($cacheChannel)+600)-time();
					header("Expires: " . date(DATE_RFC822,strtotime("$cacheChannelTime seconds")));
					header("Cache-Control: max-age=$cacheChannelTime, must-revalidate");
					echo file_get_contents($cacheChannel);
				} else {
					if (file_exists($cacheIP) && (time() < (filemtime($cacheIP)+180))) {
						$cacheIPTime = (filemtime($cacheIP)+180)-time();
						header("HTTP/1.0 429 Too Many Requests");
						header("Expires: " . date(DATE_RFC822,strtotime("$cacheIPTime seconds")));
						header("Cache-Control: max-age=$cacheIPTime, must-revalidate");
						echo $cacheIPTime;
					} else {
						header("Expires: " . date(DATE_RFC822,strtotime("10 minutes")));
						header("Cache-Control: max-age=600, must-revalidate");
						$channelData = file_get_contents("http://usher.twitch.tv/find/".$pChannel.".xml?type=any");
						if ($channelData != "") {
							$channelData = str_replace("</240p>", "</q240p>", str_replace("<240p>", "<q240p>", $channelData));
							$channelData = str_replace("</360p>", "</q360p>", str_replace("<360p>", "<q360p>", $channelData));
							$channelData = str_replace("</480p>", "</q480p>", str_replace("<480p>", "<q480p>", $channelData));
							$channelData = str_replace("</720p>", "</q720p>", str_replace("<720p>", "<q720p>", $channelData));
							$xmlData = @xml2array($channelData);
							$jsonData = json_encode(convert($xmlData), JSON_FORCE_OBJECT);
							file_put_contents($cacheChannel, $jsonData, LOCK_EX);
							file_put_contents($cacheIP, "", LOCK_EX);
							echo $jsonData;
						}
					}
				}
			} else {
				header("HTTP/1.0 400 Bad Request");
				echo "Missing channel.";
			}
			break;
	}
	
	function convert($arrayXML) {
		$nArray = array();
		foreach($arrayXML as $key => $value) {
			$tag = (isset($value["tag"]) ? $value["tag"] : $key);
			if ($tag == "NODE" || $tag == "NEEDED_INFO" || $tag == "META_GAME"
				|| $tag == "VIDEO_HEIGHT" || $tag == "BITRATE" || $tag == "BROADCAST_PART"
				|| $tag == "PERSISTENT" || $tag == "CLUSTER" || $tag == "TOKEN" || $tag == "BROADCAST_ID") {
				continue;
			}
			
			if (isset($value["value"])) {
				$nArray[$tag] = $value["value"];
			} else if (is_array($value)) {
				$nArray[$tag] = convert($value);
			}
		}
		return $nArray;
	}
	
	function xml2array($xml){
		$opened = array();
		$opened[1] = 0;
		$xml_parser = xml_parser_create();
		xml_parse_into_struct($xml_parser, $xml, $xmlarray);
		xml_parser_free($xml_parser);
		$array = array_shift($xmlarray);
		unset($array["level"]);
		unset($array["type"]);
		$arrsize = sizeof($xmlarray);
		for($j=0;$j<$arrsize;$j++){
			$val = $xmlarray[$j];
			switch($val["type"]){
				case "open":
					$opened[$val["level"]]=0;
				case "complete":
					$index = "";
					for($i = 1; $i < ($val["level"]); $i++)
						$index .= "[" . $opened[$i] . "]";
					$path = explode('][', substr($index, 1, -1));
					$value = &$array;
					foreach($path as $segment)
						$value = &$value[$segment];
					$value = $val;
					unset($value["level"]);
					unset($value["type"]);
					if($val["type"] == "complete")
						$opened[$val["level"]-1]++;
				break;
				case "close":
					$opened[$val["level"]-1]++;
					unset($opened[$val["level"]]);
				break;
			}
		}
		return $array;
	} 
?>