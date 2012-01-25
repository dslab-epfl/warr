#ifndef WEBDRIVER_IE_SENDKEYSCOMMANDHANDLER_H_
#define WEBDRIVER_IE_SENDKEYSCOMMANDHANDLER_H_

#include <ctime>
#include "BrowserManager.h"
#include "interactions.h"
#include "logging.h"

const LPCTSTR fileDialogNames[] = {
	_T("#32770"),
	_T("ComboBoxEx32"),
	_T("ComboBox"),
	_T("Edit"),
	NULL
};

namespace webdriver {

class SendKeysCommandHandler : public WebDriverCommandHandler {
public:
	struct FileNameData {
		HWND main;
		HWND hwnd;
		DWORD ieProcId;
		const wchar_t* text;
	};

	SendKeysCommandHandler(void) {
	}

	virtual ~SendKeysCommandHandler(void) {
	}

protected:
	void SendKeysCommandHandler::ExecuteInternal(BrowserManager *manager, const std::map<std::string, std::string>& locator_parameters, const std::map<std::string, Json::Value>& command_parameters, WebDriverResponse * response)
	{
		std::map<std::string, std::string>::const_iterator id_parameter_iterator = locator_parameters.find("id");
		std::map<std::string, Json::Value>::const_iterator value_parameter_iterator = command_parameters.find("value");
		if (id_parameter_iterator == locator_parameters.end()) {
			response->SetErrorResponse(400, "Missing parameter in URL: id");
			return;
		} else if (value_parameter_iterator == command_parameters.end()) {
			response->SetErrorResponse(400, "Missing parameter: value");
			return;
		} else {
			std::wstring element_id(CA2W(id_parameter_iterator->second.c_str(), CP_UTF8));

			std::wstring keys(L"");
			Json::Value key_array(value_parameter_iterator->second);
			for (unsigned int i = 0; i < key_array.size(); ++i ) {
				std::string key(key_array[i].asString());
				keys.append(CA2W(key.c_str(), CP_UTF8));
			}

			std::tr1::shared_ptr<BrowserWrapper> browser_wrapper;
			int status_code = manager->GetCurrentBrowser(&browser_wrapper);
			if (status_code != SUCCESS) {
				response->SetErrorResponse(status_code, "Unable to get browser");
				return;
			}
			HWND window_handle = browser_wrapper->GetWindowHandle();
			TCHAR pszClassName[25];
			::GetClassName(window_handle, pszClassName, 25);

			std::tr1::shared_ptr<ElementWrapper> element_wrapper;
			status_code = this->GetElement(manager, element_id, &element_wrapper);

			if (status_code == SUCCESS) {
				bool displayed;
				status_code = element_wrapper->IsDisplayed(&displayed);
				if (status_code != SUCCESS || !displayed) {
					response->SetErrorResponse(EELEMENTNOTDISPLAYED, "Element is not displayed");
					return;
				}

				if (!element_wrapper->IsEnabled()) {
					response->SetErrorResponse(EELEMENTNOTENABLED, "Element is not enabled");
					return;
				}

				CComQIPtr<IHTMLElement> element(element_wrapper->element());

				element->scrollIntoView(CComVariant(VARIANT_TRUE));

				CComQIPtr<IHTMLInputFileElement> file(element);
				if (file) {
					DWORD ie_process_id;
					::GetWindowThreadProcessId(window_handle, &ie_process_id);
					HWND top_level_window_handle = NULL;
					browser_wrapper->browser()->get_HWND(reinterpret_cast<SHANDLE_PTR*>(&top_level_window_handle));

					FileNameData key_data;
					key_data.main = top_level_window_handle;
					key_data.hwnd = window_handle;
					key_data.text = keys.c_str();
					key_data.ieProcId = ie_process_id;

					unsigned int thread_id;
					HANDLE thread_handle = reinterpret_cast<HANDLE>(_beginthreadex(NULL, 0, &SendKeysCommandHandler::SetFileValue, (void *) &key_data, 0, &thread_id));

					element->click();
					// We're now blocked until the dialog closes.
					::CloseHandle(thread_handle);
					return;
				}

				this->WaitUntilElementFocused(element);

				sendKeys(window_handle, keys.c_str(), manager->speed());
				response->SetResponse(SUCCESS, Json::Value::null);
				return;
			} else {
				response->SetErrorResponse(status_code, "Element is no longer valid");
				return;
			}
		}
	}
private:
	static unsigned int WINAPI SendKeysCommandHandler::SetFileValue(void *file_data) {
		FileNameData* data = reinterpret_cast<FileNameData*>(file_data);
		::Sleep(100);
		HWND ie_main_window_handle = data->main;
		HWND dialog_window_handle = ::GetLastActivePopup(ie_main_window_handle);

		int max_wait = 10;
		while ((dialog_window_handle == ie_main_window_handle) && --max_wait) {
			::Sleep(100);
			dialog_window_handle = ::GetLastActivePopup(ie_main_window_handle);
		}

		if (!dialog_window_handle || (dialog_window_handle == ie_main_window_handle)) {
			// No dialog directly owned by the top-level window.
			// Look for a dialog belonging to the same process as
			// the IE server window. This isn't perfect, but it's
			// all we have for now.
			max_wait = 10;
			while ((dialog_window_handle == ie_main_window_handle) && --max_wait) {
				ProcessWindowInfo process_win_info;
				process_win_info.dwProcessId = data->ieProcId;
				::EnumWindows(&BrowserFactory::FindDialogWindowForProcess, (LPARAM)&process_win_info);
				if (process_win_info.hwndBrowser != NULL) {
					dialog_window_handle = process_win_info.hwndBrowser;
				}
			}
		}

		if (!dialog_window_handle || (dialog_window_handle == ie_main_window_handle)) {
			LOG(WARN) << "No dialog found";
			return false;
		}

		return SendKeysToFileUploadAlert(dialog_window_handle, data->text);
	}

	static bool SendKeysToFileUploadAlert(HWND dialog_window_handle, const wchar_t* value) 
	{
		HWND edit_field_window_handle = NULL;
		int maxWait = 10;
		while (!edit_field_window_handle && --maxWait) {
			wait(200);
			edit_field_window_handle = dialog_window_handle;
			for (int i = 1; fileDialogNames[i]; ++i) {
				edit_field_window_handle = getChildWindow(edit_field_window_handle, fileDialogNames[i]);
			}
		}

		if (edit_field_window_handle) {
			// Attempt to set the value, looping until we succeed.
			const wchar_t* filename = value;
			size_t expected = wcslen(filename);
			size_t curr = 0;

			while (expected != curr) {
				::SendMessage(edit_field_window_handle, WM_SETTEXT, 0, (LPARAM) filename);
				wait(1000);
				curr = ::SendMessage(edit_field_window_handle, WM_GETTEXTLENGTH, 0, 0);
			}

			for (int i = 0; i < 10000; i++) 
			{
				HWND open_window_handle = ::FindWindowExW(dialog_window_handle, NULL, L"Button", L"&Open");
				if (open_window_handle) {
					LRESULT total = 0;
					total += ::SendMessage(open_window_handle, WM_LBUTTONDOWN, 0, 0);
					total += ::SendMessage(open_window_handle, WM_LBUTTONUP, 0, 0);

					if (total == 0)
					{
						return true;
					}

					wait(500);
				}
			}

			LOG(ERROR) << "Unable to set value of file input dialog";
			return false;
		}

		LOG(WARN) << "No edit found";
		return false;
	}

	bool SendKeysCommandHandler::WaitUntilElementFocused(IHTMLElement *element) {
		CComQIPtr<IHTMLElement2> element2(element);
		element2->focus();

		// Check we have focused the element.
		CComPtr<IDispatch> dispatch;
		element->get_document(&dispatch);
		CComQIPtr<IHTMLDocument2> document(dispatch);

		bool has_focus = false;
		clock_t max_wait = clock() + 1000;
		for (int i = clock(); i < max_wait; i = clock()) {
			wait(1);
			CComPtr<IHTMLElement> active_element;
			if (document->get_activeElement(&active_element) == S_OK) {
				CComQIPtr<IHTMLElement2> active_element2(active_element);
				if (element2.IsEqualObject(active_element2)) {
					has_focus = true;
					break;
				}
			}
		}

		if (!has_focus) {
			cout << "We don't have focus on element." << endl;
		}

		return has_focus;
	}
};

} // namespace webdriver

#endif // WEBDRIVER_IE_SENDKEYSCOMMANDHANDLER_H_
