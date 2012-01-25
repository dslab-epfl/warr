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
 * @fileoverview Element locator functions.
 *
 */


goog.provide('bot.locators');

goog.require('bot');
goog.require('bot.locators.className');
goog.require('bot.locators.css');
goog.require('bot.locators.id');
goog.require('bot.locators.linkText');
goog.require('bot.locators.name');
goog.require('bot.locators.partialLinkText');
goog.require('bot.locators.tagName');
goog.require('bot.locators.xpath');
goog.require('goog.array');  // for the goog.array.ArrayLike typedef
goog.require('goog.object');


/**
 * @typedef {{single:function(string,!(Document|Element)):Element,
 *     many:function(string,!(Document|Element)):!goog.array.ArrayLike}}
 */
bot.locators.strategy;


/**
 * Known element location strategies. The returned objects have two
 * methods on them, "single" and "many", for locating a single element
 * or multiple elements, respectively.
 *
 * @private
 * @const
 * @type {Object.<string,bot.locators.strategy>}
 */
bot.locators.STRATEGIES_ = {
  'className': bot.locators.className,
  'css': bot.locators.css,
  'id': bot.locators.id,
  'linkText': bot.locators.linkText,
  'name': bot.locators.name,
  'partialLinkText': bot.locators.partialLinkText,
  'tagName': bot.locators.tagName,
  'xpath': bot.locators.xpath
};

/**
 * Add or override an existing strategy for locating elements.
 *
 * @param {string} name The name of the strategy.
 * @param {!bot.locators.strategy} strategy The strategy to use.
 */
bot.locators.add = function(name, strategy) {
  bot.locators.STRATEGIES_[name] = strategy;
};


/**
 * Find the first element in the DOM matching the target. The target
 * object should have a single key, the name of which determines the
 * locator strategy and the value of which gives the value to be
 * searched for. For example {id: 'foo'} indicates that the first
 * element on the DOM with the ID 'foo' should be returned.
 *
 * @param {!Object} target The selector to search for.
 * @param {(Document|Element)=} opt_root The node from which to start the
 *     search. If not specified, will use {@code document} as the root.
 * @return {Element} The first matching element found in the DOM, or null if no
 *     such element could be found.
 */
bot.locators.findElement = function(target, opt_root) {
  var key = goog.object.getAnyKey(target);

  if (key) {
    var strategy = bot.locators.STRATEGIES_[key];
    if (strategy && goog.isFunction(strategy.single)) {
      var root = opt_root || goog.dom.getOwnerDocument(bot.getWindow());
      return strategy.single(target[key], root);
    }
  }
  throw Error('Unsupported locator strategy: ' + key);
};


/**
 * Find all elements in the DOM matching the target. The target object
 * should have a single key, the name of which determines the locator
 * strategy and the value of which gives the value to be searched
 * for. For example {name: 'foo'} indicates that all elements with the
 * 'name' attribute equal to 'foo' should be returned.
 *
 * @param {!Object} target The selector to search for.
 * @param {(Document|Element)=} opt_root The node from which to start the
 *     search. If not specified, will use {@code document} as the root.
 * @return {!goog.array.ArrayLike.<Element>} All matching elements found in the
 *     DOM.
 */
bot.locators.findElements = function(target, opt_root) {
  var key = goog.object.getAnyKey(target);

  if (key) {
    var strategy = bot.locators.STRATEGIES_[key];
    if (strategy && goog.isFunction(strategy.many)) {
      var root = opt_root || goog.dom.getOwnerDocument(bot.getWindow());
      return strategy.many(target[key], root);
    }
  }
  throw Error('Unsupported locator strategy: ' + key);
};
