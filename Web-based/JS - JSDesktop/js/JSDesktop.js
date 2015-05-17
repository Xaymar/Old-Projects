// Enable binary notation.
Object.defineProperty(Number.prototype, 'b', {set:function(){return false;},get:function(){return parseInt(this, 2);}});

/* Set up usuable flags*/
JSFlags = new Array(
	Window = new Array(
		"Visible"		=    1..b,	//Window is visible.
		"Maximized"		=   10..b,	//Window is maximized.
		"Minimized"		=  100..b,	//Window is minimized.
		"Active"		= 1000..b,	//Window is active/top-most.
		
		"ThinBorder"	=    10000..b, // Activates ThinBorder, if used in conjuction with Border will activate ThickBorder.
		"Border"		=   100000..b, // Activates Border, if used in conjuction with ThinBorder will activate ThickBorder.
		
		"Titlebar"		=  
		
		// Titlebar Features
		"Titlebar"		= ,
		"Icon"			= ,
		"Draggable"		= ,
		
		// Internal Window States
		"Active"		= ,
	)
);

/* JSDesktop Class */
/** Creates a new desktop.
 * @param elElement Object: Either a parent or the desktop itself.
 * @param tIsElementParent Boolean: Weether or not given element is the parent.
 */
function JSDesktop(elElement, tIsElementParent = false) {
	// Prevent invalid use and crashes.
	if (typeof(elElement) != "object") {
		throw "elElement needs to be of type 'object'";
	} else {
		if (typeof(tIsElementParent) != "boolean") {
			throw "tIsElementParent needs to be of type 'boolean'";
		} else {
			console.group(this); {
				// Set a (virtual) type.
				this.type = "JSDesktop";
				
				// Check what the given element is.
				if (tIsElementParent) { // Given element is parent.
					console.debug("Recieved parent element, creating child...");
					this.oElement = $(document.createElement("div"));
					$(elElement).append(this.oElement);
				} else { // Given element is us.
					console.debug("Recieved our element...");
					this.oElement = $(elElement);
				}
				console.debug("Setting element class to include JSDesktop...");
				this.oElement.addClass("JSDesktop");
				
				// Windows
				console.debug("Creating window list...");
				this.oWindowList = new Array();
			} console.groupEnd();
		}
	}
}

/** Creates a new window and returns it's handle.
 * @param iPosX Number: Position in pixels from left border of parent.
 * @param iPosY Number: Position in pixels from top border of parent.
 * @param iSizeX Number: Width in pixels.
 * @param iSizeY Number: Height in pixels.
 * @param tFlagsArr Array(Boolean): Array of Flags to apply to the window.
 */
JSDesktop.prototype.createWindow = function(iPosX, iPosY, iSizeX, iSizeY, tFlagsArr) {
	var oWindow = new JSWindow(this.oElement);
	oWindow.setPosition(iPosX, iPosY);
	oWindow.setSize(iSizeX, iSizeY);
	oWindow.setFlags(tFlagsArr);
	
	this.oWindowList.push(oWindow); // Add the window to the desktop window list.
	
	return oWindow;
}

/* JSWindow Class */
/** Creates a new window inside a parent.
 * @param elParent Object: Parent Window or Desktop Element.
 */
function JSWindow(elParent) {
	// Prevent invalid use and crashes.
	if (typeof(elParent) != "object") {
		throw "elParent needs to be of type jQuery Element";
	} else {
		console.group(this); {
			// Set a (virtual) type.
			this.type = "JSWindow";
			
			// Remember our parent
			this.oParent = $(elParent);
			
			// Initialize variables
			this.iPosition	= new Array(2);
			this.iSize		= new Array(2);
			this.iFlags		= new Array();
			
			// Create our window elements.
			console.debug("Creating window element...");
			this.oElement = $(document.createElement("div"));
			this.oElement.addClass("JSWindow");
			this.oParent.append(this.oElement);
			console.group(this.oElement); {
				console.debug("Creating border element...");
				this.oElementBorder = $(document.createElement("div"));
				this.oElementBorder.addClass("JSBorder");
				this.oElement.append(this.oElementBorder);
				console.group(this.oElementBorder); {
					console.debug("Creating titlebar element...");
					this.oElementTitlebar = $(document.createElement("div"));
					this.oElementTitlebar.addClass("JSTitlebar");
					this.oElementBorder.append(this.oElementTitlebar);
					console.group(this.oElementTitlebar); {
						console.debug("Creating icon element...");
						this.oElementTitlebarIcon = $(document.createElement("img"));
						this.oElementTitlebarIcon.addClass("JSIcon");
						this.oElementTitlebar.append(this.oElementTitlebarIcon);
						
						console.debug("Creating title element...");
						this.oElementTitlebarTitle = $(document.createElement("div"));
						this.oElementTitlebarTitle.addClass("JSTitle");
						this.oElementTitlebar.append(this.oElementTitlebarTitle);
						
						console.debug("Creating control elements...");
						this.oElementTitlebarControls = $(document.createElement("div"));
						this.oElementTitlebarControls.addClass("JSControls");
						this.oElementTitlebar.append(this.oElementTitlebarControls);
					} console.groupEnd();
				} console.groupEnd();
			} console.groupEnd();
		} console.groupEnd();
	}
}

/** Sets the windows position.
 * @param iPositionX Number: Position from left border of parent in pixels.
 * @param iPositionY Number: Position from top border of parent in pixels.
 */
JSWindow.prototype.setPosition = function(iPositionX, iPositionY) {
	this.iPosition[0] = (typeof(iPositionX) == "number" ? iPositionX : this.iPosition[0]);
	this.iPosition[1] = (typeof(iPositionY) == "number" ? iPositionY : this.iPosition[1]);
	
	// Apply the css changes
	this.oElement.css("top", this.iPosition[0] + "px");
	this.oElement.css("left", this.iPosition[1] + "px");
}

/** Gets the windows position.
 * @return Array(Number, Number): Position from left and top border of parent in pixels.
 */
JSWindow.prototype.getPosition = function() {
	return [this.iPosition[0], this.iPosition[1]];
}

/** Sets the windows size.
 * @param iSizeX Number: Width in pixels.
 * @param iSizeY Number: Height in pixels.
 */
JSWindow.prototype.setSize = function(iSizeX, iSizeY) {
	this.iSize[0] = (typeof(iSizeX) == "number" ? iSizeX : this.iSize[0]);
	this.iSize[1] = (typeof(iSizeY) == "number" ? iSizeY : this.iSize[1]);
	
	// Apply the css changes
	this.oElement.css("width", this.iSize[0] + "px");
	this.oElement.css("height", this.iSize[1] + "px");
}

/** Gets the windows size.
 * @return Array(Number, Number): Width and Height in pixels.
 */
JSWindow.prototype.getSize = function() {
	return [this.iSize[0], this.iSize[1]];
}

JSWindow.prototype.setFlags = function(tFlagsArr) {
	// Prevent invalid use and crashes.
	if (Array.isArray(tFlagsArr)) {
		
	}
}


/* Prevent non-Firebug users from getting errors */
if (typeof('console') == 'undefined') {
	console = {};
	console.log = function(){};
	console.debug = function(){};
	console.info = function(){};
	console.warn = function(){};
	console.error = function(){};
	console.assert = function(){};
	console.clear = function(){};
	console.dir = function(){};
	console.dirxml = function(){};
	console.trace = function(){};
	console.group = function(){};
	console.groupCollapsed = function(){};
	console.groupEnd = function(){};
	console.time = function(){};
	console.timeEnd = function(){};
	console.timeStamp = function(){};
	console.profile = function(){};
	console.profileEnd = function(){};
	console.count = function(){};
	console.exception = function(){};
	console.table = function(){};
}

console.log("Hello?");

/* Seting up Desktops */
$(document).ready(function(event) {
	console.group(this);
	console.debug("Finding JSDesktops...");
	var elDesktops = $("#JSDesktop");
	elDesktops.each(function(iIndex, elDesktop) {
		console.debug("Starting Desktop '" + elDesktop.getAttribute("name") + "(" + iIndex + ")'...");
		var oDesktop = new JSDesktop(elDesktop);
		oDesktop.createWindow(80, 100, 400, 200);
	});
	console.groupEnd();
});
