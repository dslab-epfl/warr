// Copyright 2010 WebDriver committers
// Copyright 2010 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * @fileoverview DOM manipulation and querying routines.
 *
 */

goog.provide('bot.dom');

goog.require('bot');
goog.require('bot.locators.xpath');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.dom.NodeIterator');
goog.require('goog.dom.NodeType');
goog.require('goog.dom.TagName');
goog.require('goog.math.Rect');
goog.require('goog.math.Size');
goog.require('goog.string');
goog.require('goog.style');


/**
 * Retrieves the active element for a node's owner document.
 * @param {!(Node|Window)} nodeOrWindow The node whose owner document to get
 *     the active element for.
 * @return {Element} The active element, if any.
 */
bot.dom.getActiveElement = function(nodeOrWindow) {
  return goog.dom.getOwnerDocument(nodeOrWindow).activeElement;
};


/**
 * Returns whether the given node is an element and, optionally, whether it has
 * the given tag name. If the tag name is not provided, returns true if the node
 * is an element, regardless of the tag name.h
 *
 * @param {Node} node The node to test.
 * @param {goog.dom.TagName=} opt_tagName Tag name to test the node for.
 * @return {boolean} Whether the node is an element with the given tag name.
 */
bot.dom.isElement = function(node, opt_tagName) {
  return !!node && node.nodeType == goog.dom.NodeType.ELEMENT &&
      (!opt_tagName || node.tagName.toUpperCase() == opt_tagName);
};


/**
 * Common aliases for properties. This maps names that users use to the correct
 * property name.
 *
 * @const
 * @private
 */
bot.dom.PROPERTY_ALIASES_ = {
  'class': 'className',
  'readonly': 'readOnly'
};


/**
 * A list of boolean properties that are defined for all elements
 * according to the HTML5 spec. If any of these are missing when
 * calling 'getProperty' they default to false.
 *
 * http://dev.w3.org/html5/spec/Overview.html#elements-in-the-dom
 *
 * @const
 * @private
 */
bot.dom.BOOLEAN_PROPERTIES_ = [
  'checked',
  'disabled',
  'draggable',
  'hidden'
];


/**
 * Looks up the given property (not to be confused with an attribute) on the
 * given element. The following properties are aliased so that they return the
 * values expected by users:
 *
 * <ul>
 * <li>class - as "className"
 * <li>readonly - as "readOnly"
 * </ul>
 *
 * @param {!Element} element The element to use.
 * @param {string} propertyName The name of the property.
 * @return {*} The value of the property.
 */
bot.dom.getProperty = function(element, propertyName) {
  var key = bot.dom.PROPERTY_ALIASES_[propertyName] || propertyName;

  var value = element[key];
  if (!goog.isDef(value) &&
      goog.array.contains(bot.dom.BOOLEAN_PROPERTIES_, key)) {
      return false;
  }
  return value;
};


/**
 * Used to determine whether we should return a boolean value from getAttribute.
 * These are all extracted from the WHATWG spec:
 *
 *   http://www.whatwg.org/specs/web-apps/current-work/
 *
 * These must all be lower-case.
 *
 * @const
 * @private
 */
bot.dom.BOOLEAN_ATTRIBUTES_ = [
  'async',
  'autofocus',
  'autoplay',
  'checked',
  'compact',
  'complete',
  'controls',
  'declare',
  'defaultchecked',
  'defaultselected',
  'defer',
  'disabled',
  'draggable',
  'ended',
  'formnovalidate',
  'hidden',
  'indeterminate',
  'iscontenteditable',
  'ismap',
  'itemscope',
  'loop',
  'multiple',
  'muted',
  'nohref',
  'noresize',
  'noshade',
  'novalidate',
  'nowrap',
  'open',
  'paused',
  'pubdate',
  'readonly',
  'required',
  'reversed',
  'scoped',
  'seamless',
  'seeking',
  'selected',
  'spellcheck',
  'truespeed',
  'willvalidate'
];


/**
 * Get the user-specified value of the given attribute of the element, or null
 * if no such value. This method endeavours to return consistent values between
 * browsers. For boolean attributes such as "selected" or "checked", it returns
 * the string "true" if it is present and null if it is not. For the style
 * attribute, it standardizes the value to a lower-case string with a trailing
 * semi-colon.
 *
 * @param {!Element} element The element to use.
 * @param {string} attributeName The name of the attribute to return.
 * @return {?string} The value of the attribute or "null" if entirely missing.
 */
bot.dom.getAttribute = function(element, attributeName) {
  // Protect ourselves from the case where documentElementsByTagName also
  // returns comments in IE.
  if (goog.dom.NodeType.COMMENT == element.nodeType) {
    return null;
  }

  attributeName = attributeName.toLowerCase();

  // In IE, the style attribute is an object, so we standardize to the
  // style.cssText property to get a string. The case of this string varies
  // across browsers, so we standardize to lower-case. Finally, Firefox always
  // includes a trailing semi-colon and we standardize to that.
  if (attributeName == 'style') {
    var css = goog.string.trim(element.style.cssText).toLowerCase();
    return css.charAt(css.length - 1) == ';' ? css : css + ';';
  }

  var attr = element.getAttributeNode(attributeName);

  // IE8/9 in standards mode handles boolean attributes differently (of
  // course!). This if-statement is nested so the compiler can easily strip it
  // out when compiled for non-IE browsers.
  if (goog.userAgent.IE) {
    if (!attr && goog.userAgent.isVersion(8) &&
        goog.array.contains(bot.dom.BOOLEAN_ATTRIBUTES_, attributeName)) {
      attr = element[attributeName];
    }
  }

  if (!attr) {
    return null;
  }

  // Attempt to always return either true or null for boolean attributes.
  // In IE, attributes will sometimes be present even when not user-specified.
  // We would like to rely on the 'specified' property of attribute nodes, but
  // that is sometimes false for user-specified boolean attributes. However,
  // IE does consistently yield 'true' or 'false' strings for boolean attribute
  // values, and so we know 'false' attribute values were not user-specified.
  if (goog.array.contains(bot.dom.BOOLEAN_ATTRIBUTES_, attributeName)) {
    return (goog.userAgent.IE && attr.value == 'false') ? null : 'true';
  }

  // For non-boolean attributes, we compensate for IE's extra attributes by
  // returning null if the 'specified' property of the attributes node is false.
  return attr.specified ? attr.value : null;
};


/**
 * List of elements that support the "disabled" attribute, as defined by the
 * HTML 4.01 specification.
 * @type {!Array.<goog.dom.TagName>}
 * @const
 * @private
 * @see http://www.w3.org/TR/html401/interact/forms.html#h-17.12.1
 */
bot.dom.DISABLED_ATTRIBUTE_SUPPORTED_ = [
  goog.dom.TagName.BUTTON,
  goog.dom.TagName.INPUT,
  goog.dom.TagName.OPTGROUP,
  goog.dom.TagName.OPTION,
  goog.dom.TagName.SELECT,
  goog.dom.TagName.TEXTAREA
];


/**
 * Determines if an element is enabled. An element is considered enabled if it
 * does not support the "disabled" attribute, or if it is not disabled.
 * @param {!Element} el The element to test.
 * @return {boolean} Whether the element is enabled.
 */
bot.dom.isEnabled = function(el) {
  var tagName = el.tagName.toUpperCase();
  if (!goog.array.contains(bot.dom.DISABLED_ATTRIBUTE_SUPPORTED_, tagName)) {
    return true;
  }

  if (bot.dom.getAttribute(el, 'disabled')) {
    return false;
  }

  // The element is not explicitly disabled, but if it is an OPTION or OPTGROUP,
  // we must test if it inherits its state from a parent.
  if (el.parentNode &&
      el.parentNode.nodeType == goog.dom.NodeType.ELEMENT &&
      goog.dom.TagName.OPTGROUP == tagName ||
      goog.dom.TagName.OPTION == tagName) {
    return bot.dom.isEnabled((/**@type{!Element}*/el.parentNode));
  }
  return true;
};


/**
 * Returns the parent element of the given node, or null. This is required
 * because the parent node may not be another element.
 *
 * @param {!Node} node The node who's parent is desired.
 * @return {Element} The parent element, if available, null otherwise.
 * @private
 */
bot.dom.getParentElement_ = function(node) {
  var elem = node.parentNode;

  while (elem &&
         elem.nodeType != goog.dom.NodeType.ELEMENT &&
         elem.nodeType != goog.dom.NodeType.DOCUMENT &&
         elem.nodeType != goog.dom.NodeType.DOCUMENT_FRAGMENT) {
    elem = elem.parentNode;
  }
  return (/** @type {Element} */ bot.dom.isElement(elem) ? elem : null);
};


/**
 * Retrieves an explicitly-set, inline style value of an element. This returns
 * '' if there isn't a style attribute on the element or if this style property
 * has not been explicitly set in script.
 *
 * @param {!Element} elem Element to get the style value from.
 * @param {string} styleName Name of the style property in selector-case.
 * @return {string} The value of the style property.
 */
bot.dom.getInlineStyle = function(elem, styleName) {
  return goog.style.getStyle(elem, styleName);
};


/**
 * Retrieves the implicitly-set, effective style of an element, or null if it is
 * unknown. It returns the computed style where available; otherwise it looks
 * up the DOM tree for the first style value not equal to 'inherit,' using the
 * IE currentStyle of each node if available, and otherwise the inline style.
 * Since the computed, current, and inline styles can be different, the return
 * value of this function is not always consistent across browsers. See:
 * http://code.google.com/p/doctype/wiki/ArticleComputedStyleVsCascadedStyle
 *
 * @param {!Element} elem Element to get the style value from.
 * @param {string} styleName Name of the style property in selector-case.
 * @return {?string} The value of the style property, or null.
 */
bot.dom.getEffectiveStyle = function(elem, styleName) {
  styleName = goog.style.toCamelCase(styleName);
  return goog.style.getComputedStyle(elem, styleName) ||
      bot.dom.getCascadedStyle_(elem, styleName);
};


/**
 * Looks up the DOM tree for the first style value not equal to 'inherit,' using
 * the currentStyle of each node if available, and otherwise the inline style.
 *
 * @param {!Element} elem Element to get the style value from.
 * @param {string} styleName CSS style property in camelCase.
 * @return {?string} The value of the style property, or null.
 * @private
 */
bot.dom.getCascadedStyle_ = function(elem, styleName) {
  var value = (elem.currentStyle || elem.style)[styleName];
  if (value != 'inherit') {
    return goog.isDef(value) ? value : null;
  }
  var parent = bot.dom.getParentElement_(elem);
  return parent ? bot.dom.getCascadedStyle_(parent, styleName) : null;
};


/**
 * @param {!Element} element The element to use.
 * @return {!goog.math.Size} The dimensions of the element.
 * @private
 */
bot.dom.getElementSize_ = function(element) {
  if (goog.isFunction(element['getBBox'])) {
    return element['getBBox']();
  }
  return goog.style.getSize(element);
};


/**
 * Determines whether an element is what a user would call "shown". This means
 * that the element not only has height and width greater than 0px, but also
 * that its visibility is not "hidden" and its display property is not "none".
 * Options and Optgroup elements are treated as special cases: they are
 * considered shown iff they have a enclosing select element that is shown.
 *
 * @param {!Element} elem The element to consider.
 * @param {boolean=} opt_ignoreOpacity Whether to ignore the element's opacity
 *     when determining whether it is shown; defaults to false.
 * @return {boolean} Whether or not the element would be visible.
 */
bot.dom.isShown = function(elem, opt_ignoreOpacity) {
  if (!bot.dom.isElement(elem)) {
    throw new Error('Argument to isShown must be of type Element');
  }

  // Title elements are shown if and only if they belong to the bot window.
  if (bot.dom.isElement(elem, goog.dom.TagName.TITLE)) {
    var titleWindow = goog.dom.getWindow(goog.dom.getOwnerDocument(elem));
    return titleWindow == bot.getWindow();
  }

  // Option or optgroup is shown iff enclosing select is shown.
  if (bot.dom.isElement(elem, goog.dom.TagName.OPTION) ||
      bot.dom.isElement(elem, goog.dom.TagName.OPTGROUP)) {
    var select = /**@type {Element}*/ (goog.dom.getAncestor(elem, function(e) {
      return bot.dom.isElement(e, goog.dom.TagName.SELECT);
    }));
    return !!select && bot.dom.isShown(select);
  }

  // Map is shown iff image that uses it is shown.
  if (bot.dom.isElement(elem, goog.dom.TagName.MAP)) {
    if (!elem.name) {
      return false;
    }
    var mapDoc = goog.dom.getOwnerDocument(elem);
    var mapImage;
    // TODO(user): Avoid brute-force search once a cross-browser xpath
    // locator is available.
    if (mapDoc['evaluate']) {
      // The "//*" XPath syntax can confuse the closure compiler, so we use
      // the "/descendant::*" syntax instead.
      // TODO(user): Try to find a reproducible case for the compiler bug.
      // TODO(user): Restrict to applet, img, input:image, and object nodes.
      var imageXpath = '/descendant::*[@usemap = "#' + elem.name + '"]';

      // TODO(user): Break dependency of bot.locators on bot.dom,
      // so bot.locators.findElement can be called here instead.
      mapImage = bot.locators.xpath.single(imageXpath, mapDoc);
    } else {
      mapImage = goog.dom.findNode(mapDoc, function(n) {
        return bot.dom.isElement(n) &&
               bot.dom.getAttribute(n, 'usemap') == '#' + elem.name;
      });
    }
    return !!mapImage && bot.dom.isShown((/** @type {!Element} */ mapImage));
  }

  // Area is shown iff enclosing map is shown.
  if (bot.dom.isElement(elem, goog.dom.TagName.AREA)) {
    var map = /**@type {Element}*/ (goog.dom.getAncestor(elem, function(e) {
      return bot.dom.isElement(e, goog.dom.TagName.MAP);
    }));
    return !!map && bot.dom.isShown(map);
  }

  // Any hidden input is not shown.
  if (bot.dom.isElement(elem, goog.dom.TagName.INPUT) &&
      elem.type.toLowerCase() == 'hidden') {
    return false;
  }

  // Any element with hidden visibility is not shown.
  if (bot.dom.getEffectiveStyle(elem, 'visibility') == 'hidden') {
    return false;
  }

  // Any element with a display style equal to 'none' or that has an ancestor
  // with display style equal to 'none' is not shown.
  function displayed(e) {
    if (bot.dom.getEffectiveStyle(e, 'display') == 'none') {
      return false;
    }
    var parent = bot.dom.getParentElement_(e);
    return !parent || displayed(parent);
  }
  if (!displayed(elem)) {
    return false;
  }

  // Any transparent element is not shown.
  if (!opt_ignoreOpacity && bot.dom.getOpacity(elem) == 0) {
    return false;
  }

  // Any element without positive size dimensions is not shown.
  function positiveSize(e) {
    var size = bot.dom.getElementSize_(e);

    if (size.height > 0 && size.width > 0) {
      return true;
    }
    // Elements with only whitespace text content, e.g. "<div>&nbsp;</div>",
    // report zero size even though they take up space and can be interacted
    // with, so workaround this explicitly.
    if (e.innerText || e.textContent) {
      var text = e.innerText || e.textContent;
      if (bot.dom.JUST_HTML_WHITESPACE_REGEXP_.test(text)) {
        return true;
      }
    }
    // A bug in WebKit causes it to report zero size for elements with nested
    // block-level elements: https://bugs.webkit.org/show_bug.cgi?id=28810
    // We compensate for that bug by assuming an element has a positive size if
    // any of its children have positive size.
    return goog.userAgent.WEBKIT && goog.array.some(e.childNodes, function(n) {
      return bot.dom.isElement(n) && positiveSize(n);
    });
  }
  if (!positiveSize(elem)) {
    return false;
  }

  return true;
};


/**
 * @param {!Element} elem The element to consider.
 * @return {string} visible text.
 */
bot.dom.getVisibleText = function(elem) {
  var lines = [''];
  bot.dom.appendVisibleTextLinesFromElement_(elem, lines);
  lines = goog.array.map(lines, goog.string.trim);
  return goog.string.trim(lines.join('\n'));
};


/**
 * @param {!Element} elem Element.
 * @param {!Array.<string>} lines Accumulated visible lines of text.
 * @private
 */
bot.dom.appendVisibleTextLinesFromElement_ = function(elem, lines) {
  // TODO (gdennis): Add cases here for <title> and textual form elements.
  if (bot.dom.isElement(elem, goog.dom.TagName.BR)) {
    lines.push('');
  } else {
    var blockElem = bot.dom.hasBlockStyle_(elem);
    // Add a newline before block elems when there is text on the current line.
    if (blockElem && goog.array.peek(lines)) {
      lines.push('');
    }
    goog.array.forEach(elem.childNodes, function(node) {
      if (node.nodeType == goog.dom.NodeType.TEXT) {
        var textNode = (/** @type {!Text} */ node);
        bot.dom.appendVisibleTextLinesFromTextNode_(textNode, lines);
      } else if (bot.dom.isElement(node)) {
        var castElem = (/** @type {!Element} */ node);
        bot.dom.appendVisibleTextLinesFromElement_(castElem, lines);
      }
    });
    // Add a newline after block elems when there is text on the current line.
    if (blockElem && goog.array.peek(lines)) {
      lines.push('');
    }
  }
};


/**
 * @param {!Element} elem Element.
 * @return {boolean} Whether the element has a block style.
 * @private
 */
bot.dom.hasBlockStyle_ = function(elem) {
  var display = bot.dom.getEffectiveStyle(elem, 'display');
  return display == 'block' || display == 'inline-block';
};


/**
 * @const
 * @type {string}
 * @private
 */
bot.dom.HTML_WHITESPACE_ = '[\\s\\xa0' + String.fromCharCode(160) + ']+';

/**
 * @const
 * @type {!RegExp}
 * @private
 */
bot.dom.HTML_WHITESPACE_REGEXP_ = new RegExp(
    bot.dom.HTML_WHITESPACE_, 'g');


/**
 * @const
 * @type {!RegExp}
 * @private
 */
bot.dom.JUST_HTML_WHITESPACE_REGEXP_ = new RegExp(
    '^' + bot.dom.HTML_WHITESPACE_ + '$');



/**
 * @param {!Text} textNode Text node.
 * @param {!Array.<string>} lines Accumulated visible lines of text.
 * @private
 */
bot.dom.appendVisibleTextLinesFromTextNode_ = function(textNode, lines) {
  var parentElement = bot.dom.getParentElement_(textNode);
  if (!parentElement) {
    return;
  }

  var shown = !bot.dom.isShown(parentElement);
  if (!parentElement || !bot.dom.isShown(parentElement)) {
    return;
  }
  var text = textNode.nodeValue.replace(bot.dom.HTML_WHITESPACE_REGEXP_, ' ');
  var currLine = lines.pop();
  if (goog.string.endsWith(currLine, ' ') &&
      goog.string.startsWith(text, ' ')) {
    text = text.substr(1);
  }
  lines.push(currLine + text);
};


/**
 * Gets the opacity of a node (x-browser).
 * This gets the inline style opacity of the node and takes into account the
 * cascaded or the computed style for this node.
 *
 * @param {!Element} elem Element whose opacity has to be found.
 * @return {number} Opacity between 0 and 1.
 */
bot.dom.getOpacity = function(elem) {
  if (!goog.userAgent.IE) {
    return bot.dom.getOpacityNonIE_(elem);
  } else {
    if (bot.dom.getEffectiveStyle(elem, 'position') == 'relative') {
      // Filter does not apply to non positioned elements.
      return 1;
    }

    var opacityStyle = bot.dom.getEffectiveStyle(elem, 'filter');
    var groups = opacityStyle.match(/^alpha\(opacity=(\d*)\)/) ||
        opacityStyle.match(
        /^progid:DXImageTransform.Microsoft.Alpha\(Opacity=(\d*)\)/);

    if (groups) {
      return Number(groups[1]) / 100;
    } else {
      return 1; // Opaque.
    }
  }
};


/**
 * Implementation of getOpacity for browsers that do support
 * the "opacity" style.
 *
 * @param {!Element} elem Element whose opacity has to be found.
 * @return {number} Opacity between 0 and 1.
 * @private
 */
bot.dom.getOpacityNonIE_ = function(elem) {
  // By default the element is opaque.
  var elemOpacity = 1;

  var opacityStyle = bot.dom.getEffectiveStyle(elem, 'opacity');
  if (opacityStyle) {
    elemOpacity = Number(opacityStyle);
  }

  // Let's apply the parent opacity to the element.
  var parentElement = bot.dom.getParentElement_(elem);
  if (parentElement) {
    elemOpacity = elemOpacity * bot.dom.getOpacityNonIE_(parentElement);
  }
  return elemOpacity;
};

/**
 * Scrolls the scrollable element so that the region is fully visible.
 * If the region is too large, it will be aligned to the top-left of the
 * scrollable element. The region should be relative to the scrollable
 * element's current scroll position.
 *
 * @param {!goog.math.Rect} region The region to use.
 * @param {!Element} scrollable The scrollable element to scroll.
 * @private
 */
bot.dom.scrollRegionIntoView_ = function(region, scrollable) {
  scrollable.scrollLeft += Math.min(
      region.left, Math.max(region.left - region.width, 0));
  scrollable.scrollTop += Math.min(
      region.top, Math.max(region.top - region.height, 0));
};

/**
 * Scrolls the region of an element into the container's view. If the
 * region is too large to fit in the view, it will be aligned to the
 * top-left of the container.
 *
 * The element and container should be attached to the current document.
 *
 * @param {!Element} elem The element to use.
 * @param {!goog.math.Rect} elemRegion The region relative to the element to be
 *     scrolled into view.
 * @param {!Element} container A container of the given element.
 * @private
 */
bot.dom.scrollElementRegionIntoContainerView_ = function(elem, elemRegion,
                                                         container) {
  // Based largely from goog.style.scrollIntoContainerView.
  var elemPos = goog.style.getPageOffset(elem);
  var containerPos = goog.style.getPageOffset(container);
  var containerBorder = goog.style.getBorderBox(container);
  
  // Relative pos. of the element's border box to the container's content box.
  var relX = elemPos.x + elemRegion.left - containerPos.x -
             containerBorder.left;
  var relY = elemPos.y + elemRegion.top - containerPos.y - containerBorder.top;
  
  // How much the element can move in the container.
  var spaceX = container.clientWidth - elemRegion.width;
  var spaceY = container.clientHeight - elemRegion.height;
  
  bot.dom.scrollRegionIntoView_(new goog.math.Rect(relX, relY, spaceX, spaceY),
                                container);
};

/**
 * Scrolls the element into the client's view. If the element or region is
 * too large to fit in the view, it will be aligned to the top-left of the
 * container.
 *
 * The element should be attached to the current document.
 *
 * @param {!Element} elem The element to use.
 * @param {!goog.math.Rect} elemRegion The region relative to the element to be
 *     scrolled into view.
 * @private
 */
bot.dom.scrollElementRegionIntoClientView_ = function(elem, elemRegion) {
  var doc = goog.dom.getOwnerDocument(elem);

  // Scroll the containers.
  var container = elem.parentNode;
  while (container &&
         container != doc.body &&
         container != doc.documentElement) {
    bot.dom.scrollElementRegionIntoContainerView_(elem, elemRegion, container);
    container = container.parentNode;
  }
  
  // Scroll the actual window.
  var elemPageOffset = goog.style.getPageOffset(elem);

  var viewportSize = goog.dom.getDomHelper(doc).getViewportSize();

  var region = new goog.math.Rect(
      elemPageOffset.x + elemRegion.left - doc.body.scrollLeft,
      elemPageOffset.y + elemRegion.top - doc.body.scrollTop,
      viewportSize.width - elemRegion.width,
      viewportSize.height - elemRegion.height);

  bot.dom.scrollRegionIntoView_(region, doc.body);
};

/**
 * Scrolls the element into the client's view and returns its position
 * relative to the client viewport. If the element or region is too
 * large to fit in the view, it will be aligned to the top-left of the
 * container.
 *
 * The element should be attached to the current document.
 *
 * @param {!Element} elem The element to use.
 * @param {!goog.math.Rect=} opt_elemRegion The region relative to the element
 *     to be scrolled into view.
 * @return {!goog.math.Coordinate} The coordinate of the element in client
 *     space.
 */
bot.dom.getLocationInView = function(elem, opt_elemRegion) {
  var elemRegion;
  if (opt_elemRegion) {
    elemRegion = new goog.math.Rect(
        opt_elemRegion.left, opt_elemRegion.top,
        opt_elemRegion.width, opt_elemRegion.height);
  } else {
    elemRegion = new goog.math.Rect(0, 0, elem.offsetWidth, elem.offsetHeight);
  }
  bot.dom.scrollElementRegionIntoClientView_(elem, elemRegion);
  var elemClientPos = goog.style.getClientPosition(elem);
  return new goog.math.Coordinate(elemClientPos.x + elemRegion.left,
                                  elemClientPos.y + elemRegion.top);
};
