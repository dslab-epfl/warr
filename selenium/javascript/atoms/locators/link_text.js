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

goog.provide('bot.locators.linkText');
goog.provide('bot.locators.partialLinkText');

goog.require('bot');
goog.require('bot.dom');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.dom.DomHelper');


/**
 * Find an element by using the text value of a link
 * @param {string} target The link text to search for.
 * @param {!(Document|Element)} root The document or element to perform the
 *     search under.
 * @param {boolean} opt_isPartial Whether the link text needs to be matched
 *     only partially.
 * @return {Element} The first matching element found in the DOM, or null if no
 *     such element could be found.
 * @private
 */
bot.locators.linkText.single_ = function(target, root, opt_isPartial) {
  // TODO(user): Fix this to work for XHTML (which is case sensitive)
  var elements = goog.dom.getDomHelper(root).getElementsByTagNameAndClass(
      goog.dom.TagName.A, /*className=*/null, root);

  var element = goog.array.find(elements, function(element) {
    var text = bot.dom.getVisibleText(element);
    return (opt_isPartial && text.indexOf(target) != -1) || text == target;
  });
  return (/**@type{Element}*/element);
};


/**
 * Find many elements by using the value of the link text
 * @param {string} target The link text to search for.
 * @param {!(Document|Element)} root The document or element to perform the
 *     search under.
 * @param {boolean} opt_isPartial Whether the link text needs to be matched
 *     only partially.
 * @return {goog.array.ArrayLike} All matching elements, or an empty list.
 * @private
 */
bot.locators.linkText.many_ = function(target, root, opt_isPartial) {
  // TODO(user): Fix this to work for XHTML (which is case sensitive)
  var elements = goog.dom.getDomHelper(root).getElementsByTagNameAndClass(
      goog.dom.TagName.A, /*className=*/null, root);
  return goog.array.filter(elements, function(element) {
    var text = bot.dom.getVisibleText(element);
    return (opt_isPartial && text.indexOf(target) != -1) || text == target;
  });
};


/**
 * Find an element by using the text value of a link
 * @param {string} target The link text to search for.
 * @param {!(Document|Element)} root The document or element to perform the
 *     search under.
 * @return {Element} The first matching element found in the DOM, or null if no
 *     such element could be found.
 */
bot.locators.linkText.single = function(target, root) {
  return bot.locators.linkText.single_(target, root, false);
};


/**
 * Find many elements by using the value of the link text
 * @param {string} target The link text to search for.
 * @param {!(Document|Element)} root The document or element to perform the
 *     search under.
 * @return {goog.array.ArrayLike} All matching elements, or an empty list.
 */
bot.locators.linkText.many = function(target, root) {
  return bot.locators.linkText.many_(target, root, false);
};


/**
 * Find an element by using part of the text value of a link.
 * @param {string} target The link text to search for.
 * @param {!(Document|Element)} root The document or element to perform the
 *     search under.
 * @return {Element} The first matching element found in the DOM, or null if no
 *     such element could be found.
 */
bot.locators.partialLinkText.single = function(target, root) {
  return bot.locators.linkText.single_(target, root, true);
};


/**
 * Find many elements by using part of the value of the link text.
 * @param {string} target The link text to search for.
 * @param {!(Document|Element)} root The document or element to perform the
 *     search under.
 * @return {goog.array.ArrayLike} All matching elements, or an empty list.
 */
bot.locators.partialLinkText.many = function(target, root) {
  return bot.locators.linkText.many_(target, root, true);
};
