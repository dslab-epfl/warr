//Very cut-down and slightly modified (generally to remove context) from
//$WEBDRIVER_HOME/firefox/src/extension/components/utils.js

function Utils() {
}

function getTextFromNode(node, toReturn, textSoFar, isPreformatted) {
    if (node['tagName'] && node.tagName == "SCRIPT") {
        return [toReturn, textSoFar];
    } else if (node['tagName'] && node.tagName == "TITLE") {
        return [textSoFar + node.text, ""];
    }
    var children = node.childNodes;

    for (var i = 0; i < children.length; i++) {
        var child = children[i];

        // Do we need to collapse the text so far?
        if (child["tagName"] && child.tagName == "PRE") {
            toReturn += collapseWhitespace(textSoFar);
            textSoFar = "";
            var bits = getTextFromNode(child, toReturn, "", true);
            toReturn += bits[1];
            continue;
        }

        // Or is this just plain text?
        if (child.nodeName == "#text") {
            if (Utils.isDisplayed(child)) {
                var textToAdd = child.nodeValue;
                textToAdd = textToAdd.replace(new RegExp(String.fromCharCode(160), "gm"), " ");
                textSoFar += textToAdd;
            }
            continue;
        }

        // Treat as another child node.
        var bits = getTextFromNode(child, toReturn, textSoFar, false);
        toReturn = bits[0];
        textSoFar = bits[1];
    }

    if (isBlockLevel(node)) {
        if (node["tagName"] && node.tagName != "PRE") {
            toReturn += collapseWhitespace(textSoFar) + "\n";
            textSoFar = "";
        } else {
            toReturn += "\n";
        }
    }
    return [toReturn, textSoFar];
}
;

function isBlockLevel(node) {
    if (node["tagName"] && node.tagName == "BR")
        return true;

    try {
        // Should we think about getting hold of the current document?
        return "block" == Utils.getStyleProperty(node, "display");
    } catch (e) {
        return false;
    }
};

Utils.isInHead = function(element) {
  while (element) {
    if (element.tagName && element.tagName.toLowerCase() == "head") {
      return true;
    }
    try {
      element = element.parentNode;
    } catch (e) {
      // Fine. the DOM has dispeared from underneath us
      return false;
    }
  }

  return false;
};

function getView(element){
  return element.ownerDocument.defaultView ? element.ownerDocument.defaultView : window;
}

Utils.isDisplayed = function(element) {
    // Ensure that we're dealing with an element.
    var el = element;
    while (el.nodeType != 1 && !(el.nodeType >= 9 && el.nodeType <= 11)) {
        el = el.parentNode;
    }

    // Hidden input elements are, by definition, never displayed
    if (el.tagName == "input" && el.type == "hidden") {
      return false;
    }

    var visibility = Utils.getStyleProperty(el, "visibility");

    var _isDisplayed = function(e) {
      var display = getView(e).getComputedStyle(e, null).getPropertyValue("display");
      if (display == "none") return display;
      if (e && e.parentNode && e.parentNode.style) {
        return _isDisplayed(e.parentNode);
      }
      return undefined;
    };

    var displayed = _isDisplayed(el);

    if (element.scrollIntoView && element.getBoundingClientRect &&
        element.tagName.toLowerCase() != "option") {
      //Option tags have 0-dimension bounding rects so ignore them
      element.scrollIntoView(true);
      var clientRect = element.getBoundingClientRect();
      if (clientRect.width == 0 || clientRect.height == 0) {
        return false;
      }
    }
    
    return displayed != "none" && visibility != "hidden";
};

Utils.getStyleProperty = function(node, propertyName) {
    if (!node)
      return undefined;

    var value = getView(node).getComputedStyle(node, null).getPropertyValue(propertyName);

    // Convert colours to hex if possible
    var raw = /rgb\((\d{1,3}),\s(\d{1,3}),\s(\d{1,3})\)/.exec(value);
    if (raw) {
        var temp = value.substr(0, raw.index);

        var hex = "#";
        for (var i = 1; i <= 3; i++) {
            var colour = (raw[i] - 0).toString(16);
            if (colour.length == 1)
                colour = "0" + colour;
            hex += colour
        }
        hex = hex.toLowerCase();
        value = temp + hex + value.substr(raw.index + raw[0].length);
    }

    if (value == "inherit" && element.parentNode.style) {
      value = Utils.getStyleProperty(node.parentNode, propertyName);
    }

    return value;
};

function collapseWhitespace(textSoFar) {
    return textSoFar.replace(/\s+/g, " ");
}

function getPreformattedText(node) {
    var textToAdd = "";
    return getTextFromNode(node, "", textToAdd, true)[1];
}

function isWhiteSpace(character) {
    return character == '\n' || character == ' ' || character == '\t' || character == '\r';
}

Utils.getText = function(element) {
    var bits = getTextFromNode(element, "", "", element.tagName == "PRE");
    var text = bits[0] + collapseWhitespace(bits[1]);
    var start = 0;
    while (start < text.length && isWhiteSpace(text[start])) {
        ++start;
    }
    var end = text.length;
    while (end > start && isWhiteSpace(text[end - 1])) {
        --end;
    }
    return text.slice(start, end);
};

/**
 * Fires the event using Utils.fireEvent, and if the event returned true,
 * perform callback, which will be passed on arguments
 * @param {!Element} element The element to fire the event on.
 * @param {string} type The type of event to fire.
 * @param {function()} callback The function to call if the fired event was not
 *     cancelled by an event listener.
 */
Utils.fireHtmlEventAndConditionallyPerformAction = function(element, type, callback) {
    Utils.fireHtmlEvent(element, type, function(response) {
      if (!response.statusCode) {
        callback();
      }
    });
};


/**
 * Fires a HTMLEvents category event on a DOM element.
 * @param {!Element} element The element to fire the event on.
 * @param {string} type The type of event to fire.
 * @param {function({statusCode:number, value:*})=} opt_callback Function to
 *     call when the script to fire the event has completed.
 */
Utils.fireHtmlEvent = function(element, type, opt_callback) {
    var callback = opt_callback || function() {};
    var args = [
      {"ELEMENT": addElementToInternalArray(element)},
      type
    ];

    // We need to do this because event handlers refer to functions that
    // the content script can't reah. See:
    // http://code.google.com/p/chromium/issues/detail?id=29071
    var script = "var e = document.createEvent('HTMLEvents'); "
      + "e.initEvent(arguments[1], true, true); " 
      + "return arguments[0].dispatchEvent(e);";

    execute_(script, args, -1, callback);
};


Utils.trim = function(str) {
    return str.replace(/^\s*/, "").replace(/\s*$/, "");
};
