#ifndef WEBDRIVER_IE_DELETECOOKIECOMMANDHANDLER_H_
#define WEBDRIVER_IE_DELETECOOKIECOMMANDHANDLER_H_

#include "BrowserManager.h"

namespace webdriver {

class DeleteCookieCommandHandler : public WebDriverCommandHandler {
public:
	DeleteCookieCommandHandler(void) {
	}

	virtual ~DeleteCookieCommandHandler(void) {
	}

protected:
	void DeleteCookieCommandHandler::ExecuteInternal(BrowserManager *manager, const std::map<std::string, std::string>& locator_parameters, const std::map<std::string, Json::Value>& command_parameters, WebDriverResponse * response) {
		std::map<std::string, std::string>::const_iterator name_parameter_iterator = locator_parameters.find("name");
		if (name_parameter_iterator == locator_parameters.end()) {
			response->SetErrorResponse(400, "Missing parameter in URL: name");
			return;
		}

		std::wstring cookie_name(CA2W(name_parameter_iterator->second.c_str(), CP_UTF8));
		std::tr1::shared_ptr<BrowserWrapper> browser_wrapper;
		int status_code = manager->GetCurrentBrowser(&browser_wrapper);
		if (status_code != SUCCESS) {
			response->SetErrorResponse(status_code, "Unable to get browser");
			return;
		}
		status_code = browser_wrapper->DeleteCookie(cookie_name);
		if (status_code != SUCCESS) {
			response->SetErrorResponse(status_code, "Unable to delete cookie");
			return;
		}

		response->SetResponse(SUCCESS, Json::Value::null);
	}
};

} // namespace webdriver

#endif // WEBDRIVER_IE_DELETECOOKIECOMMANDHANDLER_H_
