/*
Copyright 2007-2010 WebDriver committers
Copyright 2007-2010 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.openqa.selenium;

import org.openqa.selenium.environment.webserver.AppServer;

public class Pages {
  public String alertsPage;
  public String simpleTestPage;
  public String simpleXmlDocument;
  public String xhtmlTestPage;
  public String formPage;
  public String metaRedirectPage;
  public String redirectPage;
  public String javascriptEnhancedForm;
  public String javascriptPage;
  public String framesetPage;
  public String iframePage;
  public String dragAndDropPage;
  public String chinesePage;
  public String nestedPage;
  public String richTextPage;
  public String rectanglesPage;
  public String childPage;
  public String grandchildPage;
  public String uploadPage;
  public String svgPage;
  public String documentWrite;
  public String sleepingPage;
  public String errorsPage;
  public String dynamicPage;
  public String slowIframes;
  public String html5Page;
  public String tables;
  public String deletingFrame;
  public String draggableLists;
  public String droppableItems;
  public String bodyTypingPage;
  public String formSelectionPage;
  public String selectableItemsPage;
  public String underscorePage;
  public String ajaxyPage;

  public Pages(AppServer appServer) {
    ajaxyPage = appServer.whereIs("ajaxy_page.html");
    alertsPage = appServer.whereIs("alerts.html");
    simpleTestPage = appServer.whereIs("simpleTest.html");
    simpleXmlDocument = appServer.whereIs("simple.xml");
    xhtmlTestPage = appServer.whereIs("xhtmlTest.html");
    formPage = appServer.whereIs("formPage.html");
    metaRedirectPage = appServer.whereIs("meta-redirect.html");
    redirectPage = appServer.whereIs("redirect");
    javascriptEnhancedForm = appServer.whereIs("javascriptEnhancedForm.html");
    javascriptPage = appServer.whereIs("javascriptPage.html");
    framesetPage = appServer.whereIs("frameset.html");
    iframePage = appServer.whereIs("iframes.html");
    dragAndDropPage = appServer.whereIs("dragAndDropTest.html");
    chinesePage = appServer.whereIs("cn-test.html");
    nestedPage = appServer.whereIs("nestedElements.html");
    richTextPage = appServer.whereIs("rich_text.html");
    rectanglesPage = appServer.whereIs("rectangles.html");
    childPage = appServer.whereIs("child/childPage.html");
    grandchildPage = appServer.whereIs("child/grandchild/grandchildPage.html");
    uploadPage = appServer.whereIs("upload.html");
    svgPage = appServer.whereIs("svgPiechart.xhtml");
    documentWrite = appServer.whereIs("document_write_in_onload.html");
    sleepingPage = appServer.whereIs("sleep");
    errorsPage = appServer.whereIs("errors.html");
    dynamicPage = appServer.whereIs("dynamic.html");
    slowIframes = appServer.whereIs("slow_loading_iframes.html");
    html5Page = appServer.whereIs("html5Page.html");
    tables = appServer.whereIs("tables.html");
    deletingFrame = appServer.whereIs("deletingFrame.htm");
    draggableLists = appServer.whereIs("draggableLists.html");
    droppableItems = appServer.whereIs("droppableItems.html");
    bodyTypingPage = appServer.whereIs("bodyTypingTest.html");
    formSelectionPage = appServer.whereIs("formSelectionPage.html");
    selectableItemsPage = appServer.whereIs("selectableItems.html");
    underscorePage = appServer.whereIs("underscore.html");
  }
}
