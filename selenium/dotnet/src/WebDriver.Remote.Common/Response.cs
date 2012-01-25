﻿using System.Globalization;
using Newtonsoft.Json;

namespace OpenQA.Selenium.Remote
{
    /// <summary>
    /// Handles reponses from the browser
    /// </summary>
    public class Response
    {
        private object responseValue;
        private string responseSessionId;
        private WebDriverResult responseStatus;

        /// <summary>
        /// Initializes a new instance of the Response class
        /// </summary>
        public Response()
        {
        }

        /// <summary>
        /// Initializes a new instance of the Response class
        /// </summary>
        /// <param name="sessionId">Session ID in use</param>
        public Response(SessionId sessionId)
        {
            if (sessionId != null)
            {
                this.responseSessionId = sessionId.ToString();
            }
        }

        /// <summary>
        /// Gets or sets the value from JSON.
        /// </summary>
        [JsonConverter(typeof(ResponseValueJsonConverter))]
        [JsonProperty("value")]
        public object Value
        {
            get { return this.responseValue; }
            set { this.responseValue = value; }
        }

        /// <summary>
        /// Gets or sets the session ID.
        /// </summary>
        [JsonProperty("sessionId")]
        public string SessionId
        {
            get { return this.responseSessionId; }
            set { this.responseSessionId = value; }
        }

        /// <summary>
        /// Gets or sets the status value of the response.
        /// </summary>
        [JsonProperty("status")]
        public WebDriverResult Status
        {
            get { return this.responseStatus; }
            set { this.responseStatus = value; }
        }

        /// <summary>
        /// Returns a new <see cref="Response"/> from a JSON-encoded string.
        /// </summary>
        /// <param name="value">The JSON string to deserialize into a <see cref="Response"/>.</param>
        /// <returns>A <see cref="Response"/> object described by the JSON string.</returns>
        public static Response FromJson(string value)
        {
            return JsonConvert.DeserializeObject<Response>(value);
        }

        /// <summary>
        /// Returns this object as a JSON-encoded string.
        /// </summary>
        /// <returns>A JSON-encoded string representing this <see cref="Response"/> object.</returns>
        public string ToJson()
        {
            return JsonConvert.SerializeObject(this, new CookieJsonConverter());
        }

        /// <summary>
        /// Returns the object as a string.
        /// </summary>
        /// <returns>A string with the Session ID, status value, and the value from JSON.</returns>
        public override string ToString()
        {
            return string.Format(CultureInfo.InvariantCulture, "({0} {1}: {2})", this.SessionId, this.Status, this.Value);
        }
    }
}
