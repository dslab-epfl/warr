﻿using System;
using NMock2;
using NUnit.Framework;

namespace OpenQA.Selenium.Support.UI
{
    [TestFixture]
    public class WebDriverWaitTest
    {
        private const string SOME_STRING = "str";

        private static readonly TimeSpan ZERO_SECONDS = TimeSpan.FromSeconds(0);
        private static readonly TimeSpan ONE_SECONDS = TimeSpan.FromSeconds(1);
        private static readonly TimeSpan FIVE_SECONDS = TimeSpan.FromSeconds(5);

        private Mockery mocks;
        private bool executed;

        [SetUp]
        public void SetUp()
        {
            mocks = new Mockery();
            executed = false;
        }
        
        [Test]
        public void CanGetListOfOptions()
        {
            var condition = GetCondition(() => null, () => SOME_STRING);

            var wait = new WebDriverWait(new TickingClock(), null, FIVE_SECONDS, ZERO_SECONDS);
            Assert.AreEqual(SOME_STRING, wait.Until(condition));
            
            mocks.VerifyAllExpectationsHaveBeenMet();
        }

        [Test]
        public void WaitsUntilABooleanResultIsTrue()
        {
            var condition = GetCondition(() => false, () => true);

            var wait = new WebDriverWait(new TickingClock(), null, FIVE_SECONDS, ZERO_SECONDS);
            Assert.True(wait.Until(condition));
        }

        [Test]
        public void ThrowsForInvalidTypes()
        {
            var nullableBooleanCondition = GetCondition<bool?>(() => null, () => true);
            var intCondition = GetCondition(() => 1, () => 2);

            var wait = new WebDriverWait(new TickingClock(), null, FIVE_SECONDS, ZERO_SECONDS);
            
            Assert.Throws(typeof(ArgumentException), () => wait.Until(nullableBooleanCondition));
            Assert.Throws(typeof(ArgumentException), () => wait.Until(intCondition));
        }

        [Test]
        public void ThrowsAnExceptionIfTheTimerRunsOut()
        {
            var wait = new WebDriverWait(GetClock(), null, ONE_SECONDS, ZERO_SECONDS);

            Assert.Throws(typeof(TimeoutException), () => wait.Until(driver => false));
        }

        [Test]
        public void SilentlyCapturesNoSuchElementExceptions()
        {
            var element = mocks.NewMock<IWebElement>();
            var condition = GetCondition(() => { throw new NoSuchElementException(); }, () => element);

            var wait = new WebDriverWait(new TickingClock(), null, FIVE_SECONDS, ZERO_SECONDS);

            Assert.AreEqual(element, wait.Until(condition));
        }

        [Test]
        public void PassesWebDriverFromConstructorToExpectation()
        {
            var mockDriver = mocks.NewMock<IWebDriver>();
            Expect.Once.On(mockDriver).Method("GetWindowHandle").Will(Return.Value(SOME_STRING));

            Func<IWebDriver, string> condition = driver => driver.GetWindowHandle();

            var wait = new WebDriverWait(new TickingClock(), mockDriver, FIVE_SECONDS, ZERO_SECONDS);
            
            Assert.AreEqual(SOME_STRING, wait.Until(condition));
            
            mocks.VerifyAllExpectationsHaveBeenMet();
        }

        [Test]
        public void ChainsNoSuchElementExceptionWhenTimingOut()
        {
            var condition = GetCondition<string>(() => { throw new NoSuchElementException(); }, () => { throw new NoSuchElementException(); });
            
            var wait = new WebDriverWait(GetClock(), null, ONE_SECONDS, ZERO_SECONDS);

            try
            {
                wait.Until(condition);
                Assert.Fail("Expected TimeoutException to be thrown");
            }
            catch (TimeoutException e)
            {
                Assert.IsInstanceOf(typeof (NoSuchElementException), e.InnerException);
            }
        }

        private Func<IWebDriver, T> GetCondition<T>(Func<T> first, Func<T> second)
        {
            return driver =>
            {
                if (executed)
                {
                    return second();
                }
                executed = true;
                return first();
            };
        }
        
        private static IClock GetClock()
        {
            return new TickingClock(TimeSpan.FromMilliseconds(500));
        }
    }

    class TickingClock : IClock
    {
        private readonly TimeSpan increment;

        public TickingClock() : this(TimeSpan.FromSeconds(0))
        {
            
        }

        public TickingClock(TimeSpan increment)
        {
            this.increment = increment;
            Now = new DateTime(0);
        }

        public DateTime Now { get; private set; }

        public DateTime LaterBy(TimeSpan delay)
        {
            return Now + delay;
        }

        public bool IsNowBefore(DateTime then)
        {
            Now = Now + increment;
            return Now < then;
        }
    }
}