require File.expand_path("../spec_helper", __FILE__)

describe "Element" do

  it "should click" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "imageButton").click
  end

  it "should submit" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "submitButton").submit
  end

  it "should get value" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "cheese").value.should == "cheese"
  end

  it "should send string keys" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "working").send_keys("foo", "bar")
  end

  not_compliant_on :browser => :chrome do
    it "should send key presses" do
      driver.navigate.to url_for("javascriptPage.html")
      key_reporter = driver.find_element(:id, 'keyReporter')

      key_reporter.send_keys("Tet", :arrow_left, "s")
      key_reporter.value.should == "Test"
    end
  end

  it "should get attribute value" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "withText").attribute("rows").should == "5"
  end

  it "should return nil for non-existent attributes" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "withText").attribute("nonexistent").should be_nil
  end

  it "should toggle" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "checky").toggle
  end

  it "should clear" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "withText").clear
  end

  it "should get and set selected" do
    driver.navigate.to url_for("formPage.html")

    cheese = driver.find_element(:id, "cheese")
    peas   = driver.find_element(:id, "peas")

    cheese.select

    cheese.should be_selected
    peas.should_not be_selected

    peas.select

    peas.should be_selected
    cheese.should_not be_selected
  end

  it "should get enabled" do
    driver.navigate.to url_for("formPage.html")
    driver.find_element(:id, "notWorking").should_not be_enabled
  end

  it "should get text" do
    driver.navigate.to url_for("xhtmlTest.html")
    driver.find_element(:class, "header").text.should == "XHTML Might Be The Future"
  end

  it "should get displayed" do
    driver.navigate.to url_for("xhtmlTest.html")
    driver.find_element(:class, "header").should be_displayed
  end

  it "should get location" do
    driver.navigate.to url_for("xhtmlTest.html")
    loc = driver.find_element(:class, "header").location

    loc.x.should >= 1
    loc.y.should >= 1
  end

  it "should get location once scrolled into view" do
    driver.navigate.to url_for("javascriptPage.html")
    loc = driver.find_element(:id, 'keyUp').location_once_scrolled_into_view

    loc.x.should >= 1
    loc.y.should >= 1
  end

  it "should get size" do
    driver.navigate.to url_for("xhtmlTest.html")
    size = driver.find_element(:class, "header").size

    size.width.should > 0
    size.height.should > 0
  end

  not_compliant_on :browser => [:chrome, :chromedriver] do
    it "should drag and drop" do
      driver.navigate.to url_for("dragAndDropTest.html")

      img1 = driver.find_element(:id, "test1")
      img2 = driver.find_element(:id, "test2")

      img1.drag_and_drop_by 100, 100
      img2.drag_and_drop_on(img1)

      img1.location.should == img2.location
    end
  end

  it "should get css property" do
    driver.navigate.to url_for("javascriptPage.html")
    driver.find_element(:id, "green-parent").style("background-color").should == "#008000"
  end

  it "should know when two elements are equal" do
    driver.navigate.to url_for("simpleTest.html")

    body  = driver.find_element(:tag_name, 'body')
    xbody = driver.find_element(:xpath, "//body")

    body.should == xbody
    body.should eql(xbody)
  end

  it "should know when two elements are not equal" do
    driver.navigate.to url_for("simpleTest.html")

    elements = driver.find_elements(:tag_name, 'p')
    p1 = elements.fetch(0)
    p2 = elements.fetch(1)

    p1.should_not == p2
    p1.should_not eql(p2)
  end

  not_compliant_on :driver => [:remote] do
    it "should return the same #hash for equal elements when found by Driver#find_element" do
      driver.navigate.to url_for("simpleTest.html")

      body  = driver.find_element(:tag_name, 'body')
      xbody = driver.find_element(:xpath, "//body")

      body.hash.should == xbody.hash
    end

    it "should return the same #hash for equal elements when found by Driver#find_elements" do
      driver.navigate.to url_for("simpleTest.html")

      body  = driver.find_elements(:tag_name, 'body').fetch(0)
      xbody = driver.find_elements(:xpath, "//body").fetch(0)

      body.hash.should == xbody.hash
    end
  end

end
