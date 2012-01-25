#include "StdAfx.h"
#include "ElementWrapper.h"
#include "BrowserWrapper.h"
#include "atoms.h"
#include "interactions.h"
#include "logging.h"

namespace webdriver {

ElementWrapper::ElementWrapper(IHTMLElement *element, HWND containing_window_handle) {
	// NOTE: COM should be initialized on this thread, so we
	// could use CoCreateGuid() and StringFromGUID2() instead.
	UUID guid;
	RPC_WSTR guid_string = NULL;
	::UuidCreate(&guid);
	::UuidToString(&guid, &guid_string);

	// RPC_WSTR is currently typedef'd in RpcDce.h (pulled in by rpc.h)
	// as unsigned short*. It needs to be typedef'd as wchar_t* 
	wchar_t* cast_guid_string = reinterpret_cast<wchar_t*>(guid_string);
	this->element_id_ = cast_guid_string;

	::RpcStringFree(&guid_string);

	this->element_ = element;
	this->containing_window_handle_ = containing_window_handle;
}

ElementWrapper::~ElementWrapper(void) {
}

Json::Value ElementWrapper::ConvertToJson() {
	Json::Value json_wrapper;
	std::string id(CW2A(this->element_id_.c_str(), CP_UTF8));
	json_wrapper["ELEMENT"] = id;
	return json_wrapper;
}

int ElementWrapper::IsDisplayed(bool *result) {
	int status_code = SUCCESS;

	// The atom is just the definition of an anonymous
	// function: "function() {...}"; Wrap it in another function so we can
	// invoke it with our arguments without polluting the current namespace.
	std::wstring script(L"(function() { return (");
	script += atoms::IS_DISPLAYED;
	script += L")})();";

	CComPtr<IHTMLDocument2> doc;
	this->GetContainingDocument(false, &doc);
	ScriptWrapper script_wrapper(doc, script, 1);
	script_wrapper.AddArgument(this->element_);
	status_code = script_wrapper.Execute();

	if (status_code == SUCCESS) {
		*result = script_wrapper.result().boolVal == VARIANT_TRUE;
	}

	return status_code;
}

bool ElementWrapper::IsEnabled() {
	bool result(false);

	// The atom is just the definition of an anonymous
	// function: "function() {...}"; Wrap it in another function so we can
	// invoke it with our arguments without polluting the current namespace.
	std::wstring script(L"(function() { return (");
	script += atoms::IS_ENABLED;
	script += L")})();";

	CComPtr<IHTMLDocument2> doc;
	this->GetContainingDocument(false, &doc);
	ScriptWrapper script_wrapper(doc, script, 1);
	script_wrapper.AddArgument(this->element_);
	int status_code = script_wrapper.Execute();

	if (status_code == SUCCESS) {
		result = script_wrapper.result().boolVal == VARIANT_TRUE;
	}

	return result;
}

int ElementWrapper::Click() {
	long x = 0, y = 0, w = 0, h = 0;
	int status_code = this->GetLocationOnceScrolledIntoView(&x, &y, &w, &h);

	if (status_code == SUCCESS) {
		long click_x = x + (w ? w / 2 : 0);
		long click_y = y + (h ? h / 2 : 0);

		// Create a mouse move, mouse down, mouse up OS event
		LRESULT result = mouseMoveTo(this->containing_window_handle_, 10, x, y, click_x, click_y);
		if (result != SUCCESS) {
			return static_cast<int>(result);
		}
		
		result = clickAt(this->containing_window_handle_, click_x, click_y, MOUSEBUTTON_LEFT);
		if (result != SUCCESS) {
			return static_cast<int>(result);
		}

		//wait(50);
	}
	return status_code;
}

int ElementWrapper::Hover() {
	long x = 0, y = 0, w = 0, h = 0;
	int status_code = this->GetLocationOnceScrolledIntoView(&x, &y, &w, &h);

	if (status_code == SUCCESS) {
		long click_x = x + (w ? w / 2 : 0);
		long click_y = y + (h ? h / 2 : 0);

		// Create a mouse move, mouse down, mouse up OS event
		LRESULT lresult = mouseMoveTo(this->containing_window_handle_, 100, 0, 0, click_x, click_y);
	}
	return status_code;
}

int ElementWrapper::DragBy(const int offset_x, const int offset_y, const int drag_speed) {
	long x = 0, y = 0, w = 0, h = 0;
	int status_code = this->GetLocationOnceScrolledIntoView(&x, &y, &w, &h);

	if (status_code == SUCCESS) {
		long click_x = x + (w ? w / 2 : 0);
		long click_y = y + (h ? h / 2 : 0);

		// Create a mouse move, mouse down, mouse up OS event
		LRESULT lresult = mouseDownAt(this->containing_window_handle_, click_x, click_y, MOUSEBUTTON_LEFT);
		lresult = mouseMoveTo(this->containing_window_handle_, (long)drag_speed, click_x, click_y, click_x + offset_x, click_y + offset_y);
		lresult = mouseUpAt(this->containing_window_handle_, click_x + offset_x, click_y + offset_y, MOUSEBUTTON_LEFT);
	}
	return status_code;
}

int ElementWrapper::GetAttributeValue(const std::wstring& attribute_name, VARIANT *attribute_value) {
	int status_code = SUCCESS;

	// The atom is just the definition of an anonymous
	// function: "function() {...}"; Wrap it in another function so we can
	// invoke it with our arguments without polluting the current namespace.
	std::wstring script(L"(function() { return (");
	script += atoms::GET_ATTRIBUTE;
	script += L")})();";

	CComPtr<IHTMLDocument2> doc;
	this->GetContainingDocument(false, &doc);
	ScriptWrapper script_wrapper(doc, script, 2);
	script_wrapper.AddArgument(this->element_);
	script_wrapper.AddArgument(attribute_name);
	status_code = script_wrapper.Execute();

	if (status_code == SUCCESS) {
		::VariantCopy(attribute_value, &script_wrapper.result());
	}

	return SUCCESS;
}

int ElementWrapper::GetLocationOnceScrolledIntoView(long *x, long *y, long *width, long *height) {
    CComPtr<IHTMLDOMNode2> node;
	HRESULT hr = this->element_->QueryInterface(&node);

    if (FAILED(hr)) {
		LOGHR(WARN, hr) << "Cannot cast html element to node";
		return ENOSUCHELEMENT;
    }

    bool displayed;
	int result = this->IsDisplayed(&displayed);
	if (result != SUCCESS) {
		return result;
	} 

	if (!displayed) {
        return EELEMENTNOTDISPLAYED;
    }

    if (!this->IsEnabled()) {
        return EELEMENTNOTENABLED;
    }

	long top = 0, left = 0, element_width = 0, element_height = 0;
	result = this->GetLocation(&left, &top, &element_width, &element_height);
	if (result != SUCCESS || !this->IsClickPointInViewPort(left, top, element_width, element_height)) {
		// Scroll the element into view
		LOG(DEBUG) << "Will need to scroll element into view";
		hr = this->element_->scrollIntoView(CComVariant(VARIANT_TRUE));
		if (FAILED(hr)) {
		    LOGHR(WARN, hr) << "Cannot scroll element into view";
			return EOBSOLETEELEMENT;
		}

		result = this->GetLocation(&left, &top, &element_width, &element_height);
		if (result != SUCCESS) {
			return result;
		}

		if (!this->IsClickPointInViewPort(left, top, element_width, element_height)) {
			return EELEMENTNOTDISPLAYED;
		}
	}

	LOG(DEBUG) << "(x, y, w, h): " << left << ", " << top << ", " << element_width << ", " << element_height << endl;

	*x = left;
	*y = top;
	*width = element_width;
	*height = element_height;
	return SUCCESS;
}

bool ElementWrapper::IsSelected() {
	bool selected(false);
	// The atom is just the definition of an anonymous
	// function: "function() {...}"; Wrap it in another function so we can
	// invoke it with our arguments without polluting the current namespace.
	std::wstring script(L"(function() { return (");
	script += atoms::IS_SELECTED;
	script += L")})();";

	CComPtr<IHTMLDocument2> doc;
	this->GetContainingDocument(false, &doc);
	ScriptWrapper script_wrapper(doc, script, 1);
	script_wrapper.AddArgument(this->element_);
	int status_code = script_wrapper.Execute();

	if (status_code == SUCCESS && script_wrapper.ResultIsBoolean()) {
		selected = script_wrapper.result().boolVal == VARIANT_TRUE;
	}

	return selected;
}

bool ElementWrapper::IsCheckBox() {
	CComQIPtr<IHTMLInputElement> input(this->element_);
	if (!input) {
		return false;
	}

	CComBSTR type_name;
	input->get_type(&type_name);
	return _wcsicmp((LPCWSTR)((BSTR)type_name), L"checkbox") == 0;
}

bool ElementWrapper::IsRadioButton() {
	CComQIPtr<IHTMLInputElement> input(this->element_);
	if (!input) {
		return false;
	}

	CComBSTR type_name;
	input->get_type(&type_name);
	return _wcsicmp((LPCWSTR)((BSTR)type_name), L"radio") == 0;
}

void ElementWrapper::FireEvent(IHTMLDOMNode* fire_event_on, const std::wstring& event_name) {
	CComPtr<IDispatch> dispatch;
	this->element_->get_document(&dispatch);
	CComQIPtr<IHTMLDocument4> doc(dispatch);

	CComPtr<IHTMLEventObj> event_object;
	doc->createEventObject(NULL, &event_object);

	CComVariant eventref(event_object);
	CComBSTR event_name_bstr(event_name.c_str());
	VARIANT_BOOL cancellable;
	CComQIPtr<IHTMLElement3> element3(fire_event_on);
	element3->fireEvent(event_name_bstr, &eventref, &cancellable);
}

int ElementWrapper::GetLocation(long *x, long *y, long *width, long *height) {
	*x = 0, *y = 0, *width = 0, *height = 0;

	CComPtr<IHTMLElement2> element2;
	HRESULT hr = this->element_->QueryInterface(&element2);
	if (FAILED(hr)) {
		LOGHR(WARN, hr) << "Unable to cast element to correct type";
		return EOBSOLETEELEMENT;
	}

    CComPtr<IHTMLRect> rect;
	hr = element2->getBoundingClientRect(&rect);
    if (FAILED(hr)) {
		LOGHR(WARN, hr) << "Cannot figure out where the element is on screen";
		return EUNHANDLEDERROR;
    }

	long top = 0, bottom = 0, left = 0, right = 0;

    rect->get_top(&top);
    rect->get_left(&left);
	rect->get_bottom(&bottom);
    rect->get_right(&right);

	// On versions of IE prior to 8 on Vista, if the element is out of the 
	// viewport this would seem to return 0,0,0,0. IE 8 returns position in 
	// the DOM regardless of whether it's in the browser viewport.

	// Handle the easy case first: does the element have size
	long w = right - left;
	long h = bottom - top;
	if (w <= 0 || h <= 0) { return EELEMENTNOTDISPLAYED; }

	long scroll_left, scroll_top = 0;
	element2->get_scrollLeft(&scroll_left);
	element2->get_scrollTop(&scroll_top);
	left += scroll_left;
	top += scroll_top;

	long frame_offset_x = 0, frame_offset_y = 0;
	this->GetFrameOffset(&frame_offset_x, &frame_offset_y);
	left += frame_offset_x;
	top += frame_offset_y;

	*x = left;
	*y = top;
	*width = w;
	*height = h;

	return SUCCESS;
}

int ElementWrapper::GetFrameOffset(long *x, long *y) {
    CComPtr<IHTMLDocument2> owner_doc;
	int status_code = this->GetContainingDocument(true, &owner_doc);
	if (status_code != SUCCESS) {
		return status_code;
	}

	CComPtr<IHTMLWindow2> owner_doc_window;
	HRESULT hr = owner_doc->get_parentWindow(&owner_doc_window);
	if (!owner_doc_window) {
		LOG(WARN) << "Unable to get parent window";
		return ENOSUCHDOCUMENT;
	}

	CComPtr<IHTMLWindow2> parent_window;
	hr = owner_doc_window->get_parent(&parent_window);
	if (parent_window && !owner_doc_window.IsEqualObject(parent_window)) {
		CComPtr<IHTMLDocument2> parent_doc;
		status_code = this->GetParentDocument(parent_window, &parent_doc);

		CComPtr<IHTMLFramesCollection2> frames;
		hr = parent_doc->get_frames(&frames);

		long frame_count(0);
		hr = frames->get_length(&frame_count);
		CComVariant index;
		index.vt = VT_I4;
		for (long i = 0; i < frame_count; ++i) {
			// See if the document in each frame is this element's 
			// owner document.
			index.lVal = i;
			CComVariant result;
			hr = frames->item(&index, &result);
			CComQIPtr<IHTMLWindow2> frame_window(result.pdispVal);
			if (!frame_window) {
				// Frame is not an HTML frame.
				continue;
			}

			CComPtr<IHTMLDocument2> frame_doc;
			hr = frame_window->get_document(&frame_doc);

			if (frame_doc.IsEqualObject(owner_doc)) {
				// The document in this frame *is* this element's owner
				// document. Get the frameElement property of the document's
				// containing window (which is itself an HTML element, either
				// a frame or an iframe). Then get the x and y coordinates of
				// that frame element.
				std::wstring script = L"(function(){ return function() { return arguments[0].frameElement };})();";
				ScriptWrapper script_wrapper(frame_doc, script, 1);
				CComVariant window_variant(frame_window);
				script_wrapper.AddArgument(window_variant);
				script_wrapper.Execute();
				CComQIPtr<IHTMLElement> frame_element(script_wrapper.result().pdispVal);

				// Wrap the element so we can find its location.
				ElementWrapper element_wrapper(frame_element, this->containing_window_handle_);
				long frame_x, frame_y, frame_width, frame_height;
				int status_code = element_wrapper.GetLocation(&frame_x, &frame_y, &frame_width, &frame_height);
				if (status_code == SUCCESS) {
					*x = frame_x;
					*y = frame_y;
				}
				break;
			}
		}
	}
	return SUCCESS;
}

bool ElementWrapper::IsClickPointInViewPort(const long x, const long y, const long width, const long height) {
	long click_x = x + (width / 2);
	long click_y = y + (height / 2);

	WINDOWINFO window_info;
	if (!::GetWindowInfo(this->containing_window_handle_, &window_info)) {
		LOG(WARN) << "Cannot determine size of window";
		return false;
	}

    long window_width = window_info.rcClient.right - window_info.rcClient.left;
    long window_height = window_info.rcClient.bottom - window_info.rcClient.top;

	// Hurrah! Now we know what the visible area of the viewport is
	// Is the element visible in the X axis?
	if (click_x < 0 || click_x > window_width) {
		return false;
	}

	// And in the Y?
	if (click_y < 0 || click_y > window_height) {
		return false;
	}
	return true;
}

int ElementWrapper::GetContainingDocument(const bool use_dom_node, IHTMLDocument2** doc) {
	HRESULT hr = S_OK;
	CComPtr<IDispatch> dispatch_doc;
	if (use_dom_node) {
		CComPtr<IHTMLDOMNode2> node;
		hr = this->element_->QueryInterface(&node);
		if (FAILED(hr)) {
			LOG(WARN) << "Unable to cast element to IHTMLDomNode2";
			return ENOSUCHDOCUMENT;
		}

		hr = node->get_ownerDocument(&dispatch_doc);
		if (FAILED(hr)) {
			LOG(WARN) << "Unable to locate owning document";
			return ENOSUCHDOCUMENT;
		}
	} else {
		hr = this->element_->get_document(&dispatch_doc);
		if (FAILED(hr)) {
			LOG(WARN) << "Unable to locate document property";
			return ENOSUCHDOCUMENT;
		}

	}

	hr = dispatch_doc.QueryInterface<IHTMLDocument2>(doc);
	if (FAILED(hr)) {
		LOG(WARN) << "Found document but it's not the expected type";
		return ENOSUCHDOCUMENT;
	}

	return SUCCESS;
}

int ElementWrapper::GetParentDocument(IHTMLWindow2* parent_window, IHTMLDocument2** parent_doc) {
	HRESULT hr = parent_window->get_document(parent_doc);
	if (hr == E_ACCESSDENIED) {
		// Cross-domain documents may throw Access Denied. If so,
		// get the document through the IWebBrowser2 interface.
		CComPtr<IWebBrowser2> window_browser;
		CComQIPtr<IServiceProvider> service_provider(parent_window);
		hr = service_provider->QueryService(IID_IWebBrowserApp, &window_browser);
		if (FAILED(hr)) {
			return ENOSUCHDOCUMENT;
		}
		CComQIPtr<IDispatch> parent_doc_dispatch;
		hr = window_browser->get_Document(&parent_doc_dispatch);
		if (FAILED(hr)) {
			return ENOSUCHDOCUMENT;
		}
		hr = parent_doc_dispatch->QueryInterface<IHTMLDocument2>(parent_doc);
		if (FAILED(hr)) {
			return ENOSUCHDOCUMENT;
		}
	}
	return SUCCESS;
}

} // namespace webdriver