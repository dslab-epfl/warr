#ifndef WEBDRIVER_IE_SCRIPTWRAPPER_H_
#define WEBDRIVER_IE_SCRIPTWRAPPER_H_

#include <string>
#include "json.h"

using namespace std;

namespace webdriver {

// Forward declaration of classes to avoid
// circular include files.
class BrowserWrapper;
class ElementWrapper;
class BrowserManager;

class ScriptWrapper
{
public:
	ScriptWrapper(IHTMLDocument2 *document, std::wstring script, unsigned long argument_count);
	~ScriptWrapper(void);

	std::wstring script() { return this->script_; }
	unsigned long argument_count() { return this->argument_count_; }
	SAFEARRAY* arguments() { return this->argument_array_; }
	VARIANT result() { return this->result_; }
	void set_result(VARIANT value) { ::VariantCopy(&this->result_, &value); }

	void AddArgument(const std::wstring& argument);
	void AddArgument(const int argument);
	void AddArgument(const double argument);
	void AddArgument(const bool argument);
	void AddArgument(std::tr1::shared_ptr<ElementWrapper> argument);
	void AddArgument(IHTMLElement *argument);
	void AddArgument(VARIANT argument);

	bool ResultIsEmpty(void);
	bool ResultIsString(void);
	bool ResultIsInteger(void);
	bool ResultIsBoolean(void);
	bool ResultIsDouble(void);
	bool ResultIsArray(void);
	bool ResultIsObject(void);
	bool ResultIsElement(void);
	bool ResultIsElementCollection(void);
	bool ResultIsIDispatch(void);

	int Execute(void);
	int ConvertResultToJsonValue(BrowserManager *manager, Json::Value *value);

private:
	int GetArrayLength(long *length);
	int GetArrayItem(BrowserManager *manager, long index, Json::Value *item);
	int GetPropertyNameList(std::wstring *property_names);
	int GetPropertyValue(BrowserManager *manager, const std::wstring& property_name, Json::Value *property_value);
	std::wstring GetResultObjectTypeName(void);
	bool CreateAnonymousFunction(VARIANT* result);

	CComPtr<IHTMLDocument2> script_engine_host_;
	unsigned long argument_count_;
	std::wstring script_;
	long current_arg_index_;
	SAFEARRAY *argument_array_;
	VARIANT result_;
};

} // namespace webdriver

#endif // WEBDRIVER_IE_SCRIPTWRAPPER_H_
