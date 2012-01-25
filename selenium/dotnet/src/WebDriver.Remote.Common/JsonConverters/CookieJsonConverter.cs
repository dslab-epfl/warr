﻿using System;
using System.Globalization;
using Newtonsoft.Json;

namespace OpenQA.Selenium.Remote
{
    /// <summary>
    /// Provides a way to convert Cookies to JSON and back
    /// </summary>
    internal class CookieJsonConverter : JsonConverter
    {
        /// <summary>
        /// Checks if the object can be converted
        /// </summary>
        /// <param name="objectType">Type of the object</param>
        /// <returns>A value indicating if it can be converted</returns>
        public override bool CanConvert(Type objectType)
        {
            return objectType.IsAssignableFrom(typeof(Cookie));
        }

        /// <summary>
        /// Get the platform from the JSON reader
        /// </summary>
        /// <param name="reader">JSON Reader instance</param>
        /// <param name="objectType">Object type being read</param>
        /// <param name="existingValue">The exisiting value of the object</param>
        /// <param name="serializer">JSON Serializer instance</param>
        /// <returns>Platform from JSON reader</returns>
        public override object ReadJson(JsonReader reader, Type objectType, object existingValue, JsonSerializer serializer)
        {
            Platform platformValue = null;
            if (reader.TokenType == JsonToken.String)
            {
                PlatformType platformTypeValue = (PlatformType)Enum.Parse(objectType, reader.Value.ToString(), true);
                platformValue = new Platform(platformTypeValue);
            }

            return platformValue;
        }

        /// <summary>
        /// Created a cookie from the JSON string
        /// </summary>
        /// <param name="writer">The JSON writer with a string</param>
        /// <param name="value">Value of the string</param>
        /// <param name="serializer">JSON serializer instance</param>
        public override void WriteJson(JsonWriter writer, object value, JsonSerializer serializer)
        {
            Cookie cookieValue = value as Cookie;
            if (cookieValue != null)
            {
                writer.WriteStartObject();
                writer.WritePropertyName("name");
                writer.WriteValue(cookieValue.Name);
                writer.WritePropertyName("value");
                writer.WriteValue(cookieValue.Value);
                writer.WritePropertyName("path");
                if (!string.IsNullOrEmpty(cookieValue.Path))
                {
                    writer.WriteValue(cookieValue.Path);
                }
                else
                {
                    writer.WriteValue(string.Empty);
                }

                writer.WritePropertyName("domain");
                if (!string.IsNullOrEmpty(cookieValue.Domain))
                {
                    writer.WriteValue(cookieValue.Domain);
                }
                else
                {
                    writer.WriteValue(string.Empty);
                }

                if (cookieValue.Expiry != null)
                {
                    writer.WritePropertyName("expiry");
                    DateTime zeroDate = new DateTime(1970, 1, 1, 0, 0, 0, DateTimeKind.Utc);
                    TimeSpan span = cookieValue.Expiry.Value.ToUniversalTime().Subtract(zeroDate);
                    writer.WriteValue(span.TotalSeconds);
                }

                writer.WritePropertyName("secure");
                writer.WriteValue(cookieValue.Secure);
                writer.WriteEndObject();
            }
        }
    }
}
