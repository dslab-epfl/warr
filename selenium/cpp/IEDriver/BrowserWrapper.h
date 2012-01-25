#ifndef WEBDRIVER_IE_BROWSERWRAPPER_H_
#define WEBDRIVER_IE_BROWSERWRAPPER_H_

#include <exdispid.h>
#include <exdisp.h>
#include <mshtml.h>
#include <rpc.h>
#include <iostream>
#include <queue>
#include <string>
#include "json.h"
#include "BrowserFactory.h"
#include "CommandValues.h"
#include "ErrorCodes.h"
#include "messages.h"
#include "ScriptWrapper.h"

#define BASE_TEN_BASE 10
#define MAX_DIGITS_OF_NUMBER 22

using namespace std;

namespace webdriver {

class BrowserWrapper : public IDispEventSimpleImpl<1, BrowserWrapper, &DIID_DWebBrowserEvents2> {
public:
	BrowserWrapper(IWebBrowser2* browser, HWND hwnd, HWND browser_manager_handle);
	virtual ~BrowserWrapper(void);

	static inline _ATL_FUNC_INFO* BeforeNavigate2Info() {
		static _ATL_FUNC_INFO kBeforeNavigate2 = { CC_STDCALL, VT_EMPTY, 7,
			{ VT_DISPATCH, VT_VARIANT | VT_BYREF, VT_VARIANT | VT_BYREF, VT_VARIANT | VT_BYREF, VT_VARIANT | VT_BYREF, VT_VARIANT | VT_BYREF, VT_BOOL | VT_BYREF } };
	  return &kBeforeNavigate2;
	}

	static inline _ATL_FUNC_INFO* DocumentCompleteInfo() {
		static _ATL_FUNC_INFO kDocumentComplete = { CC_STDCALL, VT_EMPTY, 2, { VT_DISPATCH, VT_VARIANT|VT_BYREF } };
		return &kDocumentComplete;
	}

	static inline _ATL_FUNC_INFO* NoArgumentsInfo() {
	  static _ATL_FUNC_INFO kNoArguments = { CC_STDCALL, VT_EMPTY, 0 };
	  return &kNoArguments;
	}

	static inline _ATL_FUNC_INFO* NewWindow3Info() {
		static _ATL_FUNC_INFO kNewWindow3 = { CC_STDCALL, VT_EMPTY, 5,
			{ VT_DISPATCH | VT_BYREF, VT_BOOL | VT_BYREF, VT_I4, VT_BSTR, VT_BSTR } };
		return &kNewWindow3;
	}

	BEGIN_SINK_MAP(BrowserWrapper)
		SINK_ENTRY_INFO(1, DIID_DWebBrowserEvents2, DISPID_BEFORENAVIGATE2, BeforeNavigate2, BeforeNavigate2Info())
		SINK_ENTRY_INFO(1, DIID_DWebBrowserEvents2, DISPID_DOCUMENTCOMPLETE, DocumentComplete, DocumentCompleteInfo())
		SINK_ENTRY_INFO(1, DIID_DWebBrowserEvents2, DISPID_ONQUIT, OnQuit, NoArgumentsInfo())
		SINK_ENTRY_INFO(1, DIID_DWebBrowserEvents2, DISPID_NEWWINDOW3, NewWindow3, NewWindow3Info())
	END_SINK_MAP()

	STDMETHOD_(void, BeforeNavigate2)(IDispatch * pObject, VARIANT * pvarUrl, VARIANT * pvarFlags,
		VARIANT * pvarTargetFrame, VARIANT * pvarData, VARIANT * pvarHeaders, VARIANT_BOOL * pbCancel);
	STDMETHOD_(void, DocumentComplete)(IDispatch *pDisp,VARIANT *URL);
	STDMETHOD_(void, OnQuit)();
	STDMETHOD_(void, NewWindow3)(IDispatch **ppDisp, VARIANT_BOOL * pbCancel, DWORD dwFlags, BSTR bstrUrlContext, BSTR bstrUrl);

	bool Wait(void);
	void GetDocument(IHTMLDocument2 **doc);
	HWND GetWindowHandle(void);
	std::wstring GetTitle(void);
	std::wstring GetCookies(void);
	int AddCookie(const std::wstring& cookie);
	int DeleteCookie(const std::wstring& cookie_name);
	int SetFocusedFrameByIndex(const int frame_index);
	int SetFocusedFrameByName(const std::wstring& frame_name);
	int SetFocusedFrameByElement(IHTMLElement *frame_element);
	HWND GetActiveDialogWindowHandle(void);
	bool IsFrameFocused(void);

	std::wstring ConvertVariantToWString(VARIANT *to_convert);

	IWebBrowser2 *browser(void) { return this->browser_; }
	std::wstring browser_id(void) const { return this->browser_id_; }

	bool wait_required(void) const { return this->wait_required_; }
	void set_wait_required(const bool value) { this->wait_required_ = value; }

	bool is_closing(void) const { return this->is_closing_; }

private:
	void AttachEvents(void);
	void DetachEvents(void);
	bool IsDocumentNavigating(IHTMLDocument2 *doc);
	bool IsHtmlPage(IHTMLDocument2 *doc);
	bool GetDocumentFromWindow(IHTMLWindow2 *window, IHTMLDocument2 **doc);

	CComPtr<IHTMLWindow2> focused_frame_window_;
	CComPtr<IWebBrowser2> browser_;
	BrowserFactory factory_;
	HWND window_handle_;
	HWND browser_manager_handle_;
	std::wstring browser_id_;
	bool is_navigation_started_;
	bool wait_required_;
	bool is_closing_;
};

} // namespace webdriver

#endif // WEBDRIVER_IE_BROWSERWRAPPER_H_
