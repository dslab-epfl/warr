#include "StdAfx.h"
#include "ScriptWrapper.h"
#include "BrowserManager.h"
#include "logging.h"

namespace webdriver {

ScriptWrapper::ScriptWrapper(IHTMLDocument2 *document, std::wstring script, unsigned long argument_count) {
	this->script_engine_host_ = document;
	this->script_ = script;
	this->argument_count_ = argument_count;
	this->current_arg_index_ = 0;

	SAFEARRAYBOUND argument_bounds;
	argument_bounds.lLbound = 0;
	argument_bounds.cElements = this->argument_count_;

	this->argument_array_ = ::SafeArrayCreate(VT_VARIANT, 1, &argument_bounds);

	::VariantInit(&this->result_);
}

ScriptWrapper::~ScriptWrapper(void) {
	::SafeArrayDestroy(this->argument_array_);
}

void ScriptWrapper::AddArgument(const std::wstring& argument) {
	CComVariant dest_argument(argument.c_str());
	this->AddArgument(dest_argument);
}

void ScriptWrapper::AddArgument(const int argument) {
	CComVariant dest_argument((long)argument);
	this->AddArgument(dest_argument);
}

void ScriptWrapper::AddArgument(const double argument) {
	CComVariant dest_argument(argument);
	this->AddArgument(dest_argument);
}

void ScriptWrapper::AddArgument(const bool argument) {
	CComVariant dest_argument(argument);
	this->AddArgument(dest_argument);
}

void ScriptWrapper::AddArgument(std::tr1::shared_ptr<ElementWrapper> argument) {
	this->AddArgument(argument->element());
}

void ScriptWrapper::AddArgument(IHTMLElement *argument) {
	CComVariant dest_argument(argument);
	this->AddArgument(dest_argument);
}

void ScriptWrapper::AddArgument(VARIANT argument) {
	::SafeArrayPutElement(this->argument_array_, &this->current_arg_index_, &argument);
	++this->current_arg_index_;
}

bool ScriptWrapper::ResultIsString() {
	return this->result_.vt == VT_BSTR;
}

bool ScriptWrapper::ResultIsInteger() {
	return this->result_.vt == VT_I4 || this->result_.vt == VT_I8;
}

bool ScriptWrapper::ResultIsDouble() {
	return this->result_.vt == VT_R4 || this->result_.vt == VT_R8;
}

bool ScriptWrapper::ResultIsBoolean() {
	return this->result_.vt == VT_BOOL;
}

bool ScriptWrapper::ResultIsEmpty() {
	return this->result_.vt == VT_EMPTY;
}

bool ScriptWrapper::ResultIsIDispatch() {
	return this->result_.vt == VT_DISPATCH;
}

bool ScriptWrapper::ResultIsElementCollection() {
	if (this->result_.vt == VT_DISPATCH) {
		CComQIPtr<IHTMLElementCollection> is_collection(this->result_.pdispVal);
		if (is_collection) {
			return true;
		}
	}
	return false;
}

bool ScriptWrapper::ResultIsElement() {
	if (this->result_.vt == VT_DISPATCH) {
		CComQIPtr<IHTMLElement> is_element(this->result_.pdispVal);
		if (is_element) {
			return true;
		}
	}
	return false;
}

bool ScriptWrapper::ResultIsArray() {
	std::wstring type_name = this->GetResultObjectTypeName();

	// If the name is DispStaticNodeList, we can be pretty sure it's an array
	// (or at least has array semantics). It is unclear to what extent checking
	// for DispStaticNodeList is supported behaviour.
	if (type_name == L"DispStaticNodeList") {
		return true;
	}

	// If the name is JScriptTypeInfo then this *may* be a Javascript array.
	// Note that strictly speaking, to determine if the result is *actually*
	// a JavaScript array object, we should also be testing to see if
	// propertyIsEnumerable('length') == false, but that does not find the
	// array-like objects returned by some of the calls we make to the Google
	// Closure library.
	// IMPORTANT: Using this script, user-defined objects with a length
	// property defined will be seen as arrays instead of objects.
	if (type_name == L"JScriptTypeInfo") {
		const std::wstring script = L"(function() { return function(){ return arguments[0] && arguments[0].hasOwnProperty('length') && typeof arguments[0] === 'object' && typeof arguments[0].length === 'number';};})();";
		ScriptWrapper is_array_wrapper(this->script_engine_host_, script, 1);
		is_array_wrapper.AddArgument(this->result_);
		is_array_wrapper.Execute();
		return is_array_wrapper.result().boolVal == VARIANT_TRUE;
	}

	return false;
}

bool ScriptWrapper::ResultIsObject() {
	std::wstring type_name = this->GetResultObjectTypeName();
	if (type_name == L"JScriptTypeInfo") {
		return true;
	}
	return false;
}

int ScriptWrapper::Execute() {
	VARIANT result;

	CComVariant temp_function;
	if (!this->CreateAnonymousFunction(&temp_function)) {
		// Debug level since this is normally the point we find out that 
		// a page refresh has occured. *sigh*
		// LOG(DEBUG) << "Cannot create anonymous function: " << _bstr_t(script) << endl;
		return EUNEXPECTEDJSERROR;
	}

	if (temp_function.vt != VT_DISPATCH) {
		// No return value that we care about
		::VariantClear(&result);
		result.vt = VT_EMPTY;
		return SUCCESS;
	}

	// Grab the "call" method out of the returned function
	DISPID call_member_id;
	OLECHAR FAR* call_member_name = L"call";
	HRESULT hr = temp_function.pdispVal->GetIDsOfNames(IID_NULL, &call_member_name, 1, LOCALE_USER_DEFAULT, &call_member_id);
	if (FAILED(hr)) {
//		LOGHR(DEBUG, hr) << "Cannot locate call method on anonymous function: " << _bstr_t(script) << endl;
		return EUNEXPECTEDJSERROR;
	}

	DISPPARAMS call_parameters = { 0 };
	memset(&call_parameters, 0, sizeof call_parameters);

	long lower = 0;
	::SafeArrayGetLBound(this->argument_array_, 1, &lower);
	long upper = 0;
	::SafeArrayGetUBound(this->argument_array_, 1, &upper);
	long nargs = 1 + upper - lower;
	call_parameters.cArgs = nargs + 1;

	CComPtr<IHTMLWindow2> win;
	hr = this->script_engine_host_->get_parentWindow(&win);
	if (FAILED(hr)) {
		LOGHR(WARN, hr) << "Cannot get parent window";
		return EUNEXPECTEDJSERROR;
	}
	_variant_t *vargs = new _variant_t[nargs + 1];
	::VariantCopy(&(vargs[nargs]), &CComVariant(win));

	long index;
	for (int i = 0; i < nargs; i++) {
		index = i;
		CComVariant v;
		::SafeArrayGetElement(this->argument_array_, &index, (void*) &v);
		::VariantCopy(&(vargs[nargs - 1 - i]), &v);
	}

	call_parameters.rgvarg = vargs;
	int return_code = SUCCESS;
	EXCEPINFO exception;
	memset(&exception, 0, sizeof exception);
	hr = temp_function.pdispVal->Invoke(call_member_id, IID_NULL, LOCALE_USER_DEFAULT, DISPATCH_METHOD, &call_parameters, 
		&result,
		&exception, 0);

	if (FAILED(hr)) {
		if (DISP_E_EXCEPTION == hr) {
			CComBSTR error_description(exception.bstrDescription ? exception.bstrDescription : L"EUNEXPECTEDJSERROR");
			LOG(INFO) << "Exception message was: " << error_description;
		} else {
			// LOGHR(DEBUG, hr) << "Failed to execute: " << _bstr_t(script);
		}

		::VariantClear(&result);
		result.vt = VT_USERDEFINED;
		if (exception.bstrDescription != NULL) {
			result.bstrVal = ::SysAllocStringByteLen((char*)exception.bstrDescription, ::SysStringByteLen(exception.bstrDescription));
		} else {
			result.bstrVal = ::SysAllocStringByteLen(NULL, 0);
		}
		return_code = EUNEXPECTEDJSERROR;
	}

	// If the script returned an IHTMLElement, we need to copy it to make it valid.
	if(VT_DISPATCH == result.vt) {
		CComQIPtr<IHTMLElement> element(result.pdispVal);
		if(element) {
			IHTMLElement* &dom_element = * (IHTMLElement**) &(result.pdispVal);
			element.CopyTo(&dom_element);
		}
	}

	this->result_ = result;

	delete[] vargs;

	return return_code;
}

int ScriptWrapper::ConvertResultToJsonValue(BrowserManager *manager, Json::Value *value) {
	int status_code = SUCCESS;
	if (this->ResultIsString()) { 
		std::string string_value("");
		if (this->result_.bstrVal) {
			string_value = CW2A(this->result_.bstrVal, CP_UTF8);
		}
		*value = string_value;
	} else if (this->ResultIsInteger()) {
		*value = this->result_.lVal;
	} else if (this->ResultIsDouble()) {
		*value = this->result_.dblVal;
	} else if (this->ResultIsBoolean()) {
		*value = this->result_.boolVal == VARIANT_TRUE;
	} else if (this->ResultIsEmpty()) {
		*value = Json::Value::null;
	} else if (this->ResultIsIDispatch()) {
		if (this->ResultIsArray() || this->ResultIsElementCollection()) {
			Json::Value result_array(Json::arrayValue);

			long length = 0;
			status_code = this->GetArrayLength(&length);

			for (long i = 0; i < length; ++i) {
				Json::Value array_item_result;
				int array_item_status = this->GetArrayItem(manager, i, &array_item_result);
				result_array[i] = array_item_result;
			}
			*value = result_array;
		} else if (this->ResultIsObject()) {
			Json::Value result_object;

			std::wstring property_name_list(L"");
			status_code = this->GetPropertyNameList(&property_name_list);

			std::vector<std::wstring> property_names;
			size_t end_position(0);
			size_t start_position(0);
			while (true) {
				std::wstring property_name(L"");
				end_position = property_name_list.find_first_of(L",", start_position);
				if(end_position == std::wstring::npos) {
					property_names.push_back(property_name_list.substr(start_position, property_name_list.size() - start_position));
					break;
				} else {
					property_names.push_back(property_name_list.substr(start_position, end_position - start_position));
					start_position = end_position + 1;
				}
			}

			for (size_t i = 0; i < property_names.size(); ++i) {
				Json::Value property_value_result;
				int property_value_status = this->GetPropertyValue(manager, property_names[i], &property_value_result);
				std::string name(CW2A(property_names[i].c_str(), CP_UTF8));
				result_object[name] = property_value_result;
			}
			*value = result_object;
		} else {
			IHTMLElement *node = (IHTMLElement*) this->result_.pdispVal;
			std::tr1::shared_ptr<ElementWrapper> element_wrapper;
			manager->AddManagedElement(node, &element_wrapper);
			*value = element_wrapper->ConvertToJson();
		}
	} else {
		status_code = EUNKNOWNSCRIPTRESULT;
	}
	return status_code;
}

std::wstring ScriptWrapper::GetResultObjectTypeName() {
	std::wstring name(L"");
	if (this->result_.vt == VT_DISPATCH) {
		CComPtr<ITypeInfo> typeinfo;
		HRESULT get_type_info_result = this->result_.pdispVal->GetTypeInfo(0, LOCALE_USER_DEFAULT, &typeinfo);
		TYPEATTR* type_attr;
		CComBSTR name_bstr;
		if (SUCCEEDED(get_type_info_result) && SUCCEEDED(typeinfo->GetTypeAttr(&type_attr))
			&& SUCCEEDED(typeinfo->GetDocumentation(-1, &name_bstr, 0, 0, 0))) {
			typeinfo->ReleaseTypeAttr(type_attr);
			name = name_bstr.Copy();
		}
	}
	return name;
}

int ScriptWrapper::GetPropertyNameList(std::wstring *property_names) {
	// Loop through the properties, appending the name of each one to the string.
	std::wstring get_names_script(L"(function(){return function() { var name_list = ''; for (var name in arguments[0]) { if (name_list.length > 0) name_list+= ','; name_list += name } return name_list;}})();");
	ScriptWrapper get_names_script_wrapper(this->script_engine_host_, get_names_script, 1);
	get_names_script_wrapper.AddArgument(this->result_);
	int get_names_result = get_names_script_wrapper.Execute();

	if (get_names_result != SUCCESS) {
		return get_names_result;
	}

	// Expect the return type to be an integer. A non-integer means this was
	// not an array after all.
	if (!get_names_script_wrapper.ResultIsString()) {
		return EUNEXPECTEDJSERROR;
	}

	*property_names = get_names_script_wrapper.result().bstrVal;
	return SUCCESS;
}

int ScriptWrapper::GetPropertyValue(BrowserManager *manager, const std::wstring& property_name, Json::Value *property_value){
	std::wstring get_value_script(L"(function(){return function() {return arguments[0][arguments[1]];}})();"); 
	ScriptWrapper get_value_script_wrapper(this->script_engine_host_, get_value_script, 2);
	get_value_script_wrapper.AddArgument(this->result_);
	get_value_script_wrapper.AddArgument(property_name);
	int get_value_result = get_value_script_wrapper.Execute();
	if (get_value_result != SUCCESS) {
		return get_value_result;
	}

	int property_value_status = get_value_script_wrapper.ConvertResultToJsonValue(manager, property_value);
	return SUCCESS;
}

int ScriptWrapper::GetArrayLength(long *length) {
	// Prepare an array for the Javascript execution, containing only one
	// element - the original returned array from a JS execution.
	std::wstring get_length_script(L"(function(){return function() {return arguments[0].length;}})();");
	ScriptWrapper get_length_script_wrapper(this->script_engine_host_, get_length_script, 1);
	get_length_script_wrapper.AddArgument(this->result_);
	int length_result = get_length_script_wrapper.Execute();

	if (length_result != SUCCESS) {
		return length_result;
	}

	// Expect the return type to be an integer. A non-integer means this was
	// not an array after all.
	if (!get_length_script_wrapper.ResultIsInteger()) {
		return EUNEXPECTEDJSERROR;
	}

	*length = get_length_script_wrapper.result().lVal;
	return SUCCESS;
}

int ScriptWrapper::GetArrayItem(BrowserManager *manager, long index, Json::Value *item){
	std::wstring get_array_item_script(L"(function(){return function() {return arguments[0][arguments[1]];}})();"); 
	ScriptWrapper get_array_item_script_wrapper(this->script_engine_host_, get_array_item_script, 2);
	get_array_item_script_wrapper.AddArgument(this->result_);
	get_array_item_script_wrapper.AddArgument(index);
	int get_item_result = get_array_item_script_wrapper.Execute();
	if (get_item_result != SUCCESS) {
		return get_item_result;
	}

	int array_item_status = get_array_item_script_wrapper.ConvertResultToJsonValue(manager, item);
	return SUCCESS;
}

bool ScriptWrapper::CreateAnonymousFunction(VARIANT *result) {
	CComBSTR function_eval_script(L"window.document.__webdriver_script_fn = ");
	function_eval_script.Append(this->script_.c_str());
	CComBSTR code(function_eval_script);
	CComBSTR lang(L"JScript");
	CComVariant exec_script_result;
	CComPtr<IHTMLWindow2> window;
	HRESULT hr = this->script_engine_host_->get_parentWindow(&window);
	if (FAILED(hr)) {
		return false;
	}

	hr = window->execScript(code, lang, &exec_script_result);
	if (FAILED(hr)) {
		return false;
	}

	OLECHAR FAR* function_object_name(L"__webdriver_script_fn");
	DISPID dispid_function_object;
	hr = this->script_engine_host_->GetIDsOfNames(IID_NULL, &function_object_name, 1, LOCALE_USER_DEFAULT, &dispid_function_object);
	if (FAILED(hr)) {
		return false;
	}

	// get the value of eval result
	DISPPARAMS no_args_dispatch_parameters = { NULL, NULL, 0, 0 };
	hr = this->script_engine_host_->Invoke(dispid_function_object, IID_NULL, LOCALE_USER_DEFAULT, DISPATCH_PROPERTYGET, &no_args_dispatch_parameters, result, NULL, NULL);
	if (FAILED(hr)) {
		return false;
	}

	return true;
}
} // namespace webdriver