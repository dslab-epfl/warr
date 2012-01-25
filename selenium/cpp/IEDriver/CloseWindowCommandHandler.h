#ifndef WEBDRIVER_IE_CLOSEWINDOWCOMMANDHANDLER_H_
#define WEBDRIVER_IE_CLOSEWINDOWCOMMANDHANDLER_H_

#include "BrowserManager.h"

namespace webdriver {

class CloseWindowCommandHandler : public WebDriverCommandHandler {
public:
	CloseWindowCommandHandler(void) {
	}

	virtual ~CloseWindowCommandHandler(void) {
	}

protected:
	void CloseWindowCommandHandler::ExecuteInternal(BrowserManager *manager, const std::map<std::string, std::string>& locator_parameters, const std::map<std::string, Json::Value>& command_parameters, WebDriverResponse * response) {
		// TODO: Check HRESULT values for errors.
		std::tr1::shared_ptr<BrowserWrapper> browser_wrapper;
		int status_code = manager->GetCurrentBrowser(&browser_wrapper);
		if (status_code != SUCCESS) {
			response->SetErrorResponse(status_code, "Unable to get browser");
			return;
		}
		HRESULT hr = browser_wrapper->browser()->Quit();
		response->SetResponse(SUCCESS, Json::Value::null);
	}
};

} // namespace webdriver

#endif // WEBDRIVER_IE_CLOSEWINDOWCOMMANDHANDLER_H_
